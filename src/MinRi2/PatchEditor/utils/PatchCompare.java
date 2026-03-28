package MinRi2.PatchEditor.utils;

import java.lang.reflect.Array;

public class PatchCompare{
    public static boolean equalsValue(Object value, Object defaultValue, Class<?> type){
        if(value == null && defaultValue == null) return true;
        if(value == null || defaultValue == null) return false;

        if(ClassHelper.isArray(type)){
            int actualLength = Array.getLength(value);
            int defaultLength = Array.getLength(defaultValue);
            if(actualLength != defaultLength) return false;
            if(actualLength == 0) return true;

            Class<?> elementType = type.getComponentType();
            for(int i = 0; i < actualLength; i++){
                if(!equalsValue(Array.get(value, i), Array.get(defaultValue, i), elementType)) return false;
            }
            return true;
        }

        return value.equals(defaultValue);
    }
}
