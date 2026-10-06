package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.math.geom.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.core.JsonProcessor.*;
import dustdustry.patcheditor.core.resolve.*;
import mindustry.entities.bullet.*;
import mindustry.type.*;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.draw.*;
import org.junit.jupiter.api.*;

/** The JSON rewrites between a patch file and the editor: sugar in, sugar out, and tree bookkeeping. */
@DisplayName("JSON pipeline")
class JsonPipelineTest extends HeadlessTest{

    @Test
    void desugarStacks(){
        PatchNode consumes = HeadlessEnv.parseContent("block.silicon-smelter", """
        consumes: {
          items: [
            copper/100
            lead/20
          ]
          liquids: [
            water/0.1
          ]
        }
        """).getOrNull("consumes");

        PatchNode item = consumes.getOrNull("items").getOrNull("items").getOrNull("0");
        assertNotNull(item, "'items: [copper/100]' is unwrapped");
        assertEquals("copper", item.getOrNull("item").value);
        assertEquals("100", item.getOrNull("amount").value);

        PatchNode liquid = consumes.getOrNull("liquids").getOrNull("liquids").getOrNull("0");
        assertNotNull(liquid, "'liquids: [water/0.1]' is unwrapped");
        assertEquals("water", liquid.getOrNull("liquid").value);
        assertEquals("0.1", liquid.getOrNull("amount").value);
    }

    @Test
    void desugarConsumes(){
        PatchNode consumes = HeadlessEnv.parseContent("block.blast-drill", "consumes: { power: 12, powerBuffered: 60 }")
        .getOrNull("consumes");

        PatchNode power = consumes.getOrNull("power");
        assertNotNull(power.getOrNull("usage"), "'power: 12' becomes 'power: {usage: 12}'");
        assertEquals("12.0", power.getOrNull("usage").value);
        assertSame(Jtype.number, power.getOrNull("usage").type);
        assertEquals("60", consumes.getOrNull("powerBuffered").value, "a plain float field is not wrapped");

        //the wrapped usage is built from a float, so it keeps the float's short text
        PatchNode decimal = HeadlessEnv.parseContent("block.blast-drill", "consumes: { power: 0.1 }")
        .getOrNull("consumes").getOrNull("power");
        assertEquals("0.1", decimal.getOrNull("usage").value, "no double noise in the sugar");
    }

    @Test
    void desugarArraySugar(){
        PatchNode effect = HeadlessEnv.parseContent("unit.toxopid", "deathExplosionEffect: [{ type: WaveEffect }]")
        .getOrNull("deathExplosionEffect");

        assertEquals("MultiEffect", effect.getOrNull("type").value, "an effect array becomes a MultiEffect");
        assertEquals("WaveEffect", effect.getOrNull("effects").getOrNull("0").getOrNull("type").value, "the elements keep their type");

        assertDesugarsToMulti(BulletType.class, "MultiBulletType", "bullets");
        assertDesugarsToMulti(DrawBlock.class, "DrawMulti", "drawers");
    }

    private static void assertDesugarsToMulti(Class<?> type, String expectedType, String expectedField){
        JNode value = node("[{ type: BasicBulletType }]");
        JsonTransform.desugarJson(new ObjectNode("element", null, type), value);

        assertTrue(value.isObject(), type.getSimpleName() + " array becomes an object");
        assertEquals(expectedType, value.get("type").asString());
        assertNotNull(value.get(expectedField), "the elements move into '" + expectedField + "'");
    }

    @Test
    void desugarGeometry(){
        JNode quad = node("[1, 2, 3, 4]");
        JsonTransform.desugarJson(new ObjectNode("rect", null, Rect.class), quad);

        assertTrue(quad.isObject(), "a 4 element array for a Rect becomes an object");
        assertEquals("1", quad.get("x").asString());
        assertEquals("2", quad.get("y").asString());
        assertEquals("3", quad.get("width").asString());
        assertEquals("4", quad.get("height").asString());

        JNode vec = node("[1, 2, 3]");
        JsonTransform.desugarJson(new ObjectNode("vec", null, Vec3.class), vec);
        assertTrue(vec.isObject(), "a 3 element array for a Vec3 becomes an object");
        assertEquals("1", vec.get("x").asString());
        assertEquals("3", vec.get("z").asString());

        JNode shortQuad = node("[1, 2, 3]");
        JsonTransform.desugarJson(new ObjectNode("rect", null, Rect.class), shortQuad);
        assertTrue(shortQuad.isArray(), "a Rect array of the wrong length is untouched");
    }

    @Test
    void sugarStacks(){
        ObjectNode itemStack = ObjectResolver.getTemplate(ItemStack.class, ObjectResolver.patch);

        JNode value = node("""
        {
          item: copper
          amount: "10"
        }
        """);
        JsonTransform.sugarPatch(itemStack, value, new SugarJsonConfig().sugarStacks(true));
        assertTrue(value.isString(), "the stack object collapses to a string");
        assertEquals("copper/10", value.asString(), "the shorthand is item/amount");

        JNode numeric = node("""
        {
          item: copper
          amount: 10
        }
        """);
        JsonTransform.sugarPatch(itemStack, numeric, new SugarJsonConfig().sugarStacks(true));
        assertEquals("copper/10", numeric.asString(), "a numeric amount keeps its numeric form");

        JNode kept = node("""
        {
          item: copper
          amount: "10"
        }
        """);
        JsonTransform.sugarPatch(itemStack, kept, new SugarJsonConfig().sugarStacks(false));
        assertTrue(kept.isObject(), "sugaring off keeps the object form");

        JNode partial = node("{ item: copper }");
        JsonTransform.sugarPatch(itemStack, partial, new SugarJsonConfig().sugarStacks(true));
        assertTrue(partial.isObject(), "an incomplete stack stays an object");

        ObjectNode liquidStack = ObjectResolver.getTemplate(LiquidStack.class, ObjectResolver.patch);
        JNode liquid = node("""
        {
          liquid: water
          amount: 0.5
        }
        """);
        JsonTransform.sugarPatch(liquidStack, liquid, new SugarJsonConfig().sugarStacks(true));
        assertEquals("water/0.5", liquid.asString(), "the liquid shorthand uses the liquid key");

        ObjectNode rect = new ObjectNode("rect", null, Rect.class);
        JNode quad = node("{ x: 1, y: 2, width: 3, height: 4 }");
        JsonTransform.sugarPatch(rect, quad, new SugarJsonConfig());
        assertTrue(quad.isArray(), "four Rect fields collapse to an array");
        assertEquals("0", quad.get(0).name, "the first element is re-indexed");
        assertEquals("1", quad.get(0).asString(), "the fields keep their order");
    }

    @Test
    void processJsonFlatten(){
        ObjectNode unit = ObjectResolver.getTemplate(UnitType.class, ObjectResolver.patch);
        JNode value = node("""
        {
          weapons: {
            "+": [
              {
                name: w1
              }
            ]
          }
        }
        """);
        JsonTransform.processJson(unit, value);

        assertNull(value.get("weapons"), "the container key itself is gone");
        JNode plus = value.get("weapons" + NodeManager.pathComp + ModifierSign.PLUS.sign);
        assertNotNull(plus, "the append array is renamed to the dotted key");
        assertTrue(plus.isArray());
        assertEquals("w1", plus.get(0).get("name").asString());

        ObjectNode reconstructor = ObjectResolver.getTemplate(Reconstructor.class, ObjectResolver.patch);
        JNode upgrades = node("""
        {
          upgrades: {
            "+": [
              [
                merui
              ]
            ]
          }
        }
        """);
        JsonTransform.processJson(reconstructor, upgrades);
        assertNotNull(upgrades.get("upgrades" + NodeManager.pathComp + ModifierSign.PLUS.sign), "multi arrays flatten the same way");
    }

    @Test
    void processJsonRemove(){
        ObjectNode consumes = HeadlessEnv.contentNode("block.silicon-smelter").getOrResolve("consumes");
        JNode value = node("{ remove: { coolant: - }, power: 12 }");
        JsonTransform.processJson(consumes, value);

        JNode remove = value.get("remove");
        assertTrue(remove.isArray(), "'remove: {coolant: -}' becomes an array");
        assertEquals("coolant", remove.get(0).asString());
        assertSame(value.get(0).val, remove.val, "remove is moved to the front");
    }

    @Test
    void simplifyPath(){
        JNode value = chainUnder(null, 4);
        JsonTransform.simplifyPath(value);

        JNode collapsed = value.get("a.b.c.d.leaf");
        assertNotNull(collapsed, "a long chain collapses into a dotted key");
        assertEquals("1", collapsed.asString());

        String once = value.toJson();
        JsonTransform.simplifyPath(value);
        assertEquals(once, value.toJson(), "simplifyPath is idempotent");

        JNode shortChain = chainUnder(null, 1);
        JsonTransform.simplifyPath(shortChain);
        assertNotNull(shortChain.get("a"), "a short chain is not collapsed");

        JNode consumes = chainUnder("consumes", 4);
        JsonTransform.simplifyPath(consumes);
        assertNotNull(consumes.get("consumes").get("a"), "'consumes' chains are never collapsed");

        JNode typed = chainUnder("holder", 4);
        typed.get("holder").get("a").get("b").addChild(JNode.of("type", "Wall"));
        JsonTransform.simplifyPath(typed);
        assertNotNull(typed.get("holder").get("a"), "a typed node stops the collapse");
    }

    /** {@code root: {a: {b: ... {leaf: 1}}}}, under {@code name} when it is not null. */
    private static JNode chainUnder(String name, int levels){
        JNode root = JNode.object();
        JNode current = root;
        if(name != null){
            current = JNode.object(name);
            root.addChild(current);
        }

        String[] keys = {"a", "b", "c", "d"};
        for(int i = 0; i < levels; i++){
            JNode next = JNode.object(keys[i]);
            current.addChild(next);
            current = next;
        }
        current.addChild(JNode.of("leaf", "1"));
        return root;
    }

    @Test
    void treeSizesAndOrder(){
        JNode array = node("[1, 2, 3, 4]");
        assertEquals(4, array.size(), "an array knows how many elements it has");

        JNode object = node("{ a: 1, b: 2 }");
        assertEquals(2, object.size(), "an object knows how many fields it has");

        object.get("a").remove();
        assertEquals(1, object.size(), "removing a field decrements the size");
        assertEquals("b", object.first().name, "the remaining field keeps its order");

        JNode target = JNode.object("target");
        object.moveChildrenTo(target);
        assertEquals(0, object.size());
        assertEquals(1, target.size(), "moving children carries the order over");
        assertEquals("b", target.first().name);

        JNode typed = node("{ a: 1, b: 2 }");
        typed.setType(Jtype.array);
        assertEquals(2, typed.size(), "changing the container type keeps the children");
        assertEquals("1", typed.get(0).asString(), "object fields become array elements in order");
        assertEquals("1", typed.get(1).name, "array elements are indexed");
    }

    @Test
    void jsonProcessor(){
        JNode value = JNode.object();
        value.addChild(JNode.of("a", 1.0));
        value.addChild(JNode.of("b", "x"));
        value.addChild(JNode.object("nested"));

        String hjson = new JsonProcessor(null, value).options(PatchExportOptions.defaults()).toJson();
        assertTrue(hjson.contains("a: 1"), "hjson writes numbers unquoted (" + hjson + ")");
        assertTrue(hjson.contains("b: x"), "hjson writes strings unquoted (" + hjson + ")");

        String json = new JsonProcessor(null, value).options(new PatchExportOptions(false, false, true, OutputFormat.json)).toJson();
        assertTrue(json.contains("\"a\": 1"), "formatted json indents and quotes (" + json + ")");

        String compact = new JsonProcessor(null, value).options(new PatchExportOptions(false, false, false, OutputFormat.json)).toJson();
        assertEquals("{\"a\":1,\"b\":\"x\",\"nested\":{}}", compact);

        assertTrue(new JsonProcessor(null, value).format(OutputFormat.hjson).formatJson(false).toJson().contains("a: 1"),
        "an explicit format wins over the json formatting flag");

        assertEquals("", new JsonProcessor(null, (PatchNode)null).toJson(), "no value means no output");
    }

    @Test
    void migrateTweaker(){
        JNode indexed = JsonTransform.migrateTweaker("{\"#0\":{\"size\":2}}");
        assertTrue(indexed.isArray(), "a numbered key turns the object into an array");
        assertEquals("2", indexed.get(0).get("size").asString());

        JNode value = JsonTransform.migrateTweaker("{\"patch\":{\"#0\":{\"=\":\"3\"}}}");
        assertEquals("3", value.get("patch").get(0).asString(), "an '=' entry becomes the value itself");

        JNode named = JsonTransform.migrateTweaker("{\"name\":{\"size\":2}}");
        assertTrue(named.isObject(), "a non numeric key stays an object key");
        assertEquals("2", named.get("name").get("size").asString());
    }

    private static JNode node(String hjson){
        return JNode.of(Jval.read(hjson));
    }
}
