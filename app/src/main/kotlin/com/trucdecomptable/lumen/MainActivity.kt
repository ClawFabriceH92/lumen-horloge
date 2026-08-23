package com.trucdecomptable.lumen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

// ---------------------------------------------------------------------------
// Teintes disponibles
// ---------------------------------------------------------------------------
val TEINTES = listOf(
    Color(0xFFFFFFFF), // blanc
    Color(0xFF4FC3F7), // bleu
    Color(0xFF81C784), // vert
    Color(0xFFE57373), // rouge
    Color(0xFFFFD54F), // jaune
    Color(0xFFBA68C8), // violet
)

private const val PREFS = "lumen_prefs"
private const val KEY_TEINTE = "teinte"
private const val KEY_SECONDES = "secondes"
private const val KEY_AUTOUPDATE = "auto_update"

/**
 * Luminosité « soleil » : plancher 0.15, pic 1.0.
 */
private fun alphaPourHeure(heure: Int, minute: Int): Float {
    val h = heure + minute / 60f
    val sin: Float = if (h in 2f..22f) {
        (Math.sin(Math.PI * (h - 2f) / 20f) * 0.5 + 0.5).toFloat()
    } else 0f
    return 0.15f + 0.85f * sin.coerceIn(0f, 1f)
}

class MainActivity : ComponentActivity() {

    private var openUri: (String) -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val teinteInitiale = prefs.getInt(KEY_TEINTE, 0).coerceIn(0, TEINTES.size - 1)
        val secondesInitiales = prefs.getBoolean(KEY_SECONDES, false)
        val autoUpdate = prefs.getBoolean(KEY_AUTOUPDATE, true)

        openUri = { url ->
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                val view = LocalView.current
                LaunchedEffect(view) {
                    WindowCompat.getInsetsController(window, view)
                        .hide(WindowInsetsCompat.Type.systemBars())
                }

                // Navigation simple : 0 = horloge, 1 = paramètres
                var screen by remember { mutableStateOf(0) }

                if (screen == 1) {
                    SettingsScreen(
                        teinteInitiale = teinteInitiale,
                        secondesInitiales = secondesInitiales,
                        autoUpdate = autoUpdate,
                        saveTeinte = { prefs.edit().putInt(KEY_TEINTE, it).apply() },
                        saveSecondes = { prefs.edit().putBoolean(KEY_SECONDES, it).apply() },
                        saveAutoUpdate = { prefs.edit().putBoolean(KEY_AUTOUPDATE, it).apply() },
                        check = { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { UpdateChecker.latest() } },
                        openLink = { openUri(it) },
                        onBack = { screen = 0 }
                    )
                } else {
                    Horloge(
                        teinteInitiale = teinteInitiale,
                        secondesInitiales = secondesInitiales,
                        autoUpdate = autoUpdate,
                        saveTeinte = { prefs.edit().putInt(KEY_TEINTE, it).apply() },
                        saveSecondes = { prefs.edit().putBoolean(KEY_SECONDES, it).apply() },
                        saveAutoUpdate = { prefs.edit().putBoolean(KEY_AUTOUPDATE, it).apply() },
                        check = { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { UpdateChecker.latest() } },
                        openLink = { openUri(it) },
                        appVersion = BuildConfig.VERSION_NAME,
                        onSettings = { screen = 1 }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------
@Composable
private fun Horloge(
    teinteInitiale: Int,
    secondesInitiales: Boolean,
    autoUpdate: Boolean,
    saveTeinte: (Int) -> Unit,
    saveSecondes: (Boolean) -> Unit,
    saveAutoUpdate: (Boolean) -> Unit,
    check: suspend () -> UpdateChecker.UpdateInfo?,
    openLink: (String) -> Unit,
    appVersion: String,
    onSettings: () -> Unit
) {
    var teinte by remember { mutableStateOf(teinteInitiale) }
    var secondes by remember { mutableStateOf(secondesInitiales) }
    var pickerVisible by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalTime.now()) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    // Mise à jour dispo ?
    var update by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000L)
        }
    }

    LaunchedEffect(Unit) {
        if (autoUpdate) {
            update = try { check() } catch (e: Exception) { null }
        }
    }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val fontSizeSp: Int = (screenHeightDp * (if (secondes) 0.42f else 0.55f)).toInt()
    val heureColor: Color = TEINTES[teinte].copy(alpha = alphaPourHeure(now.hour, now.minute))
    val label = if (secondes)
        String.format("%02d:%02d:%02d", now.hour, now.minute, now.second)
    else
        String.format("%02d:%02d", now.hour, now.minute)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(teinte, secondes) {
                detectTapGestures(
                    onTap = { _ ->
                        secondes = !secondes
                        saveSecondes(secondes)
                    },
                    onLongPress = {
                        pickerVisible = true
                        hideJob?.cancel()
                        hideJob = scope.launch {
                            delay(2000L)
                            pickerVisible = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = heureColor,
                fontSize = fontSizeSp.sp,
                fontWeight = FontWeight.Thin
            )
        )

        // Roue ⚙ en haut à gauche (ouvre les paramètres)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .size(56.dp)
                .background(Color(0x33151A2E), CircleShape)
                .border(1.dp, Color(0x662A3050), CircleShape)
                .pointerInput("gear") {
                    detectTapGestures(onTap = { _ -> showSettings = true; onSettings() })
                },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "⚙",
                style = TextStyle(color = Color(0xCC8A93B0), fontSize = 28.sp)
            )
        }

        // Badge « mise à jour dispo » — coin haut-droit (uniquement si vraiment plus récent)
        if (update?.isNewer == true) {
            update?.let { u ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .background(Color(0xFF2A2308), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .pointerInput(u.apkUrl) {
                        detectTapGestures(onTap = { _ -> openLink(u.apkUrl) })
                    },
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "Mise à jour ${u.tag} · toucher pour télécharger",
                    style = TextStyle(
                        color = Color(0xFFFFD54F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            }
        }

        // Barre de teintes + toggles
        if (pickerVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TEINTES.forEachIndexed { index, c ->
                    if (index > 0) Spacer(Modifier.width(24.dp))
                    TeinteDot(
                        color = c,
                        active = index == teinte,
                        onClick = {
                            teinte = index
                            saveTeinte(index)
                            pickerVisible = false
                        }
                    )
                }
                Spacer(Modifier.width(40.dp))
                ToggleDot(
                    label = "S",
                    active = secondes,
                    onClick = {
                        secondes = !secondes
                        saveSecondes(secondes)
                        pickerVisible = false
                    }
                )
                Spacer(Modifier.width(24.dp))
                ToggleDot(
                    label = "↻",
                    active = autoUpdate,
                    onClick = {
                        saveAutoUpdate(!autoUpdate)
                        pickerVisible = false
                    }
                )
            }
        }
    }
}

@Composable
fun TeinteDot(color: Color, active: Boolean, onClick: () -> Unit) {
    val taille = if (active) 64.dp else 52.dp
    Box(
        modifier = Modifier
            .size(taille)
            .background(color.copy(alpha = if (active) 1f else 0.5f), CircleShape)
            .then(if (active) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
            .pointerInput(color) { detectTapGestures(onTap = { _ -> onClick() }) },
        contentAlignment = Alignment.Center
    ) {}
}

@Composable
private fun ToggleDot(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(if (active) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), CircleShape)
            .border(
                2.dp,
                if (active) Color(0xFF4FC3F7) else Color(0xFF555555),
                CircleShape
            )
            .pointerInput(label) { detectTapGestures(onTap = { _ -> onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = if (active) Color(0xFF4FC3F7) else Color(0xFF888888),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
