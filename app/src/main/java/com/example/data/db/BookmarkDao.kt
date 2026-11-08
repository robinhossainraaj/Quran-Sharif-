package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmark ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) > 0 FROM bookmark WHERE ayahId = :ayahId")
    fun isBookmarked(ayahId: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) > 0 FROM bookmark WHERE ayahId = :ayahId")
    suspend fun isBookmarkedDirect(ayahId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmark WHERE ayahId = :ayahId")
    suspend fun delete(ayahId: Int)

    @Query("DELETE FROM bookmark")
    suspend fun deleteAll()
}
