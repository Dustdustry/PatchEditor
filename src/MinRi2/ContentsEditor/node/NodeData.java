package MinRi2.ContentsEditor.node;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.mod.*;

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
    private final Seq<NodeData> children = new Seq<>();

    private final boolean isSign;
    private boolean resolved;

    private NodeData(String name, Object object){
        this(name, object, null);
    }

    private NodeData(String name, Object object, FieldData meta){
        this.name = name;
        this.object = object;
        this.meta = meta;

        isSign = Structs.contains(ModifierSign.all, sign -> sign.sign.equals(name));
    }

    public static NodeData getRootData(){
        if(rootData == null){
            rootData = new NodeData("root", Reflect.get(ContentPatcher.class, "root"));
            rootData.initJsonData();
        }
        return rootData;
    }

    public boolean isRoot(){
        return this == rootData;
    }

    public boolean isSign(){
        return isSign;
    }

    public Seq<NodeData> getChildren(){
        if(!resolved){
            NodeResolver.resolveFrom(this, getObject());
            resolved = true;
        }
        return children;
    }

    public NodeData getChild(String name){
        return getChildren().find(c -> c.name.equals(name));
    }

    public NodeData addChild(String name, Object object){
        return addChild(name, object, null);
    }

    public NodeData addChild(String name, Object object, FieldData meta){
        NodeData child = new NodeData(name, object, meta);
        children.add(child);
        child.parentData = this;
        child.depth = depth + 1;
        return child;
    }

    public Object getObject(){
        return object;
    }

    public boolean hasSign(){
        return Structs.contains(ModifierSign.all, this::hasSign);
    }

    public boolean hasSign(ModifierSign sign){
        return getSign(sign) != null;
    }

    public NodeData getSign(ModifierSign sign){
        return getChild(sign.sign);
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
        if(name.equals(ModifierSign.PLUS.sign) && PatchJsonIO.isArray(this)){
            type = ValueType.array;
        }

        data = new JsonValue(type);
        PatchJsonIO.addChildValue(jsonData, name, data);
        return data;
    }

    public void removeJson(String name){
        if(jsonData == null) return;
        jsonData.remove(name);

        // keep tree clean
        if(jsonData.child == null && parentData != null){
            parentData.removeJson(this.name);
            clearDynamicChildren();
            jsonData = null;
        }
    }

    public void clearJson(){
        clearDynamicChildren();

        for(var child : children){
            if(child.jsonData != null){
                child.clearJson();
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

    public void clearDynamicChildren(){
        if(jsonData != null && !jsonData.isValue() && jsonData.size > 0){
            for(var child : getChildren()){
                if(child.isSign()){
                    // clear sign's dynamic children
                    child.children.clear();
                }
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