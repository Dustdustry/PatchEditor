package MinRi2.PatchEditor.node.patch;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.EditorNode.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;

public class NodeManager{
    public static final String pathComp = ".";
    public static final String pathSplitter = "\\.";

    private PatchNode root = new PatchNode("");

    public void reset(){
        root = new PatchNode("");
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
        }
    }
}
