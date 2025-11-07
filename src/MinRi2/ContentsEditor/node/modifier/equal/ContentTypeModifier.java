package MinRi2.ContentsEditor.node.modifier.equal;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.node.modifier.*;
import arc.struct.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;

/**
 * @author minri2
 * Create by 2024/4/4
 */
public class ContentTypeModifier extends EqualModifier<UnlockableContent>{
    public static ObjectMap<Class<?>, ContentType> contentClassTypeMap = ObjectMap.of(
    Block.class, ContentType.block,
    Item.class, ContentType.item,
    Liquid.class, ContentType.liquid,
    StatusEffect.class, ContentType.status,
    UnitType.class, ContentType.unit
    );

    private ContentType contentType;

    public ContentTypeModifier(){
        builder = ModifierBuilder.contentBuilder;
        valueType = ValueType.stringValue;
    }

    @Override
    public void setNodeData(NodeData data){
        super.setNodeData(data);
        contentType = contentClassTypeMap.get(getDataType());
    }

    @Override
    protected void setDataJson(JsonValue jsonData, UnlockableContent value){
        jsonData.set(value.name);
    }

    @Override
    public UnlockableContent cast(Object object){
        return (UnlockableContent)object;
    }

}
