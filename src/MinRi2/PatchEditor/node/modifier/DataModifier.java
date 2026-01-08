package MinRi2.PatchEditor.node.modifier;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.EditorNode.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.util.serialization.JsonValue.*;

import java.util.*;

/**
 * 编辑器内提供便捷的修改方式，仅限值修改或者增删数据，不用于实现对象的修改
 * @author minri2
 * Create by 2024/4/4
 */
public abstract class DataModifier<T> implements ModifyConsumer<T>{
    protected ModifierBuilder<T> builder;
    protected ValueType valueType;
    protected EditorNode data;
    private Boolc onModified;

    public void build(Table table){
        builder.build(table);
    }

    public void onModified(Boolc onModified){
        this.onModified = onModified;
    }

    @Override
    public T getDefaultValue(){
        return cast(data.getObject());
    }

    /**
     * @param value 修改后的值
     * @return 数据修改后是否与默认值相同
     */
    protected boolean isModified(T value){
        return !Objects.equals(getDefaultValue(), value);
    }

    /**
     * 给定类型 判断数据是否符合类型
     */
    protected boolean checkTypeValid(T value, Class<?> type){
        return true;
    }

    public abstract T cast(Object object);

    public void setData(EditorNode data){
        this.data = data;
    }

    @Override
    public boolean isModified(){
        return !Objects.equals(getValue(), getDefaultValue());
    }

    @Override
    public Class<?> getDataType(){
        return data.getTypeOut();
    }

    @Override
    public Class<?> getTypeMeta(){
        return data.getTypeIn();
    }

    @Override
    public T getValue(){
        return cast(data.getDisplayValue());
    }

    @Override
    public final void onModify(T value){
        boolean modified = isModified(value);
        if(modified){
            data.setValue(PatchJsonIO.getKeyName(value));

            if(onModified != null){
                onModified.get(true);
            }
        }else{
            resetModify();
        }
    }

    @Override
    public void resetModify(){
        if(data.isAppending()){
            data.setValue(PatchJsonIO.getKeyName(data.getObject()));
        }else{
            data.clearJson();
        }

        if(onModified != null){
            onModified.get(false);
        }
    }

    @Override
    public final boolean checkValue(T value){
        return checkTypeValid(value, getDataType());
    }
}
