package com.youtube.frontend.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val watchedAt: Long = System.currentTimeMillis(),
    val progress: Long = 0,
    val duration: Long = 0
)

@Entity(tableName = "watch_later")
data class WatchLaterEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "liked_videos")
data class LikedVideoEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val likedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val avatarUrl: String,
    val subscribedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_videos", primaryKeys = ["playlistId", "videoId"])
data class PlaylistVideoCrossRef(
    val playlistId: String,
    val videoId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY watchedAt DESC")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 20): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

@Dao
interface WatchLaterDao {
    @Query("SELECT * FROM watch_later ORDER BY addedAt DESC")
    fun getWatchLater(): Flow<List<WatchLaterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WatchLaterEntity)

    @Query("DELETE FROM watch_later WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE videoId = :videoId)")
    suspend fun exists(videoId: String): Boolean
}

@Dao
interface LikedVideosDao {
    @Query("SELECT * FROM liked_videos ORDER BY likedAt DESC")
    fun getLikedVideos(): Flow<List<LikedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LikedVideoEntity)

    @Query("DELETE FROM liked_videos WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_videos WHERE videoId = :videoId)")
    suspend fun exists(videoId: String): Boolean
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY subscribedAt DESC")
    fun getSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE channelId = :channelId")
    suspend fun delete(channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId)")
    suspend fun exists(channelId: String): Boolean
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 20")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}

@Database(
    entities = [
        HistoryEntity::class,
        WatchLaterEntity::class,
        LikedVideoEntity::class,
        SubscriptionEntity::class,
        PlaylistEntity::class,
        PlaylistVideoCrossRef::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun watchLaterDao(): WatchLaterDao
    abstract fun likedVideosDao(): LikedVideosDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
