package com.example.fxsoundreplica

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fxsoundreplica.ui.theme.FxSoundReplicaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FxSoundReplicaTheme {
                val context = LocalContext.current
                val viewModel: MainViewModel = viewModel()
                
                val hasPermission = remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        hasPermission.value = isGranted
                        viewModel.bindService(context)
                    }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission.value) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.bindService(context)
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F0F)
                ) {
                    FxSoundScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FxSoundScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    val isOptimized = remember(uiState.isEnabled) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName).not()
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Preset", color = Color.White) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                TextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    placeholder = { Text("Preset Name", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2C2C2C),
                        unfocusedContainerColor = Color(0xFF2C2C2C),
                        cursorColor = Color(0xFF00E5FF)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            viewModel.saveCurrentAsPreset(newPresetName)
                            newPresetName = ""
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = "EQX",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF00E5FF),
            letterSpacing = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isOptimized) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF331111)),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "IMPORTANT: Vivo/Oppo detected. Tap to allow 'Background Activity' and disable 'Battery Optimization'.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Master Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Master Power", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (uiState.isEnabled) "Enhancements Active" else "Processing Disabled",
                            fontSize = 13.sp,
                            color = if (uiState.isEnabled) Color(0xFF00E5FF) else Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = uiState.isEnabled,
                        onCheckedChange = { viewModel.toggleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFF00E5FF),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF2C2C2C)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Presets List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PRESETS", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            IconButton(onClick = { showSaveDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Save Preset", tint = Color(0xFF00E5FF), modifier = Modifier.size(28.dp))
            }
        }
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(presets) { preset ->
                PresetItem(
                    preset = preset,
                    isSelected = uiState.id == preset.id,
                    onClick = { viewModel.applyPreset(preset) },
                    onDelete = { viewModel.deletePreset(preset.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pro Effects Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("PRO EFFECTS", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                EffectSlider("Clarity", uiState.clarity) { viewModel.updateClarity(it) }
                EffectSlider("Ambience", uiState.ambience) { viewModel.updateAmbience(it) }
                EffectSlider("Surround", uiState.surround) { viewModel.updateSurround(it) }
                EffectSlider("Reverb", uiState.reverb) { viewModel.updateReverb(it) }
                EffectSlider("Dynamic Boost", uiState.dynamicBoost) { viewModel.updateDynamicBoost(it) }
                EffectSlider("Bass Boost", uiState.bassBoost) { viewModel.updateBassBoost(it) }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Equalizer Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "10-BAND EQUALIZER", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 2.sp)
            Text(text = "SCROLL →", fontSize = 11.sp, color = Color(0xFF00E5FF).copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // POLISHED Equalizer Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            color = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
            ) {
                itemsIndexed(uiState.eqBands) { index, level ->
                    EqBandControl(index, level) { viewModel.updateEqBand(index, it) }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
        ) {
            Text("ADVANCED SOUND SETTINGS", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tip: Vivo Y29 users must turn OFF 'Hi-Fi' or 'DeepField' in Vivo Sound Settings. Ensure battery use is 'Unrestricted'.",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
fun PresetItem(preset: AudioSettings, isSelected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 90.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF262626),
        contentColor = if (isSelected) Color.Black else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(preset.name, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            if (!listOf("music", "gaming", "movie", "car_dsp", "ktv", "panoramic").contains(preset.id)) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(18.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = if (isSelected) Color.Black else Color.Gray)
                }
            }
        }
    }
}

@Composable
fun EffectSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${(value * 10).toInt()}", color = Color(0xFF00E5FF), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..10f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0xFF2C2C2C)
            )
        )
    }
}

@Composable
fun EqBandControl(index: Int, level: Float, onValueChange: (Float) -> Unit) {
    val labels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(62.dp)
    ) {
        Text(
            text = if (level > 0) "+${level.toInt()}" else "${level.toInt()}",
            fontSize = 11.sp,
            color = if (level == 0f) Color.Gray else Color(0xFF00E5FF),
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        CustomVerticalSlider(
            value = level,
            onValueChange = onValueChange,
            range = -15f..15f,
            modifier = Modifier
                .height(160.dp)
                .width(62.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = labels.getOrElse(index) { "" },
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CustomVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var heightPx by remember { mutableStateOf(1f) }
    
    Box(
        modifier = modifier
            .onSizeChanged { if (it.height > 0) heightPx = it.height.toFloat() }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val y = change.position.y
                    val ratio = (1f - (y / heightPx)).coerceIn(0f, 1f)
                    val newValue = range.start + ratio * (range.endInclusive - range.start)
                    onValueChange(newValue)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val ratio = (1f - (offset.y / heightPx)).coerceIn(0f, 1f)
                    val newValue = range.start + ratio * (range.endInclusive - range.start)
                    onValueChange(newValue)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Main Track
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Color(0xFF262626))
        )
        
        // Center Line (0dB)
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )
        
        // Thumb calculation
        val ratio = (value - range.start) / (range.endInclusive - range.start)
        val thumbOffset = with(density) { ((1f - ratio) * heightPx).toDp() }
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .offset(y = thumbOffset - 10.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, if (value == 0f) Color.Gray else Color(0xFF00E5FF), CircleShape)
            )
        }
    }
}
