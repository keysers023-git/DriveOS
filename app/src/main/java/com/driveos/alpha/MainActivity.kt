package com.driveos.alpha

import android.content.ComponentName
import android.media.session.MediaSessionManager
import android.media.session.MediaController
import android.content.Context
import android.content.Intent
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.config.Configuration
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
private val panel = Color(0xFF152132)
private val accent = Color(0xFF50CEBD)
private val muted = Color(0xFF9BAFC4)
private val white = Color(0xFFF5F8FF)

private data class TargetApp(val title: String, val pkg: String)
private val maps = TargetApp("Google Maps", "com.google.android.apps.maps")
private val waze = TargetApp("Waze", "com.waze")
private val spotify = TargetApp("Spotify", "com.spotify.music")
private val ytMusic = TargetApp("YouTube Music", "com.google.android.apps.youtube.music")

class MainActivity : ComponentActivity() {
    private fun appInstalled(app: TargetApp): Boolean = try {
        packageManager.getPackageInfo(app.pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }

    private fun openApp(app: TargetApp) {
        if (appInstalled(app)) {
            try {
                val launch = packageManager.getLaunchIntentForPackage(app.pkg)
                if (launch != null) startActivity(launch)
                else Toast.makeText(this, "Nije moguće pokrenuti ${app.title}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Greška pri otvaranju: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.pkg}")))
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${app.pkg}")))
            } catch (e: Exception) {
                Toast.makeText(this, "Trgovina aplikacija nije dostupna", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun report(): String = """
        DriveOS Alpha 0.2
        Manufacturer: ${Build.MANUFACTURER}
        Brand: ${Build.BRAND}
        Model: ${Build.MODEL}
        Device: ${Build.DEVICE}
        Board: ${Build.BOARD}
        Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
        Build: ${Build.DISPLAY}
        Display: ${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}
        Apps: ${listOf(maps,waze,spotify,ytMusic).joinToString { it.title + "=" + appInstalled(it) }}
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 4 or 1024 or 4096
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = accent, surface = panel)) {
                Screen()
            }
        }
    }

    @Composable
    private fun Screen() {
        var demo by remember { mutableStateOf(true) }
        var page by remember { mutableStateOf("dashboard") }
        var playing by remember { mutableStateOf(false) }
        var song by remember { mutableIntStateOf(0) }
        var clock by remember { mutableStateOf(clockNow()) }
        var activeMedia by remember { mutableStateOf<MediaController?>(null) }
        var mediaTitle by remember { mutableStateOf("Nema aktivne medijske sesije") }
        var mediaArtist by remember { mutableStateOf("Pokreni Spotify ili drugu glazbenu aplikaciju") }
        LaunchedEffect(demo) { while (!demo) { 
            try { val manager=getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                activeMedia=manager.getActiveSessions(ComponentName(this@MainActivity, DriveNotificationListener::class.java)).firstOrNull()
                mediaTitle=activeMedia?.metadata?.getString("android.media.metadata.TITLE") ?: "Nema aktivne reprodukcije"
                mediaArtist=activeMedia?.metadata?.getString("android.media.metadata.ARTIST") ?: "Omogući pristup obavijestima za DriveOS"
            } catch (_: SecurityException) { activeMedia=null;mediaTitle="Potrebna dozvola";mediaArtist="Otvori pristup obavijestima u postavkama" }
            delay(2000) 
        } }
        var seconds by remember { mutableIntStateOf(0) }
        val tracks = listOf("Night Drive" to "DriveOS Demo", "Coastal Highway" to "DriveOS Demo", "After Dark" to "DriveOS Demo")
        LaunchedEffect(Unit) { while(true) { clock=clockNow(); delay(15000) } }
        LaunchedEffect(demo, playing) { while(demo && playing) { delay(1000); seconds++ } }
        BackHandler(page != "dashboard") { page = "dashboard" }

        Column(Modifier.fillMaxSize().background(bg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("DRIVE", color=white, fontSize=23.sp, fontWeight=FontWeight.Black)
                Text("OS", color=accent, fontSize=23.sp, fontWeight=FontWeight.Black)
                Text("   ALPHA 0.3", color=muted, fontSize=11.sp)
                Spacer(Modifier.weight(1f))
                Text(if(demo) "● DEMO MODE" else "● LIVE MODE", color=if(demo) accent else muted, fontSize=12.sp, modifier=Modifier.clickable { demo=!demo; playing=false })
                Spacer(Modifier.width(20.dp))
                Text(clock, color=white, fontSize=21.sp, fontWeight=FontWeight.SemiBold)
            }
            when(page) {
                "diagnostics" -> Panel(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("DIJAGNOSTIKA", color=accent, fontSize=14.sp, fontWeight=FontWeight.Bold)
                        Text(report(), color=white, fontSize=14.sp)
                        FilledButton("Podijeli izvještaj") {
                            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type="text/plain"; putExtra(Intent.EXTRA_TEXT,report())
                            }, "Pošalji dijagnostiku"))
                        }
                    }
                }
                "apps" -> Panel(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("APLIKACIJE", color=accent, fontWeight=FontWeight.Bold)
                        Text("Instalirane aplikacije imaju tipku Otvori; ostale nude instalaciju.", color=muted, fontSize=13.sp)
                        listOf(maps,waze,spotify,ytMusic).forEach { app ->
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(app.title, color=white, fontSize=17.sp)
                                    Text(if(appInstalled(app)) "Instalirana" else "Nije instalirana", color=muted, fontSize=12.sp)
                                }
                                FilledButton(if(appInstalled(app)) "Otvori" else "Instaliraj") { openApp(app) }
                            }
                        }
                    }
                }
                else -> Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Panel(Modifier.weight(1.5f).fillMaxHeight()) {
                        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement=Arrangement.SpaceBetween) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                                Text("NAVIGACIJA", color=accent, fontWeight=FontWeight.Bold, fontSize=13.sp)
                                Text(if(demo) "SIMULACIJA" else "SPREMNO", color=muted, fontSize=12.sp)
                            }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                DriveMap(Modifier.fillMaxSize(), demo)
                                if (demo) {
                                    Surface(Modifier.align(Alignment.BottomStart).padding(12.dp),color=Color(0xE5152132),shape=RoundedCornerShape(12.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text("DEMO RUTA · RIJEKA",color=accent,fontSize=12.sp,fontWeight=FontWeight.Bold)
                                            Text("Za 450 m skrenite desno",color=white,fontSize=19.sp,fontWeight=FontWeight.Bold)
                                            Text("Primjer upute · nije aktivna navigacija",color=muted,fontSize=11.sp)
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                FilledButton(if(appInstalled(maps)) "Otvori Maps" else "Instaliraj Maps", Modifier.weight(1f)) { openApp(maps) }
                                FilledButton(if(appInstalled(waze)) "Otvori Waze" else "Instaliraj Waze", Modifier.weight(1f)) { openApp(waze) }
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Panel(Modifier.fillMaxWidth().weight(1.2f)) {
                            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement=Arrangement.SpaceBetween) {
                                Text("GLAZBA  " + if(demo) "· DEMO" else "· VANJSKI PLAYER", color=accent, fontSize=12.sp, fontWeight=FontWeight.Bold)
                                if(demo) {
                                    Column {
                                        Text(tracks[song].first, color=white, fontSize=24.sp, fontWeight=FontWeight.Bold, maxLines=1, overflow=TextOverflow.Ellipsis)
                                        Text(tracks[song].second, color=muted, fontSize=13.sp)
                                        Text("${seconds/60}:${(seconds%60).toString().padStart(2,'0')}  /  3:20", color=muted, fontSize=12.sp)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) {
                                        FilledButton("◀◀") { song=(song+tracks.size-1)%tracks.size; seconds=0 }
                                        FilledButton(if(playing) "Ⅱ Pause" else "▶ Play") { playing=!playing }
                                        FilledButton("▶▶") { song=(song+1)%tracks.size; seconds=0 }
                                    }
                                } else {
                                    Column {
                                        Text(mediaTitle,color=white,fontSize=19.sp,fontWeight=FontWeight.Bold,maxLines=1)
                                        Text(mediaArtist,color=muted,fontSize=12.sp,maxLines=1)
                                    }
                                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                        FilledButton("◀◀") { activeMedia?.transportControls?.skipToPrevious() }
                                        FilledButton("▶/Ⅱ") {
                                            val s=activeMedia?.playbackState?.state
                                            if(s==android.media.session.PlaybackState.STATE_PLAYING) activeMedia?.transportControls?.pause()
                                            else activeMedia?.transportControls?.play()
                                        }
                                        FilledButton("▶▶") { activeMedia?.transportControls?.skipToNext() }
                                    }
                                    FilledButton("Media dozvole") {
                                        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                    }
                                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                        FilledButton("Spotify", Modifier.weight(1f)) { openApp(spotify) }
                                        FilledButton("YT Music", Modifier.weight(1f)) { openApp(ytMusic) }
                                    }
                                }
                            }
                        }
                        Panel(Modifier.fillMaxWidth().weight(.8f)) {
                            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement=Arrangement.SpaceBetween) {
                                Text("VOZILO", color=accent, fontSize=12.sp, fontWeight=FontWeight.Bold)
                                Row(verticalAlignment=Alignment.Bottom) {
                                    Text(if(demo) "65" else "--", color=white, fontSize=44.sp, fontWeight=FontWeight.Black)
                                    Text(" km/h", color=muted, fontSize=17.sp, modifier=Modifier.padding(bottom=9.dp))
                                }
                                Text(if(demo) "Simulirana brzina · nije GPS" else "Nema očitanja brzine", color=muted, fontSize=12.sp)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                NavButton("Početna", Modifier.weight(1f), page=="dashboard") { page="dashboard" }
                NavButton("Aplikacije", Modifier.weight(1f), page=="apps") { page="apps" }
                NavButton(if(demo) "Demo: UKLJ." else "Demo: ISKLJ.", Modifier.weight(1f), demo) { demo=!demo; playing=false }
                NavButton("Dijagnostika", Modifier.weight(1f), page=="diagnostics") { page="diagnostics" }
                NavButton("Postavke", Modifier.weight(1f)) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
                NavButton("Izlaz", Modifier.weight(1f)) { finish() }
            }
        }
    }
}
private fun clockNow()=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date())

@Composable private fun Panel(modifier:Modifier=Modifier, content:@Composable ()->Unit) {
    Surface(modifier=modifier, color=panel, shape=RoundedCornerShape(19.dp), border=BorderStroke(1.dp,Color.White.copy(alpha=.07f))) {
        content()
    }
}
@Composable private fun FilledButton(text:String,modifier:Modifier=Modifier,onClick:()->Unit) {
    Button(onClick=onClick,modifier=modifier,contentPadding=PaddingValues(horizontal=12.dp,vertical=7.dp)) {
        Text(text,fontSize=13.sp,maxLines=1)
    }
}
@Composable private fun NavButton(text:String,modifier:Modifier=Modifier,selected:Boolean=false,onClick:()->Unit) {
    Surface(modifier=modifier.height(49.dp).clickable(onClick=onClick), color=if(selected) Color(0xFF174640) else panel,shape=RoundedCornerShape(12.dp)) {
        Box(contentAlignment=Alignment.Center) {
            Text(text,color=if(selected) accent else white,fontSize=12.sp,maxLines=1)
        }
    }
}

@Composable private fun DriveMap(modifier: Modifier, demo: Boolean) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val map=remember {
        Configuration.getInstance().userAgentValue="DriveOS/0.3 (com.driveos.alpha)"
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(45.3271,14.4422))
            minZoomLevel=3.0
        }
    }
    DisposableEffect(map) {
        map.onResume()
        onDispose { map.onPause(); map.onDetach() }
    }
    AndroidView(factory={map}, modifier=modifier, update={})
}
