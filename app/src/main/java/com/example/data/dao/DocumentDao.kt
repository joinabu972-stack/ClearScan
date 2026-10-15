package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentWithPages
import com.example.data.model.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Transaction
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocumentsWithPages(): Flow<List<DocumentWithPages>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId LIMIT 1")
    fun getDocumentWithPagesById(documentId: Long): Flow<DocumentWithPages?>

    @Transaction
    @Query("SELECT * FROM documents WHERE folder = :folder ORDER BY updatedAt DESC")
    fun getDocumentsByFolder(folder: String): Flow<List<DocumentWithPages>>

    @Transaction
    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchDocuments(query: String): Flow<List<DocumentWithPages>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: Long)

    @Query("UPDATE documents SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :documentId")
    suspend fun updateFavorite(documentId: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET title = :title, folder = :folder, notes = :notes, updatedAt = :updatedAt WHERE id = :documentId")
    suspend fun updateMetadata(documentId: Long, title: String, folder: String, notes: String, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>): List<Long>

    @Update
    suspend fun updatePage(page: PageEntity)

    @Delete
    suspend fun deletePage(page: PageEntity)

    @Query("DELETE FROM document_pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: Long)

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int
}
