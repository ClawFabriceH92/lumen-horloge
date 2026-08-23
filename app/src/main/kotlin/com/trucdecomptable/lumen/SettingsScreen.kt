package com.trucdecomptable.lumen

import android.content.SharedPreferences
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BG = Color(0xFF0B0E1A)
private val CARD = Color(0xFF151A2E)
private val TEXT = Color(0xFFE8ECF5)

/** Écran Paramètres. `prefs` est la source de vérité unique. */
@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    openLink: (String) -> Unit,
    onBack: () -> Unit,
    onOrientation: (Int) -> Unit
) {
    // Lecture au moment de l'ouverture — source de vérité = prefs
    var teinte by remember { mutableStateOf(prefs.getInt("teinte", 0).coerceIn(0, TEINTES.size - 1)) }
    var secondes by remember { mutableStateOf(prefs.getBoolean("secondes", false)) }
    var autoUpdate by remember { mutableStateOf(prefs.getBoolean("auto_update", true)) }
    var meteo by remember { mutableStateOf(prefs.getBoolean("meteo", false)) }
    var orientation by remember { mutableStateOf(prefs.getInt("orientation", 0)) }
    var lum by remember { mutableStateOf(prefs.getInt("luminosite", 100).toFloat()) }
    var taille by remember { mutableStateOf(prefs.getInt("taille_chiffres", 100).toFloat()) }
    var update by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

    LaunchedEffect(autoUpdate) {
        update = if (autoUpdate) {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { UpdateChecker.latest() }
            } catch (_: Exception) { null }
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(BG)
            .padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // En-tête
        Row(verticalAlignment = Alignment.CenterVertically) {
            DotButton("←", onBack)
            Spacer(Modifier.width(20.dp))
            BasicText("Paramètres", style = TextStyle(color = TEXT, fontSize = 34.sp, fontWeight = FontWeight.Bold))
        }

        // Version + statut mise à jour
        CardBlock {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Version")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "Lumen v${BuildConfig.VERSION_NAME}",
                        style = TextStyle(color = TEXT, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(10.dp))
                    when {
                        update == null ->
                            BasicText("à jour", style = TextStyle(color = Color(0xFF4ECB71), fontSize = 16.sp))
                        update?.isNewer == true ->
                            DotButton("Mise à jour ${update!!.tag}") { openLink(update!!.apkUrl) }
                        else ->
                            BasicText("à jour", style = TextStyle(color = Color(0xFF4ECB71), fontSize = 16.sp))
                    }
                }
            }
        }

        // Couleur
        CardBlock {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Caption("Couleur de l'horloge")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    TEINTES.forEachIndexed { index, c ->
                        TeinteDot(
                            color = c,
                            active = index == teinte,
                            onClick = {
                                teinte = index
                                prefs.edit().putInt("teinte", index).apply()
                            }
                        )
                        if (index < TEINTES.lastIndex) Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }

        // Secondes
        ToggleRow(
            label = "Afficher les secondes",
            hint = "HH:MM:SS au lieu de HH:MM",
            active = secondes
        ) { on ->
            secondes = on
            prefs.edit().putBoolean("secondes", on).apply()
        }

        // Auto-update
        ToggleRow(
            label = "Mise à jour automatique",
            hint = "Vérifie GitHub Releases au démarrage",
            active = autoUpdate
        ) { on ->
            autoUpdate = on
            prefs.edit().putBoolean("auto_update", on).apply()
        }

        // Météo
        ToggleRow(
            label = "Afficher la météo",
            hint = "Température + condition sous l'heure (Open-Meteo)",
            active = meteo
        ) { on ->
            meteo = on
            prefs.edit().putBoolean("meteo", on).apply()
        }

        // Orientation
        CardBlock {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Caption("Orientation de l'écran")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrientationChoice(
                        label = "Libre",
                        icon = "⬌",
                        active = orientation == 0
                    ) {
                        orientation = 0
                        prefs.edit().putInt("orientation", 0).apply()
                        onOrientation(0)
                    }
                    OrientationChoice(
                        label = "Portrait",
                        icon = "▯",
                        active = orientation == 1
                    ) {
                        orientation = 1
                        prefs.edit().putInt("orientation", 1).apply()
                        onOrientation(1)
                    }
                    OrientationChoice(
                        label = "Paysage",
                        icon = "▭",
                        active = orientation == 2
                    ) {
                        orientation = 2
                        prefs.edit().putInt("orientation", 2).apply()
                        onOrientation(2)
                    }
                }
            }
        }

        // Luminosité des chiffres
        CardBlock {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Caption("Luminosité des chiffres")
                SliderBar(
                    value = lum,
                    start = 20f, end = 100f,
                    onChange = { v ->
                        lum = v
                        prefs.edit().putInt("luminosite", v.toInt()).apply()
                    }
                )
            }
        }

        // Taille des chiffres
        CardBlock {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Caption("Taille des chiffres")
                SliderBar(
                    value = taille,
                    start = 80f, end = 180f,
                    onChange = { v ->
                        taille = v
                        prefs.edit().putInt("taille_chiffres", v.toInt()).apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun SliderBar(value: Float, start: Float, end: Float, onChange: (Float) -> Unit) {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    // Largeur disponible : écran - padding carte (20+20) - padding colonne (36+36)
    val cardW = (config.screenWidthDp - 72).coerceAtLeast(60).toFloat()
    val frac = ((value - start) / (end - start)).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(end) {
                detectTapGestures(onTap = { off ->
                    onChange(start + (off.x / cardW).coerceIn(0f, 1f) * (end - start))
                })
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color(0xFF2A3050), RoundedCornerShape(3.dp)).align(Alignment.Center))
        Box(modifier = Modifier.width((cardW * frac).dp).height(6.dp).background(Color(0xFF4FC3F7), RoundedCornerShape(3.dp)).align(Alignment.CenterStart))
        Box(modifier = Modifier.offset(x = (cardW * frac - 11f).dp).size(22.dp).background(Color.White, CircleShape).align(Alignment.Center))
    }
}

@Composable
private fun CardBlock(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CARD, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E2540), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) { content() }
}

@Composable
private fun Caption(text: String) {
    BasicText(
        text = text,
        style = TextStyle(color = Color(0xFF8A93B0), fontSize = 14.sp, letterSpacing = 1.5.sp)
    )
}

@Composable
private fun DotButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(Color(0xFF1A2035), CircleShape)
            .border(1.dp, Color(0xFF2A3050), CircleShape)
            .pointerInput(label) { detectTapGestures(onTap = { _ -> onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        BasicText(label, style = TextStyle(color = TEXT, fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun RowScope.OrientationChoice(label: String, icon: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(if (active) Color(0xFF4FC3F7) else Color(0xFF1A2035), RoundedCornerShape(12.dp))
            .border(1.dp, if (active) Color(0xFF4FC3F7) else Color(0xFF2A3050), RoundedCornerShape(12.dp))
            .padding(14.dp)
            .pointerInput(label) { detectTapGestures(onTap = { _ -> onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(icon, style = TextStyle(color = if (active) Color.Black else Color(0xFF8A93B0), fontSize = 20.sp))
            BasicText(label, style = TextStyle(color = if (active) Color.Black else Color(0xFFE8ECF5), fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun ToggleRow(label: String, hint: String, active: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CARD, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E2540), RoundedCornerShape(16.dp))
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(label, style = TextStyle(color = TEXT, fontSize = 20.sp, fontWeight = FontWeight.SemiBold))
            BasicText(hint, style = TextStyle(color = Color(0xFF8A93B0), fontSize = 14.sp))
        }
        // Interrupteur
        Box(
            modifier = Modifier
                .size(56.dp, 30.dp)
                .background(if (active) Color(0xFF4FC3F7) else Color(0xFF2A3050), RoundedCornerShape(15.dp))
                .pointerInput(active) { detectTapGestures(onTap = { _ -> onToggle(!active) }) },
            contentAlignment = if (active) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Spacer(Modifier.width(4.dp))
            Box(modifier = Modifier.size(22.dp).background(Color.White, CircleShape))
        }
    }
}
