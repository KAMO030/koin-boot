package io.github.kamo030.koinboot.component.ktor.test

import io.github.kamo030.koinboot.KtorBootInitializer
import io.github.kamo030.koinboot.core.runKoinBoot
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 测试Auth插件是否正确安装和生效
 */
class AuthPluginTest {

    @AfterEach
    fun stopKoin() =
        KoinPlatformTools.defaultContext().stopKoin()

    @Test
    fun `test Auth plugin installation with providers`() = runTest {
        // 设置Koin环境并提供AuthProvider
        val koin = runKoinBoot {
           KtorBootInitializer()
            modules(
                module {
                    // 注册模拟引擎
                    single<HttpClientEngine> {
                        MockEngine { request ->
                            // 验证Authorization头是否存在
                            val authHeader = request.headers[HttpHeaders.Authorization]
                            logger.info("Authorization header: $authHeader")
                            respond(
                                content = "Response",
                                status = HttpStatusCode.OK,
                                headers = headersOf("X-Auth-Present", authHeader?.let { "true" } ?: "false"),
                            )
                        }
                    }
                    // 注册模拟的AuthProvider
                    single<AuthProvider> {
                        BearerAuthProvider(
                            refreshTokens = {
                                // 模拟刷新令牌逻辑
                                BearerTokens("new-access-token", "new-refresh-token")
                            },
                            loadTokens = { BearerTokens("new-access-token", "new-refresh-token") },
                            sendWithoutRequestCallback = { true },
                            realm = "My Realm"
                        )
                    }
                }
            )
        }


        // 获取通过Koin创建的HttpClient
        val client = koin.get<HttpClient>()

        // 验证客户端确实安装了Auth插件
        assertNotNull(client.pluginOrNull(Auth), "Auth plugin should be installed with providers")

        // 执行请求并检查Auth插件是否生效
        val response = client.get("https://example.com")
        val authPresent = response.headers["X-Auth-Present"] == "true"

        // 如果Auth插件正确安装，请求应该包含Authorization头
        assertTrue(authPresent, "Auth plugin should add Authorization header to requests")


    }
}
