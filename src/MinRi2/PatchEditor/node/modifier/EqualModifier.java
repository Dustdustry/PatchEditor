package MinRi2.PatchEditor.node.modifier;

import MinRi2.PatchEditor.node.modifier.ModifierBuilder.*;
import arc.struct.*;
import arc.util.serialization.JsonValue.*;
import mindustry.ctype.*;

/**
 * @author minri2
 * Create by 2024/4/4
 */
public abstract class EqualModifier<T> extends DataModifier<T>{

    public static class ContentTypeModifier extends EqualModifier<Content>{
        public ContentTypeModifier(){
            builder = new ContentBuilder(this);
            valueType = ValueType.stringValue;
        }

        @Override
        public Content cast(Object object){
            return (Content)object;
        }
    }

    public static class BooleanModifier extends EqualModifier<Boolean>{
        public BooleanModifier(){
            builder = new BooleanBuilder(this);
            valueType = ValueType.booleanValue;
        }

        @Override
        public Boolean cast(Object object){
            return (Boolean)object;
        }
    }

    public static class StringModifier extends EqualModifier<String>{
        public StringModifier(){
            builder = new TextBuilder(this);
            valueType = ValueType.stringValue;
        }

        @Override
        public String cast(Object object){
            return String.valueOf(object);
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
}
