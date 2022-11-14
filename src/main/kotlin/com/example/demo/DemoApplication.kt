package com.example.demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random


abstract class ServiceData {
    data class Entity(val name: String)

    enum class NodeType {
        EVENTSET, DATASET, DAEMON, GRPC
    }
    data class Node(
        val nodeType: NodeType,
        val name: String,
        val live: Boolean = Random.nextInt(100) > 2
    ) {
        override fun toString(): String =
            when (nodeType) {
                NodeType.DATASET -> "[$name dataset]"
                NodeType.EVENTSET -> "[$name events]"
                NodeType.DAEMON -> "$name daemon"
                NodeType.GRPC -> "$name gRPC"
            }.let {
                if (live) it else "$it (offline)"
            }

        fun json(comma: Boolean = true): String =
            "[\"$this\",\"$nodeType\",$live]${if (comma) "," else ""}"
    }

    data class SankeyFlow(val from: Node, val to: Node, val weight: Int) {
        private val enabled: Boolean = from.live && to.live
        private val nodeColor = "color: " + if (!enabled) "#ff0000" else getNextColor()

        fun json(comma: Boolean = true): String =
            "[\"$from\",\"$to\",$weight, \"$nodeColor\" ]${if (comma) "," else ""}"

        companion object {
            private val colors =
                arrayOf("#a6cee3", "#b2df8a", "#fb9a99", "#fdbf6f", "#cab2d6", "#ffff99", "#1f78b4", "#33a02c")
            private val nextColor =
                colors.mapIndexed { idx, c -> c to colors[if (idx == colors.size - 1) 0 else idx + 1] }.toMap()
            private var lastColor = colors.last()
            fun getNextColor(): String = lastColor.also { lastColor = nextColor[lastColor]!! }
        }
    }

        abstract val entities: List<Entity>
        abstract fun serviceGroup(entity: Entity): List<SankeyFlow>
        abstract var flows: List<SankeyFlow>
}

open class RedactedServices : ServiceData() {
    override val entities = emptyList<Entity>()
    override fun serviceGroup(entity: Entity): List<SankeyFlow> = emptyList()
    override var flows: List<SankeyFlow> = emptyList()
}



@RestController
@RequestMapping("/chart")
@SuppressWarnings("unused")
class ChartDataRouter : RedactedServices() {

    @GetMapping(
        path = ["/serviceflow"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @SuppressWarnings("unused")
    fun serviceFlow(): Flow<String> = flow {
        suspend fun output(x: String) = emit(x).also { println(x) }
        println("==== Service Flow ====")
        output("[")
        flows.take(flows.size - 1).forEach { output(it.json()) }
        output(flows.last().json(false))
        output("]")
    }

    @GetMapping(
        path = ["/servicestatus"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @SuppressWarnings("unused")
    fun serviceStatus(): Flow<String> = flow {
        delay(50)
        suspend fun output(x: String) = emit(x).also { println(x) }
        val nodes = (flows.map { it.from } + flows.map { it.to }).distinct()

        println("==== Service Status ====")
        output("[")
        nodes.take(nodes.size - 1).forEach { output(it.json()) }
        output(nodes.last().json(false))
        output("]")
    }
}

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}
