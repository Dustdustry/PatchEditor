package MinRi2.ContentsEditor.node;

import arc.util.*;
import arc.util.serialization.Json.*;

import java.lang.reflect.*;

// Thanks to anuke's private class
public class FieldData{
    public @Nullable Field field;
    public @Nullable Class<?> type, elementType, keyType;

    public FieldData(Class<?> type){
        this(type, null, null);
    }

    public FieldData(Class<?> type, Class<?> elementType, Class<?> keyType){
        this.type = ClassHelper.unoymousClass(type);
        this.elementType = elementType;
        this.keyType = keyType;

        // fix FieldMetaData
        if(elementType == null && type.isArray()){
            this.elementType = type.getComponentType();
        }
    }

    public FieldData(FieldMetadata metadata){
        this(metadata.field.getType(), metadata.elementType, metadata.keyType);

        this.field = metadata.field;
    }
}
