package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.modifier.*;
import org.junit.jupiter.api.*;

/**
 * The upcoming editors (autotile regions, choosing none for effects and sounds) write their value
 * through the modifier table, so the write and clear paths are pinned here. Every concrete modifier
 * shares {@code ValueModifier.writeValue}, so two representative value types plus the clearing branch
 * cover the path without repeating the same assertion for each of them.
 */
@DisplayName("Modifier writes")
class ModifierWriteTest extends HeadlessTest{

    private NodeManager manager;
    private EditorNode root;

    @BeforeEach
    void buildTree(){
        manager = new NodeManager();
        root = new EditorNode(HeadlessEnv.patchRoot(), manager);
    }

    @Test
    void writesANumber(){
        //a number field edits text: NumberModifier is a StringModifier with a number value type
        EditorNode node = node("unit.toxopid.health");
        modifier("unit.toxopid.health").onModify("300");

        assertSame(Jtype.number, node.getPatch().type, "a number field writes a number");
        assertEquals("300", node.getPatch().value, "the typed text is what lands in the patch");
        assertEquals(300f, node.getDisplayValue(), "and the node reads back what the editor wrote");
    }

    @Test
    void writesABoolean(){
        EditorNode node = node("unit.toxopid.canBoost");
        modifier("unit.toxopid.canBoost").onModify(true);

        assertSame(Jtype.bool, node.getPatch().type);
        assertEquals(Boolean.TRUE, node.getDisplayValue());
    }

    @Test
    void writingTheOriginalValueClearsTheField(){
        EditorNode node = node("unit.toxopid.health");
        DataModifier<Object> modifier = modifier("unit.toxopid.health");

        Object original = modifier.getValue();
        assertEquals("200.0", original, "the field shows the original value's text");

        modifier.onModify(original);

        assertNull(node.getPatch(), "writing the original value clears the patch instead of storing it");
    }

    private EditorNode node(String path){
        EditorNode node = root.navigate(path);
        assertNotNull(node, path + " must resolve");
        return node;
    }

    @SuppressWarnings("unchecked")
    private DataModifier<Object> modifier(String path){
        EditorNode node = node(path);
        DataModifier<?> modifier = NodeModifier.getModifier(node.getObjNode());
        assertNotNull(modifier, path + " must have a modifier");
        modifier.setData(root, path);
        return (DataModifier<Object>)modifier;
    }
}
