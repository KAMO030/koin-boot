package io.github.kamo030.koinboot.component.room.test

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.util.findAndInstantiateDatabaseImpl
import co.touchlab.kermit.Logger
import io.github.kamo030.koinboot.RoomBootInitializer
import io.github.kamo030.koinboot.component.room.singleDao
import io.github.kamo030.koinboot.component.room.singleDatabase
import io.github.kamo030.koinboot.core.runKoinBoot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.mp.KoinPlatformTools
import kotlin.reflect.KClass

class RoomTest {

    @AfterEach
    fun stopKoin() =
        KoinPlatformTools.defaultContext().stopKoin()

    fun Module.builder(){
        factory<RoomDatabase.Builder<*>> { parametersHolder ->
            val databaseClass = parametersHolder.getOrNull<KClass<RoomDatabase>>()!!
            Room.inMemoryDatabaseBuilder {
                findAndInstantiateDatabaseImpl(databaseClass.java)
            }
        }
    }

    /**
     * Room singleDao 使用的示范
     *
     */
    @Test
    fun `test singleDao`() {

        val koin = runKoinBoot {
            RoomBootInitializer()
            module {
                builder()
                singleDao(AppDatabase::searchHistoryDao)
                singleDao(AppDatabase::searchHistoryDao2)
            }
        }

        val searchHistoryDao = koin.get<SearchHistoryDao>()

        runTest {
            searchHistoryDao.insert(SearchHistoryEntity(content = "test", timestamp = "2021-01-01"))
            searchHistoryDao.getAllAsFlow().first().forEach { item ->
                Logger.i { "item = $item" }
            }
        }
    }

    @Test
    fun `test multiple databases by databases class`() {

        val koin = runKoinBoot {
            RoomBootInitializer()
            module {
                builder()
                // 为了不被覆盖加一个 _q<AppDatabase>()
                singleDao(AppDatabase::searchHistoryDao, _q<AppDatabase>())
                singleDao(AppDatabase2::searchHistoryDao)
            }
        }
        // 这里的dao操作的是 `io.github.kamo030.koinboot.component.room.test.AppDatabase.db` 库
        val searchHistoryDao = koin.get<SearchHistoryDao>(_q<AppDatabase>())
        // 这里的dao操作的是 `io.github.kamo030.koinboot.component.room.test.AppDatabase2.db` 库
        val searchHistoryDao2 = koin.get<SearchHistoryDao>()

        runTest {
            searchHistoryDao.insert(SearchHistoryEntity(content = "test", timestamp = "2021-01-01"))
            searchHistoryDao.getAllAsFlow().first().forEach { item ->
                Logger.i { "item1 = $item" }
            }
            searchHistoryDao2.insert(SearchHistoryEntity(content = "test2", timestamp = "2021-01-01"))
            searchHistoryDao2.getAllAsFlow().first().forEach { item ->
                Logger.i { "item2 = $item" }
            }
        }
    }

    @Test
    fun `test multiple databases by name`() {
        val userMainQualifier = _q("user_main")
        val userSubQualifier = _q("user_sub")
        val koin = runKoinBoot {
            RoomBootInitializer()
            module {
                builder()
                // 为了不被覆盖加一个 _q<AppDatabase>()
                singleDatabase<AppDatabase>("user_main")
                singleDatabase<AppDatabase>("user_sub")
                single<SearchHistoryDao>(userMainQualifier) {
                    get<AppDatabase>(userMainQualifier).searchHistoryDao
                }
                single<SearchHistoryDao>(userSubQualifier) {
                    get<AppDatabase>(userSubQualifier).searchHistoryDao
                }
            }
        }
        // 这里的dao操作的是 `user_main.db` 库
        val searchHistoryDao = koin.get<SearchHistoryDao>(userMainQualifier)
        // 这里的dao操作的是 `user_sub.db` 库
        val searchHistoryDao2 = koin.get<SearchHistoryDao>(userSubQualifier)

        runTest {
            searchHistoryDao.insert(SearchHistoryEntity(content = "test", timestamp = "2021-01-01"))
            searchHistoryDao.getAllAsFlow().first().forEach { item ->
                Logger.i { "item1 = $item" }
            }
            searchHistoryDao2.insert(SearchHistoryEntity(content = "test2", timestamp = "2021-01-01"))
            searchHistoryDao2.getAllAsFlow().first().forEach { item ->
                Logger.i { "item2 = $item" }
            }
        }
    }

}





