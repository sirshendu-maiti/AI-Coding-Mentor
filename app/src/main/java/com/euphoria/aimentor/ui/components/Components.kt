package com.euphoria.aimentor.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euphoria.aimentor.ui.theme.*

// ─── Loading Overlay ──────────────────────────────────────────────────────

@Composable
fun LoadingOverlay(message: String = "AI is thinking...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MentorSurface)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = MentorPrimary)
                Text(message, color = MentorOnSurface, fontSize = 14.sp)
            }
        }
    }
}

// ─── Code Block ───────────────────────────────────────────────────────────

@Composable
fun CodeBlock(
    code: String,
    language: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MentorCode)
            .padding(12.dp)
    ) {
        if (language.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    color = MentorPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MentorOnSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(14.dp).clickable { /* TODO: Copy to clipboard */ }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = code,
            color = MentorCodeText,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

// ─── Feature Action Button ────────────────────────────────────────────────

@Composable
fun FeatureButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MentorPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Section Header ───────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MentorPrimary, modifier = Modifier.size(20.dp))
        Text(title, color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

// ─── Error Banner ─────────────────────────────────────────────────────────

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MentorError.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = MentorError, modifier = Modifier.size(18.dp))
            Text(message, color = MentorError, modifier = Modifier.weight(1f), fontSize = 13.sp)
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MentorError, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─── Language Chip ────────────────────────────────────────────────────────

@Composable
fun LanguageChip(
    language: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MentorPrimary else MentorSurfaceVariant
    val text = if (isSelected) Color.White else MentorOnSurface.copy(alpha = 0.7f)

    Text(
        text = language,
        color = text,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

// ─── Markdown Text Renderer ───────────────────────────────────────

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val lines = text.split("\n")
        var inCodeBlock = false
        val codeBuffer = StringBuilder()
        var codeLang = ""

        for (line in lines) {
            when {
                line.startsWith("```") -> {
                    if (inCodeBlock) {
                        CodeBlock(codeBuffer.toString().trim(), codeLang)
                        codeBuffer.clear()
                        inCodeBlock = false
                        codeLang = ""
                    } else {
                        inCodeBlock = true
                        codeLang = line.removePrefix("```").trim()
                    }
                }
                inCodeBlock -> codeBuffer.appendLine(line)
                line.startsWith("### ") -> Text(
                    line.removePrefix("### "),
                    color = MentorSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                line.startsWith("## ") -> Text(
                    line.removePrefix("## "),
                    color = MentorPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                line.startsWith("# ") -> Text(
                    line.removePrefix("# "),
                    color = MentorPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                line.startsWith("- ") || line.startsWith("* ") -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("•", color = MentorPrimary, fontWeight = FontWeight.Bold)
                    Text(parseInlineMarkdown(line.drop(2)), color = MentorOnSurface.copy(alpha = 0.9f), fontSize = 14.sp)
                }
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val parts = line.split(". ", limit = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${parts[0]}.", color = MentorPrimary, fontWeight = FontWeight.Bold)
                        Text(parseInlineMarkdown(parts.getOrElse(1) { "" }), color = MentorOnSurface.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                }
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                else -> Text(
                    parseInlineMarkdown(line),
                    color = MentorOnSurface.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * Parses inline markdown like **bold** and `code` into AnnotatedString
 */
private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentText = text
        
        // This is a simple sequential parser for ** and `
        // For a production app, a more robust regex-based parser would be better
        val tokens = mutableListOf<Pair<String, Boolean>>() // text to isCode/isBold
        
        var i = 0
        while (i < currentText.length) {
            val remaining = currentText.substring(i)
            when {
                remaining.startsWith("**") -> {
                    val end = remaining.indexOf("**", 2)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MentorOnSurface)) {
                            append(remaining.substring(2, end))
                        }
                        i += end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }
                remaining.startsWith("`") -> {
                    val end = remaining.indexOf("`", 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = MentorCode,
                            color = MentorCodeText,
                            fontSize = 13.sp
                        )) {
                            append(" ${remaining.substring(1, end)} ")
                        }
                        i += end + 1
                    } else {
                        append("`")
                        i += 1
                    }
                }
                else -> {
                    append(currentText[i].toString())
                    i++
                }
            }
        }
    }
}
