package MinRi2.PatchEditor.node.patch;

import MinRi2.PatchEditor.node.*;

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
            while(current != null && current.children.isEmpty()){
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
        public ArrayAppendOp(String path){
            super(path);
        }

        public void apply(PatchNode root) {
            PatchNode node = root.navigateChild(path, true);
            PatchNode plusNode = node.getOrCreate(findKey(node));
            plusNode.sign = ModifierSign.PLUS;
        }

        public void undo(PatchNode root) {
        }

        private String findKey(PatchNode node){
            int index = 0;
            while(node.getOrNull("#ADD_" + index) != null){
                index++;
            }
            return "#ADD_" + index;
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
            if(node == null || node.sign == null) return;

            node.getOrCreate("type").value = PatchJsonIO.classTypeName(type);
        }

        @Override
        public void undo(PatchNode root){

        }
    }
}