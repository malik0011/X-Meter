package com.example.djmeter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.djmeter.ui.theme.DjMeterTheme
import com.example.djmeter.viewmodels.MainViewModel
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
                    MainScreen(baseContext)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(context: Context, viewModel: MainViewModel = viewModel()) {
    var permissionGranted by remember { mutableStateOf(false) }
    val decibel by viewModel.decibelLevel.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Store last 50 readings for the graph
    val readings = remember { mutableStateListOf<Float>() }
    
    // Update readings when decibel changes
    LaunchedEffect(decibel) {
        if (isRecording) {
            if (readings.size >= 50) {
                readings.removeAt(0)
            }
            readings.add(decibel)
        }
    }

    // Check if permission is already granted
    LaunchedEffect(Unit) {
        permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    val bottomSheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
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
                            .width(((decibel / 120f) * 100f).coerceIn(0f, 100f).dp)
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Microphone permission is required",
                        color = Color.Red,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Grant Permission")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showBottomSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            BadgedBox(
                badge = {
                    if (isRecording) {
                        Badge { Text("") }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_graph),
                    contentDescription = "Show Graph"
                )
            }
        }

        // Bottom Sheet
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = bottomSheetState,
                containerColor = Color(0xFF1E1E1E)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    DecibelGraph(
                        readings = readings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
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

@Composable
private fun DecibelGraph(
    readings: List<Float>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Decibel History",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (readings.isNotEmpty()) {
                    val path = Path()
                    val points = readings.size
                    val width = size.width
                    val height = size.height
                    val dx = width / (points - 1)
                    
                    // Draw grid lines
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(0f, height / 2),
                        end = Offset(width, height / 2),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    // Draw graph line
                    path.moveTo(0f, height - (height * (readings[0] / 120f)))
                    
                    for (i in 1 until points) {
                        val x = i * dx
                        val y = height - (height * (readings[i] / 120f))
                        path.lineTo(x, y)
                    }
                    
                    drawPath(
                        path = path,
                        color = Color(0xFF00B4DB),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DjMeterTheme {

    }
}