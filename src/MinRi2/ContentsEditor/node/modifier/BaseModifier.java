package MinRi2.ContentsEditor.node.modifier;

import MinRi2.ContentsEditor.node.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.util.pooling.Pool.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;

import java.util.*;

/**
 * @author minri2
 * Create by 2024/4/4
 */
public abstract class BaseModifier<T> implements ModifyConsumer<T>, Poolable{
    protected ModifierBuilder<T> builder;
    protected ValueType valueType;
    protected NodeData nodeData;
    private Boolc onModified;

    public void build(Table table){
        builder.build(table, this);
    }

    public void onModified(Boolc onModified){
        this.onModified = onModified;
    }

    public abstract JsonValue getJsonValue();

    public T getDefaultValue(){
        return cast(nodeData.getObject());
    }

    /**
     * @param value 修改后的值
     * @return 数据修改后是否与默认值相同
     */
    protected boolean isModified(T value){
        return !Objects.equals(getDefaultValue(), value);
    }

    /**
     * 将数据保存
     * 由子类实现
     * @param jsonData 保存到的JsonValue
     */
    protected abstract void setDataJson(JsonValue jsonData, T value);

    /**
     * 给定类型 判断数据是否符合类型
     */
    protected boolean checkTypeValid(T value, Class<?> type){
        return true;
    }

    public abstract T cast(Object object);

    @Override
    public void reset(){
        this.nodeData = null;
    }

    public void setNodeData(NodeData data){
        this.nodeData = data;
    }

    @Override
    public boolean isModified(){
        return isModified(getValue());
    }

    @Override
    public Class<?> getDataType(){
        return NodeHelper.getType(nodeData);
    }

    @Override
    public T getValue(){
        JsonValue jsonValue = nodeData.jsonData;
        if(jsonValue == null || (!jsonValue.isValue() && nodeData.jsonData.size == 0)){
            return getDefaultValue();
        }

        return cast(nodeData.getDataObject());
    }

    @Override
    public final void onModify(T value){
        nodeData.initJsonData();

        boolean modified = isModified(value);
        if(modified){
            setDataJson(getJsonValue(), value);

            if(onModified != null){
                onModified.get(true);
            }
        }else{
            resetModify();
        }
    }

    @Override
    public void resetModify(){
        nodeData.clearJson();

        if(onModified != null){
            onModified.get(false);
        }
    }

    @Override
    public final boolean checkValue(T value){
        return checkTypeValid(value, getDataType());
    }
}
