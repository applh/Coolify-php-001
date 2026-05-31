package com.example.cameraxapp.roguelike

import com.example.cameraxapp.core.math3d.Vector3
import kotlin.math.sqrt

class SphereNode(
    val id: Int,
    val position: Vector3,
    val neighbors: MutableList<Int> = mutableListOf()
)

object IcosphereGenerator {
    fun generate(subdivisions: Int = 2): Map<Int, SphereNode> {
        val t = ((1.0 + sqrt(5.0)) / 2.0).toFloat()

        var vertices = mutableListOf(
            Vector3(-1f, t, 0f), Vector3(1f, t, 0f), Vector3(-1f, -t, 0f), Vector3(1f, -t, 0f),
            Vector3(0f, -1f, t), Vector3(0f, 1f, t), Vector3(0f, -1f, -t), Vector3(0f, 1f, -t),
            Vector3(t, 0f, -1f), Vector3(t, 0f, 1f), Vector3(-t, 0f, -1f), Vector3(-t, 0f, 1f)
        ).map { it.normalize() }.toMutableList()

        var faces = mutableListOf(
            listOf(0, 11, 5), listOf(0, 5, 1), listOf(0, 1, 7), listOf(0, 7, 10), listOf(0, 10, 11),
            listOf(1, 5, 9), listOf(5, 11, 4), listOf(11, 10, 2), listOf(10, 7, 6), listOf(7, 1, 8),
            listOf(3, 9, 4), listOf(3, 4, 2), listOf(3, 2, 6), listOf(3, 6, 8), listOf(3, 8, 9),
            listOf(4, 9, 5), listOf(2, 4, 11), listOf(6, 2, 10), listOf(8, 6, 7), listOf(9, 8, 1)
        )

        val cache = mutableMapOf<Long, Int>()

        fun getMiddlePoint(p1: Int, p2: Int): Int {
            val firstIsSmaller = p1 < p2
            val smallerIndex = if (firstIsSmaller) p1 else p2
            val greaterIndex = if (firstIsSmaller) p2 else p1
            val key = (smallerIndex.toLong() shl 32) + greaterIndex

            cache[key]?.let { return it }

            val point1 = vertices[p1]
            val point2 = vertices[p2]
            val middle = Vector3(
                (point1.x + point2.x) / 2f,
                (point1.y + point2.y) / 2f,
                (point1.z + point2.z) / 2f
            ).normalize()

            vertices.add(middle)
            val index = vertices.size - 1
            cache[key] = index
            return index
        }

        for (i in 0 until subdivisions) {
            val nextFaces = mutableListOf<List<Int>>()
            for (face in faces) {
                val a = getMiddlePoint(face[0], face[1])
                val b = getMiddlePoint(face[1], face[2])
                val c = getMiddlePoint(face[2], face[0])

                nextFaces.add(listOf(face[0], a, c))
                nextFaces.add(listOf(face[1], b, a))
                nextFaces.add(listOf(face[2], c, b))
                nextFaces.add(listOf(a, b, c))
            }
            faces = nextFaces
        }

        val nodes = vertices.mapIndexed { index, v -> SphereNode(index, v) }.associateBy { it.id }

        for (face in faces) {
            val (v1, v2, v3) = face
            if (!nodes[v1]!!.neighbors.contains(v2)) nodes[v1]!!.neighbors.add(v2)
            if (!nodes[v1]!!.neighbors.contains(v3)) nodes[v1]!!.neighbors.add(v3)

            if (!nodes[v2]!!.neighbors.contains(v1)) nodes[v2]!!.neighbors.add(v1)
            if (!nodes[v2]!!.neighbors.contains(v3)) nodes[v2]!!.neighbors.add(v3)

            if (!nodes[v3]!!.neighbors.contains(v1)) nodes[v3]!!.neighbors.add(v1)
            if (!nodes[v3]!!.neighbors.contains(v2)) nodes[v3]!!.neighbors.add(v2)
        }

        return nodes
    }
}
