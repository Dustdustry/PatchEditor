package dustdustry.patcheditor.modifier;

import arc.struct.*;
import dustdustry.patcheditor.core.*;
import arc.util.serialization.JsonValue.*;
import dustdustry.patcheditor.modifier.ModifierBuilder.*;
import mindustry.ctype.*;

import java.util.*;

public abstract class ValueModifier<T> extends DataModifier<T>{
    protected ValueType valueType = ValueType.stringValue;

    public ValueType getValueType(){
        return valueType;
    }

    public T transformValue(Object object){
        return (T)object;
    }

    @Override
    public T getValue(){
        EditorNode node = getDataNode();
        return node == null ? null : readValue(node);
    }

    @Override
    public T readValue(EditorNode node){
        return transformValue(node.getDisplayValue());
    }

    @Override
    public void writeValue(PatchNode patch, T value){
        patch.value = PatchJsonIO.getKeyName(value);
        patch.type = valueType;
    }

    @Override
    public boolean isDefault(T value, EditorNode node){
        return Objects.equals(transformValue(node.getObject()), value);
    }

    @Override
    public void resetModify(){
        EditorNode node = getDataNode();
        if(node == null) return;
        if(node.isAppended()){
            node.setValue(PatchJsonIO.getKeyName(node.getObject()), valueType, true);
        }else{
            node.clearJson(true);
        }
    }

    public static class ContentTypeModifier extends ValueModifier<Content>{
        public ContentTypeModifier(){
            builder = new ContentBuilder(this);
            valueType = ValueType.stringValue;
        }
    }

    public static class BooleanModifier extends ValueModifier<Boolean>{
        public BooleanModifier(){
            builder = new BooleanBuilder(this);
            valueType = ValueType.booleanValue;
        }
    }

    public static class StringModifier extends ValueModifier<String>{
        public StringModifier(){
            builder = new TextBuilder(this);
            valueType = ValueType.stringValue;
        }

        @Override
        public String transformValue(Object object){
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

    public static class SoundModifier extends StringModifier{
        public SoundModifier(){
            builder = new SoundBuilder(this);
            valueType = ValueType.stringValue;
        }
    }
}
