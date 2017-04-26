package lobbi44.kt.command.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * @author lobbi44
 */
internal class CommandTreeTest {
    @Test
    fun addChildNode() {
        val tree = CommandTree<String, String>()
        tree.addChild("node1")
        assertTrue(tree.hasChild("node1"))
        assertFalse(tree.hasChild("not"))
    }

    @Test
    fun addChain() {
        val tree = CommandTree<String, String>()
        tree.addChain(listOf("node1", "node2", "node3"))
        assertTrue(tree.hasChild("node1"))
        assertFalse(tree.hasChild("node2"))
        assertTrue(tree.getChild("node1").hasChild("node2"))
        }

    @Test
    fun addChainWithValue() {
        val tree = CommandTree<String, String>()
        tree.addChain(listOf("node1", "node2", "node3"), "value1")
        assertEquals("value1", tree.getValue(listOf("node1", "node2", "node3")))
    }

    @Test
    fun getChild() {
        val tree = CommandTree<String, String>()
        tree.addChain(listOf("node1", "a1", "a2", "a3"))
        tree.addChain(listOf("node1", "b1", "b2", "b3"))
        tree.addChain(listOf("node1", "c1", "c2", "c3"))
        assertEquals(setOf("a1", "b1", "c1"), tree.getChild(listOf("node1")).getChildren())
    }

    @Test
    fun testGetValueIgnoredChildDepth() {
        val tree = CommandTree<String, String>()
        tree.addChain(listOf("a1", "a2", "a3"), "value1")
        tree.addChain(listOf("b1", "b2", "b3"), "value2")
        tree.addChain(listOf("c1", "c2", "c3"), "value3")
        assertEquals(3, tree.getValueIgnored(listOf("a1", "a2", "a3")).depth)
    }

    @Test
    fun testGetValueIgnoredCorrectValue() {
        val tree = CommandTree<String, String>()
        tree.addChain(listOf("a1", "a2", "a3"), "value1")
        tree.addChain(listOf("b1", "b2", "b3"), "value2")
        tree.addChain(listOf("c1", "c2", "c3"), "value3")
        assertEquals("value1", tree.getValueIgnored(listOf("a1", "a2", "a3")).value)
    }

}