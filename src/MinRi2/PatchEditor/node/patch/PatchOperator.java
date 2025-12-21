package MinRi2.PatchEditor.node.patch;

import MinRi2.PatchEditor.node.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;

public abstract class PatchOperator{
    public final String path;

    public PatchOperator(String path){
        this.path = path;
    }

    public abstract void apply(PatchNode root);
    public abstract void undo(PatchNode root);

    public static class SetOp extends PatchOperator{
        public String value;

        public SetOp(String path, String value){
            super(path);

            this.value = value;
        }

        @Override
        public void apply(PatchNode root){
            PatchNode node = root.navigateChild(path, true);
            node.value = value;
        }

        @Override
        public void undo(PatchNode root){

        }
    }

    public static class ClearOp extends PatchOperator{

        public ClearOp(String path){
            super(path);
        }

        @Override
        public void apply(PatchNode root){
            PatchNode node = root.navigateChild(path, false);
            if(node == null) return;

            PatchNode parent = node.getParent();
            node.remove();

            PatchNode current = parent;
            while(current != null && current.children.isEmpty() && current.sign == null){
                parent = current.getParent();
                current.remove();
                current = parent;
            }
        }

        @Override
        public void undo(PatchNode root){

        }
    }

    public static class ArrayAppendOp extends PatchOperator{
        public final boolean appendPrefix;

        public ArrayAppendOp(String path, boolean appendPrefix){
            super(path);
            this.appendPrefix = appendPrefix;
        }

        public void apply(PatchNode root) {
            PatchNode node = root.navigateChild(path, true);
            String prefix = appendPrefix ? PatchJsonIO.appendPrefix : "";
            PatchNode plusNode = node.getOrCreate(findKey(prefix, node));
            plusNode.sign = ModifierSign.PLUS;
        }

        public void undo(PatchNode root) {
        }

        private String findKey(String prefix, PatchNode node){
            int index = 0;
            while(node.getOrNull(prefix + index) != null){
                index++;
            }
            return prefix + index;
        }
    }

    public static class MapPutOp extends PatchOperator{
        public final String key;

        public MapPutOp(String path, String key){
            super(path);

            this.key = key;
        }

        @Override
        public void apply(PatchNode root){
            PatchNode node = root.navigateChild(path, true);
            node.getOrCreate(key).sign = ModifierSign.PLUS;
        }

        @Override
        public void undo(PatchNode root){

        }
    }

    public static class ChangeTypeOp extends PatchOperator{
        public final Class<?> type;

        public ChangeTypeOp(String path, Class<?> type){
            super(path);
            this.type = type;
        }

        @Override
        public void apply(PatchNode root){
            PatchNode node = root.navigateChild(path, true);
            if(node == null) return;

            node.getOrCreate("type").value = PatchJsonIO.classTypeName(type);
        }

        @Override
        public void undo(PatchNode root){

        }
    }

    public static class SetSignOp extends PatchOperator{
        public final ModifierSign sign;

        public SetSignOp(String path, ModifierSign sign){
            super(path);
            this.sign = sign;
        }

        @Override
        public void apply(PatchNode root){
            root.navigateChild(path, true).sign = sign;
        }

        @Override
        public void undo(PatchNode root){

        }
    }

    public static class SetValueTypeOp extends PatchOperator{
        public final ValueType type;

        public SetValueTypeOp(String path, ValueType type){
            super(path);
            this.type = type;
        }

        @Override
        public void apply(PatchNode root){
            root.navigateChild(path, true).type = type;
        }

        @Override
        public void undo(PatchNode root){

        }
    }

    public static class ClearType extends PatchOperator{

        public ClearType(String path){
            super(path);
        }

        @Override
        public void apply(PatchNode root){
            PatchNode node = root.navigateChild(path, false);
            if(node == null || node.sign == null) return;

            PatchNode typeNode = node.getOrNull("type");
            if(typeNode != null){
                typeNode.remove();
            }
        }

        @Override
        public void undo(PatchNode root){

        }
    }
}