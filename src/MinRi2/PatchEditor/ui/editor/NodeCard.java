package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.export.*;
import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.EditorNode.*;
import MinRi2.PatchEditor.node.modifier.*;
import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.ui.editor.NodeCategorizer.*;
import MinRi2.PatchEditor.utils.*;
import arc.*;
import arc.graphics.*;
import arc.scene.actions.*;
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
    public static float buttonWidth = 330f;
    public static float buttonHeight = buttonWidth / 3f;

    private final Table cardCont, nodesTable; // workingTable / childrenNodesTable
    public boolean editing;
    public NodeCard parent, childCard;

    private final NodeManager manager;
    private EditorNode rootEditorNode;

    private String editorPath, lastEditorPath;

    private String searchText = "";

    private boolean needRebuildNodes;

    public NodeCard(NodeManager manager){
        this.manager = manager;

        cardCont = new Table();
        nodesTable = new Table();

        top().left();
        cardCont.top();
        nodesTable.top().left();

        manager.onChanged((op, node, uiUpdated) -> {
            if(editorPath == null) return;
            if(!op.path.startsWith(editorPath) || uiUpdated) return;

            needRebuildNodes = true;
        });
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(!editing && needRebuildNodes){
            rebuildNodesTable();
        }
    }

    public void setRootEditorNode(EditorNode rootEditorNode){
        this.rootEditorNode = rootEditorNode;
        if(childCard != null) childCard.setRootEditorNode(rootEditorNode);
    }

    public void setEditPath(String path){
        editorPath = path;
    }

    public NodeCard getFrontCard(){
        NodeCard card = this;

        while(card.editing && card.childCard != null){
            card = card.childCard;
        }

        return card;
    }

    private void editChildNode(String path){
        if(childCard == null){
            childCard = new NodeCard(manager);
            childCard.setRootEditorNode(rootEditorNode);
            childCard.parent = this;
        }else if(childCard.editing){
            childCard.editChildNode(null);
        }

        EditorNode editorNode = rootEditorNode.navigate(path);
        if(editorNode != null && !editorNode.isEditable()){
            editing = false;
        }else{
            editing = path != null;
            childCard.setEditPath(path);
        }

        rebuildCont();
    }

    public void extractWorking(){
        if(parent != null){
            parent.lastEditorPath = editorPath;
            parent.editChildNode(null);
        }
    }

    public void editLastData(){
        if((childCard == null || !childCard.editing) && lastEditorPath != null && lastEditorPath.startsWith(editorPath)){
            editChildNode(lastEditorPath);
        }
    }

    public void rebuild(){
        clearChildren();

        EditorNode editorNode = getEditorNode();
        if(editorNode == null){
            editChildNode(null);
            return;
        }

        defaults().growX();
        buildTitle(this);
        row();
        rebuildCont();
        add(cardCont).grow();
    }

    public EditorNode getEditorNode(){
        return editorPath == null ? null : rootEditorNode.navigate(editorPath);
    }

    private void rebuildCont(){
        cardCont.clearChildren();

        cardCont.defaults().padLeft(16f);

        if(editing){
            childCard.rebuild();
            cardCont.add(childCard).grow();
            nodesTable.clear();
        }else{
            cardCont.table(this::setupSearchTable).pad(8f).growX();
            cardCont.row();
            cardCont.add(nodesTable).fill();

            // After layout assigned size
            Core.app.post(this::rebuildNodesTable);
        }
    }

    private void setupSearchTable(Table table){
        table.image(Icon.zoom).size(64f);

        TextField field = table.add(EUI.deboundTextField(searchText, text -> {
            searchText = text;
            rebuildNodesTable();
        })).pad(8f).growX().get();

        if(Core.app.isDesktop()){
            Core.scene.setKeyboardFocus(field);
        }

        table.button(Icon.cancel, Styles.clearNonei, () -> {
            searchText = "";
            field.setText(searchText);
            rebuildNodesTable();
        }).size(64f);
    }

    private void rebuildNodesTable(){
        nodesTable.clearChildren();
        needRebuildNodes = false;

        EditorNode editorNode = getEditorNode();
        if(editorNode == null){
            // 已被清除，回退编辑但不记忆上次编辑
            Core.app.post(() -> parent.editChildNode(null));
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

            int index = 0;
            for(EditorNode child : category.nodes){
                if(!searchText.isEmpty()){
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
            t.table(infoTable -> {
                infoTable.left();
                NodeDisplay.displayNameType(infoTable, node);
            }).pad(8f).fill();

            t.table(modifier::build).pad(4).grow();
            t.table(btn -> setupChildNodeButtons(btn, node, true)).pad(6f).growY();
            addFavoriteButton(t, node);

            t.image().width(4f).color(Color.darkGray).growY().right();
            t.row();
            Cell<?> horizontalLine = t.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(t.getColumns());

            t.background(Tex.whiteui);
            t.setColor(modifier.isModified() ? modifiedColor : unmodifiedColor);

            modifier.onModified(modified -> {
                t.addAction(Actions.color(modified ? modifiedColor : unmodifiedColor, 0.2f));
            });
        });
    }

    private static boolean isWarning(EditorNode node){
        if(node.isRequired()) return true;
        if(node.getObjNode().getParent() != null) return node.getObjNode().isDescendantArray();
        return false;
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
            b.table(infoTable -> {
                infoTable.left();
                NodeDisplay.display(infoTable, node);
            }).pad(8f).grow();

            b.table(buttons -> setupChildNodeButtons(buttons, node, false)).pad(6f).growY();
            addFavoriteButton(b, node);

            b.image().width(4f).color(Color.darkGray).growY().right();
            b.row();
            Cell<?> horizontalLine = b.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(b.getColumns());
        }, style, () -> {}).disabled(node.isRemoving() || (node.getObject() == null && !node.isOverriding())).get();

        EUI.backButtonClick(btn, () -> editChildNode(node.getPath()));
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
                boolean overriding = editorNode.isOverriding() || editorNode.isParentOverriding();
                editorNode.append(editorNode.getObject() != null && !overriding && !editorNode.isAppended());
                if(overriding){
                    editorNode.setValueType(ValueType.array);
                }
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

    private void setupChildNodeButtons(Table table, EditorNode child, boolean hasModifier){
        table.defaults().width(32f).pad(4f).growY();
        EditorNode editorNode = getEditorNode();

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

        if(child.isRequired()){
            table.image(Icon.infoCircle).height(32f).tooltip("@node.mayRequired");
        }
    }

    private void addFavoriteButton(Table table, EditorNode node){
        if(!FavoriteFields.canFavorite(node)) return;

        table.addChild(new Table(t -> {
            t.right().bottom().marginBottom(8f).marginRight(8f);
            t.setFillParent(true);

            String tooltip = Vars.mobile ?
            Core.bundle.get("node.favorite.toggle.mobile", "Double tap to toggle favorite") :
            Core.bundle.get("node.favorite.toggle");

            t.button(Icon.starSmall, EStyles.favoriteButton, () -> {
                FavoriteFields.toggle(node);
            }).size(Vars.iconMed).tooltip(tooltip).checked(FavoriteFields.isFavorite(node));
        }));
    }

    private void buildTitle(Table table){
        EditorNode editorNode = getEditorNode();

        Color titleColor = parent == null ? EPalettes.main2 : EPalettes.main3;
        table.table(Tex.whiteui, nodeTitle -> {
            nodeTitle.defaults().pad(8f);

            nodeTitle.table(Tex.whiteui, nameTable -> {
                nameTable.table(t -> {
                    t.left();
                    NodeDisplay.display(t, editorNode);
                }).pad(8f).grow();

                nameTable.image().width(4f).color(Color.darkGray).growY().right();
                nameTable.row();
                Cell<?> horizontalLine = nameTable.image().height(4f).color(Color.darkGray).growX();
                horizontalLine.colspan(nameTable.getColumns());
            }).color(Pal.darkestGray).size(buttonWidth, buttonHeight);

            nodeTitle.table(buttons -> {
                buttons.defaults().size(64f).pad(8f);

                // Clear data
                buttons.button(Icon.refresh, Styles.cleari, () -> {
                    Vars.ui.showConfirm(Core.bundle.format("node-card.clear-data.confirm", editorNode.getPath()), editorNode::clearJson);
                }).tooltip("@node-card.clear-data", true);

                if(editorNode != rootEditorNode){
                    buttons.button(Icon.download, Styles.cleari, () -> {
                        try{
                            editorNode.importPatch(Core.app.getClipboardText());
                        }catch(RuntimeException e){
                            Vars.ui.showException("@node-card.appendPatchNode.failed", e);
                            return;
                        }
                        EUI.infoToast(Core.bundle.format("node-card.appendPatchNode", editorPath));
                    }).padLeft(16f).tooltip(Core.bundle.format("node-card.appendPatchNode", editorPath), true)
                    .disabled(b -> Core.app.getClipboardText() == null);

                    buttons.button(Icon.copy, Styles.cleari, () -> {
                        PatchNode patchNode = editorNode.getPatch();
                        Core.app.setClipboardText(patchNode == null ? "" : PatchJsonIO.toPatch(editorNode.getObjNode(), patchNode));
                        EUI.infoToast(Core.bundle.format("node-card.exportPatchNode", editorPath));
                    }).tooltip(Core.bundle.format("node-card.exportPatchNode", editorPath), true);

                    buttons.button(Icon.effect, Styles.cleari, () -> {
                        String patch = PatchExporter.export(editorNode.getMetaNode(), EditorSettings.getExportConfig());
                        Core.app.setClipboardText(patch);
                        EUI.infoToast(Core.bundle.format("node-card.magicExportNode", editorPath));
                    }).padLeft(16f).tooltip(Core.bundle.format("node-card.magicExportNode.tooltip", editorPath), true);
                }
            });

            nodeTitle.table(cardButtons -> {
                cardButtons.defaults().size(64f).pad(8f);

                if(parent != null){
                    cardButtons.button(Icon.upOpen, Styles.cleari, this::extractWorking).tooltip("@node-card.extract", false);
                }
            }).expandX().right().growY();
        }).color(titleColor);
    }

    @Override
    public String toString(){
        return "NodeCard{" +
        "nodeData=" + editorPath +
        '}';
    }
}
