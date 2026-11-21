package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.animation.bounceClick
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MotionEasing
import com.example.utils.Code128Encoder

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Genuine ISO/IEC 15417 Code 128B 1D Optical Barcode Visualizer.
 * Produces scannable monochrome bar/space sequences with standard quiet zones.
 */
@Composable
fun BarcodeVisualizer(
    code: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val cleanCode = remember(code) { code.trim() }
    val modules = remember(cleanCode) { Code128Encoder.encodeCode128B(cleanCode) }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (modules != null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    val totalModules = modules.size
                    val moduleWidth = size.width / totalModules.toFloat()
                    var runStart = -1
                    for (i in 0 until totalModules) {
                        if (modules[i]) {
                            if (runStart == -1) runStart = i
                        } else if (runStart != -1) {
                            drawRect(
                                color = barColor,
                                topLeft = Offset(x = runStart * moduleWidth, y = 0f),
                                size = Size(width = (i - runStart) * moduleWidth, height = size.height)
                            )
                            runStart = -1
                        }
                    }
                    if (runStart != -1) {
                        drawRect(
                            color = barColor,
                            topLeft = Offset(x = runStart * moduleWidth, y = 0f),
                            size = Size(width = (totalModules - runStart) * moduleWidth, height = size.height)
                        )
                    }
                }
            } else {
                Text(
                    text = "No valid Code 128B data",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = cleanCode.ifBlank { "NO BARCODE DATA" },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.5.sp,
                color = barColor
            )
            Text(
                text = "ISO/IEC 15417 • Code 128B",
                fontSize = 9.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

/**
 * Campus Digital Student ID Pass visual representation with scannable Code 128B optical barcode.
 */
@Composable
fun StudentPassCard(
    universityName: String = "CAMPUS STUDENT PASS",
    studentName: String = "Student Cardholder",
    program: String = "",
    batch: String = "",
    studentCode: String = "",
    barcodeNumber: String = "",
    bloodGroup: String = "",
    dob: String = "",
    modifier: Modifier = Modifier
) {
    var showPunchPassDialog by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.92f,
        animationSpec = tween(durationMillis = 350, easing = MotionEasing.EmphasizedDecelerate),
        label = "studentPassScale"
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 350, easing = MotionEasing.Emphasized),
        label = "studentPassAlpha"
    )

    val resolvedBarcode = barcodeNumber.ifBlank { studentCode }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = cardAlpha
            }
            .testTag("student_pass_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "University Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = universityName.ifBlank { "CAMPUS STUDENT PASS" }.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E3A8A),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp, 110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE2E8F0))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Student Photo",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studentName.ifBlank { "Student Cardholder" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (program.isNotBlank()) StudentInfoItem(label = "Program", value = program)
                    if (batch.isNotBlank()) StudentInfoItem(label = "Batch", value = batch)
                    if (studentCode.isNotBlank()) StudentInfoItem(label = "Code", value = studentCode)
                    if (bloodGroup.isNotBlank()) StudentInfoItem(label = "Blood Group", value = bloodGroup)
                    if (dob.isNotBlank()) StudentInfoItem(label = "Date of Birth", value = dob)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            BarcodeVisualizer(
                code = resolvedBarcode,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { showPunchPassDialog = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { showPunchPassDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_punch_pass_button")
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Full-Brightness Optical Pass")
            }
        }
    }

    if (showPunchPassDialog) {
        EntryPunchPassDialog(
            universityName = universityName,
            studentName = studentName,
            studentCode = studentCode,
            barcodeNumber = resolvedBarcode,
            onDismiss = { showPunchPassDialog = false }
        )
    }
}

@Composable
private fun StudentInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label : ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF475569),
            fontSize = 12.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            fontSize = 12.sp
        )
    }
}

/**
 * Full-screen optical barcode pass view that automatically boosts window screen brightness
 * to maximum for turnstile laser/camera scanners and restores brightness on dismiss.
 */
@Composable
fun EntryPunchPassDialog(
    universityName: String,
    studentName: String,
    studentCode: String,
    barcodeNumber: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        val previousBrightness = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (window != null) {
            val lp = window.attributes
            lp.screenBrightness = 1.0f
            window.attributes = lp
        }
        onDispose {
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = previousBrightness
                window.attributes = lp
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OPTICAL ENTRY PASS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Pass", tint = Color.Black)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = universityName.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = studentName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "ID Code: $studentCode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Hold Code 128B barcode under optical gate scanner",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    BarcodeVisualizer(
                        code = barcodeNumber,
                        barColor = Color.Black,
                        backgroundColor = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, tint = IndigoPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Screen brightness has been temporarily boosted to maximum for reliable optical barcode scanning.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }
            }
        }
    }
}
