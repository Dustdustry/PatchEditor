package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.core.resolve.*;
import mindustry.entities.bullet.*;
import org.junit.jupiter.api.*;

/** Importing a patch snippet: what the anchor keeps and what the payload brings. */
@DisplayName("Import operator")
class ImportOpTest extends HeadlessTest{

    @Test
    void importKeepsTheAnchorRole(){
        PatchNode root = new PatchNode("");
        PatchNode anchor = root.getOrCreate("weapons").getOrCreate(PatchJsonIO.appendPrefix + "0");
        anchor.sign = ModifierSign.PLUS;

        PatchNode source = new PatchNode("weapons");
        PatchJsonIO.parseJson(HeadlessEnv.contentNode("unit.avert"), source, "health: 100");
        assertNull(source.sign, "an object snippet has no sign of its own");

        new PatchOperator.ImportOp("weapons." + PatchJsonIO.appendPrefix + "0", source).apply(root);

        assertSame(ModifierSign.PLUS, anchor.sign, "the anchor keeps the role the target tree gave it");
        assertEquals("100", anchor.getOrNull("health").value, "the payload lands inside it");
    }

    @Test
    void importBringsChildSigns(){
        PatchNode root = new PatchNode("");
        PatchNode anchor = root.getOrCreate("duo");

        PatchNode source = new PatchNode("duo");
        PatchJsonIO.parseJson(HeadlessEnv.contentNode("block.duo"), source, """
        ammoTypes: {
          titanium: {
            type: BasicBulletType
          }
        }
        """);
        assertSame(ModifierSign.PLUS, source.getOrNull("ammoTypes").getOrNull("titanium").sign,
        "an unknown map key is an append in the target context");

        new PatchOperator.ImportOp("duo", source).apply(root);

        assertSame(ModifierSign.PLUS, anchor.getOrNull("ammoTypes").getOrNull("titanium").sign,
        "child signs are part of the payload");
    }

    @Test
    void importingAnArrayKeepsTheOverrideIntent(){
        //weapons already has a value, so its card is enabled: the page (and its import button)
        //can be opened without clicking the override button first.
        ObjectNode target = HeadlessEnv.contentNode("unit.toxopid.weapons");
        assertNotNull(target.object, "a field with a value is importable in the UI");

        PatchNode parsed = new PatchNode("weapons");
        PatchJsonIO.parseJson(target, parsed, "[{ name: w1 }]");
        assertSame(Jtype.array, parsed.type);
        assertSame(ModifierSign.MODIFY, parsed.sign, "a bare array overrides the container");

        PatchNode source = new PatchNode("weapons");
        PatchJsonIO.parseJson(target, source, "[{ name: w1 }]");
        assertSame(ModifierSign.MODIFY, source.sign, "an array payload carries the override role");

        PatchNode root = new PatchNode("");
        new PatchOperator.ImportOp("unit.toxopid.weapons", source).apply(root);

        PatchNode imported = root.navigateChild("unit.toxopid.weapons", false);
        assertEquals("w1", imported.getOrNull("0").getOrNull("name").value, "the payload landed");
        assertSame(Jtype.array, imported.type);
        assertSame(parsed.sign, imported.sign, "importing leaves the same state as parsing the snippet");
    }

    @Test
    void objectPayloadOntoAnUnsignedAnchorStaysUnsigned(){
        //The UI cannot reach this: a null-valued node needs the override button before its card
        //(and the import button in the title bar) is available. Recorded for the API's behaviour.
        ObjectNode target = ObjectResolver.getTemplate(BulletType.class, ObjectResolver.patch).getOrResolve("fragBullet");
        assertNull(target.object, "the field has no value yet");

        PatchNode source = new PatchNode("fragBullet");
        PatchJsonIO.parseJson(target, source, "{damage: 5}");

        PatchNode root = new PatchNode("");
        new PatchOperator.ImportOp("fragBullet", source).apply(root);

        PatchNode imported = root.navigateChild("fragBullet", false);
        assertEquals("5", imported.getOrNull("damage").value, "the payload landed");
        assertNull(imported.sign, "an object payload brings no role of its own");
    }
}
