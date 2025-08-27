package io.github.kamo030.koinboot.component.koin.test

import io.github.kamo030.koinboot.core.configuration.*
import io.github.kamo030.koinboot.core.runKoinBoot
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.scopedOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import kotlin.test.assertEquals

/**
 * Koin依赖注入框架的测试类
 * 用于测试Koin的作用域(scope)功能
 */
class KoinTest {

    @AfterEach
    fun stopKoin() =
        KoinPlatformTools.defaultContext().stopKoin()

    /**
     * 测试Koin的作用域(scope)功能
     *
     * 此测试演示了:
     * 1. 如何创建和配置Koin模块
     * 2. 如何在作用域中定义对象
     * 3. 如何创建作用域并获取实例
     * 4. 如何链接不同的作用域
     */
    @Test
    fun `test scope`() {
        // 定义一个包含作用域的Koin模块
        val appModule = module {
            // 为Test类型定义一个作用域
            scope<Test> {
                // 在作用域中定义TestInstance类型，使用其构造函数创建实例
                scopedOf<TestInstance>(::TestInstance)
            }
        }
        // 启动Koin依赖注入框架
        val koin = startKoin {
            // 注册我们之前定义的模块
            modules(appModule)
        }.koin
        // 为Test类型创建另一个作用域，使用"2"作为作用域ID
        val scope1 = koin.createScope<Test>("1")
        val instance1 = scope1.get<TestInstance>()

        // 为TestInstance类型创建另一个作用域，使用"2"作为作用域ID
        val scope2 = koin.createScope<TestInstance>("2")

        // 将scope2链接到scope1，这样scope2可以访问scope1中定义的实例
        scope2.linkTo(scope1)

        // 从链接的作用域中获取TestInstance实例
        val instance2 = scope2.get<TestInstance>()
        // 打印两个实例的哈希码，用于验证它们是否是同一个实例
        assertEquals(instance1.hashCode(), instance2.hashCode())
    }

    @Test
    fun `test conditional on instances`() {
        // 定义测试模块
        val testModule = module {
            // 注册一个TestService实例
            single { TestService("test-service") }
        }
        // 测试自动注册功能
        val autoConfiguration = koinAutoConfiguration {
            module {
                // 当TestService存在时，注册DependentService
                onExistInstance<TestService> {
                    single { DependentService(get()) }
                }

                // 当MissingService不存在时，注册一个默认实现
                onMissInstances<TestService> {
                    single { TestService("default-implementation") }
                }
            }
        }
        // 启动Koin并加载测试模块
        val koin = runKoinBoot {
            modules(testModule)
            autoConfigurations(autoConfiguration)
        }

        // 验证TestService已正确注册, 替换掉了默认实现
        val testService = koin.get<TestService>()
        assert(testService.value == "test-service") { "TestService未被正确注册" }

        // 验证由于TestService存在，DependentService已被自动注册
        val dependentService = koin.get<DependentService>()
        assert(dependentService.testService === testService) { "DependentService未被正确注册或未正确注入TestService" }
    }


    @Test
    fun `test miss properties`() {


        // 创建一个自动模块，根据属性是否存在来决定是否注册服务
        val propertiesConfiguration = koinAutoConfiguration {

            module {
                // 当指定属性不存在时执行注册
                onMissProperties("xx.xx.enable") {
                    single { TestService("onMissProperties") }
                }
                // 当指定属性存在时执行注册
                onExistProperties("xx.xx.enable") {
                    single { TestService("onExistProperties") }
                }
                // 当指定属性等于true时执行注册
                onEqProperty("xx.xx.enable", true) {
                    single { DependentService(get()) }
                }
            }

        }

        // 启动Koin并设置一些属性
        val koin = runKoinBoot {
            // 设置存在的属性
            properties(
                mapOf(
                    "xx.xx.enable" to true
                )
            )
            autoConfigurations(propertiesConfiguration)
        }

        // 验证当属性"xx.xx.enable"存在时，TestService被正确注册，且值为"onExistProperties"
        val testService = koin.get<TestService>()
        assert(testService.value == "onExistProperties") { "TestService未被正确注册: ${testService.value}" }

        // 验证当属性"xx.xx.enable"存在时且值为true，DependentService被正确注册
        val dependentService = koin.getOrNull<DependentService>()
        assert(dependentService != null) { "DependentService未被正确注册" }
    }

}

/**
 * 测试用的简单类
 * 用于在Koin作用域中创建和获取实例
 */
private class TestInstance(val value: String = "test")

/**
 * 用于测试conditionalOnInstances函数的服务类
 */
private class TestService(val value: String)

/**
 * 依赖于TestService的服务类
 */
private class DependentService(val testService: TestService)

@Serializable
private data class TestProperties(
    val xx: TestPropertiesC,
    val yy: TestPropertiesC
)

@Serializable
private data class TestPropertiesC(
    val enable: Boolean
)

