package dustdustry.patcheditor.ui;

import dustdustry.patcheditor.*;
import dustdustry.patcheditor.node.*;
import dustdustry.patcheditor.node.EditorNode.*;
import dustdustry.patcheditor.node.modifier.*;
import dustdustry.patcheditor.utils.*;
import arc.struct.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;

import java.util.*;

public class NodeCategorizer{
    private static final Comparator<EditorNode> baseComparator = Structs.comps(
        Structs.comparingBool(n -> !FieldFavorites.isFavorite(n)),
        Structs.comps(
            Structs.comparingBool(n -> !n.isRequired()),
            Structs.comparingBool(n -> !(n.hasValue() && n.getObjNode() != null))
        )
    );

    public static Seq<NodeCategory> categorizedNode(EditorNode node){
        if(node.getObjNode().elementType == Block.class) return categorizedBlock(node);
        return categorizedNormal(node);
    }

    private static Seq<NodeCategory> categorizedBlock(EditorNode node){
        OrderedMap<Category, NodeCategory> map = new OrderedMap<>();

        NodeCategory modified = new NodeCategory("modified");
        NodeCategory environment = new NodeCategory("environment");
        NodeCategory other = new NodeCategory(NodeCategory.otherCategoryName);
        for(EditorNode child : node.buildChildren().values()){
            Object object = child.getObject();
            if(!(object instanceof Block block) || object instanceof ConstructBlock){
                other.add(child);
            }else if(isEnvironmentBlock(block)){
                environment.add(child);
            }else if(block.category != null){
                if(!map.containsKey(block.category)){
                    map.put(block.category, new NodeCategory(block.category.name()));
                }
                map.get(block.category).add(child);
            }else{
                other.add(child);
            }

            if(child.hasValue() && !(child instanceof InvalidEditorNode)){
                modified.add(child);
            }
        }

        Seq<NodeCategory> seq = map.values().toSeq();
        if(environment.nodes.any()) seq.add(environment);
        if(other.nodes.any()) seq.add(other);
        for(NodeCategory category : seq){
            category.nodes.sort(baseComparator);
        }

        modified.nodes.sort(baseComparator);
        seq.insert(0, modified);
        return seq;
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
                NodeCategory other = map.get(Object.class);
                if(other == null){
                    other = new NodeCategory(NodeCategory.otherCategoryName);
                    map.put(Object.class, other);
                }
                other.add(child); // Object means unknown declaring class
                continue;
            }

            int index = NodeModifier.getModifierIndex(child.getObjNode());
            modifierIndexer.put(child, index == -1 ? Integer.MAX_VALUE : index);
            Class<?> declaring = child.getObjNode().field.getDeclaringClass();
            NodeCategory category = map.get(declaring);
            if(category == null){
                category = new NodeCategory(ClassHelper.getDisplayName(declaring));
                map.put(declaring, category);
            }
            category.add(child);
        }

        for(var entry : map){
            var category = entry.value;
            if(category.nodes.any()) category.nodes.sort(
                Structs.comps(baseComparator, Structs.comparingInt(modifierIndexer::get))
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
