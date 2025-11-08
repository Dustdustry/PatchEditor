package MinRi2.ContentsEditor.node;

import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class NodeData{
    private static NodeData rootData;

    public int depth;

    public final String name;
    private final Object object; // original
    public final @Nullable FieldData meta;
    public JsonValue jsonData;

    public @Nullable NodeData parentData;
    private final OrderedMap<String, NodeData> children = new OrderedMap<>();
    private boolean resolved;

    private NodeData(String name, Object object){
        this(name, object, null);
    }

    private NodeData(String name, Object object, FieldData meta){
        this.name = name;
        this.object = object;
        this.meta = meta;
    }

    public static NodeData getRootData(){
        if(rootData == null){
            rootData = new NodeData("root", NodeHelper.getRootObj());
            rootData.initJsonData();
        }
        return rootData;
    }

    public boolean isRoot(){
        return this == rootData;
    }

    public boolean isSign(){
        return Structs.contains(ModifierSign.all, sign -> sign.sign.equals(name));
    }

    public boolean hasSign(){
        return Structs.contains(ModifierSign.all, this::hasSign);
    }

    public boolean hasSign(ModifierSign sign){
        return getSign(sign) != null;
    }

    public NodeData getSign(ModifierSign sign){
        return getChildren().get(sign.sign);
    }

    public ObjectMap<String, NodeData> getChildren(){
        if(!resolved){
            NodeResolver.resolveFrom(this, getObject());
            resolved = true;
        }
        return children;
    }

    public NodeData addChild(String name, Object object){
        return addChild(name, object, null);
    }

    public NodeData addChild(String name, Object object, FieldData meta){
        NodeData child = new NodeData(name, object, meta);
        children.put(name, child);
        child.parentData = this;
        child.depth = depth + 1;
        return child;
    }

    public Object getObject(){
        return object;
    }

    public void initJsonData(){
        if(jsonData != null) return;
        if(parentData == null){
            jsonData = new JsonValue(ValueType.object);
        }else{
            parentData.initJsonData();
            jsonData = parentData.getJson(name);
        }
    }

    public JsonValue getJson(String name){
        initJsonData();

        JsonValue data = jsonData.get(name);
        if(data != null){
            return data;
        }

        ValueType type = ValueType.object;

        data = new JsonValue(type);
        addChildValue(jsonData, name, data);
        return data;
    }

    public void setJsonData(JsonValue value){
        jsonData = value;
        if(value == null || value.isValue()) return;

        for(JsonValue childValue : value){
            String childName = childValue.name;

            if(childName == null){
                if(childValue.isArray()){
                    // TODO
                }
                continue;
            }

            NodeData current = this;
            JsonValue currentValue = value;

            String[] childrenName = childName.split("\\.");
            for(String name : childrenName){
                NodeData childData = current.getChildren().get(name);
                if(childData == null){
                    Log.warn("Couldn't resolve @.@", current.name, name);
                    return;
                }

                currentValue = value.get(name);
                current = childData;
            }

            current.setJsonData(currentValue);
        }
    }

    public void removeJson(String name){
        if(jsonData == null) return;
        jsonData.remove(name);

        // keep tree clean
        if(jsonData.child == null && parentData != null){
            parentData.removeJson(this.name);
            jsonData = null;
        }
    }

    public void clearJson(){
        for(var entry : children){
            NodeData childNodeData = entry.value;

            if(childNodeData.jsonData != null){
                childNodeData.clearJson();
            }
        }

        if(parentData != null){
            parentData.removeJson(name);
            jsonData = null;
        }
    }

    public boolean hasJsonChild(String name){
        return jsonData != null && jsonData.has(name);
    }

    /**
     * Function 'addChild' doesn't set the child's previous jsonValue.
     * see {@link JsonValue}
     */
    public static void addChildValue(JsonValue jsonValue, String name, JsonValue childValue){
        childValue.name = name;
        childValue.parent = jsonValue;

        JsonValue current = jsonValue.child;
        if(current == null){
            jsonValue.child = childValue;
        }else{
            while(true){
                if(current.next == null){
                    current.next = childValue;
                    childValue.prev = current;
                    return;
                }
                current = current.next;
            }
        }
    }

    @Override
    public String toString(){
        return "NodeData{" +
        "name='" + name + '\'' +
        '}';
    }
}