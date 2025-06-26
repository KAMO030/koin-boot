package io.github.kamo030.koinboot.component.ktor

import io.github.kamo030.koinboot.core.configuration.koinAutoConfiguration
import io.github.kamo030.koinboot.core.configuration.missInstances
import io.github.kamo030.koinboot.core.configuration.onMissInstances
import io.github.kamo030.koinboot.core.getPropInstance
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier._q
import org.koin.core.scope.Scope
import kotlin.time.Duration.Companion.milliseconds

typealias HttpClientConfigDeclaration = HttpClientConfig<*>.() -> Unit

typealias CookiesStorageInitializer = suspend CookiesStorage.() -> Unit

private val KtorQualifier: Qualifier
    get() = _q("Ktor")

internal val KtorAutoConfiguration = koinAutoConfiguration {
    val properties = koin.getPropInstance<KtorProperties> { KtorProperties() }
    module {
        // 用户没有配置 Json 序列化器的情况下，自动使用默认配置
        // KtorQualifier 防止用户没有配置 Json 时被其他对象所引用
        onMissInstances<Json> {
            single<Json>(KtorQualifier) {
                logger.debug("Ktor: Using default Json")
                Json {
                    val contentNegotiation = properties.client.contentNegotiation
                    ignoreUnknownKeys = contentNegotiation.json.ignoreUnknownKeys
                    isLenient = contentNegotiation.json.isLenient
                    encodeDefaults = contentNegotiation.json.encodeDefaults
                    prettyPrint = contentNegotiation.json.prettyPrint
                }
            }
        }
        // 用户没有配置 HttpClient 的情况下，自动创建默认客户端
        onMissInstances<HttpClient> {
            // 如果没有配置 HttpClientEngine，则使用官方默认的
            if (missInstances(_q<HttpClientEngine>())) {
                single<HttpClient> {
                    logger.debug("Ktor: Using default HttpClientEngine")
                    HttpClient(KtorHttpClientConfig(properties))
                }
            } else {
                single<HttpClient> {
                    logger.debug("Ktor: Using user-defined HttpClientEngine")
                    HttpClient(get<HttpClientEngine>(), KtorHttpClientConfig(properties))
                }
            }
        }
        // 用户开启了日志并且没有定义Logger实例，自动创建默认Kermit实现的Logger
        if (properties.client.logging.enabled && missInstances(_q<Logger>())) {
            single<Logger> {
                logger.debug("Ktor: Using KermitKtorLoggerAdapter")
                KermitKtorLoggerAdapter()
            }
        }
    }
}

@Suppress("functionName")
fun Scope.KtorHttpClientConfig(properties: KtorProperties): HttpClientConfigDeclaration = {
    val timeout = properties.client.timeout
    val logging = properties.client.logging
    val contentNegotiation = properties.client.contentNegotiation
    val request = properties.client.request
    val retry = properties.client.retry
    val websocket = properties.client.websocket

    // 设置超时配置
    install(HttpTimeout) {
        requestTimeoutMillis = timeout.request
        connectTimeoutMillis = timeout.connect
        socketTimeoutMillis = timeout.socket
    }

    // 内容协商配置
    if (contentNegotiation.enabled) {
        install(ContentNegotiation) {
            json(getOrNull<Json>() ?: get<Json>(KtorQualifier))
        }
    }

    // 日志配置
    if (logging.enabled) {
        install(Logging) {
            logger = get()
            level = logging.level
        }
    }

    // HTTP 请求配置
    install(DefaultRequest) {
        if (request.userAgent.isNotBlank()) {
            header(HttpHeaders.UserAgent, request.userAgent)
        }
        if (request.url.isNotEmpty()) {
            url(request.url)
        }
        request.headers.forEach { (key, value) ->
            header(key, value)
        }
    }

    val responseValidators = getAll<ResponseValidator>()
    val callExceptionHandlers = getAll<CallRequestExceptionHandler>() +
            getAll<CallExceptionHandler>().map { { cause, _ -> it(cause) } }
    // 响应验证器
    if (callExceptionHandlers.isNotEmpty() || responseValidators.isNotEmpty()) {
        HttpResponseValidator {
            responseValidators.forEach(::validateResponse)
            callExceptionHandlers.forEach(::handleResponseException)
        }
    }


    // 配置 Cookie 插件
    val cookiesStorage = getOrNull<CookiesStorage>()
    val cookiesStorageInitializers = getAll<CookiesStorageInitializer>()
    if (cookiesStorage != null) {
        install(HttpCookies) {
            storage = cookiesStorage
            cookiesStorageInitializers.forEach(::default)
        }
    }

    // 使用 HttpRequestRetry 插件配置重试逻辑
    if (retry.attempts > 0) {
        install(HttpRequestRetry) {
            // 配置最大重试次数
            maxRetries = retry.attempts

            // 配置重试延迟策略
            if (retry.delayMillis > 0) {
                exponentialDelay(base = retry.delayMillis.toDouble())
            }

            // 配置重试条件
            val retryRequestCondition: HttpRetryShouldRetryContext.(HttpRequest, HttpResponse) -> Boolean =
                { request, response ->
                    // 响应状态码满足条件
                    val statusCondition = response.status.value in retry.onResponseCodes
                    // 请求方法满足条件
                    val methodCondition = when (request.method) {
                        HttpMethod.Get, HttpMethod.Head, HttpMethod.Options -> true
                        else -> retry.onAllRequests
                    }
                    statusCondition && methodCondition
                }

            retryIf(block = retryRequestCondition)
            retryOnException(retryOnTimeout = retry.onConnectTimeout)


            // 记录重试次数到请求头
            modifyRequest { req ->
                req.headers.append("X-Retry-Count", "${this.retryCount}")
            }
        }
    }

    val providers = getAll<AuthProvider>()

    if (providers.isNotEmpty()) {
        install(Auth) {
            this.providers += providers
        }
    }

    // WebSocket 配置
    if (websocket.enabled) {
        install(WebSockets) {
            pingInterval = websocket.pingInterval.milliseconds
            maxFrameSize = websocket.maxFrameSize
        }
    }

    // 重定向配置
    followRedirects = request.followRedirects
    getAll<HttpClientConfigDeclaration>().forEach { it() }
}