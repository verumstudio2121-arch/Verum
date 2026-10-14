package com.example.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.example.ui.background.BackgroundPreset
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.CustomRingtoneManager
import com.example.audio.SoundPlayer
import com.example.data.PreferencesManager
import com.example.util.AppIconManager
import com.example.ui.theme.AccentColorTheme
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentTheme
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.InterfaceStyle
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalInterfaceStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.LocalHapticManager
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit
) {
    val settings by preferencesManager.settings.collectAsState()
    val haptic = LocalHapticManager.current
    val currentAccent = LocalAccentColor.current
    val accentColor = currentAccent.primary
    val currentInterfaceStyle = remember(settings.interfaceStyle) {
        InterfaceStyle.fromId(settings.interfaceStyle)
    }
    val context = LocalContext.current
    val iconManager = remember { AppIconManager.getInstance(context) }
    val currentIconStyle by iconManager.currentIconStyle.collectAsState()
    val customIconBitmap by iconManager.customIconBitmap.collectAsState()

    val ringtoneManager = remember { CustomRingtoneManager(context) }
    var customRingtones by remember { mutableStateOf(ringtoneManager.getCustomRingtones()) }
    var previewingSound by remember { mutableStateOf<String?>(null) }
    var showColorWheelDialog by remember { mutableStateOf(false) }

    // Launcher for selecting a custom app icon image from the gallery (zero-permission Photo Picker)
    val galleryIconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val success = iconManager.setCustomIconFromUri(uri)
            if (success) {
                haptic.confirm()
            }
        }
    }

    // Launcher for selecting a custom background wallpaper photo from the gallery (zero-permission Photo Picker)
    val backgroundPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val destFile = File(context.filesDir, "custom_background.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                preferencesManager.updateCustomPhotoPath(destFile.absolutePath)
                preferencesManager.updateBackgroundType(BackgroundPreset.CUSTOM_PHOTO.id)
                haptic.confirm()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Audio file picker launcher for custom device ringtones
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val imported = ringtoneManager.importRingtoneFromUri(uri)
            if (imported != null) {
                customRingtones = ringtoneManager.getCustomRingtones()
                preferencesManager.updateDefaultAlarmSound(imported.name)
                SoundPlayer.previewSound(context, imported.name)
                previewingSound = imported.name
                haptic.confirm()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            SoundPlayer.stopPreview()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (settings.backgroundType == "Black") DeepBlack else DeepBlack.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 120.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable {
                            haptic.buttonClick()
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // APPEARANCE SECTION
            SettingsSectionTitle("APPEARANCE")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                backgroundColor = GlassSurface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // INTERFACE STYLE (Pure Liquid Glass, Minimal, Mix Liquid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Interface Style",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Three color-adaptive design themes",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }

                        // Badge showing current interface style
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.18f))
                                .border(1.dp, accentColor.copy(alpha = 0.50f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentInterfaceStyle.title,
                                color = currentAccent.light,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Interface Style Cards
                    InterfaceStyle.entries.forEach { styleOption ->
                        val isSelected = currentInterfaceStyle == styleOption
                        InterfaceStyleCard(
                            style = styleOption,
                            isSelected = isSelected,
                            accentColor = accentColor,
                            onClick = {
                                haptic.buttonClick()
                                preferencesManager.updateInterfaceStyle(styleOption.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subtle separator line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text("Theme Mode", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Dark", "Light", "System").forEach { mode ->
                            val isSelected = settings.appearance == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                                    .clickable {
                                        haptic.buttonClick()
                                        preferencesManager.updateAppearance(mode)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ACCENT COLOR SETTING
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Accent Color",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Liquid glass glow & highlights",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }

                        // Badge showing current accent name
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.18f))
                                .border(1.dp, accentColor.copy(alpha = 0.50f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentAccent.displayName,
                                color = currentAccent.light,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Vibrant Color Swatches Row with Color Wheel Launcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccentColorTheme.entries.forEach { option ->
                            val isSelected = currentAccent.id == option.id
                            AccentColorSwatch(
                                option = option,
                                isSelected = isSelected,
                                onClick = {
                                    haptic.buttonClick()
                                    preferencesManager.updateAccentColor(option.id)
                                }
                            )
                        }

                        // Color Wheel Picker Button
                        val isCustomSelected = !AccentColorTheme.entries.any { it.id.equals(currentAccent.id, ignoreCase = true) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        haptic.buttonClick()
                                        showColorWheelDialog = true
                                    }
                                )
                                .padding(vertical = 4.dp)
                                .testTag("open_color_wheel_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCustomSelected) {
                                            Brush.radialGradient(
                                                listOf(
                                                    currentAccent.primary.copy(alpha = 0.5f),
                                                    Color.Transparent
                                                )
                                            )
                                        } else {
                                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                        }
                                    )
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(
                                                listOf(
                                                    Color(0xFFFF0000),
                                                    Color(0xFFFFFF00),
                                                    Color(0xFF00FF00),
                                                    Color(0xFF00FFFF),
                                                    Color(0xFF0000FF),
                                                    Color(0xFFFF00FF),
                                                    Color(0xFFFF0000)
                                                )
                                            )
                                        )
                                        .border(
                                            width = if (isCustomSelected) 2.5.dp else 1.dp,
                                            color = if (isCustomSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Liquid center hub
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isCustomSelected) currentAccent.primary else DeepBlack.copy(alpha = 0.75f))
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCustomSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected custom color",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = "Color Wheel",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isCustomSelected) "Custom" else "Wheel",
                                color = if (isCustomSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isCustomSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // BACKGROUND & WALLPAPER SECTION
            SettingsSectionTitle("BACKGROUND & WALLPAPER")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                backgroundColor = GlassSurface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Background Style",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pure black by default, presets, or custom photos",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }

                        // Badge showing current background
                        val currentPreset = remember(settings.backgroundType) {
                            BackgroundPreset.fromId(settings.backgroundType)
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.18f))
                                .border(1.dp, accentColor.copy(alpha = 0.50f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentPreset.title,
                                color = currentAccent.light,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // All Background Preset Options
                    BackgroundPreset.entries.forEach { option ->
                        val isSelected = settings.backgroundType.equals(option.id, ignoreCase = true)
                        BackgroundOptionCard(
                            preset = option,
                            isSelected = isSelected,
                            accentColor = accentColor,
                            customPhotoPath = if (option == BackgroundPreset.CUSTOM_PHOTO) settings.customPhotoPath else null,
                            onSelect = {
                                haptic.buttonClick()
                                if (option == BackgroundPreset.CUSTOM_PHOTO && settings.customPhotoPath.isNullOrEmpty()) {
                                    backgroundPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    preferencesManager.updateBackgroundType(option.id)
                                }
                            },
                            onPickPhoto = {
                                haptic.buttonClick()
                                backgroundPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemovePhoto = {
                                haptic.buttonClick()
                                val file = File(context.filesDir, "custom_background.jpg")
                                if (file.exists()) file.delete()
                                preferencesManager.updateCustomPhotoPath(null)
                                preferencesManager.updateBackgroundType(BackgroundPreset.BLACK.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Background Dimming Scrim slider (visible when any non-black preset or custom photo is active)
                    if (!settings.backgroundType.equals(BackgroundPreset.BLACK.id, ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Background Dimming",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Darkens wallpaper for high contrast & clarity",
                                    color = TextTertiary,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = "${(settings.backgroundDim * 100).roundToInt()}%",
                                color = currentAccent.light,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = settings.backgroundDim,
                            onValueChange = { newDim ->
                                preferencesManager.updateBackgroundDim(newDim)
                            },
                            valueRange = 0.15f..0.85f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick "Reset to Default Black" button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    haptic.buttonClick()
                                    preferencesManager.updateBackgroundType(BackgroundPreset.BLACK.id)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset to Default Black",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CLOCK FORMAT SECTION
            SettingsSectionTitle("CLOCK FORMAT")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                backgroundColor = GlassSurface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 12h vs 24h
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("24-Hour Time", color = TextPrimary, fontSize = 16.sp)
                        Switch(
                            checked = settings.clockFormat == "24 hour",
                            onCheckedChange = { checked ->
                                haptic.toggle(checked)
                                preferencesManager.updateClockFormat(if (checked) "24 hour" else "12 hour")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor.copy(alpha = 0.8f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Show Seconds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Seconds in Clock", color = TextPrimary, fontSize = 16.sp)
                        Switch(
                            checked = settings.showSeconds,
                            onCheckedChange = { checked ->
                                haptic.toggle(checked)
                                preferencesManager.updateShowSeconds(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor.copy(alpha = 0.8f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ALARM PREFERENCES
            SettingsSectionTitle("ALARM PREFERENCES")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                backgroundColor = GlassSurface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Alarm Volume", color = TextPrimary, fontSize = 16.sp)
                        Text(
                            text = "${(settings.alarmVolume * 100).roundToInt()}%",
                            color = accentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeDown, contentDescription = null, tint = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = settings.alarmVolume,
                            onValueChange = { newVal ->
                                val oldBucket = (settings.alarmVolume * 20).toInt()
                                val newBucket = (newVal * 20).toInt()
                                if (newBucket != oldBucket) {
                                    if (newVal <= 0.01f || newVal >= 0.99f) {
                                        haptic.sliderSnap()
                                    } else {
                                        haptic.sliderTick()
                                    }
                                }
                                preferencesManager.updateAlarmVolume(newVal)
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Default Sound
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default Alarm Sound", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)

                        // Add Custom Ringtone Button
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f))
                                .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    haptic.buttonClick()
                                    audioPickerLauncher.launch("audio/*")
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("settings_add_custom_ringtone_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add custom ringtone",
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add Tone",
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom device ringtones
                        customRingtones.forEach { customTone ->
                            val isSelected = settings.defaultAlarmSound == customTone.name || settings.defaultAlarmSound == customTone.filePath
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) accentColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, if (isSelected) accentColor else GlassBorder, CircleShape)
                                    .clickable {
                                        haptic.buttonClick()
                                        preferencesManager.updateDefaultAlarmSound(customTone.name)
                                        SoundPlayer.previewSound(context, customTone.name)
                                        previewingSound = customTone.name
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = customTone.name,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Preset sounds
                        SoundPlayer.AVAILABLE_SOUNDS.forEach { sound ->
                            val isSelected = settings.defaultAlarmSound == sound
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, if (isSelected) Color.White.copy(alpha = 0.5f) else GlassBorder, CircleShape)
                                    .clickable {
                                        haptic.buttonClick()
                                        preferencesManager.updateDefaultAlarmSound(sound)
                                        SoundPlayer.previewSound(context, sound)
                                        previewingSound = sound
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = sound,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Vibration Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibration", color = TextPrimary, fontSize = 16.sp)
                        Switch(
                            checked = settings.vibrationEnabled,
                            onCheckedChange = { checked ->
                                haptic.toggle(checked)
                                preferencesManager.updateVibrationEnabled(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor.copy(alpha = 0.8f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Snooze Duration
                    Text("Snooze Duration", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(5, 10, 15, 20, 30).forEach { mins ->
                            val isSelected = settings.snoozeDurationMinutes == mins
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) accentColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, if (isSelected) accentColor else GlassBorder, CircleShape)
                                    .clickable {
                                        haptic.buttonClick()
                                        preferencesManager.updateSnoozeDuration(mins)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${mins}m",
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TIMER SOUND SECTION
            SettingsSectionTitle("TIMER SOUND")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                backgroundColor = GlassSurface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Sound when countdown ends", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom device ringtones
                        customRingtones.forEach { customTone ->
                            val isSelected = settings.timerSound == customTone.name || settings.timerSound == customTone.filePath
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) accentColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, if (isSelected) accentColor else GlassBorder, CircleShape)
                                    .clickable {
                                        haptic.buttonClick()
                                        preferencesManager.updateTimerSound(customTone.name)
                                        SoundPlayer.previewSound(context, customTone.name)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = customTone.name,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Presets
                        SoundPlayer.AVAILABLE_SOUNDS.forEach { sound ->
                            val isSelected = settings.timerSound == sound
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, if (isSelected) Color.White.copy(alpha = 0.5f) else GlassBorder, CircleShape)
                                    .clickable {
                                        haptic.buttonClick()
                                        preferencesManager.updateTimerSound(sound)
                                        SoundPlayer.previewSound(context, sound)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = sound,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // APP ICON CUSTOMIZATION SECTION
            // APP APPEARANCE (FEATURE 3: CUSTOM IN-APP LOGO & LAUNCHER ICON)
            SettingsSectionTitle("APP APPEARANCE")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                backgroundColor = GlassSurface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Text(
                        text = "HiTech Clock Logo",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Customize the in-app logo using any photo from your gallery",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Live Active Logo Preview (Circular / Rounded-Square)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF090B12))
                                    .border(2.dp, accentColor, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (customIconBitmap != null) {
                                    Image(
                                        bitmap = customIconBitmap!!.asImageBitmap(),
                                        contentDescription = "Active Custom In-App Logo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(20.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_app_logo),
                                        contentDescription = "Default HiTech Clock Logo",
                                        modifier = Modifier.size(68.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (customIconBitmap != null) "Active Custom Photo Logo" else "Default HiTech Clock Logo",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (customIconBitmap != null) "Displayed everywhere inside the app" else "Standard Liquid Glass emblem",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Customization Options Requested in Specs:
                    // 1. Default HiTech Clock Logo
                    // 2. Choose from Gallery
                    // 3. Remove Custom Logo
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option 1: Default HiTech Clock Logo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (customIconBitmap == null) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                .border(1.dp, if (customIconBitmap == null) accentColor else GlassBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.buttonClick()
                                    iconManager.resetToDefault()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("option_default_hitech_logo")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_app_logo),
                                        contentDescription = "Default Logo",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Default HiTech Clock Logo",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Classic Liquid Glass timepiece",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                if (customIconBitmap == null) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Option 2: Choose from Gallery
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (customIconBitmap != null) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                .border(1.dp, if (customIconBitmap != null) accentColor else GlassBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.buttonClick()
                                    galleryIconPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("option_choose_from_gallery")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accentColor.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = "Gallery",
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Choose from Gallery",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (customIconBitmap != null) "Photo active • Tap to choose another" else "Select any photo to crop and use as logo",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                if (customIconBitmap != null) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Option 3: Remove Custom Logo
                        if (customIconBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.buttonClick()
                                        iconManager.removeCustomLogo()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("option_remove_custom_logo")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Red.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove logo",
                                            tint = AccentRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Remove Custom Logo",
                                            color = AccentRed,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Restore default HiTech Clock branding",
                                            color = TextTertiary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Distinction note & launcher presets
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Home Screen Launcher Icon",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Android adaptive icon presets for your home screen launcher",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Launcher Icon Style Selector Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Preset 1: Liquid Glass (Default)
                        AppIconStyleOption(
                            title = "Liquid Glass",
                            subtitle = "Default",
                            isSelected = currentIconStyle == AppIconManager.ICON_DEFAULT,
                            accentColor = accentColor,
                            onClick = {
                                haptic.buttonClick()
                                iconManager.setPresetIconStyle(AppIconManager.ICON_DEFAULT)
                            }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "Default Icon",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }

                        // Preset 2: Clock (Classic Dark Watchface)
                        AppIconStyleOption(
                            title = "Classic Clock",
                            subtitle = "Watchface",
                            isSelected = currentIconStyle == AppIconManager.ICON_CLOCK,
                            accentColor = accentColor,
                            onClick = {
                                haptic.buttonClick()
                                iconManager.setPresetIconStyle(AppIconManager.ICON_CLOCK)
                            }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_clock_logo),
                                contentDescription = "Clock Icon",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }

                        // Preset 3: Custom Artwork Launcher Alias
                        AppIconStyleOption(
                            title = "Custom Artwork",
                            subtitle = if (customIconBitmap != null) "Gallery active" else "Emblem",
                            isSelected = currentIconStyle == AppIconManager.ICON_CUSTOM,
                            accentColor = accentColor,
                            onClick = {
                                haptic.buttonClick()
                                iconManager.setPresetIconStyle(AppIconManager.ICON_CUSTOM)
                            }
                        ) {
                            if (customIconBitmap != null) {
                                Image(
                                    bitmap = customIconBitmap!!.asImageBitmap(),
                                    contentDescription = "Custom Gallery Icon",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_custom_foreground),
                                    contentDescription = "Custom Artwork Emblem",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            }
                        }

                        // Preset 4: Cyber Neon
                        AppIconStyleOption(
                            title = "Cyber Neon",
                            subtitle = "Purple/Cyan",
                            isSelected = currentIconStyle == AppIconManager.ICON_NEON,
                            accentColor = accentColor,
                            onClick = {
                                haptic.buttonClick()
                                iconManager.setPresetIconStyle(AppIconManager.ICON_NEON)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0F071D))
                                    .border(1.dp, Color(0xFFA78BFA), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_neon_foreground),
                                    contentDescription = "Cyber Neon",
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }

                        // Preset 5: Minimal
                        AppIconStyleOption(
                            title = "Minimalist",
                            subtitle = "Clean Slate",
                            isSelected = currentIconStyle == AppIconManager.ICON_MINIMAL,
                            accentColor = accentColor,
                            onClick = {
                                haptic.buttonClick()
                                iconManager.setPresetIconStyle(AppIconManager.ICON_MINIMAL)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0D1117))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_minimal_foreground),
                                    contentDescription = "Minimalist",
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // About Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                backgroundColor = GlassSurface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Displays the active app icon (custom gallery artwork or default logo)
                    if (currentIconStyle == AppIconManager.ICON_CUSTOM && customIconBitmap != null) {
                        Image(
                            bitmap = customIconBitmap!!.asImageBitmap(),
                            contentDescription = "Active App Icon",
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.5.dp, accentColor, RoundedCornerShape(18.dp))
                        )
                    } else if (currentIconStyle == AppIconManager.ICON_CLOCK) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0A0B0E))
                                .border(1.5.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_clock_logo),
                                contentDescription = "Active Clock Icon",
                                modifier = Modifier.size(62.dp)
                            )
                        }
                    } else if (currentIconStyle == AppIconManager.ICON_NEON) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0F071D))
                                .border(1.5.dp, Color(0xFFA78BFA), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_neon_foreground),
                                contentDescription = "Active Cyber Neon Icon",
                                modifier = Modifier.size(62.dp)
                            )
                        }
                    } else if (currentIconStyle == AppIconManager.ICON_MINIMAL) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0D1117))
                                .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_minimal_foreground),
                                contentDescription = "Active Minimalist Icon",
                                modifier = Modifier.size(62.dp)
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "HiTech Clock Logo",
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "HiTech Clock",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Liquid Glass Precision Timepiece • v1.0",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // "CHANGE LOGO FROM GALLERY" BUTTON AT THE END OF SETTINGS
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.22f),
                                        accentColor.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .border(1.5.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            .clickable {
                                haptic.buttonClick()
                                galleryIconPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                            .padding(vertical = 14.dp, horizontal = 18.dp)
                            .testTag("change_logo_at_end_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Gallery logo",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Change Logo from Gallery",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (customIconBitmap != null) "Custom gallery logo active • Tap to replace" else "Choose any custom picture or logo",
                                    color = accentColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Reset button if custom logo is active
                    if (customIconBitmap != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptic.buttonClick()
                                    iconManager.resetToDefault()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reset Logo to Default",
                                    color = TextTertiary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Color Wheel Bottom Sheet Dialog
        if (showColorWheelDialog) {
            ColorWheelBottomSheet(
                initialColor = accentColor,
                onColorSelected = { selectedColor ->
                    val hex = selectedColor.toHexRgb()
                    preferencesManager.updateAccentColor(hex)
                },
                onDismiss = {
                    showColorWheelDialog = false
                }
            )
        }
    }
}

@Composable
private fun AccentColorSwatch(
    option: AccentTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "swatch_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        Brush.radialGradient(
                            listOf(
                                option.primary.copy(alpha = 0.45f),
                                option.primary.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            // Liquid Glass Orb
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                option.light,
                                option.primary,
                                option.dark
                            )
                        )
                    )
                    .drawBehind {
                        // Top specular liquid reflection sheen
                        drawArc(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = 0.70f),
                                0.5f to Color.White.copy(alpha = 0.20f),
                                1.0f to Color.Transparent
                            ),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(0f, 0f),
                            size = size
                        )
                    }
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = option.displayName,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = TextSecondary.copy(alpha = 0.7f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun AppIconStyleOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else GlassBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(88.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                iconContent()
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .border(1.5.dp, DeepBlack, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = if (isSelected) accentColor else TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * Interactive card representing one of the three color-adaptive interface styles.
 * Features live mini-preview demonstrating the style's surface, border & glow in real-time.
 */
@Composable
private fun InterfaceStyleCard(
    style: InterfaceStyle,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val currentAccent = LocalAccentColor.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.18f),
                            accentColor.copy(alpha = 0.08f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                }
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(12.dp)
            .testTag("interface_style_${style.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live mini-preview swatch demonstrating the visual language of this theme
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        when (style) {
                            InterfaceStyle.PURE_LIQUID_GLASS -> Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.28f),
                                            accentColor.copy(alpha = 0.22f),
                                            Color(0xFF0F1118)
                                        )
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.85f),
                                            currentAccent.light.copy(alpha = 0.65f),
                                            accentColor.copy(alpha = 0.40f)
                                        )
                                    ),
                                    RoundedCornerShape(12.dp)
                                )

                            InterfaceStyle.MINIMAL -> Modifier
                                .background(Color(0xFF0C0E14))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.16f),
                                    RoundedCornerShape(12.dp)
                                )

                            InterfaceStyle.MIX_LIQUID -> Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF22242E),
                                            Color(0xFF13141B)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.40f),
                                            Color.White.copy(alpha = 0.10f)
                                        )
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Mini accent detail inside preview
                when (style) {
                    InterfaceStyle.PURE_LIQUID_GLASS -> {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            accentColor.copy(alpha = 0.75f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        )
                    }
                    InterfaceStyle.MINIMAL -> {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                    InterfaceStyle.MIX_LIQUID -> {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            accentColor.copy(alpha = 0.65f),
                                            currentAccent.dark.copy(alpha = 0.50f)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details (Title, Tag badge, Description)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = style.title,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = style.tag,
                            color = if (isSelected) currentAccent.light else TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = style.description,
                    color = if (isSelected) TextSecondary else TextTertiary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Selection Radio / Check Indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) accentColor else Color.Transparent
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = DeepBlack,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive card for selecting background presets or custom photo.
 */
@Composable
private fun BackgroundOptionCard(
    preset: BackgroundPreset,
    isSelected: Boolean,
    accentColor: Color,
    customPhotoPath: String?,
    onSelect: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    val currentAccent = LocalAccentColor.current
    val hasCustomPhoto = preset == BackgroundPreset.CUSTOM_PHOTO &&
            !customPhotoPath.isNullOrEmpty() && File(customPhotoPath).exists()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.18f),
                            accentColor.copy(alpha = 0.08f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                }
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
            .padding(12.dp)
            .testTag("bg_option_${preset.id.lowercase().replace(' ', '_')}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Thumbnail Preview
                BackgroundThumbnailPreview(
                    preset = preset,
                    accentColor = accentColor,
                    customPhotoPath = customPhotoPath
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preset.title,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )

                        if (preset == BackgroundPreset.BLACK) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) accentColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DEFAULT",
                                    color = if (isSelected) currentAccent.light else TextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (hasCustomPhoto) "Custom photo active" else preset.subtitle,
                        color = if (isSelected) TextSecondary else TextTertiary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Selection Radio / Check Indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) accentColor else Color.Transparent
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = DeepBlack,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Custom Photo Action Buttons (Shown when Custom Photo option is selected)
            if (preset == BackgroundPreset.CUSTOM_PHOTO && isSelected) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pick / Change Photo button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.20f))
                            .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                            .clickable { onPickPhoto() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Choose Photo",
                            tint = currentAccent.light,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasCustomPhoto) "Change Photo" else "Choose from Gallery",
                            color = currentAccent.light,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Remove Photo button (if photo is set)
                    if (hasCustomPhoto) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { onRemovePhoto() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Photo",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remove",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Miniature visual thumbnail preview of background presets and custom photo.
 */
@Composable
private fun BackgroundThumbnailPreview(
    preset: BackgroundPreset,
    accentColor: Color,
    customPhotoPath: String?
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (preset) {
            BackgroundPreset.BLACK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

            BackgroundPreset.COSMOS -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF030408))
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF8B5CF6).copy(alpha = 0.7f), Color.Transparent),
                            center = Offset(size.width * 0.7f, size.height * 0.3f),
                            radius = size.width * 0.6f
                        )
                    )
                    drawCircle(color = Color.White, radius = 1.2f, center = Offset(size.width * 0.3f, size.height * 0.3f))
                    drawCircle(color = Color.White, radius = 1.5f, center = Offset(size.width * 0.7f, size.height * 0.6f))
                    drawCircle(color = Color.White, radius = 1.0f, center = Offset(size.width * 0.4f, size.height * 0.75f))
                }
            }

            BackgroundPreset.CYBER_GRID -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF04060C))
                ) {
                    val horizon = size.height * 0.5f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accentColor.copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(size.width * 0.5f, horizon),
                            radius = size.width * 0.5f
                        )
                    )
                    drawLine(accentColor.copy(alpha = 0.5f), Offset(0f, horizon), Offset(size.width, horizon), 1f)
                    drawLine(accentColor.copy(alpha = 0.3f), Offset(0f, size.height * 0.75f), Offset(size.width, size.height * 0.75f), 1f)
                    drawLine(accentColor.copy(alpha = 0.3f), Offset(size.width * 0.5f, horizon), Offset(size.width * 0.2f, size.height), 1f)
                    drawLine(accentColor.copy(alpha = 0.3f), Offset(size.width * 0.5f, horizon), Offset(size.width * 0.8f, size.height), 1f)
                }
            }

            BackgroundPreset.AURORA_WAVE -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF04060E))
                ) {
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(accentColor.copy(alpha = 0.5f), Color(0xFF6366F1).copy(alpha = 0.4f), Color.Transparent),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        )
                    )
                }
            }

            BackgroundPreset.MIDNIGHT_DEEP -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF020409))
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF1E3A8A).copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.width * 0.5f
                        )
                    )
                }
            }

            BackgroundPreset.MINIMAL_SLATE -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F1117))
                ) {
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF1F2430), Color(0xFF0F1117)),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        )
                    )
                }
            }

            BackgroundPreset.CUSTOM_PHOTO -> {
                val file = if (!customPhotoPath.isNullOrEmpty()) File(customPhotoPath) else null
                if (file != null && file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = "Custom photo thumbnail",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Custom Photo",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


