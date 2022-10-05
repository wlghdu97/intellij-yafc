package com.xhlab.yafc.model.math

import java.util.*
import kotlin.math.min

class Graph<T> : Iterable<Graph.Node<T>> {
    private val nodes = hashMapOf<T, Node<T>>()
    private val allNodes = arrayListOf<Node<T>>()

    private fun getNode(src: T): Node<T> {
        val node = nodes[src]
        if (node != null) {
            return node
        }
        return Node(this, src).apply {
            nodes[src] = this
        }
    }

    fun connect(from: T, to: T) {
        getNode(from).addArc(getNode(to))
    }

    fun hasConnection(from: T, to: T): Boolean {
        return getNode(from).hasConnection(getNode(to))
    }

    fun getConnections(from: T) = getNode(from).connections

    override fun iterator(): Iterator<Node<T>> {
        return allNodes.iterator()
    }

    class Node<T> constructor(
        graph: Graph<T>,
        val userData: T
    ) {
        val id = graph.allNodes.size
        internal var state = 0
        internal var extra = 0
        private val arcs = arrayListOf<Node<T>>()

        val connections: List<Node<T>>
            get() = arcs

        init {
            graph.allNodes.add(this)
        }

        fun addArc(node: Node<T>) {
            if (arcs.contains(node)) {
                return
            }
            arcs.add(node)
        }

        fun hasConnection(node: Node<T>): Boolean {
            return arcs.contains(node)
        }
    }

    fun <R> remap(map: (T) -> R): Graph<R> {
        val remapped = Graph<R>()
        for (node in allNodes) {
            val remappedNode = map(node.userData)
            for (connection in node.connections) {
                remapped.connect(remappedNode, map(connection.userData))
            }
        }

        return remapped
    }

    fun <R> aggregate(create: (T) -> R, connection: (R, T, R) -> Unit): Map<T, R> {
        val aggregation = hashMapOf<T, R>()
        for (node in allNodes) {
            aggregateInternal(node, create, connection, aggregation)
        }
        return aggregation
    }

    private fun <R> aggregateInternal(
        node: Node<T>,
        create: (T) -> R,
        connection: (R, T, R) -> Unit,
        dict: MutableMap<T, R>
    ): R {
        val result = dict[node.userData]
        if (result != null) {
            return result
        }
        val newResult = create(node.userData)
        dict[node.userData] = newResult
        for (con in node.connections) {
            connection(newResult, con.userData, aggregateInternal(con, create, connection, dict))
        }
        return newResult
    }

    // this result form will be changed when implementing AutoPlanner or ProductionTable
    fun mergeStrongConnectedComponents(): List<Set<T>> {
        for (node in allNodes) {
            node.state = UNDEFINED
        }
        val result = arrayListOf<Set<T>>()
        val stack = Stack<Node<T>>()
        var index = 0

        fun strongConnect(root: Node<T>) {
            // Algorithm from https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
            // index => state
            // lowlink => extra
            // index is undefined => state == -1
            // notOnStack => state = -2
            // v => root
            // w => neighbour
            root.extra = index
            root.state = index
            index += 1
            stack.push(root)

            for (neighbour in root.connections) {
                if (neighbour.state == UNDEFINED) {
                    strongConnect(neighbour)
                    root.extra = min(root.extra, neighbour.extra)
                } else if (neighbour.state >= 0) {
                    root.extra = min(root.extra, neighbour.state)
                }
            }

            if (root.extra == root.state) {
                val loopSet = hashSetOf<T>()
                do {
                    val w = stack.pop()
                    w.state = NOT_ON_STACK
                    loopSet.add(w.userData)
                } while (root != w)
                result.add(loopSet)
            }
        }

        for (node in allNodes) {
            if (node.state == UNDEFINED) {
                strongConnect(node)
            }
        }

        return result
    }

    companion object {
        private const val UNDEFINED = -1
        private const val NOT_ON_STACK = -2
    }
}
