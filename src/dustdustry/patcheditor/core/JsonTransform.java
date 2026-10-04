package dustdustry.patcheditor.core;

import arc.math.geom.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.resolve.*;
import dustdustry.patcheditor.core.JsonProcessor.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;

public class JsonTransform{

    /** patchTree to jsonTree */
    public static JNode toJsonTree(PatchNode patchNode){
        return toJsonTree(patchNode, newNode(patchNode));
    }

    private static JNode toJsonTree(PatchNode patchNode, JNode value){
        value.setName(patchNode.key);
        if(patchNode.value != null){
            if(patchNode.type == Jtype.number){
                value.set(parseNumber(patchNode.value));
            }else if(patchNode.type == Jtype.bool){
                value.set(Boolean.parseBoolean(patchNode.value));
            }else{
                value.set(patchNode.value);
            }
        }

        if(!value.isValue()){
            JNode appendValue = null;
            for(PatchNode childNode : patchNode.children.values()){
                JNode childValue = newNode(childNode);

                if(childNode.key.startsWith(PatchJsonIO.appendPrefix)){
                    if(appendValue == null){
                        appendValue = JNode.array();
                        value.addChild(ModifierSign.PLUS.sign, appendValue);
                    }

                    appendValue.addChild(childValue);
                }else{
                    value.addChild(childNode.key, childValue);
                }

                toJsonTree(childNode, childValue);
            }
        }

        return value;
    }

    private static JNode newNode(PatchNode patchNode){
        return switch(patchNode.type){
            case object -> JNode.object();
            case array -> JNode.array();
            default -> JNode.nullValue();
        };
    }

    /** Keeps integers integral so that large values and index-like fields survive a round trip. */
    private static Jval parseNumber(String text){
        try{
            if(text.indexOf('.') < 0 && text.indexOf('e') < 0 && text.indexOf('E') < 0){
                return Jval.valueOf(Long.parseLong(text));
            }
            return Jval.valueOf(Double.parseDouble(text));
        }catch(NumberFormatException ignored){
            //e.g. a "0.5f" literal: leave it as text, the game parser accepts both forms
            return Jval.valueOf(text);
        }
    }

    public static void extractDotSyntax(JNode value){
        if(value.name != null && value.parent != null && value.name.contains(NodeManager.pathComp)){
            String[] names = value.name.split(NodeManager.pathSplitter);

            JNode parent = value.parent;
            int position = value.index();

            value.remove();

            // dotted keys sharing a prefix merge into one object; a Jval map cannot hold duplicate sibling names
            JNode current = parent;
            for(int i = 0; i < names.length - 1; i++){
                JNode child = current.get(names[i]);
                if(child == null || child.isValue()){
                    child = JNode.object(names[i]); // insertChild keys the object by the child's own name
                    current.insertChild(position, child);
                }
                current = child;
            }

            value.setName(names[names.length - 1]);
            current.addChild(value);
        }

        for(JNode childValue : value){
            extractDotSyntax(childValue);
        }
    }

    public static void desugarJson(ObjectNode objectNode, JNode value){
        boolean isValue = value.isValue();

        if(objectNode != null){
            desugarJson(value, objectNode.type);

            // desugarJson may change the value type so cache it
            if(isValue) return;

            if(ClassHelper.isArrayLike(objectNode.type) || objectNode.isSign(ModifierSign.PLUS)){
                // "requirements": ["item/amount"] | {+: [], {"item": "xxx"}}
                ObjectNode childNode = ObjectResolver.getTemplate(objectNode.elementType, objectNode.getResolutionStrategy());
                for(JNode childValue : value){
                    if(ModifierSign.PLUS.sign.equals(childValue.name)){
                        ObjectNode plusNode = objectNode.getOrResolve(childValue.name);
                        desugarJson(plusNode, childValue);
                    }else{
                        desugarJson(childNode, childValue);
                    }
                }
                return;
            }
        }

        if(value.isValue()) return;

        for(JNode childValue : value){
            ObjectNode childNode = childValue.name == null || objectNode == null ? null : objectNode.getOrResolve(childValue.name);
            desugarJson(childNode, childValue);
        }
    }

    private static void desugarJson(JNode value, Class<?> type){
        // TODO: More sugar syntaxes support
        if(type == ItemStack.class || type == PayloadStack.class){
            if(!value.isString() || !value.asString().contains("/")) return;
            String[] split = value.asString().split("/");
            value.setType(Jtype.object);
            value.addChild(JNode.of("item", split[0]));
            value.addChild(JNode.of("amount", parseNumber(split[1])));
        }else if(type == LiquidStack.class || type == ConsumeLiquid.class){
            if(!value.isString() || !value.asString().contains("/")) return;
            String[] split = value.asString().split("/");
            value.setType(Jtype.object);
            value.addChild(JNode.of("liquid", split[0]));
            value.addChild(JNode.of("amount", parseNumber(split[1])));
        }else if(type == Consume.class && value.name != null){
            if(value.name.equals("remove")){
                if(value.isString()){
                    // remove: item -> remove: {item: -}
                    String removed = value.asString();
                    value.setType(Jtype.object);
                    value.addChild(JNode.of(removed, ModifierSign.REMOVE.sign));
                }else if(value.isArray()){
                    // remove: [item, liquid] -> remove: {item: -, liquid: -}
                    Jval container = Jval.newObject();
                    Jval.JsonMap map = container.asObject();
                    for(JNode child : value){
                        if(child.isString()){
                            map.put(child.asString(), Jval.valueOf(ModifierSign.REMOVE.sign));
                        }
                    }
                    value.set(container);
                }
            }
        }else if(type == ConsumeItems.class){
            if(value.isString()){
                // items: copper/2 -> items: {items: [copper/2]}
                String item = value.asString();
                value.setType(Jtype.object);
                JNode itemsValue = JNode.array();
                value.addChild("items", itemsValue);
                itemsValue.addChild(JNode.of(null, item));
            }else if(value.isArray()){
                // items: [copper/2] -> items: {items: [copper/2]}
                value.setType(Jtype.object);
                JNode itemsValue = JNode.array();
                value.moveChildrenTo(itemsValue);
                value.addChild("items", itemsValue);
            }
        }else if(type == ConsumeLiquids.class){
            if(value.isArray()){
                // liquids: [water/0.1] -> liquids: {liquids: [water/0.1]}
                value.setType(Jtype.object);
                JNode liquidsValue = JNode.array();
                value.moveChildrenTo(liquidsValue);
                value.addChild("liquids", liquidsValue);
            }
        }else if(type == ConsumePower.class){
            if(value.isNumber()){
                // power: 10 -> power: {usage: 10}
                float num = value.asFloat();
                value.setType(Jtype.object);
                value.addChild(JNode.of("usage", num));
            }
        }else if(value.isArray()){
            /* object: [{}] -> object: { type: MultiXXX, objects: [{}]}*/
            if(type == Effect.class){
                /* to MultiEffect */
                value.setType(Jtype.object);
                JNode elementValue = JNode.array();
                value.moveChildrenTo(elementValue);

                value.addChild(JNode.of("type", PatchJsonIO.getTypeName(MultiEffect.class)));
                value.addChild("effects", elementValue);
            }else if(type == BulletType.class){
                /* to MultiBulletType */
                value.setType(Jtype.object);
                JNode elementValue = JNode.array();
                value.moveChildrenTo(elementValue);

                value.addChild(JNode.of("type", PatchJsonIO.getTypeName(MultiBulletType.class)));
                value.addChild("bullets", elementValue);
            }else if(type == DrawBlock.class){
                /* to DrawMulti */
                value.setType(Jtype.object);
                JNode elementValue = JNode.array();
                value.moveChildrenTo(elementValue);

                value.addChild(JNode.of("type", PatchJsonIO.getTypeName(DrawMulti.class)));
                value.addChild("drawers", elementValue);
            }else if(type == Rect.class && value.size() == 4){
                toObject(value, "x", "y", "width", "height");
            }else if(type == Vec3.class && value.size() == 3){
                toObject(value, "x", "y", "z");
            }
        }
    }

    /** Array to object: keys the children in order, e.g. a Rect quad becomes x/y/width/height. */
    private static void toObject(JNode value, String... keys){
        value.setType(Jtype.object);

        Seq<JNode> children = value.children();
        for(int i = 0; i < children.size && i < keys.length; i++){
            children.get(i).setName(keys[i]);
        }
    }

    /** jsonTree to patchJsonTree. */
    public static JNode processJson(ObjectNode objectNode, JNode value){
        return processJson(objectNode, value, true);
    }

    public static JNode processJson(ObjectNode objectNode, JNode value, boolean flattenMultiArray){
        if(value.isValue()) return value;

        for(JNode child : value){
            ObjectNode childNode = child.name != null ? objectNode.getOrResolve(child.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType, objectNode.getResolutionStrategy());
            if(childNode != null) processJson(childNode, child, flattenMultiArray);
        }

        Seq<JNode> result = new Seq<>();
        for(JNode childValue : value){
            ObjectNode childNode = childValue.name != null ? objectNode.getOrResolve(childValue.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType, objectNode.getResolutionStrategy());

            if(childNode == null){
                result.add(childValue);
                continue;
            }

            if(flattenMultiArray && childNode.isMultiArrayLike()){
                for(JNode indexValue : childValue.detachChildren()){
                    indexValue.setName(childValue.name + NodeManager.pathComp + indexValue.name);
                    result.add(indexValue);
                }
            }else if(flattenMultiArray && childNode.isArrayLike() && childValue.has(ModifierSign.PLUS.sign)){
                JNode plusValue = childValue.remove(ModifierSign.PLUS.sign);
                if(!childValue.isEmpty()){
                    result.add(childValue);
                }
                plusValue.setName(childValue.name + NodeManager.pathComp + plusValue.name);
                result.add(plusValue);
            }else if(childNode.type == Consume.class && childNode.name.equals("remove")){
                Seq<String> names = new Seq<>();
                for(JNode child : childValue){
                    names.add(child.name);
                }

                childValue.setType(Jtype.array);
                Seq<JNode> removed = childValue.children();
                for(int i = 0; i < removed.size && i < names.size; i++){
                    removed.get(i).set(names.get(i));
                }
                result.insert(0, childValue);
            }else{
                result.add(childValue);
            }
        }

        value.setChildren(result);
        return value;
    }

    public static void sugarPatch(ObjectNode objectNode, JNode value, SugarJsonConfig config){
        if(objectNode == null || config == null) return;

        if(value.isObject()){
            Class<?> type = objectNode.type;
            if(config.sugarStacks && (type == ItemStack.class || type == PayloadStack.class)){
                if(value.has("item") && value.has("amount")){
                    value.set(value.get("item").asString() + "/" + value.get("amount").asString());
                    return;
                }
            }else if(config.sugarStacks && type == LiquidStack.class){
                if(value.has("liquid") && value.has("amount")){
                    value.set(value.get("liquid").asString() + "/" + value.get("amount").asString());
                    return;
                }
            }else if(type == Rect.class && value.size() == 4){
                if(value.has("x") && value.has("y") && value.has("width") && value.has("height")){
                    value.setType(Jtype.array);
                    return;
                }
            }else if(type == Vec3.class && value.size() == 3){
                if(value.has("x") && value.has("y") && value.has("z")){
                    value.setType(Jtype.array);
                    return;
                }
            }
        }

        if(value.isValue()) return;

        for(JNode childValue : value){
            ObjectNode childNode = childValue.name != null ? objectNode.getOrResolve(childValue.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType, objectNode.getResolutionStrategy());
            sugarPatch(childNode, childValue, config);
        }
    }

    public static void simplifyPath(JNode value){
        if(value == null) return;

        for(JNode childValue : value){
            simplifyPath(childValue);
        }

        if(dotSimplifiable(value)){
            int singleCount = 1;
            JNode singleEnd = value;
            while(singleEnd.size() == 1 && dotSimplifiable(singleEnd.first())){
                singleEnd = singleEnd.first();
                singleCount++;
            }

            if(singleCount >= PatchJsonIO.simplifySingleCount){
                StringBuilder name = new StringBuilder();
                JNode current = value;
                while(current.val != singleEnd.val){
                    name.append(current.name).append(NodeManager.pathComp);
                    current = current.first();
                }
                name.append(singleEnd.name);

                value.set(singleEnd.val);
                value.setName(name.toString());
            }
        }
    }

    private static boolean dotSimplifiable(JNode node){
        if(node.isArray() || node.has("type")) return false;
        JNode parent = node.parent;
        return parent != null && !parent.isArray() && !"consumes".equals(parent.name);
    }

    public static void clearRedundant(ObjectNode objectNode, PatchNode patchNode){
        ObjectSet<PatchNode> toRemoved = new ObjectSet<>();
        getRedundant(objectNode, patchNode, toRemoved);

        if(!toRemoved.isEmpty()){
            for(PatchNode childNode : toRemoved){
                PatchNode parent = childNode.getParent();
                childNode.remove();
                cleanEmptyParents(parent); // childNode must be a leaf node so parent won't be null.
            }
        }
    }

    private static void getRedundant(ObjectNode objectNode, PatchNode patchNode, ObjectSet<PatchNode> out){
        if(patchNode == null || objectNode == null) return;

        PatchNode typeNode = patchNode.getOrNull("type");
        if(typeNode != null && typeNode.value != null){
            Class<?> typeOverride = PatchJsonIO.resolveType(typeNode.value);
            if(typeOverride != null && objectNode.type != typeOverride && objectNode.type.isAssignableFrom(typeOverride)){
                objectNode = ObjectResolver.getTemplate(typeOverride, objectNode.getResolutionStrategy());
            }
        }

        for(PatchNode childNode : patchNode.children.values()){
            ObjectNode childObj = objectNode.getOrResolve(childNode.key);
            if(childObj == null && objectNode.elementType != null){
                childObj = ObjectResolver.getTemplate(objectNode.elementType, objectNode.getResolutionStrategy());
            }

            // see array and value node as leaf nodes
            if(childNode.type == Jtype.object){
                getRedundant(childObj, childNode, out);
            }else if(isRedundantPatch(childObj, childNode)){
                out.add(childNode);
            }
        }
    }

    private static boolean isRedundantPatch(ObjectNode objectNode, PatchNode patchNode){
        if(objectNode == null || patchNode.type == Jtype.object) return false;
        if(patchNode.type != Jtype.array && (patchNode.value == null || patchNode.sign != null)) return false;

        Object original = objectNode.object;
        if(original instanceof MapEntry<?,?> entry) original = entry.value;

        Object parsed = PatchJsonIO.parseJsonObject(patchNode, objectNode, original);
        return PatchCompare.equalsValue(parsed, original, objectNode.type);
    }

    public static void cleanEmptyParents(PatchNode patchNode){
        PatchNode current = patchNode;
        while(current != null && current.children.isEmpty() && current.sign == null){
            PatchNode parent = current.getParent();
            current.remove();
            current = parent;
        }
    }

    public static JNode migrateTweaker(String patch){
        return migrateTweaker(JNode.of(Jval.read(patch)));
    }

    private static JNode migrateTweaker(JNode json){
        if(json.isValue()) return json;

        for(JNode childValue : json){
            if(childValue.name != null){
                if(childValue.name.startsWith("#")){
                    String key = childValue.name.substring(1);
                    childValue.setName(key);

                    if(json.isObject() && Strings.canParsePositiveInt(key)){
                        json.setType(Jtype.array);
                    }
                }else if(childValue.isValue() && childValue.name.equals("=")){
                    json.set(childValue.asString());
                    json.setType(childValue.type());
                    break;
                }
            }

            migrateTweaker(childValue);
        }
        return json;
    }
}
