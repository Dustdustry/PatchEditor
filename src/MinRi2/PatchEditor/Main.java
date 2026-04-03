package MinRi2.PatchEditor;

import MinRi2.PatchEditor.export.*;
import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.*;
import rhino.*;

/**
 * @author minri2
 * Create by 2024/2/14
 */
public class Main extends Mod{

    public Main(){
        Events.on(ClientLoadEvent.class, e -> {
            EUI.init();

            if(OS.hasProp("exposeExporterJS")){
                Scripts scripts = Vars.mods.getScripts();
                scripts.scope.put("PatchExporter", scripts.scope, new NativeJavaClass(scripts.scope, PatchExporter.class));
            }
        });
    }
}
