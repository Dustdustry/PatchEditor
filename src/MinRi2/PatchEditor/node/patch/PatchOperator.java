package MinRi2.PatchEditor.node.patch;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.modifier.*;
import arc.struct.ObjectMap.*;
import arc.util.serialization.JsonValue.*;

public abstract class PatchOperator{
    public final String path;

    public PatchOperator(String path){
        this.path = path;
    }

    public abstract void apply(PatchNode root);
    public abstract void undo(PatchNode root);

    public static class SetOp extends PatchOperator{
        public final String value;
        public final ValueType type;

        public SetOp(String path, String value){
            this(path, value, null);
        }

        public SetOp(String path, String value, ValueType type){
            super(path);

            this.value = value;
            this.type = type;
        }

        @Override
        public void apply(PatchNode root){
            PatchNode node = root.navigateChild(path, true);
            node.value = value;
            if(type != null) node.type = type;
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

    public static class ClearChildrenOp extends PatchOperator{
        private final ModifierSign remainSign;

        public ClearChildrenOp(String path){
            this(path, null);
        }

        public ClearChildrenOp(String path, ModifierSign remainSign){
            super(path);
            this.remainSign = remainSign;
        }

        @Override
        public void apply(PatchNode root){
            PatchNode node = root.navigateChild(path, false);
            if(node == null) return;
            if(remainSign != null){
                node.remainBySign(remainSign);
            }else{
                node.clearChildren();
            }
        }

        @Override
        public void undo(PatchNode root){

        }
    }

    public static class AppendOp extends PatchOperator{
        public final Class<?> type;
        public final boolean plusSyntax;

        public AppendOp(String path, Class<?> type, boolean plusSyntax){
            super(path);
            this.type = type;
            this.plusSyntax = plusSyntax;
        }

        @Override
        public void apply(PatchNode root) {
            PatchNode node = root.navigateChild(path, true);
            node.type = ValueType.array;

            String prefix = plusSyntax ? PatchJsonIO.appendPrefix : "";
            PatchNode appended = node.getOrCreate(findKey(prefix, node));
            appended.sign = ModifierSign.PLUS;
            appended.type = ClassHelper.isArrayLike(type) ? ValueType.array : ValueType.object;

            ObjectNode template = ObjectResolver.getTemplate(type);
            DataModifier<?> modifier = NodeModifier.getModifier(template);
            if(modifier != null){
                appended.type = modifier.valueType();
                appended.value = PatchJsonIO.getKeyName(template.object);
            }
        }

        @Override
        public void undo(PatchNode root) {
        }

        private String findKey(String prefix, PatchNode node){
            int index = 0;
            while(node.getOrNull(prefix + index) != null) index++;
            return prefix + index;
        }
    }

    public static class TouchOp extends PatchOperator{
        public final String key, value;
        public final ModifierSign sign;

        public TouchOp(String path, String key, String value, ModifierSign sign){
            super(path);

            this.key = key;
            this.value = value;
            this.sign = sign;
        }

        @Override
        public void apply(PatchNode root){
            PatchNode node = root.navigateChild(path, true).getOrCreate(key);
            node.value = value;
            node.sign = sign;
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

            // type changing base on overriding
            if(node.sign != ModifierSign.PLUS) node.sign = ModifierSign.MODIFY;
            node.getOrCreate("type").value = PatchJsonIO.getClassTypeName(type);
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

    public static class ImportOp extends PatchOperator{
        public final PatchNode source;

        public ImportOp(String path, PatchNode source){
            super(path);

            this.source = source;
        }

        @Override
        public void apply(PatchNode root){
            PatchNode patchNode = root.navigateChild(path, true);
            importPatch(patchNode, source);
        }

        @Override
        public void undo(PatchNode root){

        }

        private void importPatch(PatchNode patchNode, PatchNode sourceNode){
            patchNode.type = sourceNode.type;
            patchNode.value = sourceNode.value;
            patchNode.sign = sourceNode.sign;
            for(Entry<String, PatchNode> entry : sourceNode.children){
                importPatch(patchNode.getOrCreate(entry.key), entry.value);
            }
        }
    }
}