package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.export.*;
import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.EditorNode.*;
import MinRi2.PatchEditor.node.modifier.*;
import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.ui.NodeCategorizer.*;
import MinRi2.PatchEditor.ui.dialog.*;
import MinRi2.PatchEditor.utils.*;
import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.JsonValue.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

/**
 * @author minri2
 * Create by 2024/2/16
 */
public class NodeCard extends Table{
    public static final float noteWidth = 480f;
    public static float buttonWidth = 330f;
    public static float buttonHeight = buttonWidth / 3f;

    private final Table nodesTable; // workingTable / childrenNodesTable

    private final NodeManager manager;
    private EditorNode rootEditorNode;

    private String editorPath = "", parentPath = "";
    private int depth;

    private final ObjectMap<String, String> lastEditMap = new ObjectMap<>();
    private String[] searchTextMap = new String[8];

    private boolean needRebuildNodes;

    public NodeCard(NodeManager manager){
        this.manager = manager;

        nodesTable = new Table();

        top().left();
        nodesTable.top().left();

        manager.onChanged((op, node, uiUpdated) -> {
            if(!isRelatedPath(op.path, editorPath) || uiUpdated) return;
            needRebuildNodes = true;
        });
    }

    private static Tooltip getNoteTooltip(String note, boolean allowMobile){
        Tooltip tooltip = new Tooltip(t -> {
            t.margin(12f).background(Styles.black8);
            t.labelWrap(note).width(noteWidth).style(Styles.outlineLabel);
        });
        tooltip.allowMobile = allowMobile;

        return tooltip;
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(needRebuildNodes){
            rebuildNodesTable();
        }
    }

    public void setRootEditorNode(EditorNode rootEditorNode){
        this.rootEditorNode = rootEditorNode;
    }

    public void clean(){
        lastEditMap.clear();
        clearChildren();
    }

    public void setEditPath(String path){
        EditorNode node = rootEditorNode.navigate(path);
        if(node == null || (node != rootEditorNode && !node.isEditable())){
            return;
        }

        lastEditMap.put(parentPath, editorPath);

        editorPath = path == null ? "" : path;

        int dot = editorPath.lastIndexOf(NodeManager.pathComp);
        parentPath = dot == -1 ? "" : editorPath.substring(0, dot);

        rebuild();
    }

    public void extract(){
        if(!editorPath.isEmpty()) setEditPath(parentPath);
    }

    public void editLastData(){
        String lastPath = lastEditMap.get(editorPath);
        if(lastPath != null && rootEditorNode.navigate(lastPath) != null){
            setEditPath(lastPath);
        }
    }

    public void rebuild(){
        clearChildren();

        Table currentCont = table().growX().get();
        Seq<EditorNode> nodes = rootEditorNode.navigateThrough(editorPath, new Seq<>());

        depth = nodes.size - 1;
        if(depth + 1 > searchTextMap.length){
            String[] newMap = new String[depth + 1];
            System.arraycopy(searchTextMap, 0, newMap, 0, searchTextMap.length);
            searchTextMap = newMap;
        }

        for(EditorNode node : nodes){
            currentCont = currentCont.table(cont -> {
                cont.table(Tex.whiteui, title -> {
                    buildTitle(title, node);
                }).color(node == rootEditorNode ? EPalettes.main2 : EPalettes.main3).growX();
                cont.row();
            }).padLeft(16f).growX().get();
        }

        currentCont.table(this::setupSearchTable).pad(8f).growX();
        currentCont.row();
        currentCont.add(nodesTable).grow();

        // After layout assigned size
        Core.app.post(this::rebuildNodesTable);
    }

    public EditorNode getEditorNode(){
        return rootEditorNode.navigate(editorPath);
    }

    private void setupSearchTable(Table table){
        table.image(Icon.zoom).size(64f);

        TextField field = table.add(EUI.deboundTextField(searchTextMap[depth], text -> {
            searchTextMap[depth] = text;
            rebuildNodesTable();
        })).pad(8f).growX().get();

        if(Core.app.isDesktop()){
            Core.scene.setKeyboardFocus(field);
        }

        table.button(Icon.cancel, Styles.clearNonei, () -> {
            searchTextMap[depth] = "";
            field.setText("");
            rebuildNodesTable();
        }).size(64f);
    }

    private void rebuildNodesTable(){
        nodesTable.clearChildren();
        needRebuildNodes = false;

        EditorNode editorNode = getEditorNode();
        if(editorNode == null){
            setEditPath(parentPath);
            return;
        }

        editorNode.sync();

        int columns = Math.max(1, (int)(nodesTable.getWidth() / Scl.scl() / buttonWidth));
        Seq<NodeCategory> seq = NodeCategorizer.categorizedNode(editorNode);
        for(NodeCategory category : seq){
            if(category.nodes.isEmpty() && !category.isOther) continue;

            nodesTable.table(t -> {
                t.image().color(Pal.darkerGray).size(32f, 6f);
                t.add(Strings.capitalize(category.name)).color(EPalettes.type).padLeft(16f).padRight(16f).left();
                t.image().color(Pal.darkerGray).height(4f).growX();
            }).marginTop(16f).marginBottom(8f).growX();
            nodesTable.row();
            Table cont = nodesTable.table().left().get();
            nodesTable.row();

            cont.defaults().size(buttonWidth, buttonWidth / 4).pad(4f).margin(8f).top().left();

            String searchText = searchTextMap[depth];
            int index = 0;
            for(EditorNode child : category.nodes){
                if(searchText != null && !searchText.isEmpty()){
                    String displayName = NodeDisplay.getDisplayName(child.getDisplayValue());

                    if(!Strings.matches(searchText, child.getObjNode().name)
                    && (displayName == null || !Strings.matches(searchText, displayName))){
                        continue;
                    }
                }

                if(child instanceof InvalidEditorNode){
                    addUnknownButton(cont, child);
                }else{
                    DataModifier<?> modifier = NodeModifier.getModifier(child.getObjNode());
                    if(modifier != null){
                        modifier.setData(rootEditorNode, child.getPath());
                        addEditTable(cont, child, modifier);
                    }else{
                        addChildButton(cont, child);
                    }
                }

                if(++index % columns == 0){
                    cont.row();
                }
            }

            if(category.isOther){
                if(editorNode.getObjNode().hasSign(ModifierSign.PLUS) && editorNode.getObjNode().elementType != null){
                    addPlusButton(cont, editorNode);
                }
            }
        }

        seq.clear();
    }

    private void addEditTable(Table table, EditorNode node, DataModifier<?> modifier){
        Color modifiedColor = EPalettes.modified, unmodifiedColor0 = EPalettes.unmodified;
        if(isWarning(node)) unmodifiedColor0 = EPalettes.warn;
        else if(node.isAppended()) unmodifiedColor0 = EPalettes.add;

        Color unmodifiedColor = unmodifiedColor0;

        table.table(t -> {
            if(Vars.mobile) addNoteHolder(t, node);

            t.table(infoTable -> {
                infoTable.left();
                NodeDisplay.displayNameType(infoTable, node);

                final String path = node.getPath();
                infoTable.clicked(KeyCode.mouseMiddle, () -> {
                    Core.app.setClipboardText(path);
                    EUI.infoToast(path);
                });
            }).pad(8f).fill();

            t.table(modifier::build).pad(4).grow();
            t.table(buttons -> {
                buttons.defaults().growX();
                buttons.table(top -> setupChildNodeButtons(top, node, modifier)).grow();
                buttons.row();
                buttons.table(bottom -> setupTinyButton(bottom, node)).pad(0f);
            }).pad(4f).growY();

            t.image().width(4f).color(Color.darkGray).growY().right();
            t.row();
            Cell<?> horizontalLine = t.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(t.getColumns());

            t.background(Tex.whiteui);
            final boolean[] lastPatched = {node.hasValue()};
            t.setColor(lastPatched[0] ? modifiedColor : unmodifiedColor);
            t.update(() -> {
                boolean patched = node.hasValue();
                if(patched == lastPatched[0]) return;
                lastPatched[0] = patched;
                t.clearActions();
                t.addAction(Actions.color(patched ? modifiedColor : unmodifiedColor, 0.2f));
            });
        });
    }

    private static boolean isRelatedPath(String a, String b){
        if(a == null || b == null) return false;
        return isSameOrChildPath(a, b) || isSameOrChildPath(b, a);
    }

    private static boolean isSameOrChildPath(String path, String ancestor){
        if(ancestor.isEmpty()) return true;
        return path.equals(ancestor) || path.startsWith(ancestor + NodeManager.pathComp);
    }

    private static boolean isWarning(EditorNode node){
        return node.isRequired();
    }

    private void addChildButton(Table table, EditorNode node){
        ImageButtonStyle style = EStyles.cardButtoni;
        if(isWarning(node)){
            style = EStyles.cardWarni;
        }else if(node.isAppended()){
            style = EStyles.addButtoni;
        }else if(node.isRemoving()){
            style = EStyles.cardRemovedi;
        }else if(node.hasValue()){
            style = EStyles.cardModifiedButtoni;
        }

        Button btn = table.button(b -> {
            addNoteHolder(b, node);

            b.table(infoTable -> {
                infoTable.left();
                NodeDisplay.display(infoTable, node);

                final String path = node.getPath();
                infoTable.clicked(KeyCode.mouseMiddle, () -> {
                    Core.app.setClipboardText(path);
                    EUI.infoToast(path);
                });
            }).pad(8f).grow();

            b.table(buttons -> {
                buttons.defaults().growX().pad(4f);
                buttons.table(top -> setupChildNodeButtons(top, node, null)).grow();
                buttons.row();
                buttons.table(bottom -> setupTinyButton(bottom, node)).pad(0f);
            }).pad(4f).growY();

            b.image().width(4f).color(Color.darkGray).growY().right();
            b.row();
            Cell<?> horizontalLine = b.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(b.getColumns());
        }, style, () -> {}).disabled(node.isRemoving() || (node.getObject() == null && !node.isOverriding())).get();

        EUI.backButtonClick(btn, () -> setEditPath(node.getPath()));
    }

    private void addPlusButton(Table table, EditorNode editorNode){
        ObjectNode objNode = editorNode.getObjNode();
        if(objNode.elementType == null) return;

        table.button(b -> {
            b.image(Icon.add).pad(8f).padRight(16f);

            b.add(ClassHelper.getDisplayName(objNode.elementType)).color(EPalettes.type)
            .style(Styles.outlineLabel).ellipsis(true).fillX();

            b.image().width(4f).color(Color.darkGray).growY().right();
            b.row();
            Cell<?> horizontalLine = b.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(b.getColumns());
        }, EStyles.addButtoni, () -> {
            Class<?> keyType = objNode.keyType;
            if(keyType != null){
                ContentType type = PatchJsonIO.classContentType(keyType);
                if(type == null){
                    // TODO: unsupported key type
                    Vars.ui.showErrorMessage("#Unsupported key type " + ClassHelper.getDisplayName(keyType));
                    return;
                }

                // ugly
                if(ClassHelper.isMap(editorNode.getTypeIn())){
                    EUI.selector.select(type, c -> !MapLike.contains(editorNode.getObject(), c), c -> {
                        editorNode.touch(PatchJsonIO.getKeyName(c), null, ModifierSign.PLUS);
                        return true;
                    });
                }
            }else{
                boolean append = editorNode.canAppend();
                editorNode.append(append);
                editorNode.setValueType(append ? ValueType.object : ValueType.array);
            }
        });
    }

    private void addUnknownButton(Table table, EditorNode node){
        table.table(Tex.whiteui, t -> {
            t.table(infoTable -> {
                infoTable.left();
                NodeDisplay.display(infoTable, node);
            }).pad(8f).grow();

            t.table(buttons -> {
                buttons.defaults().width(32f).pad(4f).growY();
                buttons.button(Icon.cancel, Styles.clearNonei, node::clearJson).grow().tooltip("@node.remove");
                buttons.image(Icon.infoCircle).height(32f).tooltip("@node.unknown.warn", true);
            }).pad(6f).growY();

            t.image().width(4f).color(Color.darkGray).growY().right();
            t.row();
            Cell<?> horizontalLine = t.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(t.getColumns());
        }).color(Pal.darkerGray);
    }

    private void setupChildNodeButtons(Table table, EditorNode child, DataModifier<?> modifier){
        table.defaults().padLeft(4f).padRight(4f).grow();
        EditorNode editorNode = getEditorNode();

        boolean hasModifier = modifier != null;

        // remove
        if(child.getObjNode().hasSign(ModifierSign.REMOVE) && !child.isChangedType() &&  !child.isAppended()){
            boolean undoMode = child.isRemoving();
            table.button(undoMode ? Icon.undo : Icon.cancel, Styles.clearNoneTogglei, () -> {
                if(undoMode){
                    child.clearJson();
                }else{
                    child.setRemoved();
                }
            }).tooltip(undoMode ? "@node.revertRemove" : "@node.removeKey");

            // This key has been removed. Don't show any buttons or hint.
            if(undoMode) return;
        }

        if(child.isAppended()){
            if(!hasModifier && PatchJsonIO.typeOverrideable(child.getTypeIn())){
                table.button(Icon.wrench, Styles.clearNonei, () -> {
                    EUI.classSelector.select(null, child.getTypeIn(), clazz -> {
                        child.changeType(clazz);
                        return true;
                    });
                }).tooltip("@node.changeType");
            }

            table.button(Icon.cancel, Styles.clearNonei, child::clearJson).grow().tooltip("@node.remove");
        }else if(!hasModifier && child.isOverriding()){
            // overriding null object, array, map or field
            if(PatchJsonIO.typeOverrideable(child.getTypeIn())){
                table.button(Icon.wrench, Styles.clearNonei, () -> {
                    EUI.classSelector.select(null, child.getTypeIn(), clazz -> {
                        child.changeType(clazz);
                        return true;
                    });
                }).tooltip("@node.changeType");
            }

            table.button(Icon.undo, Styles.clearNonei, child::clearJson).tooltip("@node.revertOverride");
        }else if(!hasModifier && PatchJsonIO.overrideable(child.getTypeIn()) &&
        (child.getObject() == null || child.getObjNode().field != null || editorNode.getObjNode().isMultiArrayLike())){
            // override null object, field or element of multi-dimension array
            PatchNode patchNode = child.getPatch();
            if(patchNode == null || patchNode.sign == null){
                table.button(Icon.wrench, Styles.clearNonei, () -> {
                    child.setSign(ModifierSign.MODIFY);
                }).tooltip("@node.override");
            }
        }else if(!hasModifier && ClassHelper.isContainer(editorNode.getTypeIn())
        && PatchJsonIO.typeOverrideable(child.getTypeIn())){
            // override array's element or map's key must change the type
            table.button(Icon.wrench, Styles.clearNonei, () -> {
                EUI.classSelector.select(null, child.getTypeIn(), clazz -> {
                    child.changeType(clazz);
                    return true;
                });
            }).tooltip("@node.changeType");
        }

        if(hasModifier && !child.isAppended()){
            table.button(Icon.undo, Styles.clearNonei, () -> {
                modifier.resetModify();
                modifier.syncUI();
            }).tooltip("@node-modifier.undo", true).grow().visible(child.hasValue()).visible(child::hasValue).get();
        }
    }

    private void setupTinyButton(Table table, EditorNode node){
        table.right().defaults().size(Vars.iconSmall);

        if(node.isRequired()){
            table.image(Icon.infoCircleSmall).tooltip("@node.mayRequired", true).scaling(Scaling.stretch);
        }

        if(Core.settings.getBool("patch-editor.editNotes") && FieldNotes.canNote(node)){
            table.button(Icon.editSmall, EStyles.noteButton, () -> {
                EUI.noteEditor.show(node.getFieldId());
            }).color(EPalettes.gray).checked(b -> FieldNotes.getNote(node.getFieldId()) != null)
            .tooltip("@patch-editor.note.edit");
        }else{
            String note = FieldNotes.getNote(node.getFieldId());
            if(note != null){
                table.image(Icon.bookOpenSmall).color(EPalettes.lighterGray).size(Vars.iconSmall * 0.85f)
                .get().addListener(getNoteTooltip(note, true));
            }
        }

        if(FieldFavorites.canFavorite(node)){
            table.button(Icon.starSmall, EStyles.favoriteButton, () -> {
                FieldFavorites.toggle(node);
            }).tooltip(Core.bundle.get("node.favorite.toggle")).checked(FieldFavorites.isFavorite(node));
        }
    }

    private void addNoteHolder(Table table, EditorNode node){
        String note = FieldNotes.getNote(node.getFieldId());
        if(note != null){
            Element holder = new Element(){{
                fillParent = true;
                touchable = Touchable.enabled;
                addListener(getNoteTooltip(note, true));
            }};
            table.addChild(holder);
            holder.toBack();
        }
    }

    private void buildTitle(Table table, EditorNode node){
        table.defaults().pad(8f);

        table.table(Tex.whiteui, nameTable -> {
            nameTable.table(t -> {
                t.left();
                NodeDisplay.display(t, node);
            }).pad(8f).grow();

            nameTable.image().width(4f).color(Color.darkGray).growY().right();
            nameTable.row();
            Cell<?> horizontalLine = nameTable.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(nameTable.getColumns());
        }).color(Pal.darkestGray).size(buttonWidth, buttonHeight);

        table.table(buttons -> {
            buttons.defaults().size(64f).pad(8f);

            // Clear data
            buttons.button(Icon.refresh, Styles.cleari, () -> {
                Vars.ui.showConfirm(Core.bundle.format("node-card.clear-data.confirm", node.getPath()), node::clearJson);
            }).tooltip("@node-card.clear-data", true);

            if(node != rootEditorNode){
                buttons.button(Icon.download, Styles.cleari, () -> {
                    try{
                        node.importPatch(Core.app.getClipboardText());
                    }catch(RuntimeException e){
                        Vars.ui.showException("@node-card.appendPatchNode.failed", e);
                        return;
                    }
                    EUI.infoToast(Core.bundle.format("node-card.appendPatchNode", editorPath));
                }).padLeft(16f).tooltip(Core.bundle.format("node-card.appendPatchNode", editorPath), true)
                .disabled(b -> Core.app.getClipboardText() == null);

                buttons.button(Icon.copy, Styles.cleari, () -> {
                    PatchNode patchNode = node.getPatch();
                    Core.app.setClipboardText(patchNode == null ? "" : PatchJsonIO.toPatch(node.getObjNode(), patchNode, EditorSettings.getPatchExportOptions()));
                    EUI.infoToast(Core.bundle.format("node-card.exportPatchNode", editorPath));
                }).tooltip(Core.bundle.format("node-card.exportPatchNode", editorPath), true);

                buttons.button(Icon.effect, Styles.cleari, () -> {
                    String patch = PatchExporter.export(node.getMetaNode(), EditorSettings.getExportConfig(), EditorSettings.getPatchExportOptions());
                    Core.app.setClipboardText(patch);
                    EUI.infoToast(Core.bundle.format("node-card.magicExportNode", editorPath));
                }).padLeft(16f).tooltip(Core.bundle.format("node-card.magicExportNode.tooltip", editorPath), true);
            }
        });

        table.table(cardButtons -> {
            cardButtons.defaults().size(64f).pad(8f);

            if(node != rootEditorNode){
                String parentPath = node.parentPath();
                cardButtons.button(Icon.upOpen, Styles.cleari, () -> {
                    setEditPath(parentPath);
                }).tooltip("@node-card.extract", false);
            }
        }).expandX().right().growY();
    }

    @Override
    public String toString(){
        return "NodeCard{" +
        "nodeData=" + editorPath +
        '}';
    }
}
