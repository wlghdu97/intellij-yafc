package com.xhlab.yafc.model.math

import java.util.*
import kotlin.math.min

class Graph<T> : Iterable<Graph.Node<T>> {
    private val nodes = hashMapOf<T, Node<T>>()

    private fun getNode(src: T): Node<T> {
        val node = nodes[src]
        if (node != null) {
            return node
        }
        return Node(src).apply {
            nodes[src] = this
        }
    }

    fun connect(from: T, to: T) {
        getNode(from).addArc(getNode(to))
    }

    override fun iterator(): Iterator<Node<T>> {
        return nodes.values.iterator()
    }

    class Node<T> constructor(val userData: T) {
        private val arcs = arrayListOf<Node<T>>()

        val connections: List<Node<T>>
            get() = arcs

        fun addArc(node: Node<T>) {
            if (arcs.contains(node)) {
                return
            }
            arcs.add(node)
        }
    }

    fun <R> remap(map: (T) -> R): Graph<R> {
        val remapped = Graph<R>()
        for (node in this) {
            val remappedNode = map(node.userData)
            for (connection in node.connections) {
                remapped.connect(remappedNode, map(connection.userData))
            }
        }

        return remapped
    }

    fun <R> aggregate(create: (T) -> R, connection: (R, T, R) -> Unit): Map<T, R> {
        val aggregation = hashMapOf<T, R>()
        for (node in this) {
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
}

private const val UNDEFINED = -1
private const val NOT_ON_STACK = -2

// this result form will be changed when implementing AutoPlanner or ProductionTable
fun <T> Graph<T>.mergeStrongConnectedComponents(): List<Set<T>> {

    class State(var state: Int = UNDEFINED, var extra: Int = 0)

    val states = associateWith { State() }

    fun requireState(node: Graph.Node<T>): State {
        return states[node] ?: throw NullPointerException("graph node : $node not found or modified")
    }

    val result = arrayListOf<Set<T>>()
    val stack = Stack<Graph.Node<T>>()
    var index = 0

    fun strongConnect(root: Graph.Node<T>) {
        // Algorithm from https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
        // index => state
        // lowlink => extra
        // index is undefined => state == -1
        // notOnStack => state = -2
        // v => root
        // w => neighbour
        val rootState = requireState(root).apply {
            extra = index
            state = index
        }
        index += 1
        stack.push(root)

        for (neighbour in root.connections) {
            val neighbourState = requireState(neighbour)
            if (neighbourState.state == UNDEFINED) {
                strongConnect(neighbour)
                rootState.extra = min(rootState.extra, neighbourState.extra)
            } else if (neighbourState.state >= 0) {
                rootState.extra = min(rootState.extra, neighbourState.state)
            }
        }

        if (rootState.extra == rootState.state) {
            val loopSet = hashSetOf<T>()
            do {
                val w = stack.pop()
                requireState(w).state = NOT_ON_STACK
                loopSet.add(w.userData)
            } while (root != w)
            result.add(loopSet)
        }
    }

    for (node in this) {
        if (requireState(node).state == UNDEFINED) {
            strongConnect(node)
        }
    }

    return result
}

/**
 * Kahn's Algorithm from https://en.wikipedia.org/wiki/Topological_sorting
 *
 * @return topologically ordered list, or null when graph has at least one cycle,
 */
fun <T> Graph<T>.topologicalSort(): List<T>? {
    val size = count()
    val sorted = ArrayList<Graph.Node<T>>(size)

    val nodes = toList()
    val degrees = IntArray(count()) { 0 }
    forEach { node ->
        node.connections.forEach {
            degrees[indexOf(it)] += 1
        }
    }

    val stack: Stack<Graph.Node<T>> = Stack<Graph.Node<T>>().apply {
        degrees.forEachIndexed { index, degree ->
            if (degree == 0) {
                push(nodes[index])
            }
        }
    }

    while (stack.isNotEmpty()) {
        val node = stack.pop()
        sorted.add(node)
        node.connections.forEach {
            val idx = nodes.indexOf(it)
            degrees[idx] -= 1
            if (degrees[idx] == 0) {
                stack.push(it)
            }
        }
    }

    return if (sorted.size != size) {
        null
    } else {
        sorted.map { it.userData }
    }
}

