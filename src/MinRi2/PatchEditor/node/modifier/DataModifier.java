package MinRi2.PatchEditor.node.modifier;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.node.modifier.ModifierBuilder.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.JsonValue.*;
import mindustry.ctype.*;

import java.util.*;

/**
 * 编辑器内提供便捷的修改方式，仅限值修改或者增删数据，不用于实现对象的修改
 * @author minri2
 * Create by 2024/4/4
 */
public abstract class DataModifier<T> implements ModifyConsumer<T>{
    protected ModifierBuilder<T> builder;
    protected ValueType valueType;
    protected EditorNode dataTree;
    private String dataPath;
    private Boolc onModified;

    public ValueType valueType(){
        return valueType;
    }

    public void build(Table table){
        builder.buildTable(table);
    }

    public void onModified(Boolc onModified){
        this.onModified = onModified;
    }

    @Override
    public T getDefaultValue(){
        return cast(dataTree.navigate(dataPath).getObject());
    }

    /**
     * 给定类型 判断数据是否符合类型
     */
    protected boolean checkTypeValid(T value, Class<?> type){
        return true;
    }

    public abstract T cast(Object object);

    public void setData(EditorNode dataTree, String path){
        this.dataTree = dataTree;
        this.dataPath = path;
    }

    @Override
    public boolean isModified(){
        return !Objects.equals(getDefaultValue(), getValue());
    }

    @Override
    public Class<?> getDataType(){
        return dataTree.navigate(dataPath).getTypeOut();
    }

    @Override
    public Class<?> getTypeMeta(){
        return dataTree.navigate(dataPath).getTypeIn();
    }

    @Override
    public T getValue(){
        return cast(dataTree.navigate(dataPath).getDisplayValue());
    }

    @Override
    public final void onModify(T value){
        boolean modified = !Objects.equals(getDefaultValue(), value);
        if(modified){
            dataTree.navigate(dataPath).setValue(PatchJsonIO.getKeyName(value), valueType);

            if(onModified != null){
                onModified.get(true);
            }
        }else{
            resetModify();
        }
    }

    @Override
    public void resetModify(){
        EditorNode data = dataTree.navigate(dataPath);
        if(data.isAppended()){
            data.setValue(PatchJsonIO.getKeyName(data.getObject()), valueType);
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

    public static class ContentTypeModifier extends DataModifier<Content>{
        public ContentTypeModifier(){
            builder = new ContentBuilder(this);
            valueType = ValueType.stringValue;
        }

        @Override
        public Content cast(Object object){
            return (Content)object;
        }
    }

    public static class BooleanModifier extends DataModifier<Boolean>{
        public BooleanModifier(){
            builder = new BooleanBuilder(this);
            valueType = ValueType.booleanValue;
        }

        @Override
        public Boolean cast(Object object){
            return (Boolean)object;
        }
    }

    public static class StringModifier extends DataModifier<String>{
        public StringModifier(){
            builder = new TextBuilder(this);
            valueType = ValueType.stringValue;
        }

        @Override
        public String cast(Object object){
            return PatchJsonIO.getKeyName(object);
        }
    }

    public static class EnumModifier extends StringModifier{
        public EnumModifier(Seq<String> names){
            builder = new SelectBuilder(this, names);
            valueType = ValueType.stringValue;
        }

        public EnumModifier(Enum<?>[] enums){
            builder = new SelectBuilder(this, enums);
            valueType = ValueType.stringValue;
        }
    }

    public static class NumberModifier extends StringModifier{
        public NumberModifier(){
            super();
            valueType = ValueType.doubleValue;
        }

        @Override
        public boolean checkTypeValid(String string, Class<?> type){
            if(string.isEmpty()) return false;
            try{
                if(type == byte.class || type == Byte.class){
                    Byte.parseByte(string);
                }else if(type == short.class || type == Short.class){
                    Integer.parseInt(string);
                }else if(type == long.class || type == Long.class){
                    Long.parseLong(string);
                }else if(type == float.class || type == Float.class){
                    Float.parseFloat(string);
                }else if(type == double.class || type == Double.class){
                    Double.parseDouble(string);
                }
                return true;
            }catch(Exception ignored){
                return false;
            }
        }
    }

    public static class ColorModifier extends StringModifier{
        public ColorModifier(){
            builder = new ColorBuilder(this);
        }
    }

    public static class TextureRegionModifier extends StringModifier{
        public TextureRegionModifier(){
            builder = new TextureRegionBuilder(this);
            valueType = ValueType.stringValue;
        }
    }

    /** Field Specific */
    public static class WeaponNameModifier extends StringModifier{
        public WeaponNameModifier(){
            builder = new WeaponNameBuilder(this);
            valueType = ValueType.stringValue;
        }
    }

    public static class EffectModifier extends StringModifier{
        public EffectModifier(){
            builder = new EffectBuilder(this);
            valueType = ValueType.stringValue;
        }
    }
}
