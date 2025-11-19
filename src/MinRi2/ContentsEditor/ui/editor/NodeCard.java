package MinRi2.ContentsEditor.ui.editor;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.node.modifier.*;
import MinRi2.ContentsEditor.ui.*;
import arc.*;
import arc.graphics.*;
import arc.scene.actions.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
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
    private NodeData data;
    private OrderedMap<Class<?>, Seq<NodeData>> mappedChildren;

    private NodeData lastChildData;

    private String searchText = "";

    public NodeCard(){
        cardCont = new Table();
        nodesTable = new Table();

        top().left();
        cardCont.top();
        nodesTable.top().left();
    }

    public void setData(NodeData data){
        this.data = data;
    }

    public NodeCard getFrontCard(){
        NodeCard card = this;

        while(card.editing && card.childCard != null){
            card = card.childCard;
        }

        return card;
    }

    private void editChildNode(NodeData childNodeData){
        if(childCard == null){
            childCard = new NodeCard();
            childCard.parent = this;
        }else if(childCard.editing){
            childCard.editChildNode(null);
        }

        editing = childNodeData != null;
        childCard.setData(childNodeData);
        rebuild();
    }

    public void extractWorking(){
        if(parent != null){
            parent.lastChildData = data;
            parent.editChildNode(null);
        }
    }

    public void editLastData(){
        // 仅支持最前面的卡片
        if((childCard == null || !childCard.editing) && lastChildData != null){
            editChildNode(lastChildData);
        }
    }

    public void rebuild(){
        clearChildren();
        if(data == null) return;

        defaults().growX();
        buildTitle(this);
        row();
        rebuildCont();
        add(cardCont).grow();
    }

    private void rebuildCont(){
        cardCont.clearChildren();

        cardCont.defaults().padLeft(16f);

        if(editing){
            if(childCard.data.parentData == null){
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
        if(data == null){
            return;
        }

        int columns = Math.max(1, (int)(nodesTable.getWidth() / Scl.scl() / buttonWidth));

        var map = mappedChildren();
        for(var entry : map){
            Seq<NodeData> children = entry.value;
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
            for(NodeData child : children){
                // sign have its table
                if(child.isSign()) continue;

                if(!searchText.isEmpty()){
                    String displayName = NodeDisplay.getDisplayName(child.getObject());

                    if(!Strings.matches(searchText, child.name)
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

            if(declareClass == Object.class){
                NodeData plusData = data.getSign(ModifierSign.PLUS);
                if(plusData != null) addPlusButton(cont, plusData);
            }

            children.clear();
        }

        map.clear();
    }

    private void addEditTable(Table table, NodeData node, DataModifier<?> modifier){
        Color modifiedColor = EPalettes.modified, unmodifiedColor = node.isDynamic() ? EPalettes.add : EPalettes.unmodified;
        table.table(t -> {
            t.table(infoTable -> {
                infoTable.left();
                NodeDisplay.displayNameType(infoTable, node);
            }).pad(8f).fill();

            t.table(modifier::build).pad(4).grow();
            t.table(btn -> setupEditButton(btn, node)).pad(6f).growY();

            t.image().width(4f).color(Color.darkGray).growY().right();
            t.row();
            Cell<?> horizontalLine = t.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(t.getColumns());

            t.background(Tex.whiteui);
            t.setColor(modifier.isModified() ? modifiedColor : unmodifiedColor);

            modifier.onModified(modified -> {
                t.addAction(Actions.color(modifier.isModified() ? modifiedColor : unmodifiedColor, 0.2f));
            });
        });
    }

    private void addChildButton(Table table, NodeData node){
        ImageButtonStyle style = EStyles.cardButtoni;
        if(node.isDynamic()){
            style = EStyles.addButtoni;
        }else if(data.hasJsonChild(node.name)){
            style = EStyles.cardModifiedButtoni;
        }

        table.button(b -> {
            b.table(infoTable -> {
                infoTable.left();
                NodeDisplay.display(infoTable, node);
            }).pad(8f).grow();

            b.table(btn -> setupEditButton(btn, node)).pad(6f).growY();

            b.image().width(4f).color(Color.darkGray).growY().right();
            b.row();
            Cell<?> horizontalLine = b.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(b.getColumns());
        }, style, () -> {
            editChildNode(node);

            rebuildCont();
        }).disabled(node.getObject() == null);
    }

    private void addPlusButton(Table table, NodeData plusData){
        if(plusData.meta == null || plusData.meta.elementType == null) return;

        table.button(b -> {
            b.image(Icon.add).pad(8f).padRight(16f);

            b.add(ClassHelper.getDisplayName(plusData.meta.elementType)).color(EPalettes.type)
            .style(Styles.outlineLabel).ellipsis(true).fillX();

            b.image().width(4f).color(Color.darkGray).growY().right();
            b.row();
            Cell<?> horizontalLine = b.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(b.getColumns());
        }, EStyles.addButtoni, () -> {
            NodeModifier.addDynamicChild(plusData);
            rebuildNodesTable();
        });
    }

    private void setupEditButton(Table table, NodeData data){
        table.defaults().width(32f).pad(4f).growY();

        if(data.isDynamic()){
            table.button(Icon.wrench, Styles.clearNonei, () -> {
                EUI.classSelector.select(null, PatchJsonIO.getTypeMeta(data), clazz -> {
                    NodeModifier.changeType(data, clazz);
                    rebuildNodesTable();
                    return true;
                });
            });

            table.button(Icon.cancel, Styles.clearNonei, () -> {
                data.clearJson();
                rebuildNodesTable();
            }).grow().row();
        }

        if(data.hasSign(ModifierSign.REMOVE)){
            table.button(Icon.cancelSmall, Styles.clearNonei, () -> {}).size(48f);
        }

    }

    private void buildTitle(Table table){
        Color titleColor = parent == null ? EPalettes.main2 : EPalettes.main3;
        table.table(Tex.whiteui, nodeTitle -> {
            nodeTitle.defaults().pad(8f);

            nodeTitle.table(Tex.whiteui, nameTable -> {
                nameTable.table(t -> {
                    t.left();
                    NodeDisplay.display(t, data);
                }).pad(8f).grow();

                nameTable.image().width(4f).color(Color.darkGray).growY().right();
                nameTable.row();
                Cell<?> horizontalLine = nameTable.image().height(4f).color(Color.darkGray).growX();
                horizontalLine.colspan(nameTable.getColumns());
            }).color(Pal.darkestGray).size(buttonWidth, buttonHeight);

            // Clear data
            nodeTitle.button(Icon.refresh, Styles.cleari, () -> {
                Vars.ui.showConfirm(Core.bundle.format("node-card.clear-data.confirm", data.name), () -> {
                    data.clearJson();
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

    private OrderedMap<Class<?>, Seq<NodeData>> mappedChildren(){
        if(mappedChildren == null) mappedChildren = new OrderedMap<>();
        for(var entry : mappedChildren){
            entry.value.clear();
        }
        mappedChildren.clear();

        Class<?> type = PatchJsonIO.getType(data);
        if(type == null) return mappedChildren;

        while(type != null){
            mappedChildren.put(type, new Seq<>());
            type = type.getSuperclass();
        }

        for(NodeData child : data.getChildren()){
            if(child.isSign()){
                mappedChildren.get(Object.class).addAll(child.getChildren());
                continue;
            }

            if(child.meta == null || child.meta.field == null){
                mappedChildren.get(Object.class).add(child); // Object means unknow declaring class
                continue;
            }

            mappedChildren.get(child.meta.field.getDeclaringClass()).add(child);
        }

        for(var entry : mappedChildren){
            var children = entry.value;
            if(children.any()) children.sort(Structs.comps(
                Structs.comparingBool(n -> n.getJsonData() == null),
                Structs.comparingInt(NodeModifier::getModifierIndex).reversed()
            ));
        }

        return mappedChildren;
    }

    @Override
    public String toString(){
        return "NodeCard{" +
        "nodeData=" + data +
        '}';
    }
}