# Lumen — horloge numérique Android

Horloge numérique plein écran, mode horizontal. La couleur de l'heure
s'éclaircit progressivement au fil de la journée (sombre à l'aube,
intense au midi, sombre au soir). 6 teintes au choix, secondes
optionnelles. Zéro réseau, zéro API, zéro permission — fonctionne
entièrement hors-ligne.

## Fonctionnalités

- **Plein écran verrouillé** en mode paysage, barres système masquées,
  écran qui ne s'éteint pas tant que l'app est active
- **Luminosité « soleil »** : plancher ~15 % à 2 h et 22 h, pic 100 % au
  midi — le texte reste toujours lisible
- **6 teintes** : blanc, bleu, vert, rouge, jaune, violet (mémoïsées en
  `SharedPreferences`)
- **Tap court** : affiche / masque les secondes (HH:MM ↔ HH:MM:SS)
- **Tap long** : barre de teintes 2 s, choix persistant

## Stack

| Élément | Valeur |
|---------|--------|
| Langage | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.06) |
| AGP | 8.5.2 |
| Gradle | 8.9 |
| minSdk | 26 (Android 8.0) |
| targetSdk | 35 (Android 15) |
| Dépendances réseau | **aucune** |
| Dépendances UI | Compose + `androidx.core` + `androidx.activity` |

## Architecture

**Un seul fichier Kotlin** — `app/src/main/kotlin/com/trucdecomptable/lumen/MainActivity.kt` (~200 lignes) :

- `MainActivity` : configuration plein écran (barres système, `FLAG_KEEP_SCREEN_ON`), prefs
- `Horloge` : tick horaire (`LaunchedEffect` + `delay(1000)`), calcul de luminosité, interactions
- `TeinteDot` : pastille de couleur à sélectionner

Pas de ViewModel, pas de Room, pas d'API.

## Build local

```bash
ANDROID_HOME=/opt/android-sdk gradle :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

## Build release

Le release build est signé avec un keystore dédié (pattern identique à
`cuisson-vapeur-legumes`). Sur CI GitHub Actions, fournir les secrets :

- `LUMEN_KEYSTORE_B64` (keystore en base64)
- `LUMEN_KEYSTORE_PASSWORD`
- `LUMEN_KEY_ALIAS`
- `LUMEN_KEY_PASSWORD`

En local, placer le keystore dans `~/.secrets/lumen-release.keystore`
et poser les variables d'env correspondantes.

## Structure

```
lumen-horloge/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── kotlin/com/trucdecomptable/lumen/
│   │   │   └── MainActivity.kt        ← toute la logique
│   │   └── res/
│   │       ├── drawable/              ← icône adaptive (vector)
│   │       ├── mipmap-anydpi-v26/     ← icône launcher
│   │       └── values/
├── build.gradle.kts
├── gradle/
├── gradle.properties
├── settings.gradle.kts
├── cd-lumen-horloge.md                ← cahier des charges v1.0
└── README.md
```
