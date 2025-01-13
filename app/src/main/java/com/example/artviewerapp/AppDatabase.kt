package com.example.artviewerapp

import android.content.Context
import androidx.room.*
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Entity(tableName = "favorites")
data class ObjectDetailsEntity(
    @PrimaryKey val objectID: Int,
    val title: String?,
    val primaryImage: String?,
    val accessionYear: String?,
    val artistDisplayName: String?,
    val artistDisplayBio: String?
)

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<ObjectDetailsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToFavorites(artwork: ObjectDetailsEntity)

    @Delete
    suspend fun removeFromFavorites(artwork: ObjectDetailsEntity)
}

@Database(entities = [ObjectDetailsEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "favorites_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object FavoritesRepository {
    private lateinit var dao: FavoritesDao

    fun initialize(context: Context) {
        dao = AppDatabase.getDatabase(context).favoritesDao()
    }

    suspend fun getFavorites(): List<ObjectDetailsResponse> = withContext(Dispatchers.IO) {
        dao.getAllFavorites().map { entity ->
            ObjectDetailsResponse(
                objectID = entity.objectID,
                title = entity.title,
                primaryImage = entity.primaryImage,
                accessionYear = entity.accessionYear,
                artistDisplayName = entity.artistDisplayName,
                artistDisplayBio = entity.artistDisplayBio
            )
        }
    }

    suspend fun addToFavorites(artwork: ObjectDetailsResponse) = withContext(Dispatchers.IO) {
        dao.addToFavorites(
            ObjectDetailsEntity(
                objectID = artwork.objectID,
                title = artwork.title,
                primaryImage = artwork.primaryImage,
                accessionYear = artwork.accessionYear,
                artistDisplayName = artwork.artistDisplayName,
                artistDisplayBio = artwork.artistDisplayBio
            )
        )
    }

    suspend fun removeFromFavorites(artwork: ObjectDetailsResponse) = withContext(Dispatchers.IO) {
        dao.removeFromFavorites(
            ObjectDetailsEntity(
                objectID = artwork.objectID,
                title = artwork.title,
                primaryImage = artwork.primaryImage,
                accessionYear = artwork.accessionYear,
                artistDisplayName = artwork.artistDisplayName,
                artistDisplayBio = artwork.artistDisplayBio
            )
        )
    }

    suspend fun isFavorite(artwork: ObjectDetailsResponse): Boolean = withContext(Dispatchers.IO) {
        dao.getAllFavorites().any { it.objectID == artwork.objectID }
    }
}
