package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.dao.DocumentDao
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentWithPages
import com.example.data.model.FilterType
import com.example.data.model.PageEntity
import com.example.util.ImageProcessingUtils
import com.example.util.SampleDocumentGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao
) {
    val allDocuments: Flow<List<DocumentWithPages>> = documentDao.getAllDocumentsWithPages()

    fun getDocumentById(id: Long): Flow<DocumentWithPages?> = documentDao.getDocumentWithPagesById(id)

    fun getDocumentsByFolder(folder: String): Flow<List<DocumentWithPages>> =
        if (folder == "All") documentDao.getAllDocumentsWithPages()
        else documentDao.getDocumentsByFolder(folder)

    fun searchDocuments(query: String): Flow<List<DocumentWithPages>> =
        documentDao.searchDocuments(query)

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        SampleDocumentGenerator.seedSampleDocumentsIfEmpty(context, documentDao)
    }

    suspend fun createDocumentWithPages(
        title: String,
        folder: String,
        notes: String,
        pageBitmaps: List<Pair<Bitmap, FilterType>>
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val docId = documentDao.insertDocument(
            DocumentEntity(
                title = title.ifBlank { "Scan_${now % 100000}" },
                folder = folder,
                createdAt = now,
                updatedAt = now,
                notes = notes
            )
        )

        val pages = mutableListOf<PageEntity>()
        for ((index, pair) in pageBitmaps.withIndex()) {
            val (originalBmp, filter) = pair
            val originalPath = ImageProcessingUtils.saveBitmapToInternalStorage(context, originalBmp, "orig_p${index + 1}")
            val processedBmp = ImageProcessingUtils.applyFilter(originalBmp, filter)
            val processedPath = ImageProcessingUtils.saveBitmapToInternalStorage(context, processedBmp, "proc_p${index + 1}")

            pages.add(
                PageEntity(
                    documentId = docId,
                    pageIndex = index,
                    originalImagePath = originalPath,
                    processedImagePath = processedPath,
                    filterType = filter.name,
                    rotationDegrees = 0,
                    extractedText = "Scanned Page ${index + 1}\nDocument: $title\nCaptured: ${java.util.Date()}"
                )
            )
        }
        documentDao.insertPages(pages)
        docId
    }

    suspend fun addPageToDocument(
        documentId: Long,
        bitmap: Bitmap,
        filterType: FilterType,
        pageIndex: Int
    ): Long = withContext(Dispatchers.IO) {
        val originalPath = ImageProcessingUtils.saveBitmapToInternalStorage(context, bitmap, "orig_p$pageIndex")
        val processedBmp = ImageProcessingUtils.applyFilter(bitmap, filterType)
        val processedPath = ImageProcessingUtils.saveBitmapToInternalStorage(context, processedBmp, "proc_p$pageIndex")

        val pageId = documentDao.insertPage(
            PageEntity(
                documentId = documentId,
                pageIndex = pageIndex,
                originalImagePath = originalPath,
                processedImagePath = processedPath,
                filterType = filterType.name,
                rotationDegrees = 0,
                extractedText = "Scanned Page ${pageIndex + 1}\nAdded on: ${java.util.Date()}"
            )
        )
        // Update document updatedAt timestamp
        documentDao.updateFavorite(documentId, false) // trigger timestamp update
        pageId
    }

    suspend fun updatePageFilter(
        page: PageEntity,
        newFilter: FilterType,
        newRotation: Int = page.rotationDegrees
    ) = withContext(Dispatchers.IO) {
        val originalBmp = ImageProcessingUtils.loadBitmapFromFile(page.originalImagePath)
        if (originalBmp != null) {
            val rotated = ImageProcessingUtils.rotateBitmap(originalBmp, newRotation.toFloat())
            val filtered = ImageProcessingUtils.applyFilter(rotated, newFilter)
            val newProcessedPath = ImageProcessingUtils.saveBitmapToInternalStorage(context, filtered, "reproc_p${page.pageIndex}")

            val updatedPage = page.copy(
                processedImagePath = newProcessedPath,
                filterType = newFilter.name,
                rotationDegrees = newRotation
            )
            documentDao.updatePage(updatedPage)
        }
    }

    suspend fun deleteDocument(documentId: Long) = withContext(Dispatchers.IO) {
        documentDao.deleteDocumentById(documentId)
    }

    suspend fun deletePage(page: PageEntity) = withContext(Dispatchers.IO) {
        documentDao.deletePage(page)
    }

    suspend fun toggleFavorite(documentId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        documentDao.updateFavorite(documentId, isFavorite)
    }

    suspend fun updateMetadata(documentId: Long, title: String, folder: String, notes: String) = withContext(Dispatchers.IO) {
        documentDao.updateMetadata(documentId, title, folder, notes)
    }
}
