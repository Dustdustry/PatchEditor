package MinRi2.PatchEditor.node.patch;

import arc.*;
import arc.struct.*;

public class NodeManager{
    public static final String pathComp = ".";
    public static final String pathSplitter = "\\.";

    private PatchNode root = new PatchNode("");
    private final Seq<PatchNodeListener> listeners = new Seq<>();
    private final Seq<PatchOperator> undoStack = new Seq<>();
    private final Seq<PatchOperator> redoStack = new Seq<>();

    public void reset(){
        root = new PatchNode("");
        clear();
    }

    public void clear(){
        undoStack.clear();
        redoStack.clear();
    }

    public PatchNode getRoot(){
        return root;
    }

    public PatchNode getPatch(String path, boolean create){
        return root.navigateChild(path, create);
    }

    public void applyOp(PatchOperator operator){
        applyOp(operator, false);
    }

    public void applyOp(PatchOperator operator, boolean uiUpdated){
        operator.apply(root);
        undoStack.add(operator);
        redoStack.clear();
        trimUndoStack();

        PatchNode node = getPatch(operator.path, false);
        triggerListeners(operator, node, uiUpdated);
    }

    public boolean canUndo(){
        return undoStack.any();
    }

    public boolean canRedo(){
        return redoStack.any();
    }

    public void undo(){
        if(!undoStack.any()) return;

        PatchOperator op = undoStack.pop();
        op.undo(root);
        redoStack.add(op);

        PatchNode node = getPatch(op.path, false);
        triggerListeners(op, node, false);
    }

    public void redo(){
        if(!redoStack.any()) return;

        PatchOperator op = redoStack.pop();
        op.apply(root);
        undoStack.add(op);
        trimUndoStack();

        PatchNode node = getPatch(op.path, false);
        triggerListeners(op, node, false);
    }

    private void trimUndoStack(){
        int limit = Core.settings.getInt("patch-editor.undoLimit", 100);
        if(limit <= 0){
            undoStack.clear();
        }else if(undoStack.size > limit){
            undoStack.removeRange(0, undoStack.size - limit - 1);
        }
    }

    private void triggerListeners(PatchOperator op, PatchNode node, boolean uiUpdated){
        for(PatchNodeListener listener : listeners){
            listener.onChanged(op, node, uiUpdated);
        }
    }

    public void onChanged(PatchNodeListener listener){
        listeners.add(listener);
    }

    public interface PatchNodeListener{
        void onChanged(PatchOperator operator, PatchNode after, boolean uiUpdated);
    }
}
