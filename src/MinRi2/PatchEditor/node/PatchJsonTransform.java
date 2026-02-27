package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.node.patch.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.Jval.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;

public class PatchJsonTransform{

    /** jsonTree to patchJsonTree. */
    public static JsonValue processJson(ObjectNode objectNode, JsonValue value){
        if(value.isValue()) return value;

        for(JsonValue child : value){
            ObjectNode childNode = child.name != null ? objectNode.getOrResolve(child.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType);
            if(childNode != null) processJson(childNode, child);
        }

        Seq<JsonValue> result = new Seq<>();
        for(JsonValue childValue : value){
            ObjectNode childNode = childValue.name != null ? objectNode.getOrResolve(childValue.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType);

            if(childNode == null){
                result.add(childValue);
                continue;
            }

            if(childNode.isMultiArrayLike()){
                for(JsonValue indexValue : childValue){
                    JsonHelper.remove(indexValue);
                    indexValue.setName(childValue.name + "." + indexValue.name);
                    result.add(indexValue);
                }
            }else if(childNode.isArrayLike() && childValue.has(ModifierSign.PLUS.sign)){
                JsonValue plusValue = childValue.remove(ModifierSign.PLUS.sign);
                if(childValue.child != null){
                    result.add(childValue);
                }
                plusValue.setName(childValue.name + "." + plusValue.name);
                result.add(plusValue);
            }else if(childNode.type == Consume.class && childNode.name.equals("remove")){
                // remove: {item: -, liquid: -} -> remove: [item, liquid]
                childValue.setType(ValueType.array);
                for(JsonValue removed : childValue){
                    removed.set(removed.name());
                }
                result.insert(0, childValue);
            }else{
                result.add(childValue);
            }
        }

        // mount again
        value.child = result.size > 0 ? result.get(0) : null;
        value.size = result.size;
        JsonValue prev = null;
        for(JsonValue jsonValue : result){
            jsonValue.parent = value;
            jsonValue.prev = prev;
            jsonValue.next = null;
            if(prev != null) prev.next = jsonValue;

            prev = jsonValue;
        }

        return value;
    }

    public static void extractDotSyntax(JsonValue value){
        if(value.name != null && value.parent != null && value.name.contains(NodeManager.pathComp)){
            String[] names = value.name.split(NodeManager.pathSplitter);

            int i = 0;
            JsonValue currentParent = new JsonValue(ValueType.object);
            currentParent.setName(names[i++]);
            JsonHelper.replace(value, currentParent); // don't affect the order

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

            if(ClassHelper.isArrayLike(objectNode.type)){
                // "requirements": ["item/amount"] | {+: [], {"item": "xxx"}}
                ObjectNode childNode = ObjectResolver.getTemplate(objectNode.elementType);
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
            value.addChild("item", new JsonValue(split[0]));
            value.addChild("amount", new JsonValue(split[1]));
        }else if(type == LiquidStack.class || type == ConsumeLiquid.class){
            if(!value.isString() || !value.asString().contains("/")) return;
            String[] split = value.asString().split("/");
            value.setType(ValueType.object);
            value.addChild("liquid", new JsonValue(split[0]));
            value.addChild("amount", new JsonValue(split[1]));
        }else if(type == Consume.class){
            if(value.name.equals("remove")){
                if(value.isString()){
                    // remove: item -> remove: [item]
                    String removed = value.asString();
                    value.setType(ValueType.array);
                    value.addChild("", new JsonValue(removed));
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
        }if(type == ConsumeItems.class){
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
                JsonHelper.moveChild(value, itemsValue);
                value.addChild("items", itemsValue);
            }
        }else if(type == ConsumeLiquids.class){
            if(value.isArray()){
                // liquids: [water/0.1] -> liquids: {liquids: [water/0.1]}
                value.setType(ValueType.object);
                JsonValue liquidsValue = new JsonValue(ValueType.array);
                JsonHelper.moveChild(value, liquidsValue);
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
                JsonHelper.moveChild(value, elementValue);

                value.addChild("type", new JsonValue(PatchJsonIO.getClassTypeName(MultiEffect.class)));
                value.addChild("effects", elementValue);
            }else if(type == BulletType.class){
                /* to MultiBulletType */
                value.setType(ValueType.object);
                JsonValue elementValue = new JsonValue(ValueType.array);
                JsonHelper.moveChild(value, elementValue);

                value.addChild("type", new JsonValue(PatchJsonIO.getClassTypeName(MultiBulletType.class)));
                value.addChild("bullets", elementValue);
            }else if(type == DrawBlock.class){
                /* to DrawMulti */
                value.setType(ValueType.object);
                JsonValue elementValue = new JsonValue(ValueType.array);
                JsonHelper.moveChild(value, elementValue);

                value.addChild("type", new JsonValue(PatchJsonIO.getClassTypeName(DrawMulti.class)));
                value.addChild("drawers", elementValue);
            }
        }
    }

    // fold path with dot syntax
    public static void simplifyPath(JsonValue value){
        if(value.parent == null){
            for(JsonValue childValue : value){
                simplifyPath(childValue);
            }
            return;
        }

        int singleCount = 1;
        JsonValue singleEnd = value;
        while(singleEnd.child != null && singleEnd.child.next == null && singleEnd.child.prev == null){
            if(!dotSimplifiable(singleEnd)) break;
            singleEnd = singleEnd.child;
            singleCount++;
        }

        if(singleCount >= PatchJsonIO.simplifySingleCount){
            StringBuilder name = new StringBuilder();
            JsonValue current = value;
            while(true){
                name.append(current.name);
                current = current.child;
                if(current != singleEnd.child) name.append("."); // dot syntax
                else break;
            }

            singleEnd.setName(name.toString());
            JsonHelper.replace(value, singleEnd);
            value = singleEnd;
        }

        for(JsonValue childValue : value){
            simplifyPath(childValue);
        }
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
            }
        }

        if(value.isValue()) return;

        for(JsonValue childValue : value){
            ObjectNode childNode = childValue.name != null ? objectNode.getOrResolve(childValue.name) : null;
            if(childNode == null && objectNode.elementType != null) childNode = ObjectResolver.getTemplate(objectNode.elementType);
            sugarPatch(childNode, childValue, config);
        }
    }

    private static boolean dotSimplifiable(JsonValue singleEnd){
        return !(singleEnd.isArray() || singleEnd.has("type") || singleEnd.name.equals("consumes"));
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

    public static class SugarJsonConfig{
        public boolean sugarStacks = true;
    }
}
