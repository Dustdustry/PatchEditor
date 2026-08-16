package dustdustry.patcheditor.modifier;

import arc.util.*;
import dustdustry.patcheditor.core.*;
import arc.scene.ui.layout.*;

/**
 * 编辑器内提供便捷的修改方式
 * @author minri2
 * Create by 2024/4/4
 */
public abstract class DataModifier<T> implements ModifyConsumer<T>{
    protected ModifierBuilder<T> builder;
    private EditorNode dataTree;
    private String dataPath;

    public void build(Table table, boolean readOnly){
        builder.buildTable(table, readOnly);
    }

    public void syncUI(){
        builder.sync();
    }

    public abstract T readValue(EditorNode node);

    public abstract void writeValue(PatchNode patch, T value);

    public abstract boolean isDefault(T value, EditorNode node);

    /**
     * 给定类型 判断数据是否符合类型
     */
    protected boolean checkTypeValid(T value, Class<?> type){
        return true;
    }

    public void setData(EditorNode dataTree, String path){
        this.dataTree = dataTree;
        this.dataPath = path;
    }

    protected EditorNode getDataNode(){
        return dataTree.navigate(dataPath);
    }

    @Override
    public Class<?> getDataType(){
        EditorNode node = getDataNode();
        return node == null ? null : node.getTypeOut();
    }

    @Override
    public Class<?> getTypeMeta(){
        EditorNode node = getDataNode();
        return node == null ? null : node.getTypeIn();
    }

    @Override
    public @Nullable T getValue(){
        EditorNode node = getDataNode();
        if(node == null) return null;
        try{
            return readValue(node);
        }catch(Exception e){
            Log.err(e);
            return null;
        }
    }

    @Override
    public final void onModify(T value){
        EditorNode node = getDataNode();
        if(node == null) return;
        if(isDefault(value, node)){
            resetModify();
        }else{
            PatchNode patch = new PatchNode(node.name());
            writeValue(patch, value);
            node.setPatch(patch, true);
        }
    }

    @Override
    public void resetModify(){
        EditorNode node = getDataNode();
        if(node == null) return;
        node.clearJson(true);
    }

    @Override
    public final boolean checkValue(T value){
        return checkTypeValid(value, getDataType());
    }
}
