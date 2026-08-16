package dustdustry.patcheditor.modifier;

import arc.struct.*;
import arc.util.*;
import dustdustry.patcheditor.core.*;
import rhino.*;

import java.lang.reflect.*;

public class EditorScripts{
    public static ProgressBuilder buildProgress(String script){
        Context cx = Context.enter();
        try{
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initSafeStandardObjects();

            ScriptableObject.putProperty(scope, "build", new NativeJavaClass(scope, ProgressBuilder.class));
            Seq<Field> injectFields = EditorList.getPartProgressFields().copy();
            injectFields.addAll(EditorList.getInterpFields());
            for(Field field : injectFields){
                ScriptableObject.putProperty(scope, field.getName(), Reflect.get(field));
            }

            Object result = cx.evaluateString(scope, script, "<expr>", 1);
            Object javaObject = Context.jsToJava(result, ProgressBuilder.class);
            if(javaObject instanceof ProgressBuilder builder){
                return builder;
            }else{
                throw new RuntimeException("Expression did not yield PartProgressBuilder.");
            }
        }finally{
            Context.exit();
        }
    }
}
