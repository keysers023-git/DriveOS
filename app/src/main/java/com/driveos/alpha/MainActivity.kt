package com.driveos.alpha

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val bg = Color(0xFF090F19)
private val card = Color(0xFF151F2E)
private val accent = Color(0xFF4CC9BA)
private val muted = Color(0xFFA2B2C4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (4 or 1024 or 4096)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = accent, surface = card)) {
                DriveScreen()
            }
        }
    }

    private fun launchPackage(pkg: String, label: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) startActivity(intent)
            else Toast.makeText(this, "$label nije instaliran na ovom uređaju", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ne mogu otvoriti $label: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openNavigation() {
        try {
            val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
            geo.setPackage("com.google.android.apps.maps")
            startActivity(geo)
        } catch (e: ActivityNotFoundException) {
            launchPackage("com.google.android.apps.maps", "Google Maps")
        } catch (e: Exception) {
            Toast.makeText(this, "Navigacija nije dostupna", Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    private fun DriveScreen() {
        var diagnostics by remember { mutableStateOf(false) }
        var time by remember { mutableStateOf(currentClock()) }
        LaunchedEffect(Unit) {
            while (true) {
                time = currentClock()
                delay(15_000)
            }
        }
        Column(
            Modifier.fillMaxSize().background(bg).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("DRIVE", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("OS", color = accent, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("  ALPHA 0.1", color = muted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text(time, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 22.sp)
            }
            if (diagnostics) {
                Surface(color = card, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Dijagnostika Junsun uređaja", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        DiagnosticRow("Manufacturer", Build.MANUFACTURER)
                        DiagnosticRow("Brand / Model", "${Build.BRAND} / ${Build.MODEL}")
                        DiagnosticRow("Device / Board", "${Build.DEVICE} / ${Build.BOARD}")
                        DiagnosticRow("Android / API", "${Build.VERSION.RELEASE} / ${Build.VERSION.SDK_INT}")
                        DiagnosticRow("Display", "${resources.displayMetrics.widthPixels} × ${resources.displayMetrics.heightPixels} px")
                        DiagnosticRow("Build", Build.DISPLAY)
                        Text("MCU verzija nije dostupna standardnim Android API-jem; pogledaj About device u postavkama radija.", color = muted, fontSize = 13.sp)
                        Button(onClick = {
                            val report = "DriveOS diagnostics\nManufacturer: ${Build.MANUFACTURER}\nBrand: ${Build.BRAND}\nModel: ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE} API ${Build.VERSION.SDK_INT}\nBuild: ${Build.DISPLAY}\nResolution: ${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}"
                            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                            }, "Podijeli dijagnostiku"))
                        }) { Text("Podijeli dijagnostiku") }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Tile("NAVIGACIJA", "Kamo idemo?", "Otvori Google Maps", Modifier.fillMaxWidth().weight(1f), true) { openNavigation() }
                        Row(Modifier.fillMaxWidth().weight(0.55f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Tile("ALTERNATIVA", "Waze", "Pokreni navigaciju", Modifier.weight(1f).fillMaxHeight()) {
                                launchPackage("com.waze", "Waze")
                            }
                            Tile("KARTE", "Maps", "Otvori aplikaciju", Modifier.weight(1f).fillMaxHeight()) {
                                launchPackage("com.google.android.apps.maps", "Google Maps")
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Tile("GLAZBA", "Spotify", "Otvori player", Modifier.fillMaxWidth().weight(1f)) {
                            launchPackage("com.spotify.music", "Spotify")
                        }
                        Tile("GLAZBA", "YouTube Music", "Otvori player", Modifier.fillMaxWidth().weight(1f)) {
                            launchPackage("com.google.android.apps.youtube.music", "YouTube Music")
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BottomAction(if (diagnostics) "← Dashboard" else "Dijagnostika", Modifier.weight(1f)) { diagnostics = !diagnostics }
                BottomAction("Android postavke", Modifier.weight(1f)) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
                BottomAction("Izlaz", Modifier.weight(1f)) { finish() }
            }
        }
    }
}

private fun currentClock(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

@Composable private fun DiagnosticRow(name: String, value: String) {
    Column {
        Text(name.uppercase(Locale.getDefault()), color = accent, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 15.sp)
    }
}

@Composable private fun Tile(kicker: String, title: String, subtitle: String, modifier: Modifier, highlight: Boolean = false, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), color = if (highlight) Color(0xFF15363B) else card,
        shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, if (highlight) accent.copy(alpha = .42f) else Color.White.copy(alpha = .08f))) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(kicker, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Column {
                Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable private fun BottomAction(title: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(48.dp).clickable(onClick = onClick), color = card, shape = RoundedCornerShape(12.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
