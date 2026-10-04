package dustdustry.patcheditor.core;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;

import java.util.*;

public class JNode implements Iterable<JNode>{
    /** Underlying value; swapped when this node changes its type or value. */
    public Jval val;
    /** Key inside the parent object, or the index of the parent array. Null when detached. */
    public @Nullable String name;
    /** Owning container, null when detached. */
    public @Nullable JNode parent;

    private @Nullable NodeIterable iterable;

    protected JNode(@Nullable String name, Jval val, @Nullable JNode parent){
        this.name = name;
        this.val = val == null ? Jval.NULL : val;
        this.parent = parent;
    }

    public static JNode of(Jval val){
        return new JNode(null, val, null);
    }

    public static JNode of(String name, Jval val){
        return new JNode(name, val, null);
    }

    public static JNode object(){
        return object(null);
    }

    public static JNode object(String name){
        return new JNode(name, Jval.newObject(), null);
    }

    public static JNode array(){
        return array(null);
    }

    public static JNode array(String name){
        return new JNode(name, Jval.newArray(), null);
    }

    public static JNode of(String name, String value){
        return new JNode(name, Jval.valueOf(value), null);
    }

    public static JNode of(String name, double value){
        return new JNode(name, Jval.valueOf(value), null);
    }

    public static JNode of(String name, float value){
        return new JNode(name, Jval.valueOf(value), null);
    }

    public static JNode of(String name, long value){
        return new JNode(name, Jval.valueOf(value), null);
    }

    public static JNode of(String name, boolean value){
        return new JNode(name, Jval.valueOf(value), null);
    }

    /** Nameless value, named when it is added to a container. */
    public static JNode nullValue(){
        return nullValue(null);
    }

    public static JNode nullValue(String name){
        return new JNode(name, Jval.NULL, null);
    }

    public Jtype type(){
        return val.getType();
    }

    public boolean isObject(){
        return val.isObject();
    }

    public boolean isArray(){
        return val.isArray();
    }

    public boolean isValue(){
        return !isObject() && !isArray();
    }

    public boolean isString(){
        return val.isString();
    }

    public boolean isNumber(){
        return val.isNumber();
    }

    public boolean isBoolean(){
        return val.isBoolean();
    }

    public boolean isNull(){
        return val.isNull();
    }

    /** Number of children; zero for anything that is not a container. */
    public int size(){
        if(val.isObject()) return val.asObject().size;
        if(val.isArray()) return val.asArray().size;
        return 0;
    }

    public boolean isEmpty(){
        return size() == 0;
    }

    /** First child in order, or null when this node has none. */
    public @Nullable JNode first(){
        if(val.isObject()){
            Jval.JsonMap map = val.asObject();
            return map.size == 0 ? null : new JNode(map.getKeyAt(0), map.getValueAt(0), this);
        }
        return get(0);
    }

    /** Element at the given index, or null when this node is not a container / the index is out of range. */
    public @Nullable JNode get(int index){
        if(val.isArray()){
            Seq<Jval> array = val.asArray();
            return index < 0 || index >= array.size ? null : new JNode(String.valueOf(index), array.get(index), this);
        }

        if(val.isObject()){
            Jval.JsonMap map = val.asObject();
            return index < 0 || index >= map.size ? null : new JNode(map.getKeyAt(index), map.getValueAt(index), this);
        }

        return null;
    }

    /** Position of this node inside its parent container, or -1 when it has none. */
    public int index(){
        if(parent == null) return -1;
        if(parent.val.isObject()) return name == null ? -1 : parent.val.asObject().indexOfKey(name);
        if(parent.val.isArray()) return parent.val.asArray().indexOf(val, true);
        return -1;
    }

    /** Field of an object (or array element, when the name looks like an index), or null. */
    public @Nullable JNode get(String key){
        if(key == null) return null;

        if(val.isObject()){
            Jval child = val.asObject().get(key);
            return child == null ? null : new JNode(key, child, this);
        }

        if(val.isArray()){
            int index = Strings.parseInt(key, -1);
            return index < 0 ? null : get(index);
        }

        return null;
    }

    public boolean has(String key){
        return get(key) != null;
    }

    /** The children, in order. Children of an array are named by their index. */
    public Seq<JNode> children(){
        Seq<JNode> out = new Seq<>();
        getChildren(out);
        return out;
    }

    /** Fills {@code out} with the children, in order. */
    void getChildren(Seq<JNode> out){
        out.clear();

        if(val.isObject()){
            Jval.JsonMap map = val.asObject();
            for(int i = 0; i < map.size; i++){
                out.add(new JNode(map.getKeyAt(i), map.getValueAt(i), this));
            }
        }else if(val.isArray()){
            Seq<Jval> array = val.asArray();
            for(int i = 0; i < array.size; i++){
                out.add(new JNode(String.valueOf(i), array.get(i), this));
            }
        }
    }

    public @Nullable String getString(String key){
        JNode child = get(key);
        return child == null ? null : child.asString();
    }

    /** Value text, in the shape of the removed arc JsonValue: booleans become "true"/"false", null stays null. */
    public @Nullable String asString(){
        if(isNull()) return null;
        if(isBoolean()) return Boolean.toString(val.asBool());
        if(isObject() || isArray()) throw new IllegalStateException("Not a value: " + val);
        return val.asString();
    }

    public float asFloat(){
        if(isBoolean()) return val.asBool() ? 1f : 0f;
        return val.asFloat();
    }

    /** Replaces this node's value, keeping its position in the parent. */
    public void set(Jval value){
        replaceValue(value == null ? Jval.NULL : value);
    }

    public void set(String value){
        replaceValue(Jval.valueOf(value));
    }

    public void set(double value){
        replaceValue(Jval.valueOf(value));
    }

    public void set(float value){
        replaceValue(Jval.valueOf(value));
    }

    public void set(long value){
        replaceValue(Jval.valueOf(value));
    }

    public void set(boolean value){
        replaceValue(Jval.valueOf(value));
    }

    /** Turns this node into a container of the given type, keeping the children in order. */
    public void setType(Jtype type){
        if(val.getType() == type) return;

        if(type == Jtype.object){
            Jval container = Jval.newObject();
            Jval.JsonMap map = container.asObject();
            for(JNode child : this){
                map.put(child.name, child.val);
            }
            replaceValue(container);
        }else if(type == Jtype.array){
            Jval container = Jval.newArray();
            Jval.JsonArray array = container.asArray();
            for(JNode child : this){
                array.add(child.val);
            }
            replaceValue(container);
        }
    }

    /** Renames this node, keeping its position inside an object parent. */
    public void setName(String newName){
        if(attached() && parent.val.isObject() && !name.equals(newName)){
            Jval.JsonMap map = parent.val.asObject();
            int index = map.indexOfKey(name);
            if(map.containsKey(newName)) map.removeKey(newName);
            map.removeKey(name);
            map.insert(Mathf.clamp(index == -1 ? map.size : index, 0, map.size), newName, val);
        }

        this.name = newName;
    }

    /** Appends a child; objects key it by name, arrays by position. */
    public JNode addChild(String childName, JNode child){
        if(val.isArray()){
            Jval.JsonArray array = val.asArray();
            array.add(child.val);
            child.attach(this, String.valueOf(array.size - 1));
        }else if(val.isObject()){
            val.asObject().put(childName, child.val);
            child.attach(this, childName);
        }else{
            throw new IllegalStateException("Not a container: " + val);
        }

        return child;
    }

    public JNode addChild(JNode child){
        return addChild(child.name, child);
    }

    /** Inserts a child at the given position, clamped to the container. */
    public JNode insertChild(int index, JNode child){
        if(val.isArray()){
            Jval.JsonArray array = val.asArray();
            int at = Mathf.clamp(index, 0, array.size);
            array.insert(at, child.val);
            child.attach(this, String.valueOf(at));
        }else if(val.isObject()){
            Jval.JsonMap map = val.asObject();
            int at = Mathf.clamp(index, 0, map.size);
            if(child.name != null && map.containsKey(child.name)) map.removeKey(child.name);
            map.insert(Mathf.clamp(at, 0, map.size), child.name, child.val);
            child.attach(this, child.name);
        }else{
            throw new IllegalStateException("Not a container: " + val);
        }

        return child;
    }

    public void remove(){
        if(!attached()){
            parent = null;
            return;
        }

        if(parent.val.isObject()){
            parent.val.asObject().removeKey(name);
        }else{
            parent.val.asArray().remove(parent.val.asArray().indexOf(val, true));
        }

        parent = null;
    }

    public @Nullable JNode remove(String key){
        JNode child = get(key);
        if(child != null) child.remove();
        return child;
    }

    /** Drops every child. Wrappers taken before stay inert, see {@link #attached()}. */
    public void clear(){
        if(val.isObject()) val.asObject().clear();
        else if(val.isArray()) val.asArray().clear();
    }

    /** Takes every child out of this node and returns them in order, detached from it. */
    public Seq<JNode> detachChildren(){
        Seq<JNode> out = children();
        clear();

        for(JNode child : out){
            child.parent = null;
        }

        return out;
    }

    /** Moves every child into another container, keeping their order. */
    public void moveChildrenTo(JNode target){
        if(target == null || size() == 0) return;

        for(JNode child : detachChildren()){
            target.addChild(child);
        }
    }

    /** Rebuilds the container from the given children, in order. */
    public void setChildren(Seq<JNode> children){
        if(isArray()){
            Jval.JsonArray array = val.asArray();
            array.clear();
            for(JNode child : children){
                array.add(child.val);
                child.attach(this, String.valueOf(array.size - 1));
            }
        }else if(isObject()){
            Jval.JsonMap map = val.asObject();
            map.clear();
            for(JNode child : children){
                map.put(child.name, child.val);
                child.attach(this, child.name);
            }
        }
    }

    void attach(JNode parent, String name){
        this.parent = parent;
        this.name = name;
    }

    /** True when the parent container still holds this node, under {@link #name} or by identity. */
    private boolean attached(){
        if(parent == null) return false;
        if(parent.val.isObject()) return name != null && parent.val.asObject().get(name) == val;
        if(parent.val.isArray()) return parent.val.asArray().indexOf(val, true) != -1;
        return false;
    }

    private void replaceValue(Jval newValue){
        boolean attached = attached();
        Jval old = val;
        val = newValue;

        if(!attached) return;

        if(parent.val.isObject()){
            parent.val.asObject().put(name, newValue);
        }else{
            parent.val.asArray().set(parent.val.asArray().indexOf(old, true), newValue);
        }
    }

    public String toJson(Jformat format){
        return val.toString(format);
    }

    /** Compact JSON. */
    public String toJson(){
        return val.toString(Jformat.plain);
    }

    @Override
    public String toString(){
        return val.toString(Jformat.plain);
    }

    /** Iterates a snapshot of the children, so a loop may remove or move them while it runs. */
    @Override
    public Iterator<JNode> iterator(){
        if(iterable == null) iterable = new NodeIterable(this);
        return iterable.iterator();
    }
    public static class NodeIterable implements Iterable<JNode>{
        private final JNode node;

        private final NodeIterator iterator1 = new NodeIterator(), iterator2 = new NodeIterator();

        public NodeIterable(JNode node){
            this.node = node;
        }

        @Override
        public Iterator<JNode> iterator(){
            if(iterator1.done){
                iterator1.reset();
                return iterator1;
            }

            if(iterator2.done){
                iterator2.reset();
                return iterator2;
            }

            NodeIterator fresh = new NodeIterator();
            fresh.reset();
            return fresh;
        }
        private class NodeIterator implements Iterator<JNode>{
            private final Seq<JNode> children = new Seq<>();
            int index;

            boolean done = true;

            void reset(){
                node.getChildren(children);
                index = 0;
                done = false;
            }

            @Override
            public boolean hasNext(){
                if(index >= children.size){
                    done = true;
                    children.clear(); //release the snapshot as soon as the loop is done
                }
                return index < children.size;
            }

            @Override
            public JNode next(){
                if(index >= children.size) throw new NoSuchElementException(String.valueOf(index));
                return children.get(index++);
            }
            @Override
            public void remove(){
                if(index <= 0 || index > children.size) throw new IllegalStateException("next must be called before remove");

                int at = --index;
                children.get(at).remove();
                children.remove(at);

                if(node.isArray()){
                    for(int i = at; i < children.size; i++){
                        children.get(i).name = String.valueOf(i);
                    }
                }
            }
        }

    }
}
