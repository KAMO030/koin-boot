package io.github.kamo030.koinboot.component.multiplatformsettings.test

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.github.kamo030.MultiplatformSettingsBootInitializer
import io.github.kamo030.koinboot.component.multiplatformsettings.MultiplatformSettingsProperties
import io.github.kamo030.koinboot.component.multiplatformsettings.MultiplatformSettingsProperties.Companion.MULTIPLATFORM_SETTINGS_DESKTOP_PATH
import io.github.kamo030.koinboot.component.multiplatformsettings.getSettings
import io.github.kamo030.koinboot.component.multiplatformsettings.multiplatform_settings_desktop_path
import io.github.kamo030.koinboot.core.getPropInstance
import io.github.kamo030.koinboot.core.runKoinBoot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import kotlin.test.assertEquals

class MultiplatformSettingsTest {

    @AfterEach
    fun stopKoin() =
        KoinPlatformTools.defaultContext().stopKoin()

    /**
     * 测试多平台设置功能
     *
     * 此测试用例验证:
     * 1. 使用MapSettings.Factory作为Settings.Factory的实现
     * 2. 通过Koin依赖注入框架正确初始化设置组件
     * 3. 能够正确获取并验证设置实例的类型
     */
    @Test
    fun `test settings`() {

        // 创建一个设置工厂模块，注册MapSettings.Factory作为Settings.Factory的实现 替换掉默认的设置工厂
        val settingsFactoryModule = module {
            single<Settings.Factory> { MapSettings.Factory() }
        }

        // 使用MultiplatformSettingsAutoModule启动Koin
        // 这个自动模块会处理Settings相关依赖的自动注册
        val koin = runKoinBoot {
            MultiplatformSettingsBootInitializer()
            // 加载我们自定义的设置工厂模块
            modules(settingsFactoryModule)
        }


        // 使用getSettings扩展函数获取指定名称的设置实例
        val settings = koin.getSettings("test")
        // 验证获取的设置实例确实是MapSettings类型
        assert(settings is MapSettings) { "settings is not MapSettings" }
    }

    @Test
    fun `test settings properties`() {
        val path = "C:\\Users\\Administrator\\Desktop"
        val koin = runKoinBoot {
            properties {
                +MultiplatformSettingsProperties(
                    desktop = MultiplatformSettingsProperties.DeskTop(
                        path = path + 1
                    )
                )
            }
            // or
            properties {
                // 会覆盖掉之前的值
                multiplatform_settings_desktop_path = path
                // or
                // 这里使用的是原本KoinProperties对String类型的invoke重载
                // MultiplatformSettingsProperties.MULTIPLATFORM_SETTINGS_DESKTOP_PATH(path)
            }
        }
        val multiplatformSettingsProperties =
            koin.getPropInstance<MultiplatformSettingsProperties>() ?: MultiplatformSettingsProperties()
        val multiplatformSettingsPropertiesDeskTop = koin.getPropInstance<MultiplatformSettingsProperties.DeskTop>()
            ?: MultiplatformSettingsProperties.DeskTop()
        assertEquals(path, multiplatformSettingsProperties.desktop.path)
        assertEquals(path, multiplatformSettingsPropertiesDeskTop.path)
        assertEquals(path, koin.getProperty(MULTIPLATFORM_SETTINGS_DESKTOP_PATH))
        assertEquals(path, koin.getProperty(MULTIPLATFORM_SETTINGS_DESKTOP_PATH))
    }
}