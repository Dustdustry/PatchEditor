package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.struct.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.core.resolve.*;
import org.junit.jupiter.api.*;

/** Importing a snippet as new array elements instead of replacing the array. */
@DisplayName("Append elements")
class AppendElementsTest extends HeadlessTest{

    private static EditorNode node(String path, NodeManager manager){
        EditorNode root = new EditorNode(HeadlessEnv.patchRoot(), manager);
        EditorNode node = root.navigate(path);
        assertNotNull(node, "the game must resolve '" + path + "'");
        return node;
    }

    @Test
    void objectPayloadIsOneElement(){
        NodeManager manager = new NodeManager();
        EditorNode node = node("unit.toxopid.weapons", manager);

        node.appendElements("{ name: w1 }");

        PatchNode patch = manager.getRoot().navigateChild("unit.toxopid.weapons", false);
        assertNotNull(patch, "the array node was created");
        assertSame(Jtype.object, patch.type, "an append container is an object holding the '+' entries");

        PatchNode appended = patch.getOrNull(PatchJsonIO.appendPrefix + "0");
        assertNotNull(appended, "the element landed in an append node");
        assertSame(ModifierSign.PLUS, appended.sign, "an appended element is marked as an append");
        assertEquals("w1", appended.getOrNull("name").value);
        assertNull(patch.getOrNull(PatchJsonIO.appendPrefix + "1"), "exactly one element was appended");

        JNode tree = JsonTransform.toJsonTree(patch);
        JNode plus = tree.get(ModifierSign.PLUS.sign);
        assertNotNull(plus, "the append folds into the '+' key");
        assertTrue(plus.isArray());
        assertEquals("w1", plus.get(0).get("name").asString(), "the element is inside the appended array");
    }

    @Test
    void arrayPayloadIsOneElementEach(){
        NodeManager manager = new NodeManager();
        EditorNode weapons = node("unit.toxopid.weapons", manager);

        weapons.appendElements("[{ name: w1}, { name: w2 }]");

        PatchNode patch = manager.getRoot().navigateChild("unit.toxopid.weapons", false);
        assertEquals("w1", patch.getOrNull(PatchJsonIO.appendPrefix + "0").getOrNull("name").value);
        assertEquals("w2", patch.getOrNull(PatchJsonIO.appendPrefix + "1").getOrNull("name").value);
        assertSame(ModifierSign.PLUS, patch.getOrNull(PatchJsonIO.appendPrefix + "1").sign);
    }

    @Test
    void valuePayloadUsesTheElementSugar(){
        NodeManager manager = new NodeManager();
        EditorNode requirements = node("unit.avert.cachedRequirements", manager);

        requirements.appendElements("titanium/10");

        PatchNode appended = manager.getRoot().navigateChild("unit.avert.cachedRequirements", false)
        .getOrNull(PatchJsonIO.appendPrefix + "0");
        assertEquals("titanium", appended.getOrNull("item").value, "the item/amount shorthand is expanded");
        assertEquals("10", appended.getOrNull("amount").value);
    }

    @Test
    void appendingAddsToTheExistingAppends(){
        NodeManager manager = new NodeManager();
        EditorNode weapons = node("unit.toxopid.weapons", manager);

        weapons.appendElements("{ name: w1 }");
        weapons.appendElements("{ name: w2 }");

        PatchNode patch = manager.getRoot().navigateChild("unit.toxopid.weapons", false);
        assertEquals("w1", patch.getOrNull(PatchJsonIO.appendPrefix + "0").getOrNull("name").value);
        assertEquals("w2", patch.getOrNull(PatchJsonIO.appendPrefix + "1").getOrNull("name").value);
    }

    @Test
    void overridingArrayAppendsByIndex(){
        NodeManager manager = new NodeManager();
        EditorNode weapons = node("unit.toxopid.weapons", manager);

        weapons.setSign(ModifierSign.MODIFY);
        weapons.appendElements("{ name: w1 }");

        PatchNode patch = manager.getRoot().navigateChild("unit.toxopid.weapons", false);
        assertSame(Jtype.array, patch.type, "an overriding array stays an array");
        assertNull(patch.getOrNull(PatchJsonIO.appendPrefix + "0"), "no append marker inside an override");
        assertSame(ModifierSign.PLUS, patch.getOrNull("0").sign, "an element is marked appended, as AppendOp does");
        assertEquals("w1", patch.getOrNull("0").getOrNull("name").value);
    }

    @Test
    void multiArrayPayloadAppendsOneRow(){
        NodeManager manager = new NodeManager();
        EditorNode upgrades = node("block.additive-reconstructor.upgrades", manager);

        upgrades.appendElements("[[dagger, stell]]");

        PatchNode row = manager.getRoot().navigateChild("block.additive-reconstructor.upgrades", false)
        .getOrNull(PatchJsonIO.appendPrefix + "0");
        assertSame(Jtype.array, row.type, "a multi-array element is a row");
        assertSame(ModifierSign.PLUS, row.sign);
        assertEquals("dagger", row.getOrNull("0").value);
        assertEquals("stell", row.getOrNull("1").value);
    }

    @Test
    void nonArrayNodesAreIgnored(){
        NodeManager manager = new NodeManager();
        EditorNode duo = node("block.duo", manager);

        duo.appendElements("{ health: 100 }");

        assertNull(manager.getRoot().navigateChild("block.duo", false), "nothing was written");
    }

    @Test
    void parseElementsSplitsThePayload(){
        ObjectNode weapons = HeadlessEnv.contentNode("unit.toxopid.weapons");

        assertEquals(1, PatchJsonIO.parseElements(weapons, "{ name: w1 }").size);

        Seq<PatchNode> many = PatchJsonIO.parseElements(weapons, """
        [
          {
            name: w1
          }
          {
            name: w2
          }
        ]
        """);
        assertEquals(2, many.size);
        assertEquals("w1", many.get(0).getOrNull("name").value);
        assertEquals("w2", many.get(1).getOrNull("name").value);

        assertEquals(0, PatchJsonIO.parseElements(HeadlessEnv.contentNode("block.duo"), "{}").size,
        "a node without an element type has nothing to append");
    }

    @Test
    void theOperatorIsUndoable(){
        ObjectNode weapons = HeadlessEnv.contentNode("unit.toxopid.weapons");
        NodeManager manager = new NodeManager();

        manager.applyOp(new PatchOperator.AppendElementsOp(weapons.name,
        PatchJsonIO.parseElements(weapons, "{ name: w1 }"),
        weapons.elementType, true, weapons.getResolutionStrategy()));

        assertNotNull(manager.getRoot().navigateChild(weapons.name, false), "the array node was created");

        manager.undo();
        assertNull(manager.getRoot().navigateChild(weapons.name, false), "undo removed it again");

        manager.redo();
        assertEquals("w1", manager.getRoot().navigateChild(weapons.name, false)
        .getOrNull(PatchJsonIO.appendPrefix + "0").getOrNull("name").value, "redo brought the element back");
    }
}
