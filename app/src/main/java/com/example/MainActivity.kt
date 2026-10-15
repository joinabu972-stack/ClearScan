package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.model.PageEntity
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.FilterEditorScreen
import com.example.ui.screens.ReviewAndSaveScreen
import com.example.ui.screens.ScanCaptureScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScannerViewModel

sealed interface AppScreen {
    data object Dashboard : AppScreen
    data object Scan : AppScreen
    data object ReviewSave : AppScreen
    data class DocumentDetail(val documentId: Long) : AppScreen
    data class FilterEditor(val page: PageEntity) : AppScreen
    data object Tools : AppScreen
}

class MainActivity : ComponentActivity() {

    private val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    ClearScanApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ClearScanApp(viewModel: ScannerViewModel) {
    val screenStack = remember { mutableStateListOf<AppScreen>(AppScreen.Dashboard) }
    val currentScreen = screenStack.lastOrNull() ?: AppScreen.Dashboard

    val documents by viewModel.documents.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val currentDocument by viewModel.currentDocument.collectAsState()
    val capturedPages by viewModel.captureSessionPages.collectAsState()

    fun navigateTo(screen: AppScreen) {
        screenStack.add(screen)
    }

    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
        }
    }

    when (val screen = currentScreen) {
        is AppScreen.Dashboard -> {
            DashboardScreen(
                viewModel = viewModel,
                documents = documents,
                selectedFolder = selectedFolder,
                searchQuery = searchQuery,
                isGridView = isGridView,
                onNavigateToScan = {
                    viewModel.clearCaptureSession()
                    navigateTo(AppScreen.Scan)
                },
                onNavigateToImport = {
                    navigateTo(AppScreen.Scan)
                },
                onNavigateToDocument = { docId ->
                    viewModel.selectDocument(docId)
                    navigateTo(AppScreen.DocumentDetail(docId))
                },
                onNavigateToTools = {
                    navigateTo(AppScreen.Tools)
                }
            )
        }

        is AppScreen.Scan -> {
            BackHandler {
                navigateBack()
            }
            ScanCaptureScreen(
                viewModel = viewModel,
                capturedPages = capturedPages,
                onFinishCapture = {
                    navigateTo(AppScreen.ReviewSave)
                },
                onBack = {
                    navigateBack()
                }
            )
        }

        is AppScreen.ReviewSave -> {
            BackHandler {
                navigateBack()
            }
            ReviewAndSaveScreen(
                viewModel = viewModel,
                capturedPages = capturedPages,
                onSaved = { docId ->
                    // Pop Review and Scan, push DocumentDetail
                    screenStack.removeIf { it is AppScreen.Scan || it is AppScreen.ReviewSave }
                    navigateTo(AppScreen.DocumentDetail(docId))
                },
                onBack = {
                    navigateBack()
                }
            )
        }

        is AppScreen.DocumentDetail -> {
            BackHandler {
                navigateBack()
            }
            DocumentDetailScreen(
                viewModel = viewModel,
                documentWithPages = currentDocument,
                onNavigateToFilterEditor = { page ->
                    navigateTo(AppScreen.FilterEditor(page))
                },
                onBack = {
                    navigateBack()
                }
            )
        }

        is AppScreen.FilterEditor -> {
            BackHandler {
                navigateBack()
            }
            FilterEditorScreen(
                viewModel = viewModel,
                page = screen.page,
                onSaved = {
                    navigateBack()
                },
                onBack = {
                    navigateBack()
                }
            )
        }

        is AppScreen.Tools -> {
            BackHandler {
                navigateBack()
            }
            ToolsScreen(
                viewModel = viewModel,
                onNavigateToScan = {
                    navigateTo(AppScreen.Scan)
                },
                onBack = {
                    navigateBack()
                },
                onDocumentCreated = { docId ->
                    viewModel.selectDocument(docId)
                    navigateTo(AppScreen.DocumentDetail(docId))
                }
            )
        }
    }
}
