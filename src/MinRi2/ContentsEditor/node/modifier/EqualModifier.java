package MinRi2.ContentsEditor.node.modifier;

import arc.util.serialization.JsonValue.*;
import mindustry.ctype.*;

/**
 * @author minri2
 * Create by 2024/4/4
 */
public abstract class EqualModifier<T> extends DataModifier<T>{

    /**
     * @author minri2
     * Create by 2024/4/4
     */
    public static class ContentTypeModifier extends EqualModifier<UnlockableContent>{
        public ContentTypeModifier(){
            builder = new ModifierBuilder.ContentBuilder(this);
            valueType = ValueType.stringValue;
        }

        @Override
        public UnlockableContent cast(Object object){
            return (UnlockableContent)object;
        }

    }

    /**
     * @author minri2
     * Create by 2024/4/4
     */
    public static class BooleanModifier extends EqualModifier<Boolean>{
        public BooleanModifier(){
            builder = new ModifierBuilder.BooleanBuilder(this);
            valueType = ValueType.booleanValue;
        }

        @Override
        public Boolean cast(Object object){
            return (Boolean)object;
        }
    }

    /**
     * @author minri2
     * Create by 2024/4/4
     */
    public static class StringModifier extends EqualModifier<String>{
        public StringModifier(){
            builder = new ModifierBuilder.TextBuilder(this);
            valueType = ValueType.stringValue;
        }

        @Override
        public String cast(Object object){
            return String.valueOf(object);
        }
    }

    /**
     * @author minri2
     * Create by 2024/4/4
     */
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
}
