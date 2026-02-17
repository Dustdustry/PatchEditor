package MinRi2.PatchEditor.node.patch;

import arc.struct.*;

public class NodeManager{
    public static final String pathComp = ".";
    public static final String pathSplitter = "\\.";

    private PatchNode root = new PatchNode("");
    private final Seq<PatchNodeListener> listeners = new Seq<>();

    public void reset(){
        root = new PatchNode("");
    }

    public PatchNode getRoot(){
        return root;
    }

    public PatchNode getPatch(String path, boolean create){
        return root.navigateChild(path, create);
    }

    public void applyOp(PatchOperator operator){
        operator.apply(root);

        PatchNode node = getPatch(operator.path, false);
        for(PatchNodeListener listener : listeners){
            listener.onChanged(operator, node);
        }
    }

    public void onChanged(PatchNodeListener listener){
        listeners.add(listener);
    }

    public interface PatchNodeListener{
        void onChanged(PatchOperator operator, PatchNode after);
    }
}
