package dustdustry.patcheditor;

import dustdustry.patcheditor.export.*;
import dustdustry.patcheditor.ui.*;
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
            EVars.init();
            EUI.init();

            if(OS.hasProp("exposeExporterJS")){
                Scripts scripts = Vars.mods.getScripts();
                scripts.scope.put("PatchExporter", scripts.scope, new NativeJavaClass(scripts.scope, PatchExporter.class));
            }
        });
    }
}
