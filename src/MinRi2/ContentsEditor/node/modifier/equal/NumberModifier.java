package MinRi2.ContentsEditor.node.modifier.equal;

/**
 * @author minri2
 * Create by 2024/4/4
 */
public class NumberModifier extends StringModifier{

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
