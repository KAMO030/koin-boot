package io.github.kamo030.koinboot.component.koin.test

import io.github.kamo030.koinboot.core.*
import org.koin.core.component.KoinComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KoinPropertiesTest : KoinComponent {

    val properties = KoinProperties().apply {
        "server" {
            "host"("localhost")
            "port"(8080)
        }
        "database" {
            "host"("127.0.0.1")
            "port"(3306)
            "address"(listOf("127.0.0.1", "127.0.0.2"))
        }
        "database-list"(
            listOf(
                mapOf("host" to "127.0.0.1", "port" to 3306),
                mapOf("host" to "127.0.0.2", "port" to 3306)
            )
        )
    }

    val databaseHost by property<String>("database.address")

    @Test
    fun `test koin properties`() {
        assertEquals("localhost", properties["server.host"])
        assertEquals(8080, properties["server.port"])
        assertEquals("127.0.0.1", properties["database.host"])
        assertEquals(3306, properties["database.port"])
    }

    @Test
    fun `test koin one property`() {
        assertEquals("127.0.0.1", properties.asPropInstance<List<String>>("database.address")?.first())
    }

    @Test
    fun `test koin properties as instance`() {
        val rootProperties = properties.asPropInstance<RootProperties>()
        val serverPropInstance = properties.asPropInstance<ServerProperties>()
        val databasePropInstance = properties.asPropInstance<DatabaseProperties>()
        assertNotNull(rootProperties)
        assertNotNull(serverPropInstance)
        assertNotNull(databasePropInstance)
        assertEquals(rootProperties.server.host, serverPropInstance.host)
        assertEquals(rootProperties.server.port, serverPropInstance.port)
        assertEquals("localhost", serverPropInstance.host)
        assertEquals(8080, serverPropInstance.port)
        assertEquals(rootProperties.database.host, databasePropInstance.host)
        assertEquals(rootProperties.database.port, databasePropInstance.port)
        assertEquals("127.0.0.1", databasePropInstance.host)
        assertEquals(3306, databasePropInstance.port)
    }

    @Test
    fun `test koin properties on koin component`() {
        runKoinBoot {
            properties(properties)
        }
        assertEquals("127.0.0.1", databaseHost)
    }

    @Test
    fun `test koin array property`() {
        assertEquals("127.0.0.1", properties.asPropInstance<List<DatabaseItem>>("database-list")?.first()?.host)
    }

}

@KoinPropInstance
private data class RootProperties(
    val server: ServerProperties,
    val database: DatabaseProperties
)

@KoinPropInstance("server")
private data class ServerProperties(
    val host: String,
    val port: Int
)

@KoinPropInstance("database")
private data class DatabaseProperties(
    val host: String,
    val port: Int
)

@KoinPropInstance("database-list")
private data class DatabaseItem(
    val host: String,
    val port: Int
)