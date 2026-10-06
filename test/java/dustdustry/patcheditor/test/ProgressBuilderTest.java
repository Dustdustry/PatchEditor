package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.modifier.*;
import mindustry.entities.part.DrawPart.*;
import org.junit.jupiter.api.*;

/**
 * Progress chains. Parsing them goes through Mindustry's own private {@code ContentParser#parseProgressOp},
 * which is why this is also a reflection guard.
 */
@DisplayName("Progress builders")
class ProgressBuilderTest extends HeadlessTest{

    @Test
    void constantBases(){
        ProgressBuilder named = PatchJsonIO.parseProgressBuilder(json("\"warmup\""));
        assertFalse(named.isConstantBase());
        assertSame(PartProgress.warmup, named.base, "the named constant is resolved by reflection");
        assertSame(PartProgress.warmup, named.apply(), "with no operations the base itself is applied");

        ProgressBuilder number = PatchJsonIO.parseProgressBuilder(json("0.5"));
        assertTrue(number.isConstantBase());
        assertTrue(number.ops.isEmpty());
    }

    @Test
    void singleOperation(){
        ProgressBuilder builder = PatchJsonIO.parseProgressBuilder(json("""
        {
          type: warmup
          op: mul
          amount: 2
        }
        """));

        assertEquals(1, builder.ops.size);
        assertEquals("mul", builder.ops.first().name);
        assertEquals(2f, builder.ops.first().getFloat("amount"), 0.0001f);
        assertNotSame(PartProgress.warmup, builder.apply(), "applying an operation produces a new progress");
    }

    @Test
    void operationArray(){
        ProgressBuilder builder = PatchJsonIO.parseProgressBuilder(json("""
        {
          type: warmup
          ops: [
            {
              op: mul
              amount: 2
            }
            {
              op: clamp
            }
          ]
        }
        """));

        assertEquals(2, builder.ops.size);
        assertEquals("mul", builder.ops.get(0).name, "operations keep their order");
        assertEquals("clamp", builder.ops.get(1).name, "operations keep their order");
    }

    @Test
    void invalidOperationList(){
        try{
            PatchJsonIO.parseProgressBuilder(json("""
            {
              type: warmup
              ops: {
                op: mul
              }
            }
            """));
            fail("a non-array operation list must be rejected");
        }catch(RuntimeException expected){
            assertTrue(expected.getMessage().contains("array"), "the error says what was expected: " + expected.getMessage());
        }
    }

    @Test
    void toPatchNode(){
        ProgressBuilder builder = new ProgressBuilder(PartProgress.warmup);
        builder.mul(2f).clamp();

        PatchNode node = new PatchNode("progress");
        PatchJsonIO.toPatchNode(builder, node);

        assertEquals("warmup", node.getOrNull("type").value, "the base is written as a named constant");
        PatchNode ops = node.getOrNull("ops");
        assertSame(Jtype.array, ops.type);
        assertEquals("mul", ops.getOrNull("0").getOrNull("op").value);
        assertEquals("2.0", ops.getOrNull("0").getOrNull("amount").value);
        assertSame(Jtype.number, ops.getOrNull("0").getOrNull("amount").type, "numbers stay numbers");
        assertEquals("clamp", ops.getOrNull("1").getOrNull("op").value);
    }

    @Test
    void toPatchNodeConstants(){
        PatchNode named = new PatchNode("progress");
        PatchJsonIO.toPatchNode(new ProgressBuilder(PartProgress.warmup), named);
        assertEquals("warmup", named.value, "a bare constant is written as a value");
        assertTrue(named.children.isEmpty(), "no operations means no children");

        PatchNode number = new PatchNode("progress");
        PatchJsonIO.toPatchNode(new ProgressBuilder(0.5f), number);
        assertSame(Jtype.number, number.type);
        assertEquals("0.5", number.value);
    }

    @Test
    void roundTrip(){
        ProgressBuilder builder = PatchJsonIO.parseProgressBuilder(json("""
        {
          type: warmup
          ops: [
            {
              op: mul
              amount: 2
            }
            {
              op: delay
              amount: 0.25
            }
          ]
        }
        """));

        PatchNode node = new PatchNode("progress");
        PatchJsonIO.toPatchNode(builder, node);

        ProgressBuilder again = PatchJsonIO.parseProgressBuilder(JsonTransform.toJsonTree(node));
        assertEquals(builder.ops.size, again.ops.size);
        for(int i = 0; i < builder.ops.size; i++){
            assertEquals(builder.ops.get(i).name, again.ops.get(i).name, "operation " + i + " survives the round trip");
        }
    }

    private static JNode json(String hjson){
        return JNode.of(Jval.read(hjson));
    }
}
