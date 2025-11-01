package MinRi2.ContentsEditor.node;

import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.JsonValue.*;
import mindustry.*;
import mindustry.ctype.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class NodeData{
    private static NodeData rootData;

    public int depth;

    public final String name;
    public final Object object;
    public final @Nullable FieldMetadata meta;
    public JsonValue jsonData;

    public @Nullable NodeData parentData;
    private final OrderedMap<String, NodeData> children = new OrderedMap<>();
    private final OrderedMap<String, NodeData> dynamicChildren = new OrderedMap<>();

    private NodeData(String name, Object object){
        this(name, object, null);
    }

    private NodeData(String name, Object object, FieldMetadata meta){
        this.name = name;
        this.object = object;
        this.meta = meta;
    }

    public static NodeData getRootData(){
        if(rootData == null){
            rootData = new NodeData("root", NodeHelper.getRootObj(), null);
            rootData.initJsonData();
        }
        return rootData;
    }

    public boolean isRoot(){
        return this == rootData;
    }

    public ObjectMap<String, NodeData> getChildren(){
        if(children.isEmpty()) resolve(object);
        return children;
    }

    private void addChild(NodeData node){
        children.put(node.name, node);
        node.parentData = this;
        node.depth = depth + 1;
    }

    private void resolve(Object object){
        if(object == null) return; // ignore null?

        if(object == NodeHelper.getRootObj()){
            addChild(new NodeData("name", "root"));
            var map = NodeHelper.getNameToType();
            for(ContentType type : ContentType.all){
                if(map.containsValue(type, true)){
                    addChild(new NodeData(type.toString().toLowerCase(Locale.ROOT), type));
                }
            }
        }else if(object instanceof Object[] arr){
            int i = 0;
            for(Object o : arr){
                String name = "" + i++;
                addChild(new NodeData(name, o));
            }
        }else if(object instanceof Seq<?> seq){
            for(int i = 0; i < seq.size; i++){
                String name = "" + i++;
                addChild(new NodeData(name, seq.get(i)));
            }
        }else if(object instanceof ObjectSet<?> set){
            int i = 0;
            for(Object o : set){
                String name = "" + i++;
                addChild(new NodeData(name, o));
            }
        }else if(object instanceof ObjectMap<?, ?> map){
            for(var entry : map){
                String name = NodeHelper.getKeyName(entry.key);
                addChild(new NodeData(name, entry.value));
            }
        }else if(object instanceof ContentType ctype){
            OrderedMap<String, Content> map = new OrderedMap<>(); // in order
            for(Content content : Vars.content.getBy(ctype)){
                map.put(NodeHelper.getKeyName(content), content);
            }
            resolve(map);
        }else{
            for(var entry : NodeHelper.getFields(object.getClass())){
                String name = entry.key;
                Field field = entry.value.field;
                if(!NodeHelper.fieldEditable(field)) continue;
                Object childObj = Reflect.get(object, field);
                addChild(new NodeData(name, childObj, entry.value));
            }
        }
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

        if(value == null || value.isValue()){
            return;
        }

        for(JsonValue childValue : value){
            String childName = childValue.name;

            if(childName == null){
                if(childValue.isArray()){

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
                    break;
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