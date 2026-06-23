package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeDao {
    @Query("SELECT * FROM office_documents WHERE isTrash = 0 ORDER BY lastModified DESC")
    fun getAllActiveDocuments(): Flow<List<OfficeDocument>>

    @Query("SELECT * FROM office_documents WHERE isTrash = 1 ORDER BY lastModified DESC")
    fun getTrashDocuments(): Flow<List<OfficeDocument>>

    @Query("SELECT * FROM office_documents WHERE id = :id LIMIT 1")
    fun getDocumentById(id: Int): Flow<OfficeDocument?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: OfficeDocument): Long

    @Update
    suspend fun updateDocument(document: OfficeDocument)

    @Delete
    suspend fun deleteDocument(document: OfficeDocument)

    @Query("DELETE FROM office_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)

    @Query("UPDATE office_documents SET isTrash = :isTrash, lastModified = :lastModified WHERE id = :id")
    suspend fun updateTrashStatus(id: Int, isTrash: Boolean, lastModified: Long = System.currentTimeMillis())

    @Query("UPDATE office_documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM office_documents WHERE isTrash = 1")
    suspend fun emptyTrash()
}
