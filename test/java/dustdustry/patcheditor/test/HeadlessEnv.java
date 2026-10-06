package dustdustry.patcheditor.test;

import arc.*;
import arc.graphics.g2d.*;
import arc.mock.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.core.resolve.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;

/**
 * The minimum arc/game environment the editor needs before it can be loaded headless.
 * <p>
 * Content has to be fabricated because the editor is data driven: a patch is parsed against an object
 * tree resolved from live content, and with an empty {@code Vars.content} every content node resolves to
 * {@code null}. It must be registered <em>before</em> the parser is first used, because
 * {@code ContentParser#init()} snapshots its class-to-content-type map from {@code Vars.content} once.
 */
public class HeadlessEnv{
    private static boolean ready;

    public static void init(){
        if(ready) return;
        ready = true;

        Core.settings = new MockSettings();
        Core.atlas = new TextureAtlas();
        Vars.content = new ContentLoader();
        Vars.state = new GameState();

        registerContent();

        ObjectResolver.patch = new PatchResolution();
        ObjectResolver.content = new ContentResolution();
    }

    private static void registerContent(){
        content(new Item("copper"));
        content(new Item("lead"));
        content(new Item("metaglass"));
        content(new Item("titanium"));
        content(new Item("blast-compound"));
        content(new Liquid("water"));

        content(new ItemTurret("duo"));
        content(new Drill("blast-drill"));
        content(new Reconstructor("additive-reconstructor"));
        content(new GenericCrafter("silicon-smelter"));

        content(new UnitType("toxopid"));
        content(new UnitType("avert"));
    }

    private static void content(Content content){
        Vars.content.handleContent(content);
    }

    public static ObjectNode patchRoot(){
        return ObjectNode.createRoot(ObjectResolver.patch);
    }

    public static ObjectNode contentNode(String path){
        return patchRoot().navigate(path);
    }

    public static PatchNode parse(ObjectNode objectNode, String patch){
        PatchNode node = new PatchNode("");
        PatchJsonIO.parseJson(objectNode, node, patch);
        return node;
    }

    public static PatchNode parseContent(String path, String patch){
        return parse(path == null || path.isEmpty() ? patchRoot() : contentNode(path), patch);
    }
}
