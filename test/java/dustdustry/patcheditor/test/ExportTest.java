package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.export.*;
import dustdustry.patcheditor.export.ObjectExporter.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.meta.*;
import org.junit.jupiter.api.*;

/** Exporting: a patch tree to json, and an object tree back into a publishable patch. */
@DisplayName("Export")
class ExportTest extends HeadlessTest{

    @Test
    void patchTreeToJson(){
        PatchNode node = new PatchNode("filter");
        node.getOrCreate("size").value = "2";
        node.getOrCreate(PatchJsonIO.appendPrefix + "0").value = "copper-wall";
        node.getOrCreate(PatchJsonIO.appendPrefix + "1").value = "router";

        JNode json = JsonTransform.toJsonTree(node);
        assertEquals("filter", json.name);
        assertEquals("2", json.get("size").asString(), "named children become fields");

        JNode append = json.get(ModifierSign.PLUS.sign);
        assertNotNull(append, "append children fold into the '+' array");
        assertTrue(append.isArray());
        assertEquals("copper-wall", append.get(0).asString());
        assertEquals("router", append.get(1).asString());
        assertNull(append.get(2), "there are exactly two appended values");

        PatchNode number = new PatchNode("damage");
        number.value = "12.5";
        number.type = Jtype.number;
        JNode numeric = JsonTransform.toJsonTree(number);
        assertTrue(numeric.isNumber(), "a number node becomes a number value");
        assertEquals(12.5, numeric.asFloat(), 0.0001);
    }

    @Test
    void primitives(){
        assertEquals("3", json(3, int.class), "an int is exported as a number");
        assertEquals("true", json(true, boolean.class));
        assertEquals("1.5", json(1.5f, float.class));
        assertEquals("hi", json("hi", String.class));
    }

    @Test
    void floatText(){
        //A float must keep its own shortest text: 0.1f as a double would print 0.10000000149011612.
        assertEquals("0.1", json(0.1f, float.class), "a float keeps its short form");
        assertEquals("0.3", json(0.3f, float.class));
        assertEquals("1.5", json(1.5f, float.class));
        assertEquals("-2.5", json(-2.5f, float.class));
        assertEquals("0.1", json(0.1d, double.class), "a double keeps its own form");
        assertEquals("0.0", json(0f, float.class), "a bare value reads back as the float's own text");
        assertEquals("1.0E8", json(1e8f, float.class), "a bare value keeps java's float text");

        //the json writer is the only place that reformats numbers (Jval behaviour)
        assertEquals("0", ObjectExporter.exportJson(new ObjectNode("value", 0f, float.class)).toJson());
        assertEquals("1", ObjectExporter.exportJson(new ObjectNode("value", 1f, float.class)).toJson());
        assertEquals("1.0e8", ObjectExporter.exportJson(new ObjectNode("value", 1e8f, float.class)).toJson());
    }

    @Test
    void shorthands(){
        assertEquals("#ff0000ff", json(Color.red, Color.class), "colors are exported as hex");
        assertEquals("copper/12", json(new ItemStack(Vars.content.item("copper"), 12), ItemStack.class));
        assertEquals("water/0.5", json(new LiquidStack(Vars.content.liquid("water"), 0.5f), LiquidStack.class));
        assertEquals("heat", json(Attribute.heat, Attribute.class));
        assertEquals("battery", json(BlockFlag.battery, BlockFlag.class));
        assertEquals("linear", json(Interp.linear, Interp.class));
        assertEquals("warmup", json(PartProgress.warmup, PartProgress.class));
        assertEquals("sharded", json(Team.sharded, Team.class));
    }

    @Test
    void geometry(){
        assertEquals("[1,2]", json(new Vec2(1f, 2f), Vec2.class));
        assertEquals("[1,2,3]", json(new Vec3(1f, 2f, 3f), Vec3.class));
        assertEquals("[1,2,3,4]", json(new Rect(1f, 2f, 3f, 4f), Rect.class));
        assertEquals("[1.5,2.25]", json(new Vec2(1.5f, 2.25f), Vec2.class), "fractions keep their form");
    }

    @Test
    void contentReferences(){
        Item copper = Vars.content.item("copper");

        ObjectNode reference = new ObjectNode("item", copper, Item.class);
        reference.addSign(ModifierSign.MODIFY);
        assertEquals("copper", text(ObjectExporter.exportJson(reference)),
        "a marked content reference is exported as a name");

        ObjectNode definition = new ObjectNode("item", copper, Item.class);
        assertNotEquals("copper", text(ObjectExporter.exportJson(definition)),
        "an unmarked content definition is exported field by field");
    }

    @Test
    void missingValues(){
        assertNull(ObjectExporter.exportJson(new ObjectNode("nothing", null, int.class)), "a null object exports nothing");
        assertNull(ObjectExporter.exportJson(new ObjectNode("region", Core.atlas.find("error"), TextureRegion.class)), "the unnamed error region exports nothing");
    }

    @Test
    void defaultFilter(){
        ObjectNode duo = HeadlessEnv.contentNode("block.duo");
        String full = PatchExporter.export(duo, new ExportConfig());

        ExportConfig changedOnly = new ExportConfig();
        changedOnly.allowDefault = false;
        String minimal = PatchExporter.export(duo, changedOnly);

        assertTrue(full.contains("type: ItemTurret"), "the concrete type is always exported");
        assertTrue(minimal.contains("type: ItemTurret"), "the concrete type survives the default filter");
        assertTrue(minimal.length() < full.length(), "filtering defaults shortens the export");
        assertFalse(minimal.contains("consumes"), "blacklisted fields are not exported");
    }

    @Test
    void exportRoundTrip(){
        ObjectNode duo = HeadlessEnv.contentNode("block.duo");

        ExportConfig changedOnly = new ExportConfig();
        changedOnly.allowDefault = false;
        PatchNode patch = HeadlessEnv.parse(duo, PatchExporter.export(duo, changedOnly));

        assertNotNull(patch.getOrNull("type"), "the exported type key comes back");
        assertEquals("ItemTurret", patch.getOrNull("type").value, "the exported type key is still resolvable");
    }

    private static String json(Object object, Class<?> type){
        JNode value = ObjectExporter.exportJson(new ObjectNode("value", object, type));
        assertNotNull(value, "the value must be exportable");
        return text(value);
    }

    /** A bare value reads as its own text, a container as compact json. */
    private static String text(JNode value){
        return value.isValue() ? String.valueOf(value.asString()) : value.toJson();
    }
}
