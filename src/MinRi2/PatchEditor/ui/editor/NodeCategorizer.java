package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.modifier.*;
import arc.struct.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;

public class NodeCategorizer{
    public static Seq<NodeCategory> categorizedNode(EditorNode node){
        if(node.getObjNode().elementType == Block.class) return categorizedBlock(node);
        return categorizedNormal(node);
    }

    private static Seq<NodeCategory> categorizedBlock(EditorNode node){
        OrderedMap<Category, NodeCategory> map = new OrderedMap<>();

        for(Category category : Category.all){
            map.put(category, new NodeCategory(category.name()));
        }

        NodeCategory environment = new NodeCategory("environment");
        NodeCategory other = new NodeCategory(NodeCategory.otherCategoryName);
        for(EditorNode child : node.buildChildren().values()){
            Object object = child.getObject();
            if(!(object instanceof Block block) || object instanceof ConstructBlock){
                other.add(child);
            }else if(isEnvironmentBlock(block)){
                environment.add(child);
            }else{
                map.get(block.category).add(child);
            }
        }

        return map.values().toSeq().retainAll(c -> c.nodes.any()).add(environment, other);
    }

    public static Seq<NodeCategory> categorizedNormal(EditorNode node){
        OrderedMap<Class<?>, NodeCategory> map = new OrderedMap<>();

        Class<?> type = node.getTypeOut();
        while(type != null){
            String name = type == Object.class ? NodeCategory.otherCategoryName : ClassHelper.getDisplayName(type);
            map.put(type, new NodeCategory(name));
            type = type.getSuperclass();
        }

        ObjectIntMap<EditorNode> modifierIndexer = new ObjectIntMap<>();
        for(EditorNode child : node.buildChildren().values()){
            if(child.getObjNode() == null || child.getObjNode().field == null){
                map.get(Object.class).add(child); // Object means unknown declaring class
                continue;
            }

            int index = NodeModifier.getModifierIndex(child.getObjNode());
            modifierIndexer.put(child, index == -1 ? Integer.MAX_VALUE : index);
            map.get(child.getObjNode().field.getDeclaringClass()).add(child);
        }

        for(var entry : map){
            var category = entry.value;
            if(category.nodes.any()) category.nodes.sort(
            Structs.comps(
            Structs.comparingBool(n -> !n.isRequired()),
            Structs.comps(
            Structs.comparingBool(n -> !(n.hasValue() && n.getObjNode() != null)),
            Structs.comparingInt(modifierIndexer::get)
            )
            )
            );
        }

        return map.values().toSeq();
    }

    private static boolean isEnvironmentBlock(Block block){
        return block instanceof Floor || block instanceof Prop
        || block instanceof RemoveWall || block instanceof Cliff
        || block instanceof TreeBlock || block instanceof TallBlock;
    }

    public static class NodeCategory{
        public static final String otherCategoryName = "Other";

        public final String name;
        public final Seq<EditorNode> nodes = new Seq<>();

        public final boolean isOther;

        public NodeCategory(String name){
            this.name = name;
            isOther = name.equals(otherCategoryName);
        }

        public void add(EditorNode node){
            nodes.add(node);
        }
    }
}
