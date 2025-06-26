package io.github.kamo030.koinboot.component.ktor.test

import io.github.kamo030.KtorBootInitializer
import io.github.kamo030.koinboot.component.ktor.*
import io.github.kamo030.koinboot.core.getPropInstance
import io.github.kamo030.koinboot.core.runKoinBoot
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.mp.KoinPlatformTools
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KtorTest {

    @AfterEach
    fun stopKoin() =
        KoinPlatformTools.defaultContext().stopKoin()

    @Test
    fun `test ktor properties`() {
        val koin = runKoinBoot {
            KtorBootInitializer()
            properties {
                ktor_client_timeout_request = 1000
                ktor_client_timeout_connect = 1000
                ktor_client_timeout_socket = 1000
                ktor_client_logging_enabled = true
                ktor_client_logging_level = LogLevel.BODY
                ktor_client_request_follow_redirects = true
                ktor_client_request_user_agent = "Myuko Ktor Client"
                ktor_client_request_headers = mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "123"
                )
            }
        }

        val ktorProperties = koin.getPropInstance<KtorProperties>() ?: KtorProperties()
        assertEquals(1000, ktorProperties.client.timeout.request)
        assertEquals(LogLevel.BODY, ktorProperties.client.logging.level)
    }


    @Test
    fun `test ktor auto  module`() {
        val koin = runKoinBoot {
            KtorBootInitializer()
        }

        assertNotNull(koin.get<HttpClient>()) { "HttpClient not found" }
    }


    @Test
    fun `test ktor merge config`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = "OK",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }
        val koin = runKoinBoot {
            KtorBootInitializer()
            modules(module {
                single<HttpClientConfigDeclaration> {
                    {
                        defaultRequest {
                            url("https://ktor.io/docs/")
                        }
                    }

                }
                single<HttpClientEngine> { mockEngine }
            })
        }

        launch {
            val rep = koin.get<HttpClient>().get("")
            assertEquals(200, rep.status.value)
            assertEquals("https://ktor.io/docs/", rep.request.url.toString())
        }
    }

}