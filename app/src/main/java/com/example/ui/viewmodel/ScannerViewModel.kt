package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentWithPages
import com.example.data.model.FilterType
import com.example.data.model.PageEntity
import com.example.data.repository.DocumentRepository
import com.example.util.ImageProcessingUtils
import com.example.util.SampleDocumentGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository

    val folders = listOf("All", "Documents", "ID Cards", "Invoices", "Receipts", "Notes", "Personal")

    private val _selectedFolder = MutableStateFlow("All")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Scanned pages in current capture session (before committing to document)
    private val _captureSessionPages = MutableStateFlow<List<Pair<Bitmap, FilterType>>>(emptyList())
    val captureSessionPages: StateFlow<List<Pair<Bitmap, FilterType>>> = _captureSessionPages.asStateFlow()

    private val _selectedDocumentId = MutableStateFlow<Long?>(null)
    val selectedDocumentId: StateFlow<Long?> = _selectedDocumentId.asStateFlow()

    private val _activeEditingPage = MutableStateFlow<PageEntity?>(null)
    val activeEditingPage: StateFlow<PageEntity?> = _activeEditingPage.asStateFlow()

    private val _pdfExportState = MutableStateFlow<PdfExportState>(PdfExportState.Idle)
    val pdfExportState: StateFlow<PdfExportState> = _pdfExportState.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = DocumentRepository(application, db.documentDao())
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // Filtered documents list based on folder selection and search query
    val documents: StateFlow<List<DocumentWithPages>> = combine(
        repository.allDocuments,
        _selectedFolder,
        _searchQuery
    ) { docs, folder, query ->
        docs.filter { docWithPages ->
            val matchesFolder = if (folder == "All") true else docWithPages.document.folder.equals(folder, ignoreCase = true)
            val matchesQuery = if (query.isBlank()) true else {
                docWithPages.document.title.contains(query, ignoreCase = true) ||
                docWithPages.document.notes.contains(query, ignoreCase = true) ||
                docWithPages.pages.any { it.extractedText.contains(query, ignoreCase = true) }
            }
            matchesFolder && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current selected document flow
    val currentDocument: StateFlow<DocumentWithPages?> = combine(
        repository.allDocuments,
        _selectedDocumentId
    ) { docs, id ->
        if (id == null) null else docs.find { it.document.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun selectFolder(folder: String) {
        _selectedFolder.value = folder
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun selectDocument(id: Long?) {
        _selectedDocumentId.value = id
    }

    fun setEditingPage(page: PageEntity?) {
        _activeEditingPage.value = page
    }

    // Capture Session methods
    fun addCapturedPage(bitmap: Bitmap, filter: FilterType = FilterType.MAGIC) {
        _captureSessionPages.value = _captureSessionPages.value + (bitmap to filter)
    }

    fun removeCapturedPage(index: Int) {
        val list = _captureSessionPages.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _captureSessionPages.value = list
        }
    }

    fun clearCaptureSession() {
        _captureSessionPages.value = emptyList()
    }

    fun saveCaptureSessionAsDocument(
        title: String,
        folder: String,
        notes: String,
        onComplete: (Long) -> Unit
    ) {
        val pages = _captureSessionPages.value
        if (pages.isEmpty()) return

        viewModelScope.launch {
            val docId = repository.createDocumentWithPages(title, folder, notes, pages)
            clearCaptureSession()
            _selectedDocumentId.value = docId
            onComplete(docId)
        }
    }

    fun addDemoDocument(sampleType: String, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val bmp = SampleDocumentGenerator.createSampleDocument(getApplication(), sampleType)
            val title = when (sampleType.uppercase()) {
                "ID_CARD" -> "Scanned ID Card"
                "RECEIPT" -> "Grocery Mart Receipt"
                "NOTES" -> "Meeting Sketch & Notes"
                else -> "Business Invoice"
            }
            val folder = when (sampleType.uppercase()) {
                "ID_CARD" -> "ID Cards"
                "RECEIPT" -> "Receipts"
                "NOTES" -> "Notes"
                else -> "Invoices"
            }
            val docId = repository.createDocumentWithPages(
                title = title,
                folder = folder,
                notes = "Auto-scanned $title via ClearScan engine",
                pageBitmaps = listOf(bmp to FilterType.MAGIC)
            )
            onComplete(docId)
        }
    }

    fun addPageToCurrentDocument(bitmap: Bitmap, filter: FilterType = FilterType.MAGIC) {
        val docId = _selectedDocumentId.value ?: return
        val currentDoc = currentDocument.value
        val nextIndex = currentDoc?.pages?.size ?: 0
        viewModelScope.launch {
            repository.addPageToDocument(docId, bitmap, filter, nextIndex)
        }
    }

    fun updatePageFilter(page: PageEntity, filter: FilterType, rotation: Int) {
        viewModelScope.launch {
            repository.updatePageFilter(page, filter, rotation)
            _activeEditingPage.value = null
        }
    }

    fun toggleFavorite(document: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(document.id, !document.isFavorite)
        }
    }

    fun deleteDocument(documentId: Long) {
        viewModelScope.launch {
            repository.deleteDocument(documentId)
            if (_selectedDocumentId.value == documentId) {
                _selectedDocumentId.value = null
            }
        }
    }

    fun deletePage(page: PageEntity) {
        viewModelScope.launch {
            repository.deletePage(page)
        }
    }

    fun updateMetadata(documentId: Long, title: String, folder: String, notes: String) {
        viewModelScope.launch {
            repository.updateMetadata(documentId, title, folder, notes)
        }
    }

    // PDF Exporting & Sharing
    fun generatePdfForDocument(documentWithPages: DocumentWithPages, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _pdfExportState.value = PdfExportState.Generating
            val imagePaths = documentWithPages.pages.sortedBy { it.pageIndex }.map { it.processedImagePath }
            val file = ImageProcessingUtils.generatePdf(
                context = getApplication(),
                pageImagePaths = imagePaths,
                documentTitle = documentWithPages.document.title
            )
            if (file != null) {
                _pdfExportState.value = PdfExportState.Success(file)
                onReady(file)
            } else {
                _pdfExportState.value = PdfExportState.Error("Failed to generate PDF document.")
            }
        }
    }

    fun sharePdf(file: File) {
        ImageProcessingUtils.shareFile(
            context = getApplication(),
            file = file,
            mimeType = "application/pdf",
            chooserTitle = "Share Scanned PDF"
        )
    }

    fun sharePageAsImage(page: PageEntity) {
        val file = File(page.processedImagePath)
        if (file.exists()) {
            ImageProcessingUtils.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "image/jpeg",
                chooserTitle = "Share Scanned Image"
            )
        }
    }

    fun resetPdfState() {
        _pdfExportState.value = PdfExportState.Idle
    }
}

sealed interface PdfExportState {
    data object Idle : PdfExportState
    data object Generating : PdfExportState
    data class Success(val file: File) : PdfExportState
    data class Error(val message: String) : PdfExportState
}
