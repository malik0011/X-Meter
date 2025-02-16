package com.example.djmeter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.djmeter.ui.theme.DjMeterTheme
import com.example.djmeter.viewmodels.MainViewModel
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DjMeterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    var permissionGranted by remember { mutableStateOf(false) }
    val decibel by viewModel.decibelLevel.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    // Animation for the decibel value
    val animatedDecibel by animateFloatAsState(
        targetValue = decibel,
        animationSpec = tween(durationMillis = 100)
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Logo Section
        Spacer(modifier = Modifier.height(32.dp))
        LogoSection()
        Spacer(modifier = Modifier.height(48.dp))

        if (permissionGranted) {
            Text(
                "Sound Level",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Animated meter visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(horizontal = 32.dp)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(15.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(((animatedDecibel / 120f) * 100f).coerceIn(0f, 100f).dp)
                        .background(
                            when {
                                decibel >= 90 -> Color.Red
                                decibel >= 70 -> Color.Yellow
                                else -> Color.Green
                            },
                            RoundedCornerShape(15.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main decibel display
            Text(
                "${decibel.toInt()} dB",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    decibel >= 90 -> Color.Red
                    decibel >= 70 -> Color.Yellow
                    else -> Color.Green
                }
            )
            
            // Additional audio information
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                AudioInfoRow("Sampling Rate", "44.1 kHz")
                AudioInfoRow("Channel", "Mono")
                AudioInfoRow("Intensity", getIntensityDescription(decibel))
                AudioInfoRow("Update Rate", "10 Hz")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Record button with pulsating animation
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isRecording) 1.1f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = ""
            )
            
            Button(
                onClick = {
                    if (isRecording) viewModel.stopRecording()
                    else viewModel.startRecording()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color.Red else Color.Green
                ),
                modifier = Modifier
                    .padding(8.dp)
                    .scale(if (isRecording) scale else 1f)
            ) {
                Text(if (isRecording) "Stop Recording" else "Start Recording")
            }
        } else {
            Text(
                "Microphone permission is required",
                color = Color.Red,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun LogoSection() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer circle with gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00B4DB),
                        Color(0xFF0083B0)
                    )
                ),
                radius = radius,
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // Inner sound wave animation
        val infiniteTransition = rememberInfiniteTransition(label = "")
        val waveScale by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = ""
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(waveScale)
                .padding(24.dp)
        ) {
            val width = size.width
            val height = size.height
            val wavePath = Path()
            
            // Draw stylized sound wave
            val points = 32
            val amplitude = height * 0.2f
            val step = width / points

            wavePath.moveTo(0f, height / 2)
            for (i in 0..points) {
                val x = i * step
                val y = height / 2 + (amplitude * sin(i * Math.PI / 8)).toFloat()
                wavePath.lineTo(x, y)
            }

            drawPath(
                path = wavePath,
                color = Color.White,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }

    // App name with modern typography
    Text(
        text = "DJ METER",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        letterSpacing = 2.sp,
        style = TextStyle(
            shadow = Shadow(
                color = Color(0xFF00B4DB),
                offset = Offset(0f, 2f),
                blurRadius = 3f
            )
        )
    )
}

@Composable
private fun AudioInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getIntensityDescription(decibel: Float): String {
    return when {
        decibel >= 90 -> "Very Loud"
        decibel >= 70 -> "Loud"
        decibel >= 50 -> "Moderate"
        decibel >= 30 -> "Quiet"
        else -> "Very Quiet"
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DjMeterTheme {

    }
}