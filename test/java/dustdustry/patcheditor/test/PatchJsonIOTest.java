package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.math.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.core.resolve.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.meta.*;
import org.junit.jupiter.api.*;

/** Parsing: HJSON in, patch tree out, including the sign every node ends up with. */
@DisplayName("Patch parsing")
class PatchJsonIOTest extends HeadlessTest{

    @Test
    void keyNames(){
        assertEquals("null", PatchJsonIO.getKeyName(null), "a null object has a fixed key");
        assertEquals("copper", PatchJsonIO.getKeyName(Vars.content.item("copper")), "content keys are their names");
        assertEquals("battery", PatchJsonIO.getKeyName(BlockFlag.battery), "enums use their constant name");
        assertEquals(Wall.class.getName(), PatchJsonIO.getKeyName(Wall.class), "classes use their binary name");
        assertEquals("linear", PatchJsonIO.getKeyName(Interp.linear), "Interp constants come from their fields");
        assertEquals("warmup", PatchJsonIO.getKeyName(PartProgress.warmup), "PartProgress constants come from their fields");

        Object unknown = new Object();
        assertEquals(String.valueOf(unknown), PatchJsonIO.getKeyName(unknown), "unknown objects fall back to toString");
    }

    @Test
    void typeNames(){
        assertEquals("Wall", PatchJsonIO.getTypeName(Wall.class), "ClassMap names win over class names");
        assertEquals("BasicBulletType", PatchJsonIO.getTypeName(BasicBulletType.class), "ClassMap names win over class names");
        assertEquals(Object.class.getName(), PatchJsonIO.getTypeName(Object.class), "a class outside the ClassMap falls back to its name");
    }

    @Test
    void classContentTypes(){
        assertSame(ContentType.block, PatchJsonIO.classContentType(Block.class));
        assertSame(ContentType.item, PatchJsonIO.classContentType(Item.class));
        assertNull(PatchJsonIO.classContentType(Wall.class), "subclasses have no content type of their own");
        assertNull(PatchJsonIO.classContentType(null));
    }

    @Test
    void typeParsers(){
        assertSame(BulletType.class, PatchJsonIO.getTypeParser(BasicBulletType.class), "bullet subclasses parse as BulletType");
        assertSame(Block.class, PatchJsonIO.getTypeParser(Wall.class), "block subclasses parse as Block");
        assertSame(Item.class, PatchJsonIO.getTypeParser(Item.class), "a base class parses as itself");
    }

    @Test
    void cloneObjects(){
        Item copper = Vars.content.item("copper");

        Object itemStack = PatchJsonIO.cloneObject(new ItemStack(copper, 5));
        assertTrue(itemStack instanceof ItemStack);
        assertSame(copper, ((ItemStack)itemStack).item);
        assertEquals(5f, ((ItemStack)itemStack).amount, 0.0001f);

        assertTrue(PatchJsonIO.cloneObject(new LiquidStack(Vars.content.liquid("water"), 0.5f)) instanceof LiquidStack);
        assertNull(PatchJsonIO.cloneObject(new Object()), "anything else is not cloneable");
        assertNull(PatchJsonIO.cloneObject(null));
    }

    @Test
    void scalars(){
        PatchNode patch = HeadlessEnv.parseContent("block.silicon-smelter", """
        health: 500
        update: false
        localizedName: my-smelter
        """);

        assertEquals("500", value(patch, "health"));
        assertSame(Jtype.number, type(patch, "health"));
        assertEquals("false", value(patch, "update"));
        assertSame(Jtype.bool, type(patch, "update"));
        assertEquals("my-smelter", value(patch, "localizedName"));
        assertSame(Jtype.string, type(patch, "localizedName"));
        assertNull(patch.getOrNull("update").sign, "a plain scalar is not marked as overriding");
    }

    @Test
    void dotSyntax(){
        PatchNode patch = HeadlessEnv.parseContent("block.silicon-smelter", """
        health: 1
        consumes.power.usage: 4
        update: true
        """);

        PatchNode consumes = patch.getOrNull("consumes");
        assertNotNull(consumes, "the dotted key creates the first level");
        assertEquals("4", consumes.getOrNull("power").getOrNull("usage").value, "the leaf value lands on the last key");
        assertNull(patch.getOrNull("consumes.power.usage"), "the dotted key itself is gone");
        assertEquals("health", patch.children.keys().iterator().next(), "dotted keys do not reorder the patch");
    }

    @Test
    void appendSyntax(){
        PatchNode patch = HeadlessEnv.parseContent("unit.avert", """
        weapons.+: [
          {}
        ]
        commands.+: [
          mine
        ]
        """);

        PatchNode appendedWeapon = patch.getOrNull("weapons").getOrNull(PatchJsonIO.appendPrefix + "0");
        assertNotNull(appendedWeapon, "'+:' produces an append node");
        assertSame(ModifierSign.PLUS, appendedWeapon.sign);

        PatchNode appendedCommand = patch.getOrNull("commands").getOrNull(PatchJsonIO.appendPrefix + "0");
        assertSame(ModifierSign.PLUS, appendedCommand.sign, "an appended scalar carries the plus sign");
        assertEquals("mine", appendedCommand.value);
    }

    @Test
    void arrayOverride(){
        PatchNode upgrades = HeadlessEnv.parseContent("block.additive-reconstructor", """
        upgrades: [
          [
            merui
            cleroi
          ]
        ]
        """).getOrNull("upgrades");

        assertSame(Jtype.array, upgrades.type);
        assertSame(ModifierSign.MODIFY, upgrades.sign, "a bare array overrides the whole container");
        assertSame(ModifierSign.PLUS, upgrades.getOrNull("0").sign, "array elements are appended");
        assertEquals("merui", upgrades.getOrNull("0").getOrNull("0").value, "the inner array survives");
    }

    @Test
    void mapSigns(){
        PatchNode ammo = HeadlessEnv.parseContent("block.duo", """
        ammoTypes: {
          copper: -
          blast-compound: {
            type: RailBulletType
          }
        }
        """).getOrNull("ammoTypes");

        assertSame(ModifierSign.REMOVE, ammo.getOrNull("copper").sign, "'-' on a known map key means remove");
        assertEquals("-", ammo.getOrNull("copper").value);
        assertSame(ModifierSign.PLUS, ammo.getOrNull("blast-compound").sign, "an unknown map key is appended");
    }

    @Test
    void typeKey(){
        ObjectNode bullet = ObjectResolver.getTemplate(BulletType.class, ObjectResolver.patch);
        PatchNode patch = HeadlessEnv.parse(bullet, """
        hitEffect: {
          type: MultiEffect
          effects: [
            {
              type: WaveEffect
            }
          ]
        }
        """);

        PatchNode hitEffect = patch.getOrNull("hitEffect");
        assertSame(ModifierSign.MODIFY, hitEffect.sign, "a typed object overrides its field");
        assertEquals("MultiEffect", hitEffect.getOrNull("type").value);
        assertNull(hitEffect.value, "a typed object has no scalar value");

        PatchNode element = hitEffect.getOrNull("effects").getOrNull("0");
        assertSame(ModifierSign.PLUS, element.sign, "array elements are appended");
        assertEquals("WaveEffect", element.getOrNull("type").value, "the element keeps its own type key");
    }

    @Test
    void fieldMetadata(){
        assertFalse(PatchJsonIO.getFields(Block.class).isEmpty());
        assertFalse(PatchJsonIO.getFields(BasicBulletType.class).isEmpty());
        assertTrue(PatchJsonIO.getFields(Block.class).containsKey("health"), "metadata is keyed by field name");
    }

    private static String value(PatchNode patch, String path){
        PatchNode node = patch.navigateChild(path, false);
        assertNotNull(node, "expected a node at '" + path + "'");
        return node.value;
    }

    private static Jtype type(PatchNode patch, String path){
        PatchNode node = patch.navigateChild(path, false);
        assertNotNull(node, "expected a node at '" + path + "'");
        return node.type;
    }
}
