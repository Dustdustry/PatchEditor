package dustdustry.patcheditor.node.patch;

import arc.*;
import arc.struct.*;

public class NodeManager{
    public static final String pathComp = ".";
    public static final String pathSplitter = "\\.";

    private PatchNode root = new PatchNode("");
    private final ObjectSet<String> patchedPaths = new ObjectSet<>();
    private final Seq<PatchNodeListener> listeners = new Seq<>();
    private final Seq<PatchOperator> undoStack = new Seq<>();
    private final Seq<PatchOperator> redoStack = new Seq<>();

    public void reset(){
        root = new PatchNode("");
        patchedPaths.clear();
        clearStacks();
    }

    public void clearStacks(){
        undoStack.clear();
        redoStack.clear();
    }

    public void indexPaths(){
        patchedPaths.clear();
        collectPaths(root);
    }

    public PatchNode getRoot(){
        return root;
    }

    public boolean hasPatch(String path){
        return patchedPaths.contains(path == null ? "" : path);
    }

    public PatchNode getPatch(String path, boolean create){
        return root.navigateChild(path, create);
    }

    public void applyOp(PatchOperator operator){
        applyOp(operator, false);
    }

    public void applyOp(PatchOperator operator, boolean uiUpdated){
        if(!operator.shouldApply(root)) return;

        withPathsIndexing(operator.path, () -> {
            operator.apply(root);
            undoStack.add(operator);
            redoStack.clear();
            trimUndoStack();
        });

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
        withPathsIndexing(op.path, () -> op.undo(root));
        redoStack.add(op);

        PatchNode node = getPatch(op.path, false);
        triggerListeners(op, node, false);
    }

    public void redo(){
        if(!redoStack.any()) return;

        PatchOperator op = redoStack.pop();
        withPathsIndexing(op.path, () -> op.apply(root));
        undoStack.add(op);
        trimUndoStack();

        PatchNode node = getPatch(op.path, false);
        triggerListeners(op, node, false);
    }

    private void collectPaths(PatchNode node){
        patchedPaths.add(node.getPath());
        for(PatchNode child : node.children.values()){
            collectPaths(child);
        }
    }

    private void withPathsIndexing(String path, Runnable action){
        String normalizedPath = path == null ? "" : path;

        ObjectSet<String> before = collectChildPaths(normalizedPath);
        action.run();
        ObjectSet<String> after = collectChildPaths(normalizedPath);

        for(String removedPath : before){
            patchedPaths.remove(removedPath);
        }
        for(String addedPath : after){
            patchedPaths.add(addedPath);
        }

        refreshAncestorPaths(normalizedPath);
        patchedPaths.add("");
    }

    private ObjectSet<String> collectChildPaths(String path){
        ObjectSet<String> paths = new ObjectSet<>();
        PatchNode node = getPatch(path, false);
        if(node == null) return paths;

        collectPaths(node, paths);
        return paths;
    }

    private void collectPaths(PatchNode node, ObjectSet<String> out){
        out.add(node.getPath());
        for(PatchNode child : node.children.values()){
            collectPaths(child, out);
        }
    }

    private void refreshAncestorPaths(String path){
        String current = path;
        while(current != null && !current.isEmpty()){
            int dot = current.lastIndexOf(pathComp);
            String parentPath = dot == -1 ? "" : current.substring(0, dot);
            if(parentPath.isEmpty()){
                patchedPaths.add("");
            }else if(getPatch(parentPath, false) != null){
                patchedPaths.add(parentPath);
            }else{
                patchedPaths.remove(parentPath);
            }
            current = parentPath;
        }
    }

    private void trimUndoStack(){
        int limit = Core.settings.getInt("patch-editor.undoLimit", 20);
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
