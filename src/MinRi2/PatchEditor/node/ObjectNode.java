package MinRi2.PatchEditor.node;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Json.*;
import mindustry.mod.*;

import java.lang.reflect.*;

public class ObjectNode{
    private static ObjectNode rootNode;

    public final String name;
    public final @Nullable Object object;
    public final @Nullable Field field;
    public final @Nullable Class<?> type, elementType, keyType;

    private ObjectNode parent;
    private final Seq<ObjectNode> children = new Seq<>();
    private boolean resolved = false;

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

    public static ObjectNode getRoot(){
        if(rootNode == null) rootNode = new ObjectNode("root", Reflect.get(DataPatcher.class, "root"), Object.class);
        return rootNode;
    }

    public Seq<String> getObjectPath(){
        Seq<String> path = new Seq<>();

        // not include root
        ObjectNode current = this;
        while(current != rootNode){
            path.add(current.name);
            current = current.parent;
        }

        return path.reverse();
    }

    public ObjectNode getOrResolve(ModifierSign sign){
        return getChildren().find(node -> node.name.equals(sign.sign));
    }

    public ObjectNode getOrResolve(String name){
        return getChildren().find(node -> node.name.equals(name));
    }

    public Seq<ObjectNode> getChildren(){
        if(!resolved){
            ObjectResolver.resolve(this);
            resolved = true;
        }
        return children;
    }

    public ObjectNode getParent(){
        return parent;
    }

    public ObjectNode addSign(ModifierSign sign, Class<?> elementType, Class<?> keyType){
        return addChild(sign.sign, null, null, elementType, keyType);
    }

    public ObjectNode addChild(String name, Object object, Class<?> elementType, Class<?> keyType){
        Class<?> type = object != null ? object.getClass() : null;
        return addChild(name, object, type, elementType, keyType);
    }

    public ObjectNode addChild(String name, Object object, FieldMetadata metadata){
        return addChild(new ObjectNode(name, object, metadata));
    }

    public ObjectNode addChild(String name, Object object, Class<?> type, Class<?> elementType, Class<?> keyType){
        return addChild(new ObjectNode(name, object, type, elementType, keyType));
    }

    public ObjectNode addChild(ObjectNode child){
        children.add(child);
        child.parent = this;
        return child;
    }
}
