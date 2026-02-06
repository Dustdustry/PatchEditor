package MinRi2.PatchEditor.node;

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

    public boolean hasSign(ModifierSign sign){
        return getOrResolve(sign.sign) != null;
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

    public ObjectNode addSign(ModifierSign sign){
        // extend type from parent
        return addSign(sign, type, elementType, keyType);
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
