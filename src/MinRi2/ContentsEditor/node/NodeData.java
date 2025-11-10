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

    public boolean isSign(ModifierSign sign){
        return sign.sign.equals(name);
    }

    public Seq<NodeData> getChildren(){
        if(!resolved){
            NodeResolver.resolveNode(this, getObject());
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
            jsonData = parentData.createChildData(name);
        }
    }

    public JsonValue createChildData(String name){
        initJsonData();

        ValueType type = ValueType.object;
        if(name.equals(ModifierSign.PLUS.sign) && PatchJsonIO.isArray(this)){
            type = ValueType.array;
        }

        JsonValue data = new JsonValue(type);
        PatchJsonIO.addChildValue(jsonData, name, data);
        return data;
    }

    public void setJsonData(JsonValue data){
        if(parentData == null){
            jsonData = data;
            return;
        }

        parentData.initJsonData();
        if(jsonData == null){
            PatchJsonIO.addChildValue(parentData.jsonData, name, data);
        }else{
            JsonValue old = jsonData;
            if(old.prev != null) old.prev.next = data;
            if(old.next != null) old.next.prev = data;
            jsonData = data;
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
        clearDynamicChildren();

        for(var child : children){
            child.clearDynamicChildren();
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
        if(isSign){
            children.clear();
        }
    }

    @Override
    public String toString(){
        return "NodeData{" +
        "name='" + name + '\'' +
        '}';
    }
}