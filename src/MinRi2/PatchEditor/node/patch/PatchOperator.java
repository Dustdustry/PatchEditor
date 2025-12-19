package MinRi2.PatchEditor.node.patch;

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
            PatchNode node = root.navigateChild(path, true);
            PatchNode parent = node.getParent();
            node.remove();

            PatchNode current = parent;
            while(current != null && current.children.isEmpty()){
                current.remove();
                current = current.getParent();
            }
        }

        @Override
        public void undo(PatchNode root){

        }
    }

    public class ArrayAppendOp extends PatchOperator{
        protected ArrayAppendOp(String path){
            super(path);
        }

        public void apply(PatchNode root) {
        }

        public void undo(PatchNode root) {
        }
    }
}