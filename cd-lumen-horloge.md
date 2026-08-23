# CD Design — Lumen / Horloge numérique

**Version :** 1.0 — MVP "au plus simple"
**Date :** 23/08/2026

---

## 1. Résumé

Application Android minimaliste : une horloge numérique plein écran en mode horizontal. La couleur du texte s'éclaircit progressivement au fil de la journée (sombre à l'aube → brillante au midi → sombre au soir). Fabrice peut choisir la teinte de base (blanche, bleue, verte, rouge, jaune, violette) via un sélecteur discret.

Aucune API externe, aucun réseau, aucun GPS. Fonctionne entièrement hors-ligne.

---

## 2. Fonctionnalités

### P0 — MVP (livrable 1)

| ID | Fonctionnalité | Détail |
|----|----------------|--------|
| F01 | Affichage de l'heure en temps réel | HH:MM en grand format, secondes optionnelles |
| F02 | Plein écran + orientation verrouillée | `screenOrientation = SCREEN_ORIENTATION_LANDSCAPE`, barre de statut cachée |
| F03 | Écran ne s'éteint pas | `FLAG_KEEP_SCREEN_ON` tant que l'app est à l'avant |
| F04 | Couleur qui évolue avec l'heure | Luminosité du texte : ~15 % (2 h du matin) → 100 % (midi) → ~15 % (23 h). Fond noir permanent |
| F05 | Sélecteur de teinte | Tap long sur l'écran → barre de teintes apparaît 2 s → disparaît. 6 teintes : blanc, bleu, vert, rouge, jaune, violet |
| F06 | Affichage optionnel des secondes | Tap court / tap long pour activer-désactiver les secondes (HH:MM ↔ HH:MM:SS) |

### P1 — v1.1 (si demandé)

| ID | Fonctionnalité | Détail |
|----|----------------|--------|
| F07 | Date affichée en petit | Sous l'heure, format `JDD MMM` (ex : 23 Aoû) |
| F08 | Teinte libre (color picker) | Remplace le sélecteur 6 teintes par un cercle HSL |
| F09 | Widget bureau | Widget 4×2 affichant l'heure + teinte |

### P2 — v2.0 (optionnel)

| ID | Fonctionnalité |
|----|----------------|
| F10 | Alerte de lever / coucher de soleil (notification silencieuse) |
| F11 | Modes "Bureau", "Tablette", "Mural" (taille de police) |

---

## 3. Spécifications techniques

### 3.1 Stack

- **Langage :** Kotlin 2.0
- **UI :** Jetpack Compose (BOM 2024.06)
- **Min SDK :** 26 (Android 8.0) — couvre ~95 % des téléphones
- **Target SDK :** 34 (Android 14)
- **Pas de dépendance réseau**, pas de ViewModel, pas de Room

### 3.2 Architecture

Monofichier `MainActivity.kt` (~150 lignes) :
- `setContent` → composant `Horloge`
- État : `teinteActuelle: Int` (0-5), `secondesVisibles: Boolean`
- `LaunchedEffect` + `delay(1000)` pour le tick horaire
- Aucun state manager

### 3.3 Plein écran + orientation

```kotlin
androidx.activity.compose.setContent {
    // Verrouiller l'orientation
    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    // Masquer les barres système
    WindowCompat.getInsetsController(window, view).apply {
        systemBarsBehavior = BEHAVIOR_TRANSIENT
        hide(WindowInsets.Type.systemBars())
    }
    // Écran ne s'éteint pas
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}
```

### 3.4 Calcul de luminosité — la "magie" soleil

La luminosité du text est une fonction sinusoïdale de l'heure locale :

```kotlin
fun luminositePourHeure(heure: Int, minute: Int): Float {
    val h = heure + minute / 60f
    // Min à 2h, max à 12h. Période 20h (2h → 22h = 20h de cycle)
    // Décalage : 2h = 0 → phase 0
    val phase = ((h - 2f) / 20f * 2f * PI).toFloat()
    val sin = (sin(phase) * 0.5f + 0.5f).coerceIn(0.15f, 1f)
    return sin
}
```

- `h = 2h` → luminosité ≈ 0.15 (texte sombre sur fond noir)
- `h = 12h` → luminosité = 1.0 (texte pleine teinte)
- `h = 22h` → luminosité ≈ 0.15 (retour au sombre)
- `h = 0h à 2h` / `22h à 24h` → 0.15 (plafond nuit)

Le texte est donc **toujours visible** (min 15 %) mais l'effet "soleil qui se lève" est perceptible.

### 3.5 Teintes disponibles

| Index | Nom | `Color` |
|-------|-----|---------|
| 0 | Blanc | `Color(0xFFFFFFFF)` |
| 1 | Bleu | `Color(0xFF4FC3F7)` |
| 2 | Vert | `Color(0xFF81C784)` |
| 3 | Rouge | `Color(0xFFE57373)` |
| 4 | Jaune | `Color(0xFFFFD54F)` |
| 5 | Violet | `Color(0xFFBA68C8)` |

La teinte sélectionnée est persistée en `SharedPreferences` (simple, pas de room).

### 3.6 Interactions

| Gesture | Action |
|---------|--------|
| **Tap court** (1 coup) | Toggle secondes (HH:MM ↔ HH:MM:SS) |
| **Tap long** (maintenir 500 ms) | Affiche la barre de 6 teintes pendant 2 s |
| **Tap sur une teinte** | Change la teinte, sauve en prefs, barre disparaît |

### 3.7 Structure du projet

```
lumen-horloge/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── kotlin/fr/fabrice/lumen/
│   │       └── MainActivity.kt        ← toute la logique (~150 lignes)
│   └── ...
├── build.gradle.kts (root)
├── app/build.gradle.kts
└── settings.gradle.kts
```

**Un seul fichier Kotlin.** Zéro dépendance externe hors Compose.

### 3.8 Manifest — clés

```xml
<application
    android:label="Lumen"
    android:theme="@style/Theme.Lumen.NoActionBar"
    android:icon="@mipmap/ic_launcher">
    <activity
        android:name=".MainActivity"
        android:exported="true"
        android:configChanges="orientation|screenSize"
        android:excludeFromRecents="false">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

### 3.9 `android:excludeFromRecents` — à trancher

Si l'app est destinée à rester posée (mode mural / table), Fabrice peut vouloir qu'elle ne figure PAS dans le recents. Par défaut : `false`. Optionnel en P1.

---

## 4. Critères d'acceptation

- [ ] L'ouverture de l'app affiche immédiatement HH:MM en plein écran, orientation verrouillée horizontale
- [ ] Aucune barre de statut, aucune barre de navigation visible
- [ ] L'écran ne s'éteint pas après 10 minutes
- [ ] À 22 h le texte est sombre, à 14 h plus clair, à 2 h du matin presque éteint
- [ ] Tap court : les secondes apparaissent/disparaissent instantanément
- [ ] Tap long : barre de 6 teintes apparaît, tap sur une teinte la change, barre disparaît après 2 s
- [ ] Après fermeture et réouverture, la dernière teinte est conservée
- [ ] L'app tourne sur un téléphone Android 8 minimum, sans internet

---

## 5. Hors périmètre (v1.0)

- Alarme, chronomètre, minuteur
- Compte à rebours
- Météo, lever/coucher de soleil réels (API)
- Multi-écran (deux horloges)
- Widget bureau (P1)
- Thème clair (fond clair — non demandé)

---

## 6. Estimation

| Tâche | Effort |
|-------|--------|
| Scaffolding projet Android + Compose | 15 min |
| `MainActivity.kt` (plein écran, tick horaire, teinte) | 45 min |
| Interactions (tap/tap long, prefs) | 20 min |
| Test manuel sur un téléphone | 15 min |
| **Total** | **~1h30** |
