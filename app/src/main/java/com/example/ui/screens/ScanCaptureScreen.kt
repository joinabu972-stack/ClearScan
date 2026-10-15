package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.example.data.model.FilterType
import com.example.ui.viewmodel.ScannerViewModel
import com.example.util.ImageProcessingUtils
import com.example.util.SampleDocumentGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanCaptureScreen(
    viewModel: ScannerViewModel,
    capturedPages: List<Pair<Bitmap, FilterType>>,
    onFinishCapture: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFlashOn by remember { mutableStateOf(false) }
    var isGridOn by remember { mutableStateOf(true) }
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedDocType by remember { mutableStateOf("Document") }

    // Android zero-permission Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = ImageProcessingUtils.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                viewModel.addCapturedPage(bitmap, FilterType.MAGIC)
                if (!isBatchMode) {
                    onFinishCapture()
                }
            }
        }
    }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_animation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Scanner Viewfinder Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 180.dp, top = 70.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Target box aspect ratio based on doc type
                val frameWidth = canvasWidth * 0.88f
                val frameHeight = if (selectedDocType == "ID Card") {
                    frameWidth * 0.63f // ID card aspect ratio
                } else if (selectedDocType == "Receipt") {
                    frameWidth * 1.5f
                } else {
                    frameWidth * 1.414f // Standard A4 aspect ratio
                }.coerceAtMost(canvasHeight * 0.85f)

                val left = (canvasWidth - frameWidth) / 2f
                val top = (canvasHeight - frameHeight) / 2f

                // Outer dimmed overlay
                drawRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    size = size
                )

                // Viewfinder clear window with cyan border
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(frameWidth, frameHeight),
                    cornerRadius = CornerRadius(20f, 20f)
                )

                // Grid guidelines
                if (isGridOn) {
                    val strokeGrid = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    // 1/3 and 2/3 vertical
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(left + frameWidth / 3f, top),
                        end = Offset(left + frameWidth / 3f, top + frameHeight),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(left + 2f * frameWidth / 3f, top),
                        end = Offset(left + 2f * frameWidth / 3f, top + frameHeight),
                        strokeWidth = 1f
                    )
                    // 1/3 and 2/3 horizontal
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(left, top + frameHeight / 3f),
                        end = Offset(left + frameWidth, top + frameHeight / 3f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(left, top + 2f * frameHeight / 3f),
                        end = Offset(left + frameWidth, top + 2f * frameHeight / 3f),
                        strokeWidth = 1f
                    )
                }

                // 4 Sharp Scanner Corner Brackets
                val cornerLen = 45f
                val cornerStroke = 7f
                val cornerColor = Color(0xFF06B6D4) // Bright Cyan

                // Top-Left
                drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), cornerStroke)
                drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), cornerStroke)

                // Top-Right
                drawLine(cornerColor, Offset(left + frameWidth, top), Offset(left + frameWidth - cornerLen, top), cornerStroke)
                drawLine(cornerColor, Offset(left + frameWidth, top), Offset(left + frameWidth, top + cornerLen), cornerStroke)

                // Bottom-Left
                drawLine(cornerColor, Offset(left, top + frameHeight), Offset(left + cornerLen, top + frameHeight), cornerStroke)
                drawLine(cornerColor, Offset(left, top + frameHeight), Offset(left, top + frameHeight - cornerLen), cornerStroke)

                // Bottom-Right
                drawLine(cornerColor, Offset(left + frameWidth, top + frameHeight), Offset(left + frameWidth - cornerLen, top + frameHeight), cornerStroke)
                drawLine(cornerColor, Offset(left + frameWidth, top + frameHeight), Offset(left + frameWidth, top + frameHeight - cornerLen), cornerStroke)

                // Animated Laser Scanning Line
                val laserY = top + (frameHeight * laserProgress)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF38BDF8),
                            Color(0xFF0284C7),
                            Color(0xFF38BDF8),
                            Color.Transparent
                        ),
                        startX = left,
                        endX = left + frameWidth
                    ),
                    start = Offset(left + 10f, laserY),
                    end = Offset(left + frameWidth - 10f, laserY),
                    strokeWidth = 4f
                )
            }

            // Target Helper Instruction
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = "Align $selectedDocType within frame",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Top Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Document", "ID Card", "Receipt").forEach { type ->
                            val isSelected = selectedDocType == type
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { selectedDocType = type }
                            ) {
                                Text(
                                    text = type,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { isFlashOn = !isFlashOn }) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) Color.Yellow else Color.White
                        )
                    }
                    IconButton(onClick = { isGridOn = !isGridOn }) {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = "Grid",
                            tint = if (isGridOn) Color(0xFF38BDF8) else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Batch mode toggle chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isBatchMode) Color(0xFF0284C7) else Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { isBatchMode = !isBatchMode }
                ) {
                    Text(
                        text = if (isBatchMode) "● Batch Mode ON (Multi-page)" else "○ Single Mode",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Bottom Controls Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Captured pages preview tray
            if (capturedPages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${capturedPages.size} ${if (capturedPages.size == 1) "page" else "pages"} captured",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = onFinishCapture,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save & Review", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    itemsIndexed(capturedPages) { index, pair ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color(0xFF0284C7), RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                bitmap = pair.first.asImageBitmap(),
                                contentDescription = "Captured page $index",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { viewModel.removeCapturedPage(index) },
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // Quick Demo simulation tags (especially helpful for browser preview / emulator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        val sample = SampleDocumentGenerator.createSampleDocument(context, "INVOICE")
                        viewModel.addCapturedPage(sample, FilterType.MAGIC)
                        if (!isBatchMode) onFinishCapture()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Snap Invoice", fontSize = 11.sp, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        val sample = SampleDocumentGenerator.createSampleDocument(context, "ID_CARD")
                        viewModel.addCapturedPage(sample, FilterType.MAGIC)
                        if (!isBatchMode) onFinishCapture()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Snap ID Card", fontSize = 11.sp, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        val sample = SampleDocumentGenerator.createSampleDocument(context, "RECEIPT")
                        viewModel.addCapturedPage(sample, FilterType.MAGIC)
                        if (!isBatchMode) onFinishCapture()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Snap Receipt", fontSize = 11.sp, color = Color.White)
                }
            }

            // Shutter Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo Picker Gallery Button
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("gallery_import_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Import from Photos",
                        tint = Color.White
                    )
                }

                // Main Circular Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF38BDF8),
                                    Color(0xFF0284C7)
                                )
                            )
                        )
                        .clickable {
                            // Capture simulated document based on selected type or user document
                            val sample = SampleDocumentGenerator.createSampleDocument(context, selectedDocType)
                            viewModel.addCapturedPage(sample, FilterType.MAGIC)
                            if (!isBatchMode) {
                                onFinishCapture()
                            }
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Batch Finish or Done Button
                if (capturedPages.isNotEmpty()) {
                    IconButton(
                        onClick = onFinishCapture,
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF10B981), CircleShape)
                            .testTag("finish_capture_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }
        }
    }
}
