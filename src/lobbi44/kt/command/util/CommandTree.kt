package lobbi44.kt.command.util

import java.security.InvalidParameterException
import java.util.*

/**
 * @author lobbi44
 *
 * This class provides an uni-directional tree that can be queried from top to bottom.
 * Each Node can either have children or be the end node with the actual value.
 * Moreover this tree can have arbitrary root nodes
 *
 * @param K The type of the name used to identify the branches
 * @param T The type of the actual values stored in the leaves
 */

class CommandTree<K, T> {

    /**
     * The very first node in a tree structure always has an identifier of null
     * and is only used internally
     */
    private val rootNode: Node<K>

    constructor() {
        rootNode = Node(null)
    }

    private constructor(node: Node<K>) {
        rootNode = node
    }

    fun getChild(name: K): CommandTree<K, T> {
        return CommandTree(rootNode.getChild(name))
    }

    fun getChild(names: List<K>): CommandTree<K, T> {
        val endNode = names.fold(rootNode, { node, name -> node as? EndNode<K, *> ?: node.getChild(name) })
        return CommandTree(endNode)
    }

    fun getChildren(): Set<K> {
        return rootNode.getChildren()
    }

    /**
     * Adds a node to the current level of the tree.
     * If the value is not null this node will be an end node
     * and no further notes can be attached to it
     */
    fun addChild(name: K, value: T? = null) {
        val node = makeNode(name, value)
        rootNode.addChild(node)
    }

    /**
     * Creates a chain of nodes in a hierarchical order descending from the roots of this tree.
     * If a node with a given name already exists it will be ignored.
     * If a value != null is given the last created child will be an end node
     * with the given value associated with it
     *
     * names = {n1, n2, n3, n4}, value = "Test" will result in a n1->n2->n3->n4("Test") tree structure
     *
     * @param names The chain of children to create
     * @return A CommandTree originating from the last child in the chan
     */
    fun addChain(names: List<K>, value: T? = null): CommandTree<K, T> {

        val node = names.foldIndexed(rootNode, { i, node: Node<K>, name: K ->
            val n = if (value != null && i == names.lastIndex) EndNode(name, value) else Node(name)
            node.addChildIfAbsent(n)
        })
        return CommandTree(node)
    }

    fun getValue(names: List<K>): T? {
        val endNode = names.fold(rootNode, { node, name -> node.getChild(name) })
        return getEndNodeValue(endNode)
    }

    /**
     * Follows the tree until it comes to an end node.
     * All following entries in the list are ignored
     */
    fun getValueIgnored(names: List<K>): DepthValue<T?> {
        //todo: https://discuss.kotlinlang.org/t/unit-return-value-useful-to-break-out-of-nested-lambdas/2083 to make more performant
        var i = 0

        names.foldIndexed(rootNode, { i, node, name ->
            val child = node.getChild(name)
            if (child is EndNode<K, *>)
                return DepthValue(i + 1, getEndNodeValue(node))
            else
                child
        })
        //This can never be reached
        return DepthValue(0, null)
    }

    data class DepthValue<T>(val depth: Int, val value: T)

    /**
     * @return returns null if the value is not an end node
     */
    private fun getEndNodeValue(endNode: Node<K>): T? {
        @Suppress("UNCHECKED_CAST")
        return (endNode as? EndNode<K, T>)?.value
    }

    fun hasChild(name: K) = rootNode.hasChild(name)


    private fun makeNode(name: K, value: T? = null) = if (value == null) Node(name) else EndNode(name, value)


    /**
     * The nodes the tree consists of. This class provides methods to query the
     * subNodes by their corresponding name.
     * If the name is null, this node cannot be a subnode of any other node, therefore it has to be a rootnode
     * @param K The type of the name-identifier used to query the branches
     */
    private open class Node<K>(val name: K?) {
        /**
         * This hashMap stores the branches originating from this node.
         * It maps each subNode to its identifier.
         */
        private val subNodes = HashMap<K, Node<K>>()

        /**
         * Adds a child node if it is not already in the collection
         * @throws InvalidParameterException if a node with the same name already exists
         * @return The newly created node
         */
        fun addChild(node: Node<K>): Node<K> {
            //returns the old value if a value is already present
            if (node.name == null) throw IllegalArgumentException("A subnode cannot have a name of null")
            val nodeValue = subNodes.putIfAbsent(node.name, node)
            //old value or null if successfull
            if (nodeValue != null)
                throw InvalidParameterException("A node with name ${node.name} is already present in the node ${name}")
            return node
        }

        /**
         * Adds a child node if it is not already in the collection.
         * Otherwise it returns the old node
         * @return The old node if a node with the same name already exists or the new one
         */
        fun addChildIfAbsent(node: Node<K>): Node<K> {
            if (node.name == null) throw IllegalArgumentException("A subnode cannot have a name of null")
            val oldVal = subNodes.putIfAbsent(node.name, node)
            if (oldVal != null) return oldVal else return node
        }

        fun hasChild(name: K) = subNodes.contains(name)

        fun getChild(name: K): Node<K> {
            val ret = subNodes[name]
            if (ret == null)
                throw IllegalArgumentException("A node with the name ${name} could not be found")
            return ret
        }

        fun getChildren(): Set<K> {
            return subNodes.keys
        }

    }

    /**
     * This node has no successors and stores the actual value that can be retrieved
     * from the CommandTree
     */
    private class EndNode<K, T>(name: K, val value: T) : Node<K>(name) {

    }

}
