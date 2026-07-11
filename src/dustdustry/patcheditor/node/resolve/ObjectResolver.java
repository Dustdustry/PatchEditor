package dustdustry.patcheditor.node.resolve;

import dustdustry.patcheditor.node.*;
import dustdustry.patcheditor.utils.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Json.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.mod.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;

public class ObjectResolver{
    public static final Object emptyObject = new Object();
    public static ResolutionStrategy patch;
    public static ResolutionStrategy content;

    // For dynamic editor node
    private static ObjectMap<ResolutionStrategy, ObjectMap<Class<?>, ObjectNode>> templateNode;

    public static void resolve(ObjectNode node, ResolutionStrategy strategy){
        if(node.isRoot()){
            strategy.resolveRoot(node);
            return;
        }

        Class<?> objectType = node.type;
        Object object = node.object;
        if(object == emptyObject) return;
        if(object == null && objectType == null) return;

        if(object != null){
            if(object instanceof MapEntry<?, ?> entry) object = entry.value;
            objectType = object.getClass();
        }else{
            object = ObjectExample.getExample(objectType, objectType);
        }

        // type resolve
        if(!strategy.isTypeResolvable(objectType)) return;
        if(node.elementType != null && !strategy.isTypeEditable(node.elementType)) return;

        if(ClassHelper.isArrayLike(objectType)){
            if(strategy.shouldAddPlusSign(node, objectType)){
                node.addSign(ModifierSign.PLUS, null, node.elementType, null);
            }
        }else if(ClassHelper.isMap(objectType)){
            if(strategy.shouldAddPlusSign(node, objectType)){
                node.addSign(ModifierSign.PLUS, null, node.elementType, node.keyType);
            }
        }

        // object resolve
        if(objectType.isArray()){
            for(int i = 0; i < Array.getLength(object); i++){
                Object o = Array.get(object, i);
                if(o == null) continue;
                ObjectNode child = node.addChild("" + i, o, node.elementType);
                if(strategy.shouldAddModifySign(node, child)){
                    child.addSign(ModifierSign.MODIFY, node.type, node.elementType, null);
                }
            }
        }else if(object instanceof Seq<?> seq){
            for(int i = 0; i < seq.size; i++){
                Object o = seq.get(i);
                ObjectNode child = node.addChild("" + i, o, node.elementType);
                if(strategy.shouldAddModifySign(node, child)){
                    child.addSign(ModifierSign.MODIFY, node.type, node.elementType, null);
                }
            }
        }else if(object instanceof ObjectSet<?> set){
            int i = 0;
            for(Object o : set){
                ObjectNode child = node.addChild("" + i++, o, node.elementType);
                if(strategy.shouldAddModifySign(node, child)){
                    child.addSign(ModifierSign.MODIFY, node.type, node.elementType, null);
                }
            }
        }else if(object instanceof ObjectMap<?, ?> map){
            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                ObjectNode entryNode = node.addChild(name, new MapEntry<>(entry), node.elementType, node.elementType, node.keyType);
                if(strategy.shouldAddModifySign(node, entryNode)){
                    entryNode.addSign(ModifierSign.MODIFY);
                }
                if(strategy.shouldAddRemoveSign(node, entryNode)){
                    entryNode.addSign(ModifierSign.REMOVE);
                }
            }
        }else if(object instanceof ObjectFloatMap<?> map){
            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                ObjectNode entryNode = node.addChild(name, new MapEntry<>(entry.key, entry.value), node.elementType, node.elementType, node.keyType);
                if(strategy.shouldAddModifySign(node, entryNode)){
                    entryNode.addSign(ModifierSign.MODIFY);
                }
                if(strategy.shouldAddRemoveSign(node, entryNode)){
                    entryNode.addSign(ModifierSign.REMOVE);
                }
            }
        }else if(object instanceof ContentType ctype){
            OrderedMap<String, Content> map = new OrderedMap<>(); // in order
            for(Content content : Vars.content.getBy(ctype)){
                map.put(PatchJsonIO.getKeyName(content), content);
            }

            for(var entry : map){
                String name = PatchJsonIO.getKeyName(entry.key);
                node.addChild(name, entry.value);
            }
        }else if(object instanceof EnumSet<?> set){
            Class<?> enumClass = set.array != null ? set.array.getClass().getComponentType() : null;
            if(enumClass != null){
                int i = 0;
                for(Object o : set.array){
                    ObjectNode child = node.addChild("" + i++, o, enumClass);
                    if(strategy.shouldAddModifySign(node, child)){
                        child.addSign(ModifierSign.MODIFY, enumClass, enumClass, null);
                    }
                }
            }
        }else if(object instanceof Attributes attributes){
            for(Attribute att : Attribute.all){
                node.addChild(att.name, attributes.get(att)).addSign(ModifierSign.MODIFY);
            }
        }else{
            resolveFields(node, object, objectType, strategy);
        }
    }

    private static void resolveFields(ObjectNode node, Object object, Class<?> objectType, ResolutionStrategy strategy){
        Seq<String> blacklist = strategy.getFieldBlacklist(objectType);
        for(var entry : PatchJsonIO.getFields(objectType)){
            String name = entry.key;
            if(blacklist != null && blacklist.contains(name)) continue;

            FieldMetadata fieldMeta = entry.value;
            if(!strategy.isFieldResolvable(fieldMeta.field)) continue;
            if(fieldMeta.elementType != null && !strategy.isTypeEditable(fieldMeta.elementType)) continue;

            FieldMetadata copiedMeta = new FieldMetadata(fieldMeta.field);
            Object childObj = object == null ? null : Reflect.get(object, fieldMeta.field);
            if(childObj instanceof ObjectFloatMap<?>){
                copiedMeta.keyType = copiedMeta.elementType;
                copiedMeta.elementType = float.class;
            }

            ObjectNode child = node.addChild(name, childObj, copiedMeta);

            if(strategy.shouldAddModifySign(node, child)){
                if(!ClassHelper.isMap(child.type)){
                    child.addSign(ModifierSign.MODIFY);
                }
            }
        }

        strategy.addSpecialChildren(node, object);
    }

    public static ObjectNode getShadowNode(ObjectNode node, Class<?> newType){
        ObjectNode shadowNode = new ObjectNode(node.name, ObjectExample.getExample(newType, newType), node.field, node.type, node.elementType, node.keyType);
        shadowNode.strategy = node.getResolutionStrategy();
        if(node.hasSign(ModifierSign.MODIFY)){
            shadowNode.addSign(ModifierSign.MODIFY);
        }
        return shadowNode;
    }

    public static ObjectNode getTemplate(Class<?> type, Object object, ResolutionStrategy strategy){
        ObjectNode objectNode = new ObjectNode("", object, type);
        objectNode.strategy = strategy;
        return objectNode;
    }

    public static ObjectNode getTemplate(Class<?> type, ResolutionStrategy strategy){
        if(templateNode == null) templateNode = new ObjectMap<>();

        ObjectMap<Class<?>, ObjectNode> cache = templateNode.get(strategy);
        if(cache == null){
            cache = new ObjectMap<>();
            templateNode.put(strategy, cache);
        }

        ObjectNode objectNode = cache.get(type);
        if(objectNode != null) return objectNode;

        objectNode = new ObjectNode("", ObjectExample.getExample(PatchJsonIO.resolveType(type), type), type);
        objectNode.strategy = strategy;
        cache.put(type, objectNode);
        return objectNode;
    }

    public static void clearTemplate(){
        if(templateNode != null) templateNode.clear();
    }
}
