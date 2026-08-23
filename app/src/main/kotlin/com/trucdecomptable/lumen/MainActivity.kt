package com.trucdecomptable.lumen

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
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
// Teintes disponibles (CD §3.5)
// ---------------------------------------------------------------------------
private val TEINTES = listOf(
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

/**
 * Luminosité « soleil » (CD §3.4) : plancher 0.15, pic 1.0.
 * Min à 2h et 22h (nuit), max à midi.
 * Visuellement : alpha du texte = 0.15 + 0.85 × sin.
 */
private fun alphaPourHeure(heure: Int, minute: Int): Float {
    val h = heure + minute / 60f
    val sin: Float = if (h in 2f..22f) {
        (Math.sin(Math.PI * (h - 2f) / 20f) * 0.5 + 0.5).toFloat()
    } else 0f
    return 0.15f + 0.85f * sin.coerceIn(0f, 1f)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Écran qui ne s'éteint pas tant que l'app est à l'avant (CD F03)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Fond noir immédiat (pas de flash)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
        // Fullscreen : le Compose cache les barres + comportement transient
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val teinteInitiale = prefs.getInt(KEY_TEINTE, 0).coerceIn(0, TEINTES.size - 1)
        val secondesInitiales = prefs.getBoolean(KEY_SECONDES, false)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                // Masquer barres système (plein écran, CD F02)
                val view = LocalView.current
                LaunchedEffect(view) {
                    WindowCompat.getInsetsController(window, view)
                        .hide(WindowInsetsCompat.Type.systemBars())
                }
                Horloge(
                    teinteInitiale = teinteInitiale,
                    secondesInitiales = secondesInitiales,
                    saveTeinte = { prefs.edit().putInt(KEY_TEINTE, it).apply() },
                    saveSecondes = { prefs.edit().putBoolean(KEY_SECONDES, it).apply() }
                )
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
    saveTeinte: (Int) -> Unit,
    saveSecondes: (Boolean) -> Unit
) {
    var teinte by remember { mutableStateOf(teinteInitiale) }
    var secondes by remember { mutableStateOf(secondesInitiales) }
    var pickerVisible by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalTime.now()) }
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    // Tick horaire : 1 mise à jour par seconde (CD F01)
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000L)
        }
    }

    // Taille de police relative à la hauteur d'écran (plein écran paysage)
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
                    onTap = { _ ->                  // tap court : toggle secondes (CD F06)
                        secondes = !secondes
                        saveSecondes(secondes)
                    },
                    onLongPress = {                 // tap long : barre de teintes 2 s (CD F05)
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
                fontWeight = androidx.compose.ui.text.font.FontWeight.Thin
            )
        )

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
            }
        }
    }
}

@Composable
private fun TeinteDot(color: Color, active: Boolean, onClick: () -> Unit) {
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
