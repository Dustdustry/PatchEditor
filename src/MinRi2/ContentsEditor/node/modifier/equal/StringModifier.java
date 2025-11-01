package MinRi2.ContentsEditor.node.modifier.equal;

import MinRi2.ContentsEditor.node.modifier.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;

/**
 * @author minri2
 * Create by 2024/4/4
 */
public class StringModifier extends EqualModifier<String>{
    public StringModifier(){
        builder = ModifierBuilder.textBuilder;
        valueType = ValueType.stringValue;
    }

    @Override
    protected void setDataJson(JsonValue jsonData, String value){
        jsonData.set(value);
    }

    @Override
    protected String getDataJson(JsonValue jsonData){
        return jsonData.asString();
    }

    @Override
    public String cast(Object object){
        return String.valueOf(object);
    }
}
