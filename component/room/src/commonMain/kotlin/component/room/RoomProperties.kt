package io.github.kamo030.koinboot.component.room

import androidx.room.RoomDatabase
import io.github.kamo030.koinboot.core.KoinPropInstance
import io.github.kamo030.koinboot.core.KoinProperties


@KoinPropInstance("room")
data class RoomProperties(
    val databaseName: String = "app_room_database_main",
    val databaseSuffixName: String = "",
    val journalMode: RoomDatabase.JournalMode = RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING,
    val queryCoroutineContext: CoroutineContextType = CoroutineContextType.IO,
    val migration: Migration = Migration(),
) {

    companion object {
        /** 数据库名称  */
        const val ROOM_DATABASE_NAME = "room.databaseName"

        /** 数据库后缀名称  */
        const val ROOM_DATABASE_SUFFIX_NAME = "room.databaseSuffixName"
        const val ROOM_JOURNAL_MODE = "room.journalMode"

        const val ROOM_QUERY_COROUTINE_CONTEXT = "room.queryCoroutineContext"

        // 迁移配置
        const val ROOM_MIGRATION_FALLBACK_TO_DESTRUCTIVE = "room.migration.fallbackToDestructive"
        const val ROOM_MIGRATION_FALLBACK_TO_DESTRUCTIVE_ON_DOWNGRADE =
            "room.migration.fallbackToDestructiveOnDowngrade"

    }

    @KoinPropInstance("room.migration")
    data class Migration(
        val fallbackToDestructive: Boolean = true,
        val fallbackToDestructiveOnDowngrade: Boolean = true
    )

    enum class CoroutineContextType {
        IO, DEFAULT, MAIN, UNCONFINED
    }
}

// 扩展属性，便于访问
// 数据库配置相关扩展属性

/**
 * 数据库默认名称
 *
 * 使用 `singleDao`、 `singleDatabase` 等场景不生效
 */
var KoinProperties.room_database_name: String
    get() = (this[RoomProperties.ROOM_DATABASE_NAME] as String?) ?: RoomProperties().databaseName
    set(value) {
        RoomProperties.ROOM_DATABASE_NAME(value)
    }

/**
 * 数据库名称后缀
 *
 */
var KoinProperties.room_database_suffix_name: String
    get() = (this[RoomProperties.ROOM_DATABASE_SUFFIX_NAME] as String?) ?: RoomProperties().databaseSuffixName
    set(value) {
        RoomProperties.ROOM_DATABASE_SUFFIX_NAME(value)
    }

var KoinProperties.room_journal_mode: RoomDatabase.JournalMode
    get() = (this[RoomProperties.ROOM_JOURNAL_MODE] as RoomDatabase.JournalMode?) ?: RoomProperties().journalMode
    set(value) {
        RoomProperties.ROOM_JOURNAL_MODE(value)
    }

var KoinProperties.room_query_coroutine_context: RoomProperties.CoroutineContextType
    get() = (this[RoomProperties.ROOM_QUERY_COROUTINE_CONTEXT] as RoomProperties.CoroutineContextType?)
        ?: RoomProperties().queryCoroutineContext
    set(value) {
        RoomProperties.ROOM_QUERY_COROUTINE_CONTEXT(value)
    }

// 迁移配置相关扩展属性
var KoinProperties.room_migration_fallback_to_destructive: Boolean
    get() = (this[RoomProperties.ROOM_MIGRATION_FALLBACK_TO_DESTRUCTIVE] as Boolean?)
        ?: RoomProperties.Migration().fallbackToDestructive
    set(value) {
        RoomProperties.ROOM_MIGRATION_FALLBACK_TO_DESTRUCTIVE(value)
    }

var KoinProperties.room_migration_fallback_to_destructive_on_downgrade: Boolean
    get() = (this[RoomProperties.ROOM_MIGRATION_FALLBACK_TO_DESTRUCTIVE_ON_DOWNGRADE] as Boolean?)
        ?: RoomProperties.Migration().fallbackToDestructiveOnDowngrade
    set(value) {
        RoomProperties.ROOM_MIGRATION_FALLBACK_TO_DESTRUCTIVE_ON_DOWNGRADE(value)
    }