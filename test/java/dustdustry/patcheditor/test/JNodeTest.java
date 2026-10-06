package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.struct.*;
import arc.util.serialization.*;
import dustdustry.patcheditor.core.*;
import java.util.*;
import org.junit.jupiter.api.*;

/** The reused child iterator of {@link JNode}. */
@DisplayName("JNode iterator")
class JNodeTest{

    @Test
    void walksChildrenInOrder(){
        JNode node = JNode.of(Jval.read("{ a: 1, b: 2, c: 3 }"));

        Iterator<JNode> it = node.iterator();
        assertEquals("a", it.next().name);
        assertEquals("b", it.next().name);
        assertEquals("c", it.next().name);
        assertFalse(it.hasNext());
    }

    @Test
    void reusesIteratorInstances(){
        JNode node = JNode.of(Jval.read("{ a: 1, b: 2 }"));

        Iterator<JNode> first = node.iterator();
        while(first.hasNext()) first.next();
        assertSame(first, node.iterator(), "a finished loop hands its instance back");

        Iterator<JNode> outer = node.iterator();
        Iterator<JNode> inner = node.iterator();
        Iterator<JNode> deep = node.iterator();
        assertNotSame(outer, inner, "nested loops get different instances");
        assertNotSame(inner, deep, "3+ nested loops fall back to a fresh instance");

        assertEquals("a", outer.next().name);
        assertEquals("a", inner.next().name);
        assertEquals("a", deep.next().name);
    }

    @Test
    void arrayChildrenAreIndexed(){
        JNode node = JNode.of(Jval.read("[1, 2, 3]"));

        int i = 0;
        for(JNode child : node){
            assertEquals(String.valueOf(i++), child.name, "array children are named by their index");
        }
        assertEquals(3, i);
    }

    @Test
    void removeDropsTheCurrentChild(){
        JNode node = JNode.of(Jval.read("{ a: 1, b: 2, c: 3 }"));

        for(Iterator<JNode> it = node.iterator(); it.hasNext(); ){
            JNode child = it.next();
            if("b".equals(child.name)) it.remove();
        }

        assertEquals(2, node.size());
        assertEquals("a", node.first().name);
        assertEquals("c", node.get(1).name, "the following child is still visited");
    }

    @Test
    void removeBeforeNextIsRejected(){
        JNode node = JNode.of(Jval.read("[1, 2]"));

        assertThrows(IllegalStateException.class, () -> node.iterator().remove());
    }

    @Test
    void valuesHaveNoChildren(){
        assertFalse(JNode.of(Jval.valueOf("text")).iterator().hasNext());
    }

    @Test
    void iterationIsSnapshot(){
        JNode node = JNode.of(Jval.read("{ a: 1 }"));

        int visited = 0;
        for(JNode child : node){
            if(visited++ == 0) node.addChild(JNode.of("b", "2"));
        }

        assertEquals(1, visited, "the loop runs over the children the node had when it started");
        assertEquals(2, node.size());
    }

    @Test
    void removalDoesNotSkipTheNextChild(){
        JNode node = JNode.of(Jval.read("[a, b, c, d]"));

        int visited = 0;
        for(JNode child : node){
            visited++;
            if("b".equals(child.asString())) child.remove();
        }

        assertEquals(4, visited, "every child is visited exactly once");
        assertEquals(3, node.size());
        assertEquals("a", node.get(0).asString());
        assertEquals("c", node.get(1).asString(), "the child after the removed one is not skipped");
        assertEquals("d", node.get(2).asString());
        assertEquals("1", node.get(1).name, "the array is re-indexed after the removal");
    }

    @Test
    void staleWrappersCannotWriteBack(){
        JNode node = JNode.of(Jval.read("{ a: 1, b: 2 }"));

        Seq<JNode> children = node.children();
        node.clear();

        children.get(0).setName("renamed");
        children.get(0).set("3");
        children.get(1).remove();

        assertEquals(0, node.size(), "a child taken out of a cleared node must not write back into it");
        assertNull(node.first(), "the cleared node stays empty");
        assertEquals("renamed", children.get(0).name, "the wrapper itself still renames");

        node.addChild(JNode.of("a", "4"));
        children.get(0).remove();
        assertEquals(1, node.size(), "a stale wrapper must not remove a key that now belongs to another child");
        assertEquals("4", node.get("a").asString());
    }

    @Test
    void detachedChildrenAreOrphaned(){
        JNode node = JNode.of(Jval.read("{ a: 1, b: 2 }"));

        Seq<JNode> children = node.detachChildren();

        assertEquals(0, node.size(), "the node is emptied");
        assertEquals(2, children.size, "every child is handed over, in order");
        assertEquals("a", children.get(0).name);
        assertEquals("b", children.get(1).name);
        assertNull(children.get(0).parent, "a detached child no longer points at the node");

        children.get(0).setName("renamed");
        assertEquals(0, node.size(), "the node stays empty");
        assertEquals("renamed", children.get(0).name);

        node.addChild(children.get(1));
        assertEquals(1, node.size(), "a detached child can be attached again");
        assertEquals("b", node.first().name);
    }
}
