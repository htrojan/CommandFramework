package lobbi44.kt.command.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

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

}