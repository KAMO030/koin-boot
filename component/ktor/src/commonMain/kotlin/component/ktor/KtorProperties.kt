@file:Suppress("unused")
package io.github.kamo030.koinboot.component.ktor


import io.github.kamo030.koinboot.core.KoinPropInstance
import io.github.kamo030.koinboot.core.KoinProperties
import io.ktor.client.plugins.logging.*

@KoinPropInstance("ktor")
data class KtorProperties(
    val client: Client = Client()
) {
    companion object {

        // 请求超时时间，单位为毫秒
        const val KTOR_CLIENT_TIMEOUT_REQUEST = "ktor.client.timeout.request"

        // 连接超时时间，单位为毫秒
        const val KTOR_CLIENT_TIMEOUT_CONNECT = "ktor.client.timeout.connect"

        // Socket超时时间，单位为毫秒
        const val KTOR_CLIENT_TIMEOUT_SOCKET = "ktor.client.timeout.socket"

        // 是否启用日志记录
        const val KTOR_CLIENT_LOGGING_ENABLED = "ktor.client.logging.enabled"

        // 日志级别，如INFO、DEBUG等
        const val KTOR_CLIENT_LOGGING_LEVEL = "ktor.client.logging.level"

        // HTTP 请求配置
        // 是否自动跟随重定向
        const val KTOR_CLIENT_REQUEST_FOLLOW_REDIRECTS = "ktor.client.request.followRedirects"

        // 客户端User-Agent标识
        const val KTOR_CLIENT_REQUEST_USER_AGENT = "ktor.client.request.userAgent"

        // 请求基础URL
        const val KTOR_CLIENT_REQUEST_URL = "ktor.client.request.url"

        // 请求默认头信息
        const val KTOR_CLIENT_REQUEST_HEADERS = "ktor.client.request.headers"

        // 重试配置
        // 重试次数
        const val KTOR_CLIENT_RETRY_ATTEMPTS = "ktor.client.retry.attempts"

        // 重试间隔时间，单位为毫秒
        const val KTOR_CLIENT_RETRY_DELAY_MILLIS = "ktor.client.retry.delayMillis"

        // 是否对所有请求类型进行重试
        const val KTOR_CLIENT_RETRY_ON_ALL_REQUESTS = "ktor.client.retry.onAllRequests"

        // 是否在连接超时时进行重试
        const val KTOR_CLIENT_RETRY_ON_CONNECT_TIMEOUT = "ktor.client.retry.onConnectTimeout"

        // 需要进行重试的HTTP状态码列表
        const val KTOR_CLIENT_RETRY_ON_RESPONSE_CODES = "ktor.client.retry.onResponseCodes"

        // 内容协商配置
        // 是否启用内容协商
        const val KTOR_CLIENT_CONTENT_NEGOTIATION_ENABLED = "ktor.client.content.negotiation.enabled"

        // 是否忽略JSON中未知的键
        const val KTOR_CLIENT_CONTENT_NEGOTIATION_JSON_IGNORE_UNKNOWN_KEYS =
            "ktor.client.content.negotiation.json.ignoreUnknownKeys"

        // 是否使用宽松的JSON解析
        const val KTOR_CLIENT_CONTENT_NEGOTIATION_JSON_IS_LENIENT = "ktor.client.content.negotiation.json.isLenient"

        // 是否编码默认值
        const val KTOR_CLIENT_CONTENT_NEGOTIATION_JSON_ENCODE_DEFAULTS =
            "ktor.client.content.negotiation.json.encodeDefaults"

        // WebSocket 配置
        // 是否启用WebSocket
        const val KTOR_CLIENT_WEBSOCKET_ENABLED = "ktor.client.websocket.enabled"

        // WebSocket ping帧发送间隔，单位为毫秒
        const val KTOR_CLIENT_WEBSOCKET_PING_INTERVAL = "ktor.client.websocket.pingInterval"

        // WebSocket ping超时时间，单位为毫秒
        const val KTOR_CLIENT_WEBSOCKET_PING_TIMEOUT = "ktor.client.websocket.pingTimeout"

        // WebSocket最大帧大小，单位为字节
        const val KTOR_CLIENT_WEBSOCKET_MAX_FRAME_SIZE = "ktor.client.websocket.maxFrameSize"

    }

    @KoinPropInstance("ktor.client")
    data class Client(
        val timeout: Timeout = Timeout(),
        val logging: Logging = Logging(),
        val request: RequestConfig = RequestConfig(),
        val retry: RetryConfig = RetryConfig(),
        val contentNegotiation: ContentNegotiationConfig = ContentNegotiationConfig(),
        val websocket: WebSocketConfig = WebSocketConfig(),
    )

    @KoinPropInstance("ktor.client.request")
    data class RequestConfig(
        val followRedirects: Boolean = true,
        val url: String = "",
        val userAgent: String = "Myuko Ktor Client",
        val headers: Map<String, String> = emptyMap()
    )

    @KoinPropInstance("ktor.client.retry")
    data class RetryConfig(
        val attempts: Int = 0,
        val delayMillis: Long = 100,
        // 是否重试所有请求类型，默认只重试幂等请求（GET、HEAD等）
        val onAllRequests: Boolean = false,
        // 是否在连接超时时重试
        val onConnectTimeout: Boolean = false,
        // 特定HTTP状态码的重试配置，如503、504等
        val onResponseCodes: Set<Int> = (500..599).toSet()
    )

    @KoinPropInstance("ktor.client.content.negotiation")
    data class ContentNegotiationConfig(
        val enabled: Boolean = true,
        val json: JsonConfig = JsonConfig()
    )

    @KoinPropInstance("ktor.client.content.negotiation.json")
    data class JsonConfig(
        val ignoreUnknownKeys: Boolean = true,
        val isLenient: Boolean = true,
        val encodeDefaults: Boolean = false,
        val prettyPrint: Boolean = false
    )


    @KoinPropInstance("ktor.client.websocket")
    data class WebSocketConfig(
        val enabled: Boolean = false,
        val pingInterval: Long = 15000, // 15秒
        val maxFrameSize: Long = 65536 // 64 KB
    )


    @KoinPropInstance("ktor.client.timeout")
    data class Timeout(
        val request: Long = 30000,
        val connect: Long = 30000,
        val socket: Long = 30000
    )

    @KoinPropInstance("ktor.client.logging")
    data class Logging(
        val enabled: Boolean = false,
        val level: LogLevel = LogLevel.INFO
    )
}

// 扩展属性，便于访问
// 超时配置相关扩展属性
var KoinProperties.ktor_client_timeout_request: Long
    get() = (this[KtorProperties.KTOR_CLIENT_TIMEOUT_REQUEST] as Long?) ?: 30000
    set(value) {
        KtorProperties.KTOR_CLIENT_TIMEOUT_REQUEST(value)
    }

var KoinProperties.ktor_client_timeout_connect: Long
    get() = (this[KtorProperties.KTOR_CLIENT_TIMEOUT_CONNECT] as Long?) ?: 30000
    set(value) {
        KtorProperties.KTOR_CLIENT_TIMEOUT_CONNECT(value)
    }

var KoinProperties.ktor_client_timeout_socket: Long
    get() = (this[KtorProperties.KTOR_CLIENT_TIMEOUT_SOCKET] as Long?) ?: 30000
    set(value) {
        KtorProperties.KTOR_CLIENT_TIMEOUT_SOCKET(value)
    }

// 日志配置相关扩展属性
var KoinProperties.ktor_client_logging_enabled: Boolean
    get() = (this[KtorProperties.KTOR_CLIENT_LOGGING_ENABLED] as Boolean?) ?: false
    set(value) {
        KtorProperties.KTOR_CLIENT_LOGGING_ENABLED(value)
    }

var KoinProperties.ktor_client_logging_level: LogLevel
    get() = (this[KtorProperties.KTOR_CLIENT_LOGGING_LEVEL] as LogLevel?) ?: LogLevel.INFO
    set(value) {
        KtorProperties.KTOR_CLIENT_LOGGING_LEVEL(value)
    }

// HTTP 请求配置相关扩展属性
var KoinProperties.ktor_client_request_follow_redirects: Boolean
    get() = (this[KtorProperties.KTOR_CLIENT_REQUEST_FOLLOW_REDIRECTS] as Boolean?) ?: true
    set(value) {
        KtorProperties.KTOR_CLIENT_REQUEST_FOLLOW_REDIRECTS(value)
    }

var KoinProperties.ktor_client_request_url: String
    get() = (this[KtorProperties.KTOR_CLIENT_REQUEST_URL] as String?) ?: ""
    set(value) {
        KtorProperties.KTOR_CLIENT_REQUEST_URL(value)
    }

var KoinProperties.ktor_client_request_user_agent: String
    get() = (this[KtorProperties.KTOR_CLIENT_REQUEST_USER_AGENT] as String?) ?: "Myuko Ktor Client"
    set(value) {
        KtorProperties.KTOR_CLIENT_REQUEST_USER_AGENT(value)
    }

@Suppress("UNCHECKED_CAST")
var KoinProperties.ktor_client_request_headers: Map<String, String>
    get() = (this[KtorProperties.KTOR_CLIENT_REQUEST_HEADERS] as? Map<String, String>) ?: emptyMap()
    set(value) {
        KtorProperties.KTOR_CLIENT_REQUEST_HEADERS(value)
    }

// 重试配置相关扩展属性
var KoinProperties.ktor_client_retry_attempts: Int
    get() = (this[KtorProperties.KTOR_CLIENT_RETRY_ATTEMPTS] as Int?) ?: 0
    set(value) {
        KtorProperties.KTOR_CLIENT_RETRY_ATTEMPTS(value)
    }

var KoinProperties.ktor_client_retry_delay_millis: Long
    get() = (this[KtorProperties.KTOR_CLIENT_RETRY_DELAY_MILLIS] as Long?) ?: 100
    set(value) {
        KtorProperties.KTOR_CLIENT_RETRY_DELAY_MILLIS(value)
    }

var KoinProperties.ktor_client_retry_on_all_requests: Boolean
    get() = (this[KtorProperties.KTOR_CLIENT_RETRY_ON_ALL_REQUESTS] as Boolean?) ?: false
    set(value) {
        KtorProperties.KTOR_CLIENT_RETRY_ON_ALL_REQUESTS(value)
    }

var KoinProperties.ktor_client_retry_on_connect_timeout: Boolean
    get() = (this[KtorProperties.KTOR_CLIENT_RETRY_ON_CONNECT_TIMEOUT] as Boolean?) ?: false
    set(value) {
        KtorProperties.KTOR_CLIENT_RETRY_ON_CONNECT_TIMEOUT(value)
    }

@Suppress("UNCHECKED_CAST")
var KoinProperties.ktor_client_retry_on_response_codes: Set<Int>
    get() = (this[KtorProperties.KTOR_CLIENT_RETRY_ON_RESPONSE_CODES] as? Set<Int>) ?: (500..599).toSet()
    set(value) {
        KtorProperties.KTOR_CLIENT_RETRY_ON_RESPONSE_CODES(value)
    }

// 内容协商配置相关扩展属性
var KoinProperties.ktor_client_content_negotiation_enabled: Boolean
    get() = (this[KtorProperties.KTOR_CLIENT_CONTENT_NEGOTIATION_ENABLED] as Boolean?) ?: true
    set(value) {
        KtorProperties.KTOR_CLIENT_CONTENT_NEGOTIATION_ENABLED(value)
    }

var KoinProperties.ktor_client_content_negotiation_json_ignore_unknown_keys: Boolean
    get() = (this[KtorProperties.KTOR_CLIENT_CONTENT_NEGOTIATION_JSON_IGNORE_UNKNOWN_KEYS] as Boolean?) ?: true
    set(value) {
        KtorProperties.KTOR_CLIENT_CONTENT_NEGOTIATION_JSON_IGNORE_UNKNOWN_KEYS(value)
    }

var KoinProperties.ktor_client_content_negotiation_json_is_lenient: Boolean
    get() = (this[KtorProperties.KTOR_CLIENT_CONTENT_NEGOTIATION_JSON_IS_LENIENT] as Boolean?) ?: true
    set(value) {
        KtorProperties.KTOR_CLIENT_CONTENT_NEGOTIATION_JSON_IS_LENIENT(value)
    }

var KoinProperties.ktor_client_content_negotiation_json_encode_defaults: Boolean
    get() = (this[KtorProperties.KTOR_CLIENT_CONTENT_NEGOTIATION_JSON_ENCODE_DEFAULTS] as Boolean?) ?: false
    set(value) {
        KtorProperties.KTOR_CLIENT_CONTENT_NEGOTIATION_JSON_ENCODE_DEFAULTS(value)
    }


// WebSocket 配置相关扩展属性
var KoinProperties.ktor_client_websocket_enabled: Boolean
    get() = (this[KtorProperties.KTOR_CLIENT_WEBSOCKET_ENABLED] as Boolean?) ?: false
    set(value) {
        KtorProperties.KTOR_CLIENT_WEBSOCKET_ENABLED(value)
    }

var KoinProperties.ktor_client_websocket_ping_interval: Long
    get() = (this[KtorProperties.KTOR_CLIENT_WEBSOCKET_PING_INTERVAL] as Long?) ?: 15000
    set(value) {
        KtorProperties.KTOR_CLIENT_WEBSOCKET_PING_INTERVAL(value)
    }

var KoinProperties.ktor_client_websocket_ping_timeout: Long
    get() = (this[KtorProperties.KTOR_CLIENT_WEBSOCKET_PING_TIMEOUT] as Long?) ?: 15000
    set(value) {
        KtorProperties.KTOR_CLIENT_WEBSOCKET_PING_TIMEOUT(value)
    }

var KoinProperties.ktor_client_websocket_max_frame_size: Long
    get() = (this[KtorProperties.KTOR_CLIENT_WEBSOCKET_MAX_FRAME_SIZE] as Long?) ?: 65536
    set(value) {
        KtorProperties.KTOR_CLIENT_WEBSOCKET_MAX_FRAME_SIZE(value)
    }
