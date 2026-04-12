package MinRi2.PatchEditor.node;

import MinRi2.PatchEditor.node.patch.*;
import MinRi2.PatchEditor.utils.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Json.*;
import mindustry.mod.*;

import java.lang.reflect.*;

public class ObjectNode{
    public final String name;
    public final @Nullable Object object;
    public final @Nullable Field field;
    public final @Nullable Class<?> type, elementType, keyType;

    private boolean isRoot;

    private ObjectNode parent;
    private boolean resolved = false;
    private final OrderedMap<String, ObjectNode> children = new OrderedMap<>();

    public ObjectNode(String name, Object object, Class<?> type){
        this(name, object, type, null, null);
    }

    public ObjectNode(String name, Object object, Class<?> type, Class<?> elementType, Class<?> keyType){
        this(name, object, null, type, elementType, keyType);
    }

    public ObjectNode(String name, Object object, FieldMetadata meta){
        this(name, object, meta.field, meta.field.getType(), meta.elementType, meta.keyType);
    }

    public ObjectNode(String name, Object object, Field field, Class<?> type, Class<?> elementType, Class<?> keyType){
        this.name = name;
        this.object = object;
        this.field = field;

        this.type = ClassHelper.unoymousClass(type);
        this.keyType = keyType;

        // fix FieldMetaData
        if(type != null && type.isArray() && elementType == null){
            elementType = type.getComponentType();
        }

        this.elementType = elementType;
    }

    public static ObjectNode createRoot(){
        ObjectNode node = new ObjectNode("root", Reflect.get(DataPatcher.class, "root"), Object.class);
        node.isRoot = true;
        return node;
    }

    public ObjectNode getParent(){
        return parent;
    }

    // Signs are assigned by parent.
    public boolean hasSign(ModifierSign sign){
        return sign != null && children.containsKey(sign.sign);
    }

    public ObjectNode getOrResolve(String name){
        return getChildren().get(name);
    }

    public ObjectMap<String, ObjectNode> getChildren(){
        if(!resolved){
            ObjectResolver.resolve(this);
            resolved = true;
        }
        return children;
    }

    public boolean isSign(){
        for(ModifierSign sign : ModifierSign.values()){
            if(sign.sign.equals(name)) return true;
        }
        return false;
    }

    public boolean isRoot(){
        return isRoot;
    }

    public boolean isArrayLike(){
        return ClassHelper.isArrayLike(type);
    }

    public boolean isMultiArrayLike(){
        return ClassHelper.isArrayLike(type) && ClassHelper.isArrayLike(elementType);
    }

    public boolean isDescendantArray(){
        if(!isArrayLike()) return false;
        ObjectNode parent = getParent();
        while(parent != null){
            if(parent.isArrayLike()) return true;
            parent = parent.getParent();
        }
        return false;
    }

    public ObjectNode addSign(ModifierSign sign){
        // extend type from parent
        return addSign(sign, type, elementType, keyType);
    }

    public ObjectNode navigate(String path){
        if(path == null || path.isEmpty()) return this;

        ObjectNode current = this;
        int start = 0;
        while(true){
            int dot = path.indexOf(NodeManager.pathComp, start);
            String name = dot == -1 ? path.substring(start) : path.substring(start, dot);
            current = current.getChildren().get(name);
            if(current == null || dot == -1) return current;
            start = dot + 1;
        }
    }

    public ObjectNode addSign(ModifierSign sign, Class<?> type, Class<?> elementType, Class<?> keyType){
        return addChild(sign.sign, null, type, elementType, keyType);
    }

    public ObjectNode addChild(String name, Object object){
        return addChild(name, object, object == null ? null : object.getClass(), null, null);
    }

    public ObjectNode addChild(String name, Object object, FieldMetadata metadata){
        return addChild(new ObjectNode(name, object, metadata));
    }

    public ObjectNode addChild(String name, Object object, Class<?> type){
        return addChild(name, object, type, null, null);
    }

    public ObjectNode addChild(String name, Object object, Class<?> type, Class<?> elementType, Class<?> keyType){
        return addChild(new ObjectNode(name, object, type, elementType, keyType));
    }

    public ObjectNode addChild(ObjectNode child){
        children.put(child.name, child);
        child.parent = this;
        return child;
    }

    @Override
    public String toString(){
        return "ObjectNode{" +
        "name='" + name + '\'' +
        ", type=" + type +
        '}';
    }
}
