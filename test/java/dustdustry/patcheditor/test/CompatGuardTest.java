package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.audio.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.serialization.*;
import dustdustry.patcheditor.core.*;
import java.lang.reflect.*;
import mindustry.ctype.*;
import mindustry.editor.*;
import mindustry.editor.data.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;
import org.junit.jupiter.api.*;

/**
 * Guards every string-keyed reflective access the editor makes into Mindustry and arc. These couplings
 * break silently on a game update: no compile error, just a {@code null} or a reflection exception
 * somewhere deep in the editor.
 */
@DisplayName("Reflective game couplings")
class CompatGuardTest extends HeadlessTest{

    @Test
    void dataPatcherFields(){
        assertField(DataPatcher.class, "parser", true);
        assertField(DataPatcher.class, "nameToType", true);
        assertField(DataPatcher.class, "root", true);
    }

    @Test
    void contentParserApi(){
        assertDeclaredMethod(ContentParser.class, "getJson");

        //The tree parameter is pinned on purpose: these two signatures are what upstream switches to Jval.
        assertDeclaredMethod(ContentParser.class, "parseProgressOp", PartProgress.class, String.class, JsonValue.class);
    }

    @Test
    void parserRoutes(){
        assertNotNull(PatchJsonIO.getParser(), "PatchJsonIO.getParser() must resolve DataPatcher.parser");
        assertNotNull(PatchJsonIO.getParser().getJson(), "ContentParser#getJson() must not be null");
        assertFalse(PatchJsonIO.getFields(WeaponLike.class).isEmpty(), "field metadata lookup must return fields");
    }

    @Test
    void nameToTypeRoute(){
        ObjectMap<String, ContentType> map = PatchJsonIO.getNameToType();
        assertNotNull(map, "PatchJsonIO.getNameToType() must resolve DataPatcher.nameToType");
        assertTrue(map.containsValue(ContentType.block, true), "nameToType must contain the block content type");
        assertTrue(map.containsValue(ContentType.unit, true), "nameToType must contain the unit content type");
    }

    @Test
    void internalReadRoute(){
        // Declared on the anonymous Json subclass inside DataPatcher.createParser(), not on arc's Json.
        Json json = PatchJsonIO.getParser().getJson();
        assertDeclaredMethod(json.getClass(), "internalRead", Class.class, Class.class, JsonValue.class, Class.class);
    }

    @Test
    void objectReading(){
        //The editor reads game objects back through the parser's own text entry point.
        JNode tree = JNode.object();
        tree.addChild(JNode.of("name", "my-weapon"));

        WeaponLike read = PatchJsonIO.readValue(WeaponLike.class, null, tree);
        assertNotNull(read, "a tree must be readable as an object");
        assertEquals("my-weapon", read.name, "the field comes back through the parser");
    }

    @Test
    void classMapVocabulary(){
        String[] names = {
            "Block", "Wall", "Conveyor", "BasicBulletType", "PointDefenseWeapon",
            "ForceFieldAbility", "MultiEffect", "DrawMulti", "ShootAlternate", "ConsumePower"
        };

        for(String name : names){
            assertTrue(ClassMap.classes.containsKey(name), "ClassMap.classes must contain '" + name + "'");
        }
    }

    @Test
    void partProgressConstants(){
        assertTrue(PartProgress.class.isInterface(), "PartProgress is expected to stay an interface of named progress constants");
        assertField(PartProgress.class, "warmup", true);
    }

    @Test
    void arcInternals(){
        assertField(AudioSource.class, "handle", false);
        assertField(Cell.class, "colspan", false);
        assertField(Cell.class, "column", false);
        assertField(Cell.class, "endRow", false);
        assertField(Cell.class, "cellAboveIndex", false);
        assertField(Table.class, "columns", false);
        assertField(ColorPicker.class, "current", false);
        assertField(ColorPicker.class, "hexField", false);
        assertField(MapContentView.class, "contentIcons", true);
    }

    @Test
    void editorMountMembers(){
        //EditorMount reaches into the game's map editor by name, and casts the results to these types.
        assertField(MapEditorDialog.class, "infoDialog", false);
        assertField(MapInfoDialog.class, "patches", false);
        assertField(MapAssetsDialog.class, "list", false);
        assertField(MapAssetsDialog.class, "currentType", false);
        assertField(MapAssetsDialog.class, "searchString", false);
        assertDeclaredMethod(MapAssetsDialog.class, "rebuild");

        assertFieldType(MapEditorDialog.class, "infoDialog", MapInfoDialog.class);
        assertFieldType(MapInfoDialog.class, "patches", MapAssetsDialog.class);
        assertFieldType(MapAssetsDialog.class, "searchString", String.class);
    }

    private static void assertFieldType(Class<?> owner, String name, Class<?> type){
        try{
            assertEquals(type, owner.getDeclaredField(name).getType(),
            owner.getName() + "#" + name + " is read reflectively as " + type.getSimpleName() + " by the editor");
        }catch(NoSuchFieldException e){
            throw new AssertionError(owner.getName() + "#" + name + " is read reflectively by the editor but no longer exists", e);
        }
    }

    private static void assertField(Class<?> owner, String name, boolean expectStatic){
        Field field;
        try{
            field = owner.getDeclaredField(name);
        }catch(NoSuchFieldException e){
            throw new AssertionError(owner.getName() + "#" + name + " is read reflectively by the editor but no longer exists", e);
        }

        if(expectStatic && !Modifier.isStatic(field.getModifiers())){
            throw new AssertionError(owner.getName() + "#" + name + " is read as a static field by the editor but is not static anymore");
        }
    }

    private static void assertDeclaredMethod(Class<?> owner, String name, Class<?>... params){
        try{
            owner.getDeclaredMethod(name, params);
        }catch(NoSuchMethodException e){
            StringBuilder signature = new StringBuilder(owner.getName()).append("#").append(name).append("(");
            for(int i = 0; i < params.length; i++){
                if(i > 0) signature.append(", ");
                signature.append(params[i].getSimpleName());
            }
            throw new AssertionError(signature.append(") is invoked reflectively by the editor but no longer exists").toString(), e);
        }
    }

    private static Method findMethod(Class<?> owner, String name, int parameterCount){
        for(Method method : owner.getDeclaredMethods()){
            if(method.getName().equals(name) && method.getParameterCount() == parameterCount) return method;
        }
        return null;
    }

    public static class WeaponLike{
        public float reload = 1f;
        public String name = "test";
    }
}
