package com.trucdecomptable.lumen

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

// ---------------------------------------------------------------------------
// Palette
// ---------------------------------------------------------------------------
val TEINTES = listOf(
    Color(0xFFFFFFFF),    // blanc
    Color(0xFF4FC3F7),    // bleu
    Color(0xFF81C784),    // vert
    Color(0xFFE57373),    // rouge
    Color(0xFFFFD54F),    // jaune
    Color(0xFFBA68C8),    // violet
)

private const val PREFS = "lumen_prefs"
private const val KEY_TEINTE = "teinte"
private const val KEY_SECONDES = "secondes"
private const val KEY_AUTOUPDATE = "auto_update"
private const val KEY_ORIENTATION = "orientation"  // 0=Libre, 1=Portrait, 2=Paysage
private const val KEY_METEO = "meteo"  // afficher la météo sous l'heure

private fun alphaPourHeure(heure: Int, minute: Int): Float {
    val h = heure + minute / 60f
    val sin: Float = if (h in 2f..22f) {
        (Math.sin(Math.PI * (h - 2f) / 20f) * 0.5f + 0.5f).toFloat()
    } else 0f
    return 0.15f + 0.85f * sin.coerceIn(0f, 1f)
}

// ---------------------------------------------------------------------------
// Activity
// ---------------------------------------------------------------------------
class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences
    private var openUri: (String) -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        openUri = { url ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
        // Applique l'orientation persistée dès le démarrage
        applyOrientation(prefs.getInt(KEY_ORIENTATION, 0))
        val act = this
        setContent {
            LumenScreen(prefs = prefs, activity = act) { openUri(it) }
        }
    }

    /** Force l'orientation de l'activité selon le mode 0=Libre 1=Portrait 2=Paysage. */
    fun applyOrientation(mode: Int) {
        requestedOrientation = when (mode) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            2 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

// ---------------------------------------------------------------------------
// Racine — gère navigation + plein écran
// ---------------------------------------------------------------------------
@Composable
fun LumenScreen(
    prefs: SharedPreferences,
    activity: MainActivity,
    openLink: (String) -> Unit
) {
    val window = activity.window
    val view = LocalView.current
    var screen by remember { mutableStateOf(0) }

    // Plein écran (défaut : activé)
    var pleinEcran by remember { mutableStateOf(true) }

    // Applique / annule les barres système quand l'état change
    LaunchedEffect(pleinEcran) {
        val c = WindowCompat.getInsetsController(window, view)
        if (pleinEcran) {
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            c.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Orientation persistée (0=Libre, 1=Portrait, 2=Paysage)
    val orientationMode = prefs.getInt(KEY_ORIENTATION, 0)
    LaunchedEffect(orientationMode) {
        val mode = when (orientationMode) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            2 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        activity.requestedOrientation = mode
    }

    if (screen == 1) {
        SettingsScreen(
            prefs = prefs,
            openLink = openLink,
            onBack = { screen = 0 },
            onOrientation = { mode -> activity.applyOrientation(mode) }
        )
    } else {
        HorlogeScreen(
            prefs = prefs,
            pleinEcran = pleinEcran,
            setPleinEcran = { pleinEcran = it },
            openLink = openLink,
            onSettings = { screen = 1 }
        )
    }
}

// ---------------------------------------------------------------------------
// Écran d'horloge
// ---------------------------------------------------------------------------
@Composable
fun HorlogeScreen(
    prefs: SharedPreferences,
    pleinEcran: Boolean,
    setPleinEcran: (Boolean) -> Unit,
    openLink: (String) -> Unit,
    onSettings: () -> Unit
) {
    var teinte by remember { mutableStateOf(prefs.getInt(KEY_TEINTE, 0).coerceIn(0, TEINTES.size - 1)) }
    var secondes by remember { mutableStateOf(prefs.getBoolean(KEY_SECONDES, false)) }
    var autoUpdate by remember { mutableStateOf(prefs.getBoolean(KEY_AUTOUPDATE, true)) }
    var meteo by remember { mutableStateOf(prefs.getBoolean(KEY_METEO, false)) }
    var meteoData by remember { mutableStateOf<MeteoChecker.Meso?>(null) }
    var pickerVisible by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalTime.now()) }
    var hintVisible by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    val scope = rememberCoroutineScope()
    var hidePickerJob by remember { mutableStateOf<Job?>(null) }
    var hintJob by remember { mutableStateOf<Job?>(null) }

    // Horloge temps réel
    LaunchedEffect(Unit) {
        while (true) { now = LocalTime.now(); delay(1000L) }
    }

    // Vérification mise à jour
    LaunchedEffect(autoUpdate) {
        if (autoUpdate) {
            update = try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { UpdateChecker.latest() }
            } catch (_: Exception) { null }
        } else {
            update = null
        }
    }

    // Météo (Open-Meteo, IP géo, sans clé) — refresh toutes les 15 min
    LaunchedEffect(meteo) {
        if (meteo) {
            while (true) {
                meteoData = try { MeteoChecker.latest() } catch (_: Exception) { null }
                kotlinx.coroutines.delay(15 * 60 * 1000L)
            }
        } else {
            meteoData = null
        }
    }

    // Hint "Tap = plein écran" quand les bars re-déparent
    LaunchedEffect(pleinEcran) {
        if (!pleinEcran) {
            hintVisible = true
            hintJob?.cancel()
            hintJob = scope.launch { delay(2500L); hintVisible = false }
        } else {
            hintVisible = false
            hintJob?.cancel()
        }
    }

    val config = LocalConfiguration.current
    val portrait = config.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val screenHeightDp = config.screenHeightDp
    val fontSizeSp: Int = if (portrait) {
        (screenHeightDp * (if (secondes) 0.16f else 0.20f)).toInt()
    } else {
        (screenHeightDp * (if (secondes) 0.42f else 0.55f)).toInt()
    }
    val heureColor = TEINTES[teinte].copy(alpha = alphaPourHeure(now.hour, now.minute))
    val label = if (secondes)
        String.format("%02d:%02d:%02d", now.hour, now.minute, now.second)
    else
        String.format("%02d:%02d", now.hour, now.minute)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(pleinEcran, teinte, secondes) {
                detectTapGestures(
                    onTap = { _ ->
                        // Rapide : toggle plein écran / barres visibles
                        val nowEcran = pleinEcran
                        setPleinEcran(!nowEcran)
                    },
                    onLongPress = {
                        // Long : ouvrir le picker
                        pickerVisible = true
                        hidePickerJob?.cancel()
                        hidePickerJob = scope.launch { delay(2200L); pickerVisible = false }
                    }
                )
            },
        contentAlignment = if (portrait) Alignment.TopCenter else Alignment.Center
    ) {

        // Chiffres
        Box(
            modifier = if (portrait) Modifier.padding(top = 120.dp) else Modifier
        ) {
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = label,
                    style = TextStyle(
                        color = heureColor,
                        fontSize = fontSizeSp.sp,
                        fontWeight = FontWeight.Thin
                    )
                )
                // Météo sous l'heure
                if (meteo) {
                    BasicText(
                        text = meteoData?.let { m ->
                            "${m.temperature.toInt()}°C  ·  ${m.condition}"
                        } ?: "…",
                        style = TextStyle(
                            color = Color(0x668A93B0),
                            fontSize = if (portrait) 13.sp else 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Roue ⚙ — visible uniquement quand les contrôles sont actifs (barres affichées)
        if (!pleinEcran) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp)
                    .size(56.dp)
                    .background(Color(0x33151A2E), CircleShape)
                    .border(1.dp, Color(0x662A3050), CircleShape)
                    .pointerInput("gear") {
                        detectTapGestures(onTap = { _ -> onSettings() })
                    },
                contentAlignment = Alignment.Center
            ) {
                BasicText("⚙", style = TextStyle(color = Color(0xCC8A93B0), fontSize = 28.sp))
            }

            // Badge mise à jour — visible avec la roue
            update?.let { u ->
                if (u.isNewer) {
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
                            text = "Mise à jour ${u.tag}",
                            style = TextStyle(
                                color = Color(0xFFFFD54F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // Hint "Tap = plein écran" — bas centre, visible seulement quand bars sont actives
        if (!pleinEcran && hintVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "Tap = plein écran",
                    style = TextStyle(color = Color(0x888A93B0), fontSize = 13.sp)
                )
            }
        }

        // Picker couleurs — bas centre, déclenché au long-press
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
                            prefs.edit().putInt(KEY_TEINTE, index).apply()
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
                        prefs.edit().putBoolean(KEY_SECONDES, secondes).apply()
                        pickerVisible = false
                    }
                )
                Spacer(Modifier.width(24.dp))
                ToggleDot(
                    label = "↻",
                    active = autoUpdate,
                    onClick = {
                        autoUpdate = !autoUpdate
                        prefs.edit().putBoolean(KEY_AUTOUPDATE, autoUpdate).apply()
                        pickerVisible = false
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Composants
// ---------------------------------------------------------------------------
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
fun ToggleDot(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(if (active) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), CircleShape)
            .border(2.dp, if (active) Color(0xFF4FC3F7) else Color(0xFF555555), CircleShape)
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
