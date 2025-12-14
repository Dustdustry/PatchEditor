package MinRi2.ContentsEditor.node;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.mod.*;

/**
 * Store the json nodes with untree-structured data.
 * @author minri2
 * Create by 2024/2/15
 */
public class NodeData{
    private static NodeData rootData;

    public int depth;

    public final String name;

    /** Original object */
    private final Object object;
    public final @Nullable FieldData meta;

    /** untree-structured，Use {@link PatchJsonIO#toJson(NodeData)} to transform to tree data */
    private JsonValue jsonData;

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

        isSign = Structs.contains(ModifierSign.all, this::isSign);
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

    public boolean isDynamic(){
        return parentData != null && parentData.isSign(ModifierSign.PLUS);
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

    public NodeData addChild(String name, FieldData meta){
        return addChild(name, null, meta);
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

    public JsonValue getJsonData(){
        return jsonData;
    }

    public boolean hasSign(ModifierSign sign){
        return getSign(sign) != null;
    }

    public NodeData getSign(ModifierSign sign){
        return getChild(sign.sign);
    }

    public void initJsonData(){
        if(jsonData != null) return;

        ValueType type = PatchJsonIO.isArrayLike(this) ? ValueType.array : ValueType.object;
        jsonData = new JsonValue(type);
        if(!isRoot()) jsonData.setName(name);

        if(parentData != null && parentData.jsonData == null) parentData.initJsonData();
    }

    public void clearJson(){
        jsonData = null;

        if(parentData != null && !parentData.isDynamic() && !parentData.children.contains(c -> c.jsonData != null)){
            parentData.clearJson();
        }

        if(isDynamic()){
            remove();
        }else if(isSign){
            for(NodeData child : children){
                child.parentData = null;
            }
            children.clear();
            resolved = false;
        }else{
            for(NodeData child : children){
                if(child.jsonData != null) child.clearJson();
            }
        }
    }

    public void remove(){
        if(parentData == null) return;
        parentData.getChildren().remove(this, true);
        parentData = null;
    }

    public void setJsonData(JsonValue value){
        if(value == null){
            jsonData = null;
            return;
        }

        initJsonData();

        if(value.isValue()){
            jsonData.setName(value.name);
            jsonData.set(value.asString());
            return;
        }

        setJsonData(value.name, value.type());
    }

    public void setJsonData(String name, ValueType type){
        if(jsonData != null){
            jsonData.setName(name);
            jsonData.setType(type);
        }
    }

    public boolean hasJsonChild(String name){
        if(jsonData == null) return false;
        NodeData child = getChild(name);
        return child != null && child.jsonData != null;
    }

    @Override
    public String toString(){
        return "NodeData{" +
        "name='" + name + '\'' +
        '}';
    }
}