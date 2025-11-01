package MinRi2.ContentsEditor.node.modifier.equal;

import MinRi2.ContentsEditor.node.modifier.*;
import arc.func.*;
import arc.util.serialization.*;
import mindustry.type.*;
import mindustry.world.*;

/**
 * @author minri2
 * Create by 2024/4/4
 */
public abstract class EqualModifier<T> extends BaseModifier<T>{

    @Override
    public JsonValue getJsonValue(){
        return nodeData.jsonData;
    }
}
