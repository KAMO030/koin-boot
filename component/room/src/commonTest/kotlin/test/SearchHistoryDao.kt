package io.github.kamo030.koinboot.component.room.test


import androidx.room.Dao;
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    /**
     * 插入搜索历史记录
     * 如果已存在相同id的记录，则替换
     * @return 插入的记录ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistoryEntity): Long


    /**
     * 获取所有搜索历史记录，按时间戳降序排列（最新的在前）
     * 返回Flow以便在数据变化时自动更新
     */
    @Query("SELECT * FROM `search_history` ORDER BY timestamp DESC")
    fun getAllAsFlow(): Flow<List<SearchHistoryEntity>>

}

@Dao
interface SearchHistoryDao2 {

    /**
     * 插入搜索历史记录
     * 如果已存在相同id的记录，则替换
     * @return 插入的记录ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistoryEntity): Long


    /**
     * 获取所有搜索历史记录，按时间戳降序排列（最新的在前）
     * 返回Flow以便在数据变化时自动更新
     */
    @Query("SELECT * FROM `search_history` ORDER BY timestamp DESC")
    fun getAllAsFlow(): Flow<List<SearchHistoryEntity>>

}