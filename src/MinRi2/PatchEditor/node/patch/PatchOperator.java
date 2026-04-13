package MinRi2.PatchEditor.node.patch;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.modifier.*;
import MinRi2.PatchEditor.utils.*;
import arc.struct.ObjectMap.*;
import arc.util.serialization.JsonValue.*;

public abstract class PatchOperator{
    public final String path;

    protected boolean existed;
    protected PatchNode snapshot;

    public PatchOperator(String path){
        this.path = path;
    }

    public abstract void apply(PatchNode root);

    // TODO: necessary status?
    public void undo(PatchNode root){
        if(existed){
            PatchNode node = root.navigateChild(path, true);
            if(snapshot != null) setTreeFrom(node, snapshot);
        }else{
            removeClean(root, path);
        }
    }

    protected void captureSnapshot(PatchNode root){
        PatchNode node = root.navigateChild(path, false);
        existed = node != null;
        snapshot = existed ? cloneTree(node) : null;
    }

    protected static PatchNode cloneTree(PatchNode src){
        PatchNode copy = new PatchNode(src.key);
        setTreeFrom(copy, src);
        return copy;
    }

    protected static void setTreeFrom(PatchNode target, PatchNode src){
        target.value = src.value;
        target.type = src.type;
        target.sign = src.sign;

        target.clearChildren();
        for(PatchNode child : src.children.values()){
            PatchNode childCopy = target.getOrCreate(child.key);
            setTreeFrom(childCopy, child);
        }
    }

    protected static void removeClean(PatchNode root, String path){
        PatchNode node = root.navigateChild(path, false);
        if(node == null) return;

        PatchNode parent = node.getParent();
        node.remove();

        PatchJsonTransform.cleanEmptyParents(parent);
    }

    public static class SetOp extends PatchOperator{
        public final String value;
        public final ValueType type;

        public SetOp(String path, String value, ValueType type){
            super(path);

            this.value = value;
            this.type = type;
        }

        @Override
        public void apply(PatchNode root){
            captureSnapshot(root);
            PatchNode node = root.navigateChild(path, true);
            node.value = value;
            if(type != null) node.type = type;
        }
    }

    public static class ClearOp extends PatchOperator{

        public ClearOp(String path){
            super(path);
        }

        @Override
        public void apply(PatchNode root){
            captureSnapshot(root);
            PatchNode node = root.navigateChild(path, false);
            if(node == null) return;

            removeClean(root, path);
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
            captureSnapshot(root);
            PatchNode node = root.navigateChild(path, false);
            if(node == null) return;
            if(remainSign != null){
                node.remainBySign(remainSign);
            }else{
                node.clearChildren();
            }
        }
    }

    public static class AppendOp extends PatchOperator{
        public final Class<?> elementType;
        public final boolean plusSyntax;

        public AppendOp(String path, Class<?> elementType, boolean plusSyntax){
            super(path);
            this.elementType = elementType;
            this.plusSyntax = plusSyntax;
        }

        @Override
        public void apply(PatchNode root) {
            captureSnapshot(root);
            PatchNode node = root.navigateChild(path, true);

            String prefix = plusSyntax ? PatchJsonIO.appendPrefix : "";
            PatchNode appended = node.getOrCreate(findKey(prefix, node));
            appended.sign = ModifierSign.PLUS;
            appended.type = ClassHelper.isArrayLike(elementType) ? ValueType.array : ValueType.object;

            ObjectNode template = ObjectResolver.getTemplate(elementType);
            DataModifier<?> modifier = NodeModifier.getModifier(template);
            if(modifier != null){
                appended.type = modifier.valueType();
                appended.value = PatchJsonIO.getKeyName(template.object);
            }
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
            captureSnapshot(root);
            PatchNode node = root.navigateChild(path, true).getOrCreate(key);
            node.value = value;
            node.sign = sign;
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
            captureSnapshot(root);
            PatchNode node = root.navigateChild(path, true);
            if(node == null) return;

            // type changing base on overriding
            if(node.sign != ModifierSign.PLUS) node.sign = ModifierSign.MODIFY;
            node.getOrCreate("type").value = PatchJsonIO.getTypeName(type);
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
            captureSnapshot(root);
            root.navigateChild(path, true).sign = sign;
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
            captureSnapshot(root);
            root.navigateChild(path, true).type = type;
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
            captureSnapshot(root);
            PatchNode patchNode = root.navigateChild(path, true);
            importPatch(patchNode, source, false);
        }

        private void importPatch(PatchNode patchNode, PatchNode sourceNode, boolean importSign){
            patchNode.type = sourceNode.type;
            patchNode.value = sourceNode.value;
            if(importSign) patchNode.sign = sourceNode.sign;
            for(Entry<String, PatchNode> entry : sourceNode.children){
                importPatch(patchNode.getOrCreate(entry.key), entry.value, true);
            }
        }
    }

    public static class BatchOp extends PatchOperator{
        public final PatchOperator[] ops;

        public BatchOp(String path, PatchOperator... ops){
            super(path);
            this.ops = ops;
        }

        @Override
        public void apply(PatchNode root){
            captureSnapshot(root);
            for(PatchOperator op : ops){
                if(op == null) continue;
                op.apply(root);
            }
        }
    }
}
