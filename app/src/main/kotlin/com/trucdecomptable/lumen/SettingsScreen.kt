package com.trucdecomptable.lumen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.launch

private val BG = Color(0xFF0B0E1A)
private val CARD = Color(0xFF151A2E)
private val TEXT = Color(0xFFE8ECF5)
private val ACCENT = Color(0xFF4FC3F7)

/** Écran de paramètres (roue ⚙). */
@Composable
fun SettingsScreen(
    teinteInitiale: Int,
    secondesInitiales: Boolean,
    autoUpdate: Boolean,
    saveTeinte: (Int) -> Unit,
    saveSecondes: (Boolean) -> Unit,
    saveAutoUpdate: (Boolean) -> Unit,
    check: suspend () -> UpdateChecker.UpdateInfo?,
    openLink: (String) -> Unit,
    onBack: () -> Unit
) {
    var teinte by remember { mutableStateOf(teinteInitiale) }
    var secondes by remember { mutableStateOf(secondesInitiales) }
    var autoUpdateEnabled by remember { mutableStateOf(autoUpdate) }
    var update by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (autoUpdate) update = try { check() } catch (e: Exception) { null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // En-tête + retour
        Row(verticalAlignment = Alignment.CenterVertically) {
            DotButton(
                label = "←",
                onClick = onBack
            )
            Spacer(Modifier.width(20.dp))
            BasicText(
                text = "Paramètres",
                style = TextStyle(color = TEXT, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            )
        }

        // Version
        CardBlock {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Version")
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "Lumen v${BuildConfig.VERSION_NAME}",
                        style = TextStyle(color = TEXT, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )
                    if (update?.isNewer == true) {
                        update?.let { u ->
                            UpdateBadge(u.tag) { openLink(u.apkUrl) }
                        }
                    } else {
                        BasicText(
                            text = "à jour",
                            style = TextStyle(color = Color(0xFF4ECB71), fontSize = 16.sp)
                        )
                    }
                }
            }
        }

        // Couleur
        CardBlock {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Caption("Couleur de l'horloge")
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    TEINTES.forEachIndexed { index, c ->
                        TeinteDot(
                            color = c,
                            active = index == teinte,
                            onClick = { teinte = index; saveTeinte(index) }
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
            active = secondes,
            onToggle = { secondes = !secondes; saveSecondes(secondes) }
        )

        // Auto-update
        ToggleRow(
            label = "Mise à jour automatique",
            hint = "Vérifie GitHub Releases au démarrage",
            active = autoUpdateEnabled,
            onToggle = {
                autoUpdateEnabled = !autoUpdateEnabled
                saveAutoUpdate(autoUpdateEnabled)
                if (!autoUpdateEnabled) {
                    update = null
                } else {
                    scope.launch { update = try { check() } catch (e: Exception) { null } }
                }
            }
        )

        Spacer(Modifier.weight(1f))
        BasicText(
            text = "Lumen — horloge locale, sans cloud.",
            style = TextStyle(color = Color(0xFF4A5375), fontSize = 12.sp)
        )
    }
}

@Composable
private fun Caption(text: String) {
    BasicText(text = text, style = TextStyle(color = Color(0xFF8A93B0), fontSize = 14.sp))
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
private fun UpdateBadge(tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF2A2308), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .pointerInput(tag) { detectTapGestures(onTap = { _ -> onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = "Mise à jour $tag · toucher pour télécharger",
            style = TextStyle(color = Color(0xFFFFD54F), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    hint: String,
    active: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CARD, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E2540), RoundedCornerShape(16.dp))
            .padding(start = 22.dp, end = 18.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            BasicText(
                text = label,
                style = TextStyle(color = TEXT, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            )
            BasicText(
                text = hint,
                style = TextStyle(color = Color(0xFF6B7497), fontSize = 13.sp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 30.dp)
                .background(if (active) ACCENT else Color(0xFF2A3050), RoundedCornerShape(15.dp))
                .pointerInput(active) { detectTapGestures(onTap = { _ -> onToggle() }) },
            contentAlignment = if (active) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(modifier = Modifier.size(24.dp).background(Color.White, CircleShape)) {}
        }
    }
}

@Composable
private fun DotButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(CARD, CircleShape)
            .border(2.dp, Color(0xFF2A3050), CircleShape)
            .pointerInput(label) { detectTapGestures(onTap = { _ -> onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = ACCENT, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        )
    }
}
