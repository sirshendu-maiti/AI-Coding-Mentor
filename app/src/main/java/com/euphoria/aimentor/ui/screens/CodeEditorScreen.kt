package com.euphoria.aimentor.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euphoria.aimentor.data.model.ProgrammingLanguage
import com.euphoria.aimentor.ui.components.*
import com.euphoria.aimentor.ui.theme.*
import com.euphoria.aimentor.utils.OcrUtils
import com.euphoria.aimentor.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(viewModel: MainViewModel) {
    val codeInput by viewModel.codeInput.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val beginnerMode by viewModel.beginnerMode.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showResultSheet by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }

    LaunchedEffect(analysisResult) {
        if (analysisResult != null) showResultSheet = true
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val extracted = OcrUtils.extractTextFromUri(context, it)
                if (extracted.isNotBlank()) viewModel.setCodeFromOcr(extracted)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCamera = true
    }

    Box(modifier = Modifier.fillMaxSize().background(MentorBackground)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Code Editor", color = MentorOnSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Beginner Mode", color = MentorOnSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                    Switch(
                        checked = beginnerMode,
                        onCheckedChange = { viewModel.setBeginnerMode(it) },
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(checkedThumbColor = MentorPrimary)
                    )
                }
            }

            // Language Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                ProgrammingLanguage.values().forEach { lang ->
                    LanguageChip(
                        language = if (lang == ProgrammingLanguage.AUTO) "Auto" else lang.displayName,
                        isSelected = selectedLanguage == lang,
                        onClick = { viewModel.selectLanguage(lang) }
                    )
                }
            }

            // Input Source Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputSourceButton(
                    icon = Icons.Default.Image,
                    label = "Gallery",
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
                InputSourceButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Scan Code",
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                )
                Spacer(Modifier.weight(1f))
                if (codeInput.isNotBlank()) {
                    IconButton(onClick = { viewModel.updateCode("") }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = MentorError.copy(alpha = 0.7f))
                    }
                }
            }

            // Code Text Input
            OutlinedTextField(
                value = codeInput,
                onValueChange = { viewModel.updateCode(it) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                placeholder = {
                    Text(
                        "Paste code or use 'Scan Code' to capture from paper/screen...",
                        color = MentorOnSurface.copy(alpha = 0.3f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MentorCodeText
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MentorPrimary,
                    unfocusedBorderColor = MentorSurfaceVariant,
                    focusedContainerColor = MentorCode,
                    unfocusedContainerColor = MentorCode
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionChip(Icons.Default.BugReport, "Debug", MentorError) { viewModel.analyzeCode() }
                ActionChip(Icons.Default.Lightbulb, "Explain", MentorSecondary) { viewModel.explainCode() }
                ActionChip(Icons.Default.Speed, "Complexity", MentorWarning) { viewModel.analyzeComplexity() }
                ActionChip(Icons.Default.Science, "Tests", MentorSuccess) { viewModel.generateTestCases() }
            }
        }

        if (uiState.isLoading) LoadingOverlay()

        // Full Screen Camera Overlay
        if (showCamera) {
            CameraCapture(
                onImageCaptured = { uri ->
                    showCamera = false
                    scope.launch {
                        val extracted = OcrUtils.extractTextFromUri(context, uri)
                        if (extracted.isNotBlank()) viewModel.setCodeFromOcr(extracted)
                    }
                },
                onDismiss = { showCamera = false }
            )
        }
    }

    if (showResultSheet && analysisResult != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showResultSheet = false
                viewModel.clearAnalysis()
            },
            containerColor = MentorSurface
        ) {
            // FIX: Added verticalScroll to the container Column since it was removed from MarkdownText
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mentor Insights", color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { showResultSheet = false; viewModel.clearAnalysis() }) {
                        Icon(Icons.Default.Close, null, tint = MentorOnSurface.copy(alpha = 0.4f))
                    }
                }
                MarkdownText(analysisResult!!)
            }
        }
    }
}

@Composable
private fun InputSourceButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MentorSurfaceVariant),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MentorPrimary)
        Spacer(Modifier.width(6.dp))
        Text(label, color = MentorOnSurface, fontSize = 12.sp)
    }
}

@Composable
private fun ActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
