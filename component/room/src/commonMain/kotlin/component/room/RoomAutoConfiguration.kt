package io.github.kamo030.koinboot.component.room

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.kamo030.koinboot.core.configuration.KoinBootQualifiers
import io.github.kamo030.koinboot.core.configuration.koinAutoConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier._q
import org.koin.dsl.bind
import kotlin.reflect.KProperty1

val KoinBootQualifiers.RoomDatabaseClass: Qualifier
    get() = _q("RoomDatabaseClass")

val KoinBootQualifiers.RoomDatabaseScope: Qualifier
    get() = _q("RoomDatabaseScope")

val RoomAutoConfiguration = koinAutoConfiguration {

    val roomProperties = propertyInstance<RoomProperties>() ?: RoomProperties()

    module {

        /*
         * 使用以下方式获取，每次get都会创建新的实例，但是如果 `name` 相同，实际上操作的是同一个 `.db` 库
         * ```code
         * koin.get<RoomDatabase>{ parametersOf("name", databaseClass) }
         * ```
         *
         * 按照 RoomDatabase 的具体子类型分库
         * */
        factory { parametersHolder ->
            get<RoomDatabase.Builder<*>> { parametersHolder }
                // 应用迁移配置
                .applyMigrationConfig(roomProperties.migration)
                // 应用查询配置
                .applyQueryConfig(roomProperties.queryCoroutineContext)
                .setDriver(getOrNull<SQLiteDriver>() ?: BundledSQLiteDriver())
                .setJournalMode(roomProperties.journalMode)
                .build()
        } bind RoomDatabase::class

        /**
         * 提供给 `singleDao`、 `singleDatabase` 使用，或者通过以下方式创建以 `name` 命名 `.db` 库 的 `R: RoomDatabase` 类实例
         *
         * 简单来说就是 一个 ScopeID 对应一个 `R: RoomDatabase` 类实例，ScopeID可以是自定义的也可以是类全名
         * */
        scope(KoinBootQualifiers.RoomDatabaseScope) {
            scoped { parametersHolder ->
                // 调用 上面定义的工厂
                // 通过 ScopeID 进行分库
                this.getKoin().get<RoomDatabase> { parametersHolder.add(id) }
            } bind RoomDatabase::class
        }

    }
}


/**
 * 应用迁移配置
 */
private fun RoomDatabase.Builder<*>.applyMigrationConfig(
    migration: RoomProperties.Migration
): RoomDatabase.Builder<*> {
    // 回退到破坏性迁移
    return fallbackToDestructiveMigration(migration.fallbackToDestructive)
        // 降级时回退到破坏性迁移
        .fallbackToDestructiveMigrationOnDowngrade(migration.fallbackToDestructiveOnDowngrade)
}

/**
 * 应用查询配置
 */
private fun RoomDatabase.Builder<*>.applyQueryConfig(
    queryCoroutineContext: RoomProperties.CoroutineContextType
): RoomDatabase.Builder<*> = when (queryCoroutineContext) {
    RoomProperties.CoroutineContextType.IO -> Dispatchers.IO
    RoomProperties.CoroutineContextType.DEFAULT -> Dispatchers.Default
    RoomProperties.CoroutineContextType.MAIN -> Dispatchers.Main
    RoomProperties.CoroutineContextType.UNCONFINED -> Dispatchers.Unconfined
}.run(::setQueryCoroutineContext)



/**
 * 注册数据库DAO
 *
 * @param daoProperty 数据库DAO属性
 *
 *
 * ```code
 * module {
 *  singleDao(AppDatabase::appDao)
 * }
 * ```
 *
 *  R 数据库抽象类, 使用 reified 通过给 parameters 传递给 RoomDatabase.Builder 创建数据库实例
 *  T 数据库DAO
 *
 *  通过 ScopeID（R 的类型） 进行分库
 *  这样可以容器里实现有多个不同的 `R: RoomDatabase` 类的单例实例， 每个实例的 `.db` 库的名字不一样
 */
inline fun <reified R : RoomDatabase, reified T> Module.singleDao(
    daoProperty: KProperty1<in R, T>,
    qualifier: Qualifier? = null
) =
    single<T>(qualifier) {
        val scope =
            this.getKoin().getOrCreateScope(R::class.qualifiedName.toString(), KoinBootQualifiers.RoomDatabaseScope)
        val roomDatabase = scope.get<RoomDatabase> { parametersOf(R::class) } as R
        daoProperty.get(roomDatabase)
    }

/**
 * 注册数据库对象
 *
 * @param name 实例Qualifier, `.db` 库的名字
 *
 *
 * ```kotlin
 * module {
 *   singleDatabase<AppDatabase>("user_main")
 *   singleDatabase<AppDatabase>("user_sub")
 *
 *   single<UserDao>(_q("user_main")){
 *     get<AppDatabase>(_q("user_main")).userDao
 *   }
 *   // 上面的操作的是 user_main.db 库
 *   // 下面的操作的是 user_sub.db 库
 *   single<UserDao>(_q("user_sub")){
 *     get<AppDatabase>(_q("user_sub")).userDao
 *  }
 * }
 * ```
 *
 *  R 数据库抽象类, 使用 reified 通过给 parameters 传递给 RoomDatabase.Builder 创建数据库实例
 *
 *  这样可以实现容器里有多个 同类的 `R: RoomDatabase` 类实例 并且他们的 RoomDatabase `.db` 库的名字不一样
 */
inline fun <reified R : RoomDatabase> Module.singleDatabase(name: String) =
    single<R>(_q(name)) {
        val scope = this.getKoin().getOrCreateScope(name, KoinBootQualifiers.RoomDatabaseScope)
        scope.get<RoomDatabase> { parametersOf(R::class) } as R
    } bind RoomDatabase::class