package com.nhyle.hyperionmaritime.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhyle.hyperionmaritime.data.mtestm.ImportedSheet
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.ui.theme.PassGreen
import com.nhyle.hyperionmaritime.ui.theme.PureWhite
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel

@Composable
fun MTestMImportScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val importedSheets by viewModel.importedSheets.collectAsStateWithLifecycle()
    val pendingSheets by viewModel.pendingMtbSheets.collectAsStateWithLifecycle()
    val selectedExamIds by viewModel.selectedMtbExamIds.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isMtbProcessing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.mtbStatusMessage.collectAsStateWithLifecycle()

    var sheetToDelete by remember { mutableStateOf<ImportedSheet?>(null) }

    var sheetToExport by remember { mutableStateOf<ImportedSheet?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onMtbFilePicked(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { destUri ->
            sheetToExport?.let { sheet ->
                viewModel.writeSheetToCsvUri(sheet, destUri)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadImportedSheets()
    }

    BackHandler {
        viewModel.navigateBack()
    }

    // Delete Confirmation Dialog
    sheetToDelete?.let { sheet ->
        AlertDialog(
            onDismissRequest = { sheetToDelete = null },
            shape = RectangleShape,
            containerColor = colors.surface,
            modifier = Modifier.border(1.dp, colors.border),
            title = {
                Text(
                    text = "DELETE IMPORTED SHEET",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${sheet.title}' (${sheet.questionCount} questions)?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteImportedSheet(sheet.id)
                        sheetToDelete = null
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = PureWhite
                    )
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { sheetToDelete = null },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("CANCEL", color = colors.foreground)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "MTESTM IMPORT",
            subtitle = "${importedSheets.size} CUSTOM SHEET(S)",
            showBackButton = false,
            onMenuClick = onOpenDrawer
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Upload Card
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(colors.accent, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = "Upload",
                                    tint = colors.accentForeground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SELECT .MTB, .CSV, OR .XLSX FILE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Import question sheets from MTestM (.mtb), Excel (.xlsx), or CSV (.csv).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.accentForeground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("pick_mtb_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BROWSE & PICK FILE",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        if (statusMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isProcessing) colors.accent else colors.foreground,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }

                        if (isProcessing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.accent
                                )
                                Text(
                                    text = "Processing package...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Sheet Selection Modal / In-line list when file is loaded
            if (pendingSheets.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RectangleShape,
                        color = colors.surface,
                        border = BorderStroke(1.5.dp, colors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECT SHEETS TO IMPORT",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.accent,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                IconButton(onClick = { viewModel.clearPendingMtbFile() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = colors.muted
                                    )
                                }
                            }

                            Text(
                                text = "${selectedExamIds.size} of ${pendingSheets.size} sheets selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.selectAllMtbExams(true) },
                                    shape = RectangleShape
                                ) {
                                    Text("SELECT ALL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                TextButton(
                                    onClick = { viewModel.selectAllMtbExams(false) },
                                    shape = RectangleShape
                                ) {
                                    Text("DESELECT ALL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                pendingSheets.forEach { sheetInfo ->
                                    val isSelected = selectedExamIds.contains(sheetInfo.examId)
                                    Surface(
                                        shape = RectangleShape,
                                        color = if (isSelected) colors.surface2 else colors.surface,
                                        border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleSelectMtbExam(sheetInfo.examId) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { viewModel.toggleSelectMtbExam(sheetInfo.examId) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = colors.accent,
                                                    checkmarkColor = colors.accentForeground
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sheetInfo.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.foreground
                                                )
                                                Text(
                                                    text = "${sheetInfo.questionCount} Questions",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.muted,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { viewModel.confirmImportSelectedMtbSheets() },
                                enabled = selectedExamIds.isNotEmpty() && !isProcessing,
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PassGreen,
                                    contentColor = PureWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "IMPORT ${selectedExamIds.size} SELECTED SHEET(S)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Imported Sheets List
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IMPORTED SHEETS (${importedSheets.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accent,
                        letterSpacing = 1.2.sp
                    )

                    if (importedSheets.size > 1) {
                        TextButton(
                            onClick = { viewModel.startDrillForAllImportedSheets() },
                            shape = RectangleShape
                        ) {
                            Text(
                                text = "DRILL ALL (${importedSheets.sumOf { it.questionCount }})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = colors.accent
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (importedSheets.isEmpty()) {
                item {
                    Surface(
                        shape = RectangleShape,
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NO IMPORTED SHEETS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.muted,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap 'Browse & Pick File' above to import your question sheets (.mtb, .xlsx, .csv). You can drill and review each imported sheet individually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(importedSheets, key = { it.id }) { sheet ->
                    Surface(
                        shape = RectangleShape,
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sheet.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foreground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${sheet.questionCount} Questions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { viewModel.startDrillForImportedSheet(sheet) },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.accentForeground
                                ),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Drill",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DRILL",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    sheetToExport = sheet
                                    exportLauncher.launch("${sheet.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.csv")
                                },
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text(
                                    text = "EXPORT",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = colors.foreground
                                )
                            }

                            IconButton(
                                onClick = { sheetToDelete = sheet },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = colors.muted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}