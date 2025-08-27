package io.github.kamo030.koinboot.component.room.test

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * 编译时会自动生成子类实现
 */
@Database(
    entities = [SearchHistoryEntity::class],
    version = 1
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val searchHistoryDao: SearchHistoryDao
    abstract val searchHistoryDao2: SearchHistoryDao2
}

@Database(
    entities = [SearchHistoryEntity::class],
    version = 1
)
@ConstructedBy(AppDatabase2Constructor::class)
abstract class AppDatabase2 : RoomDatabase() {
    abstract val searchHistoryDao: SearchHistoryDao
    abstract val searchHistoryDao2: SearchHistoryDao2
}


/**
 * 编译时会自动生成 `actual` 的实现
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * 编译时会自动生成 `actual` 的实现
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabase2Constructor : RoomDatabaseConstructor<AppDatabase2> {
    override fun initialize(): AppDatabase2
}