package MinRi2.PatchEditor.node.patch;

import arc.func.*;
import arc.struct.*;
import arc.util.*;

public class PatchNodeManager{
    public static final String pathComp = ".";
    public static final String pathSplitter = "\\.";

    private PatchNode root = new PatchNode("root");
    private final Seq<Cons<String>> listeners = new Seq<>();

    public void reset(){
        root = new PatchNode("root");
    }

    public PatchNode getRoot(){
        return root;
    }

    public PatchNode getPatch(String path){
        return root.navigateChild(path);
    }

    public void applyOp(PatchOperator operator){
        try{
            operator.apply(root);
        }catch(Exception error){
            Log.err("Unable to apply patch operator to " + operator.path, error);
            return;
        }
    }

    public void listen(String path, Runnable listener){
    }
}
