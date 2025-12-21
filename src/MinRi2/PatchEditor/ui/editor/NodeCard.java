package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.EditorNode.*;
import MinRi2.PatchEditor.node.modifier.*;
import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.graphics.*;
import arc.scene.actions.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
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

    private final EditorNode rootEditorNode;

    private String editorPath, lastChildPath;
    private OrderedMap<Class<?>, Seq<EditorNode>> mappedChildren;

    private String searchText = "";

    public NodeCard(EditorNode rootEditorNode){
        this.rootEditorNode = rootEditorNode;

        cardCont = new Table();
        nodesTable = new Table();

        top().left();
        cardCont.top();
        nodesTable.top().left();
    }

    public void setEditorNode(String path){
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
            childCard = new NodeCard(rootEditorNode);
            childCard.parent = this;
        }else if(childCard.editing){
            childCard.editChildNode(null);
        }

        editing = path != null;
        childCard.setEditorNode(path);

        rebuildCont();
    }

    public void extractWorking(){
        if(parent != null){
            parent.lastChildPath = editorPath;
            parent.editChildNode(null);
        }
    }

    public void editLastData(){
        if((childCard == null || !childCard.editing) && lastChildPath != null && lastChildPath.startsWith(editorPath)){
            editChildNode(lastChildPath);
        }
    }

    public void rebuild(){
        clearChildren();
        if(editorPath == null) return;

        defaults().growX();
        buildTitle(this);
        row();
        rebuildCont();
        add(cardCont).grow();
    }

    public EditorNode getEditorNode(){
        return rootEditorNode.navigate(editorPath);
    }

    private void rebuildCont(){
        cardCont.clearChildren();

        cardCont.defaults().padLeft(16f);

        if(editing){
            if(childCard.getEditorNode() == null){
                Core.app.post(() -> editChildNode(null));
                return;
            }

            childCard.rebuild();
            cardCont.add(childCard).grow();
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

        // 下一帧可能正好被清除
        EditorNode editorNode = getEditorNode();
        if(editorNode == null){
            return;
        }

        int columns = Math.max(1, (int)(nodesTable.getWidth() / Scl.scl() / buttonWidth));

        var map = mappedChildren();
        for(var entry : map){
            Seq<EditorNode> children = entry.value;
            Class<?> declareClass = entry.key;
            if(children.isEmpty() && declareClass != Object.class) continue;

            if(declareClass != Object.class){
                nodesTable.table(t -> {
                    t.image().color(Pal.darkerGray).size(32f, 6f);
                    t.add(ClassHelper.getDisplayName(declareClass)).color(EPalettes.type).padLeft(16f).padRight(16f).left();
                    t.image().color(Pal.darkerGray).height(4f).growX();
                }).marginTop(16f).marginBottom(8f).growX();
                nodesTable.row();
            }
            Table cont = nodesTable.table().left().get();
            nodesTable.row();

            cont.defaults().size(buttonWidth, buttonWidth / 4).pad(4f).margin(8f).top().left();

            int index = 0;
            for(EditorNode child : children){
                if(!searchText.isEmpty()){
                    String displayName = NodeDisplay.getDisplayName(child.getDisplayValue());

                    if(!Strings.matches(searchText, child.getObjNode().name)
                    && (displayName == null || !Strings.matches(searchText, displayName))){
                        continue;
                    }
                }

                DataModifier<?> modifier = NodeModifier.getModifier(child);
                if(modifier != null){
                    addEditTable(cont, child, modifier);
                }else{
                    addChildButton(cont, child);
                }
                if(++index % columns == 0){
                    cont.row();
                }
            }

            if(declareClass == Object.class && editorNode.getObjNode().hasSign(ModifierSign.PLUS) && editorNode.getObjNode().elementType != null){
                addPlusButton(cont, editorNode);
            }

            children.clear();
        }

        map.clear();
    }

    private void addEditTable(Table table, EditorNode node, DataModifier<?> modifier){
        Color modifiedColor = EPalettes.modified, unmodifiedColor0 = EPalettes.unmodified;
//        if(node.isDynamic()) unmodifiedColor0 = EPalettes.add;
        if(isRequired(node)) unmodifiedColor0 = EPalettes.required;

        Color unmodifiedColor = unmodifiedColor0;

        table.table(t -> {
            t.table(infoTable -> {
                infoTable.left();
                NodeDisplay.displayNameType(infoTable, node);
            }).pad(8f).fill();

            t.table(modifier::build).pad(4).grow();
            t.table(btn -> setupEditButton(btn, node, true)).pad(6f).growY();

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

    private static boolean isRequired(EditorNode node){
        return node.getPatch() == null && PatchJsonIO.fieldRequired(node);
    }

    private void addChildButton(Table table, EditorNode node){
//        EditorNode removeData = node.getSign(ModifierSign.REMOVE);
        boolean isKeyRemoved = false; // removeData != null && removeData.getPatch() != null;

        ImageButtonStyle style = EStyles.cardButtoni;
        if(isRequired(node)){
            style = EStyles.cardRequiredi;
        }else if(node instanceof DynamicEditorNode){
            style = EStyles.addButtoni;
        }else if(isKeyRemoved){
            style = EStyles.cardRemovedi;
        }else if(node.hasValue()){
            style = EStyles.cardModifiedButtoni;
        }

        table.button(b -> {
            b.table(infoTable -> {
                infoTable.left();
                NodeDisplay.display(infoTable, node);
            }).pad(8f).grow();

            b.table(buttons -> setupEditButton(buttons, node, false)).pad(6f).growY();

            b.image().width(4f).color(Color.darkGray).growY().right();
            b.row();
            Cell<?> horizontalLine = b.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(b.getColumns());
        }, style, () -> {
//            EditorNode modifyData = node.getSign(ModifierSign.MODIFY);
//            editChildNode(modifyData == null || modifyData.getPath() == null ? node : modifyData);
            editChildNode(node.getPath());
        }).disabled(node.getObject() == null && !node.isOverriding());
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
                ContentType type = PatchJsonIO.getContentType(keyType);
                if(type == null){
                    // TODO: unsupported key type
                    Vars.ui.showErrorMessage("#Unsupported key type " + ClassHelper.getDisplayName(keyType));
                    return;
                }
                EUI.selector.select(type, c -> true, c -> {
                    editorNode.putKey(PatchJsonIO.getKeyName(c));
                    rebuildNodesTable();
                    return true;
                });
            }else{
                PatchNode patchNode = editorNode.getPatch();
                if(patchNode != null && patchNode.sign == ModifierSign.MODIFY){
                    editorNode.append(false);
                }else{
                    // if array is null, don't use plus syntax
                    editorNode.append(editorNode.getObject() != null);
                }
                rebuildNodesTable();
            }
        });
    }

    private void setupEditButton(Table table, EditorNode node, boolean hasModifier){
        table.defaults().width(32f).pad(4f).growY();
        EditorNode editorNode = getEditorNode();

//        if(removeData != null){
//            boolean undoMode = removeData.getPath() != null;
//            table.button(undoMode ? Icon.undo : Icon.cancel, Styles.clearNoneTogglei, () -> {
//                if(undoMode) removeData.clearJson();
//                rebuildNodesTable();
//            }).tooltip(undoMode ? "##revertRemove" : "##removeKey");
//            if(undoMode) return;
//        }

        if(node.isOverriding()){
            table.button(Icon.undo, Styles.clearNonei, () -> {
                node.setSign(null);
                node.clearJson();
                rebuildNodesTable();
            }).tooltip("##revertOverride");
        }else if(!hasModifier && (node instanceof DynamicEditorNode || node.isChangedType())){
            if(!ClassHelper.isArray(node.getTypeIn())){
                table.button(Icon.wrench, Styles.clearNonei, () -> {
                    EUI.classSelector.select(null, node.getTypeIn(), clazz -> {
                        node.changeType(clazz);
                        rebuildNodesTable();
                        return true;
                    });
                }).tooltip("##changeType");
            }

            table.button(Icon.cancel, Styles.clearNonei, () -> {
                node.clearJson();
                rebuildNodesTable();
            }).grow().row();
        }else if(!hasModifier && (node.getObject() == null || ClassHelper.isArrayLike(node.getTypeIn()))){
            PatchNode patchNode = node.getPatch();
            if(patchNode == null || patchNode.sign == null){
                table.button(Icon.wrench, Styles.clearNonei, () -> {
                    node.setSign(ModifierSign.MODIFY);
                    rebuildNodesTable();
                }).tooltip("##override");
            }
        }else if(!hasModifier && ClassHelper.isMap(editorNode.getTypeIn())){
            table.button(Icon.wrench, Styles.clearNonei, () -> {
                EUI.classSelector.select(null, node.getTypeIn(), clazz -> {
                    node.changeType(clazz);
                    rebuildNodesTable();
                    return true;
                });
            }).tooltip("##override");
        }

        if(isRequired(node)){
            table.image(Icon.infoCircle).height(32f).tooltip("##mayRequired");
        }
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

            // Clear data
            nodeTitle.button(Icon.refresh, Styles.cleari, () -> {
                Vars.ui.showConfirm(Core.bundle.format("node-card.clear-data.confirm", editorNode.getPath()), () -> {
                    editorNode.clearJson();
                    getFrontCard().rebuildNodesTable();
                });
            }).size(64f).tooltip("@node-card.clear-data", true);

            nodeTitle.table(cardButtons -> {
                cardButtons.defaults().size(64f).pad(8f);

                if(parent != null){
                    cardButtons.button(Icon.upOpen, Styles.cleari, this::extractWorking).tooltip("@node-card.extract", false);
                }
            }).expandX().right().growY();
        }).color(titleColor);
    }

    private OrderedMap<Class<?>, Seq<EditorNode>> mappedChildren(){
        EditorNode editorNode = getEditorNode();

        if(mappedChildren == null) mappedChildren = new OrderedMap<>();
        for(var entry : mappedChildren){
            entry.value.clear();
        }
        mappedChildren.clear();

        Class<?> type = editorNode.getTypeOut();
        if(type == null) return mappedChildren;

        while(type != null){
            mappedChildren.put(type, new Seq<>());
            type = type.getSuperclass();
        }

        for(EditorNode child : editorNode.getChildren().values()){
            if(child.getObjNode() == null || child.getObjNode().field == null){
                mappedChildren.get(Object.class).add(child); // Object means unknow declaring class
                continue;
            }

            mappedChildren.get(child.getObjNode().field.getDeclaringClass()).add(child);
        }

        for(var entry : mappedChildren){
            var children = entry.value;
            if(children.any()) children.sort(
            Structs.comps(
                Structs.comparingBool(n -> !isRequired(n)),
                Structs.comps(
                    Structs.comparingBool(n -> !n.hasValue()),
                    Structs.comparingInt(NodeModifier::getModifierIndex).reversed()
                )
            )
            );
        }

        return mappedChildren;
    }

    @Override
    public String toString(){
        return "NodeCard{" +
        "nodeData=" + editorPath +
        '}';
    }
}