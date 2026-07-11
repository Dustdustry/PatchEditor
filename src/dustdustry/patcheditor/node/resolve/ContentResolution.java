package dustdustry.patcheditor.node.resolve;

import arc.struct.*;
import dustdustry.patcheditor.node.*;
import mindustry.mod.*;
import mindustry.world.*;

import java.lang.reflect.*;

public class ContentResolution extends ResolutionStrategy{
    protected final ObjectMap<Class<?>, Seq<String>> allowPatchMap = ObjectMap.of(
        Block.class, Seq.with("size", "update")
    );

    @Override
    public boolean isFieldResolvable(Field field){
        int modifiers = field.getModifiers();
        return (!field.getType().isPrimitive() || !Modifier.isFinal(modifiers))
        && isTypeEditable(field.getType())
        && allowPatch(field);
    }

    protected boolean allowPatch(Field field){
        Class<?> clazz = field.getDeclaringClass();
        Seq<String> allowPatchList = getAllowPatchList(clazz);
        return (allowPatchList != null && allowPatchList.contains(field.getName()))
        || !(field.isAnnotationPresent(NoPatch.class) || clazz.isAnnotationPresent(NoPatch.class));
    }

    private Seq<String> getAllowPatchList(Class<?> type){
        Class<?> current = type;
        while(current != Object.class){
            Seq<String> list = allowPatchMap.get(current);
            if(list != null) return list;
            current = current.getSuperclass();
        }
        return null;
    }
}
