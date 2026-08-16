package dustdustry.patcheditor.core;

import arc.math.geom.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
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
    public static JsonValue toJsonValue(PatchNode patchNode){
        return toJsonValue(patchNode, new JsonValue(patchNode.type));
    }

    private static JsonValue toJsonValue(PatchNode patchNode, JsonValue value){
        value.setName(patchNode.key);
        if(patchNode.value != null){
            if(patchNode.type == ValueType.doubleValue || patchNode.type == ValueType.longValue){
                value.set(Double.parseDouble(patchNode.value), patchNode.value);
            }else{
                value.set(patchNode.value);
            }
        }

        int size = 0;
        JsonValue appendValue = null;
        for(PatchNode childNode : patchNode.children.values()){
            JsonValue childValue = new JsonValue(childNode.type);

            if(childNode.key.startsWith(PatchJsonIO.appendPrefix)){
                if(appendValue == null){
                    appendValue = new JsonValue(ValueType.array);
                    value.addChild(ModifierSign.PLUS.sign, appendValue);
                    size++;
                }

                appendValue.addChild(childValue.name, childValue);
            }else{
                value.addChild(childNode.key, childValue);
                size++;
            }

            toJsonValue(childNode, childValue);
        }

        value.size = size;
        return value;
    }

    public static void extractDotSyntax(JsonValue value){
        if(value.name != null && value.parent != null && value.name.contains(NodeManager.pathComp)){
            String[] names = value.name.split(NodeManager.pathSplitter);

            int i = 0;
            JsonValue currentParent = new JsonValue(ValueType.object);
            currentParent.setName(names[i++]);
            JsonValues.replace(value, currentParent); // don't affect the order

            while(i < names.length - 1){
                currentParent.addChild(names[i++], currentParent = new JsonValue(ValueType.object));
            }

            currentParent.addChild(names[i], value);
        }

        for(JsonValue childValue : value){
            extractDotSyntax(childValue);
        }
    }

    public static void desugarJson(ObjectNode objectNode, JsonValue value){
        boolean isValue = value.isValue();

        if(objectNode != null){
            desugarJson(value, objectNode.type);

            // desugarJson may change the value type so cache it
            if(isValue) return;

            if(ClassHelper.isArrayLike(objectNode.type) || objectNode.isSign(ModifierSign.PLUS)){
                // "requirements": ["item/amount"] | {+: [], {"item": "xxx"}}
                ObjectNode childNode = ObjectResolver.getTemplate(objectNode.elementType, objectNode.getResolutionStrategy());
                for(JsonValue childValue : value){
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

        for(JsonValue childValue : value){
            ObjectNode childNode = childValue.name == null || objectNode == null ? null : objectNode.getOrResolve(childValue.name);
            desugarJson(childNode, childValue);
        }
    }

    private static void desugarJson(JsonValue value, Class<?> type){
        // TODO: More sugar syntaxes support
        if(type == ItemStack.class || type == PayloadStack.class){
            if(!value.isString() || !value.asString().contains("/")) return;
            String[] split = value.asString().split("/");
            value.setType(ValueType.object);
            JsonValue amountValue = new JsonValue(split[1]);
            amountValue.setType(ValueType.doubleValue);
            value.addChild("item", new JsonValue(split[0]));
            value.addChild("amount", amountValue);
        }else if(type == LiquidStack.class || type == ConsumeLiquid.class){
            if(!value.isString() || !value.asString().contains("/")) return;
            String[] split = value.asString().split("/");
            value.setType(ValueType.object);
            JsonValue amountValue = new JsonValue(split[1]);
            amountValue.setType(ValueType.doubleValue);
            value.addChild("liquid", new JsonValue(split[0]));
            value.addChild("amount", amountValue);
        }else if(type == Consume.class && value.name != null){
            if(value.name.equals("remove")){
                if(value.isString()){
                    // remove: item -> remove: {item: -}
                    String removed = value.asString();
                    value.setType(ValueType.object);
                    value.addChild(removed, new JsonValue(ModifierSign.REMOVE.sign));
                }else if(value.isArray()){
                    // remove: [item, liquid] -> remove: {item: -, liquid: -}
                    value.setType(ValueType.object);
                    for(JsonValue child : value){
                        if(child.isString()){
                            child.setName(child.asString());
                            child.set(ModifierSign.REMOVE.sign);
                        }
                    }
                }
            }
        }else if(type == ConsumeItems.class){
            if(value.isString()){
                // items: copper/2 -> items: {items: [copper/2]}
                String item = value.asString();
                value.setType(ValueType.object);
                JsonValue itemsValue = new JsonValue(ValueType.array);
                value.addChild("items", itemsValue);
                itemsValue.addChild("", new JsonValue(item));
            }else if(value.isArray()){
                // items: [copper/2] -> items: {items: [copper/2]}
                value.setType(ValueType.object);
                JsonValue itemsValue = new JsonValue(ValueType.array);
                JsonValues.moveChild(value, itemsValue);
                value.addChild("items", itemsValue);
            }
        }else if(type == ConsumeLiquids.class){
            if(value.isArray()){
                // liquids: [water/0.1] -> liquids: {liquids: [water/0.1]}
                value.setType(ValueType.object);
                JsonValue liquidsValue = new JsonValue(ValueType.array);
                JsonValues.moveChild(value, liquidsValue);
                value.addChild("liquids", liquidsValue);
            }
        }else if(type == ConsumePower.class){
            if(value.isNumber()){
                // power: 10 -> power: {usage: 10}
                float num = value.asFloat();
                value.setType(ValueType.object);
                value.addChild("usage", new JsonValue(num));
            }
        }else if(value.isArray()){
            /* object: [{}] -> object: { type: MultiXXX, objects: [{}]}*/
            if(type == Effect.class){
                /* to MultiEffect */
                value.setType(ValueType.object);
                JsonValue elementValue = new JsonValue(ValueType.array);
                JsonValues.moveChild(value, elementValue);

                value.addChild("type", new JsonValue(PatchJsonIO.getTypeName(MultiEffect.class)));
                value.addChild("effects", elementValue);
            }else if(type == BulletType.class){
                /* to MultiBulletType */
                value.setType(ValueType.object);
                JsonValue elementValue = new JsonValue(ValueType.array);
                JsonValues.moveChild(value, elementValue);

                value.addChild("type", new JsonValue(PatchJsonIO.getTypeName(MultiBulletType.class)));
                value.addChild("bullets", elementValue);
            }else if(type == DrawBlock.class){
                /* to DrawMulti */
                value.setType(ValueType.object);
                JsonValue elementValue = new JsonValue(ValueType.array);
                JsonValues.moveChild(value, elementValue);

                value.addChild("type", new JsonValue(PatchJsonIO.getTypeName(DrawMulti.class)));
                value.addChild("drawers", elementValue);
            }else if(type == Rect.class && value.size == 4){
                value.setType(ValueType.object);
                value.get(0).setName("x");
                value.get(1).setName("y");
                value.get(2).setName("width");
                value.get(3).setName("height");
            }else if(type == Vec3.class && value.size == 3){
                value.setType(ValueType.object);
                value.get(0).setName("x");
                value.get(1).setName("y");
                value.get(2).setName("z");
            }
        }
    }

    /** jsonTree to patchJsonTree. */
    public static JsonValue processJson(ObjectNode objectNode, JsonValue value){
        return processJson(objectNode, value, true);
    }

    public static JsonValue processJson(ObjectNode objectNode, JsonValue value, boolean flattenMultiArray){
        if(value.isValue()) return value;

        for(JsonValue child : value){
            ObjectNode childNode = child.name != null ? objectNode.getOrResolve(child.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType, objectNode.getResolutionStrategy());
            if(childNode != null) processJson(childNode, child, flattenMultiArray);
        }

        Seq<JsonValue> result = new Seq<>();
        for(JsonValue childValue : value){
            ObjectNode childNode = childValue.name != null ? objectNode.getOrResolve(childValue.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType, objectNode.getResolutionStrategy());

            if(childNode == null){
                result.add(childValue);
                continue;
            }

            if(flattenMultiArray && childNode.isMultiArrayLike()){
                for(JsonValue indexValue : childValue){
                    JsonValues.remove(indexValue);
                    indexValue.setName(childValue.name + NodeManager.pathComp + indexValue.name);
                    result.add(indexValue);
                }
            }else if(flattenMultiArray && childNode.isArrayLike() && childValue.has(ModifierSign.PLUS.sign)){
                JsonValue plusValue = childValue.remove(ModifierSign.PLUS.sign);
                if(childValue.child != null){
                    result.add(childValue);
                }
                plusValue.setName(childValue.name + NodeManager.pathComp + plusValue.name);
                result.add(plusValue);
            }else if(childNode.type == Consume.class && childNode.name.equals("remove")){
                childValue.setType(ValueType.array);
                for(JsonValue removed : childValue){
                    removed.set(removed.name());
                }
                result.insert(0, childValue);
            }else{
                result.add(childValue);
            }
        }

        value.child = result.size > 0 ? result.get(0) : null;
        value.size = result.size;
        JsonValue prev = null;
        for(JsonValue jv : result){
            jv.parent = value;
            jv.prev = prev;
            jv.next = null;
            if(prev != null) prev.next = jv;

            prev = jv;
        }

        return value;
    }

    public static void sugarPatch(ObjectNode objectNode, JsonValue value, SugarJsonConfig config){
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
            }else if(type == Rect.class && value.size == 4){
                if(value.has("x") && value.has("y") && value.has("width") && value.has("height")){
                    value.setType(ValueType.array);
                    value.get("x").setName("0");
                    value.get("y").setName("1");
                    value.get("width").setName("2");
                    value.get("height").setName("3");
                }
            }else if(type == Vec3.class && value.size == 3){
                if(value.has("x") && value.has("y") && value.has("z")){
                    value.setType(ValueType.array);
                    value.get("x").setName("0");
                    value.get("y").setName("1");
                    value.get("z").setName("2");
                }
            }
        }

        if(value.isValue()) return;

        for(JsonValue childValue : value){
            ObjectNode childNode = childValue.name != null ? objectNode.getOrResolve(childValue.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType, objectNode.getResolutionStrategy());
            sugarPatch(childNode, childValue, config);
        }
    }

    public static void simplifyPath(JsonValue value){
        if(value == null) return;

        for(JsonValue childValue : value){
            simplifyPath(childValue);
        }

        if(dotSimplifiable(value)){
            int singleCount = 1;
            JsonValue singleEnd = value;
            while(singleEnd.child != null && singleEnd.child.next == null && singleEnd.child.prev == null && dotSimplifiable(singleEnd.child)){
                singleEnd = singleEnd.child;
                singleCount++;
            }

            if(singleCount >= PatchJsonIO.simplifySingleCount){
                StringBuilder name = new StringBuilder();
                JsonValue current = value;
                while(current != singleEnd){
                    name.append(current.name).append(NodeManager.pathComp);
                    current = current.child;
                }
                name.append(singleEnd.name);

                singleEnd.setName(name.toString());
                JsonValues.replace(value, singleEnd);
            }
        }
    }

    private static boolean dotSimplifiable(JsonValue node){
        if(node.isArray() || node.has("type")) return false;
        JsonValue parent = node.parent;
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
            if(childNode.type == ValueType.object){
                getRedundant(childObj, childNode, out);
            }else if(isRedundantPatch(childObj, childNode)){
                out.add(childNode);
            }
        }
    }

    private static boolean isRedundantPatch(ObjectNode objectNode, PatchNode patchNode){
        if(objectNode == null || patchNode.type == ValueType.object) return false;
        if(patchNode.type != ValueType.array && (patchNode.value == null || patchNode.sign != null)) return false;

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

    public static JsonValue migrateTweaker(String patch){
        JsonValue tweakerJson = PatchJsonIO.getParser().getJson().fromJson(null, Jval.read(patch).toString(Jformat.plain));
        return migrateTweaker(tweakerJson);
    }

    private static JsonValue migrateTweaker(JsonValue json){
        if(json.isValue()) return json;

        for(JsonValue childValue : json){
            if(childValue.name != null){
                if(childValue.name.startsWith("#")){
                    String key = childValue.name.substring(1);
                    childValue.setName(key);

                    if(json.isObject() && Strings.canParsePositiveInt(key)){
                        json.setType(ValueType.array);
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
