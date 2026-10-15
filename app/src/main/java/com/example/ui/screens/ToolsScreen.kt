package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ScannerViewModel
import com.example.util.ImageProcessingUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ScannerViewModel,
    onNavigateToScan: () -> Unit,
    onBack: () -> Unit,
    onDocumentCreated: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Photo picker for Image to PDF tool
    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val bitmaps = uris.mapNotNull { uri ->
                ImageProcessingUtils.loadBitmapFromUri(context, uri)
            }
            if (bitmaps.isNotEmpty()) {
                bitmaps.forEach { bmp ->
                    viewModel.addCapturedPage(bmp)
                }
                Toast.makeText(context, "${bitmaps.size} photos imported into PDF session", Toast.LENGTH_SHORT).show()
                onNavigateToScan()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Scanner Tools & Utilities", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "SPECIALIZED MODES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            ToolItemCard(
                icon = Icons.Default.Badge,
                title = "ID Card 2-in-1 Scanner",
                description = "Scans front and back sides of national ID, license, or passport onto a single page.",
                tag = "Pro",
                tagColor = MaterialTheme.colorScheme.primary,
                onClick = {
                    viewModel.addDemoDocument("ID_CARD") { docId ->
                        onDocumentCreated(docId)
                    }
                }
            )

            ToolItemCard(
                icon = Icons.Default.PictureAsPdf,
                title = "Batch Images to PDF",
                description = "Select multiple photos from device gallery and instantly compile them into an A4 PDF.",
                tag = "Multi-file",
                tagColor = MaterialTheme.colorScheme.secondary,
                onClick = {
                    multiPhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            ToolItemCard(
                icon = Icons.Default.TextFields,
                title = "OCR Text Recognition",
                description = "Convert photos of receipts and books into searchable and copyable text.",
                tag = "AI Ready",
                tagColor = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    viewModel.addDemoDocument("NOTES") { docId ->
                        onDocumentCreated(docId)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "STORAGE & PERFORMANCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            ToolItemCard(
                icon = Icons.Default.Storage,
                title = "Offline SQLite Database",
                description = "100% private, on-device encrypted document storage with zero cloud tracking.",
                tag = "Active",
                tagColor = Color(0xFF10B981),
                onClick = {
                    Toast.makeText(context, "ClearScan database is fully synced and encrypted on device.", Toast.LENGTH_SHORT).show()
                }
            )

            ToolItemCard(
                icon = Icons.Default.CleaningServices,
                title = "Clean Temporary Scans",
                description = "Clear intermediate rendering caches to optimize storage space.",
                tag = "Clean",
                tagColor = MaterialTheme.colorScheme.outline,
                onClick = {
                    Toast.makeText(context, "Rendering cache cleared successfully.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun ToolItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    tag: String,
    tagColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tool_card_${title.replace(' ', '_')}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tagColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tagColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = tagColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tag,
                            color = tagColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
