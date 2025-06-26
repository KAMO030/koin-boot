package io.github.kamo030.koinboot.core.configuration

// 自动配置接口
interface KoinAutoConfiguration {

    /**
     * 配置匹配条件
     *
     * @return 如果为true则匹配成功会被调用 [configure] 方法
     */
    fun KoinAutoConfigurationScope.match(): Boolean = true

    /**
     * 配置
     *
     * @param KoinAutoConfigurationScope 配置作用域
     */
    fun KoinAutoConfigurationScope.configure()

    /**
     * 配置顺序，数字越小优先级越高
     */
    val order: Int
        get() = Int.MAX_VALUE
}


fun koinAutoConfiguration(
    order: Int = Int.MAX_VALUE,
    match: KoinAutoConfigurationScope.() -> Boolean = { true },
    configure: KoinAutoConfigurationScope.() -> Unit
): KoinAutoConfiguration = object : KoinAutoConfiguration {

    override fun KoinAutoConfigurationScope.match(): Boolean = match()

    override fun KoinAutoConfigurationScope.configure() {
        configure.invoke(this)
    }

    override val order: Int = order
}


