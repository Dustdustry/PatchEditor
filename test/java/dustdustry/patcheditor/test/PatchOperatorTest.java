package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.struct.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.core.resolve.*;
import mindustry.type.*;
import org.junit.jupiter.api.*;

/**
 * Every change to the patch tree goes through a {@link PatchOperator}, and every operator must be
 * exactly reversible: undo restores the whole subtree it touched, and an operator that changes
 * nothing must not push an entry onto the undo stack.
 */
@DisplayName("Patch operators")
class PatchOperatorTest extends HeadlessTest{

    private NodeManager manager;

    @BeforeEach
    void freshManager(){
        manager = new NodeManager();
    }

    @Test
    void importOpIsReversible(){
        manager.applyOp(new PatchOperator.ImportOp("unit.avert", parse("unit.avert", "health: 100")));
        assertNotNull(manager.getRoot().navigateChild("unit.avert", false), "the import created the node");

        manager.undo();
        assertNull(manager.getRoot().navigateChild("unit.avert", false), "undo removed the node it created");

        manager.redo();
        assertEquals("100", manager.getRoot().navigateChild("unit.avert", false).getOrNull("health").value,
        "redo brought the imported field back");
    }

    @Test
    void undoRestoresAMergedSubtree(){
        manager.applyOp(new PatchOperator.ImportOp("block.duo", parse("block.duo", "health: 100, size: 2")));
        PatchNode before = copy(manager.getRoot().navigateChild("block.duo", false));

        //an import merges: it overwrites what it carries and leaves the other fields alone
        manager.applyOp(new PatchOperator.ImportOp("block.duo", parse("block.duo", "health: 250, solid: false")));
        PatchNode merged = manager.getRoot().navigateChild("block.duo", false);
        assertEquals("250", merged.getOrNull("health").value, "the carried field was overwritten");
        assertEquals("2", merged.getOrNull("size").value, "a field the import did not mention is kept");
        assertEquals("false", merged.getOrNull("solid").value, "the new field was added");

        manager.undo();
        assertTreeEquals(before, manager.getRoot().navigateChild("block.duo", false), "undo restored every field");
    }

    @Test
    void appendOpIsReversible(){
        manager.applyOp(new PatchOperator.AppendOp("unit.toxopid.weapons", Weapon.class, true, ObjectResolver.patch));
        String path = "unit.toxopid.weapons." + PatchJsonIO.appendPrefix + "0";
        assertEquals(ModifierSign.PLUS, manager.getRoot().navigateChild(path, false).sign);

        manager.undo();
        assertNull(manager.getRoot().navigateChild("unit.toxopid.weapons", false), "the whole array node is gone again");
    }

    @Test
    void batchOpIsOneUndoStep(){
        manager.applyOp(new PatchOperator.ImportOp("block.duo", parse("block.duo", "health: 100, size: 2")));
        PatchNode before = copy(manager.getRoot().navigateChild("block.duo", false));

        //what EditorNode.setPatch does: drop the children, then import the new ones
        manager.applyOp(new PatchOperator.BatchOp("block.duo",
        new PatchOperator.ClearChildrenOp("block.duo"),
        new PatchOperator.ImportOp("block.duo", parse("block.duo", "health: 250"))));

        PatchNode replaced = manager.getRoot().navigateChild("block.duo", false);
        assertEquals("250", replaced.getOrNull("health").value);
        assertNull(replaced.getOrNull("size"), "the batch cleared the old children first");

        manager.undo();
        assertTreeEquals(before, manager.getRoot().navigateChild("block.duo", false),
        "one undo reverts the whole batch, not just its last operator");
    }

    @Test
    void operatorsThatChangeNothingStayOffTheUndoStack(){
        manager.applyOp(new PatchOperator.AppendOp("unit.toxopid.weapons", Weapon.class, true, ObjectResolver.patch));
        String appended = "unit.toxopid.weapons." + PatchJsonIO.appendPrefix + "0";

        //already PLUS, and an empty element list: neither changes anything
        manager.applyOp(new PatchOperator.SetSignOp(appended, ModifierSign.PLUS));
        manager.applyOp(new PatchOperator.AppendElementsOp("unit.toxopid.weapons", new Seq<PatchNode>(),
        Weapon.class, true, ObjectResolver.patch));

        manager.undo();
        assertFalse(manager.canUndo(), "only the AppendOp was an undo step");
        assertNull(manager.getRoot().navigateChild("unit.toxopid.weapons", false), "and it undid that one step");
    }

    private static PatchNode parse(String path, String patch){
        PatchNode node = new PatchNode(path);
        PatchJsonIO.parseJson(HeadlessEnv.contentNode(path), node, patch);
        return node;
    }

    private static PatchNode copy(PatchNode src){
        PatchNode copy = new PatchNode(src.key);
        copyInto(copy, src);
        return copy;
    }

    private static void copyInto(PatchNode target, PatchNode src){
        target.type = src.type;
        target.value = src.value;
        target.sign = src.sign;
        for(PatchNode child : src.children.values()){
            copyInto(target.getOrCreate(child.key), child);
        }
    }

    private static void assertTreeEquals(PatchNode expected, PatchNode actual, String message){
        assertNotNull(actual, message + ": the node is missing");
        assertEquals(expected.key, actual.key, message + ": key");
        assertSame(expected.type, actual.type, message + ": type of " + expected.key);
        assertEquals(expected.value, actual.value, message + ": value of " + expected.key);
        assertSame(expected.sign, actual.sign, message + ": sign of " + expected.key);
        assertEquals(expected.children.size, actual.children.size, message + ": children of " + expected.key);

        for(PatchNode child : expected.children.values()){
            assertTreeEquals(child, actual.getOrNull(child.key), message);
        }
    }
}
