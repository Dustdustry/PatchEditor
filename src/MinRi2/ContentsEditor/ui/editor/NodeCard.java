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
import mindustry.ctype.ContentType;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

/**
 * @author minri2
 * Create by 2024/2/16
 */
public class NodeCard extends Table{
    public static float buttonWidth = 320f;
    public static float buttonHeight = buttonWidth / 3f;

    private final Table cardCont, nodesTable; // workingTable / childrenNodesTable
    public boolean editing;
    public NodeCard parent, childCard;
    private NodeData nodeData;
    private Seq<NodeData> sortedChildren;

    private NodeData lastChildData;

    private String searchText = "";

    public NodeCard(){
        cardCont = new Table();
        nodesTable = new Table();

        top().left();
        cardCont.top();
        nodesTable.top().left();
    }

    public void setNodeData(NodeData nodeData){
        this.nodeData = nodeData;
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

        childCard.setNodeData(childNodeData);
        childCard.rebuild();

        rebuild();
    }

    public void extractWorking(){
        if(parent != null){
            parent.lastChildData = nodeData;
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

        if(nodeData == null){
            return;
        }

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
            childCard.rebuildCont();
            cardCont.add(childCard).grow();
        }else{
            cardCont.table(this::setupSearchTable).pad(8f).growX();

            cardCont.row();

            cardCont.add(nodesTable).fill();

            // 下一帧再重构
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
        if(nodeData == null){
            return;
        }

        int columns = Math.max(1, (int)(nodesTable.getWidth() / Scl.scl() / buttonWidth));

        nodesTable.defaults().size(buttonWidth, buttonWidth / 4).pad(4f).margin(8f).top().left();

        int index = 0;
        for(NodeData child : sortChildren()){
            if(!searchText.isEmpty()){
                String displayName = NodeDisplay.getDisplayName(child.getObject());

                if(!Strings.matches(searchText, child.name)
                && (displayName == null || !Strings.matches(searchText, displayName))){
                    continue;
                }
            }

            DataModifier<?> modifier = NodeModifier.getModifier(child);
            if(modifier != null){
                addEditTable(nodesTable, child, modifier);
            }else{
                addChildButton(nodesTable, child);
            }

            if(++index % columns == 0){
                nodesTable.row();
            }
        }
    }

    private void addEditTable(Table table, NodeData childData, DataModifier<?> modifier){
        table.table(t -> {
            t.table(infoTable -> {
                // Add node info
                NodeDisplay.displayNameType(infoTable, childData);
            }).fill();

            t.table(modifier::build).pad(4).grow();

            t.image().width(4f).color(Color.darkGray).growY().right();
            t.row();
            Cell<?> horizontalLine = t.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(t.getColumns());

            t.background(Tex.whiteui);
            t.setColor(modifier.isModified() ? EPalettes.modified : EPalettes.unmodified);

            modifier.onModified(modified -> {
                Color color = modified ? EPalettes.modified : EPalettes.unmodified;
                t.addAction(Actions.color(color, 0.2f));
            });
        });
    }

    private void addChildButton(Table table, NodeData childData){
        ImageButtonStyle style = nodeData.hasJsonChild(childData.name) ? EStyles.cardModifiedButtoni : EStyles.cardButtoni;

        table.button(b -> {
            NodeDisplay.display(b, childData);

            b.image().width(4f).color(Color.darkGray).growY().right();
            b.row();
            Cell<?> horizontalLine = b.image().height(4f).color(Color.darkGray).growX();
            horizontalLine.colspan(b.getColumns());
        }, style, () -> {
            editChildNode(childData);

            rebuildCont();
        });
    }

    private void buildTitle(Table table){
        Color titleColor = parent == null ? EPalettes.main2 : EPalettes.main3;
        table.table(Tex.whiteui, nodeTitle -> {
            nodeTitle.table(Tex.whiteui, nameTable -> {
                NodeDisplay.display(nameTable, nodeData);

                nameTable.image().width(4f).color(Color.darkGray).growY().right();
                nameTable.row();
                Cell<?> horizontalLine = nameTable.image().height(4f).color(Color.darkGray).growX();
                horizontalLine.colspan(nameTable.getColumns());
            }).color(Pal.darkestGray).size(buttonWidth, buttonHeight).pad(8f).expandX().left();

            nodeTitle.table(buttonTable -> {
                buttonTable.defaults().size(64f).pad(8f);

                // Clear data
                buttonTable.button(Icon.refresh, Styles.cleari, () -> {
                    Vars.ui.showConfirm(Core.bundle.format("node-card.clear-data.confirm", nodeData.name), () -> {
                        nodeData.clearJson();
                        getFrontCard().rebuildNodesTable();
                    });
                }).tooltip("@node-card.clear-data", true);

                if(parent != null){
                    buttonTable.button(Icon.upOpen, Styles.cleari, this::extractWorking).tooltip("@node-card.extract", false);
                }
            }).growY();
        }).color(titleColor);
    }

    private Seq<NodeData> sortChildren(){
        if(sortedChildren == null){
            sortedChildren = new Seq<>();
        }

        sortedChildren.clear();
        nodeData.getChildren().values().toSeq(sortedChildren);

        sortedChildren.sort(Structs.comps(
            Structs.comparingBool(n -> n.jsonData == null),
            Structs.comparingInt(NodeModifier::getModifierIndex).reversed()
        ));

        return sortedChildren;
    }

    @Override
    public String toString(){
        return "NodeCard{" +
        "nodeData=" + nodeData +
        '}';
    }
}