package com.example.cutter

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.*
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.*
import androidx.media3.transformer.Composition
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.io.File

// 50 languages (index order matters: first 6 have full translations, rest fall back to English)
val LANGS = listOf("English","हिन्दी","Español","Français","العربية","বাংলা","اردو","Português","Русский","Deutsch",
    "日本語","中文","한국어","Italiano","Türkçe","Tiếng Việt","ไทย","Bahasa Indonesia","Bahasa Melayu","Nederlands",
    "Polski","Українська","Română","Ελληνικά","Čeština","Svenska","Magyar","Suomi","Dansk","Norsk",
    "עברית","فارسی","ગુજરાતી","मराठी","தமிழ்","తెలుగు","ಕನ್ನಡ","മലയാളം","ਪੰਜਾਬੀ","नेपाली",
    "සිංහල","Kiswahili","Hausa","አማርኛ","Filipino","Български","Hrvatski","Slovenčina","Српски","Català")

// Order: Cut|Join|Settings|Choose file|Choose files|Quality|Original|Folder|Folder ok|Preview|Stop|Start|End|CutSave|CutDelete|JoinSave|ExtractAudio|Processing|Saved|Error|Share|Language|Nothing left|Total
val TR = listOf(
"Cut|Join|Settings|Choose audio / video|Choose multiple files|Quality|Original|Choose save folder|Folder selected ✓|Preview|Stop preview|Start|End|Cut & Save (keep selected)|Cut & Delete (remove selected)|Join & Save|Extract audio (M4A)|Processing|Saved ✅|Error|Share|Language|Nothing would be left|Total|Cancel|New project|Cancelled|Select Save folder/location",
"काटें|जोड़ें|सेटिंग्स|ऑडियो / वीडियो चुनें|कई फाइलें चुनें|क्वालिटी|ओरिजिनल|सेव फोल्डर चुनें|फोल्डर चुना गया ✓|प्रीव्यू|प्रीव्यू रोकें|शुरू|अंत|कट एंड सेव (चुना हिस्सा रखें)|कट एंड डिलीट (चुना हिस्सा हटाएँ)|जोड़ें और सेव करें|ऑडियो निकालें (M4A)|प्रोसेसिंग|सेव हो गया ✅|त्रुटि|शेयर करें|भाषा|कुछ नहीं बचेगा|कुल|रद्द करें|नया प्रोजेक्ट|रद्द किया गया|सेव फोल्डर / लोकेशन चुनें",
"Cortar|Unir|Ajustes|Elegir audio / vídeo|Elegir varios archivos|Calidad|Original|Elegir carpeta de guardado|Carpeta elegida ✓|Vista previa|Detener vista previa|Inicio|Fin|Cortar y guardar (conservar selección)|Cortar y eliminar (quitar selección)|Unir y guardar|Extraer audio (M4A)|Procesando|Guardado ✅|Error|Compartir|Idioma|No quedaría nada|Total|Cancelar|Nuevo proyecto|Cancelado|Elegir carpeta / ubicación de guardado",
"Couper|Fusionner|Réglages|Choisir audio / vidéo|Choisir plusieurs fichiers|Qualité|Original|Choisir le dossier|Dossier choisi ✓|Aperçu|Arrêter l'aperçu|Début|Fin|Couper et enregistrer (garder la sélection)|Couper et supprimer (retirer la sélection)|Fusionner et enregistrer|Extraire l'audio (M4A)|Traitement|Enregistré ✅|Erreur|Partager|Langue|Il ne resterait rien|Total|Annuler|Nouveau projet|Annulé|Choisir le dossier / l'emplacement",
"قص|دمج|الإعدادات|اختر صوت / فيديو|اختر عدة ملفات|الجودة|الأصلية|اختر مجلد الحفظ|تم اختيار المجلد ✓|معاينة|إيقاف المعاينة|البداية|النهاية|قص وحفظ (إبقاء المحدد)|قص وحذف (إزالة المحدد)|دمج وحفظ|استخراج الصوت (M4A)|جارٍ المعالجة|تم الحفظ ✅|خطأ|مشاركة|اللغة|لن يتبقى شيء|الإجمالي|إلغاء|مشروع جديد|تم الإلغاء|اختر مجلد / موقع الحفظ",
"কাটুন|জোড়ুন|সেটিংস|অডিও / ভিডিও বাছুন|একাধিক ফাইল বাছুন|কোয়ালিটি|অরিজিনাল|সেভ ফোল্ডার বাছুন|ফোল্ডার বাছাই হয়েছে ✓|প্রিভিউ|প্রিভিউ থামান|শুরু|শেষ|কাট ও সেভ (নির্বাচিত অংশ রাখুন)|কাট ও ডিলিট (নির্বাচিত অংশ মুছুন)|জোড়া ও সেভ|অডিও আলাদা করুন (M4A)|প্রসেসিং|সেভ হয়েছে ✅|ত্রুটি|শেয়ার|ভাষা|কিছুই থাকবে না|মোট|বাতিল|নতুন প্রজেক্ট|বাতিল করা হয়েছে|সেভ ফোল্ডার / লোকেশন বাছুন"
).map { it.split("|") }

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF5B8DEF), secondary = Color(0xFF7ED6C1),
                background = Color(0xFFF6F9FF), surface = Color.White)) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { App() }
            }
        }
    }
}

fun info(c: Context, u: Uri): Pair<Long, Boolean> {
    val r = MediaMetadataRetriever(); r.setDataSource(c, u)
    val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
    val v = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
    r.release(); return d to v
}

fun fmt(ms: Long): String {
    val s = ms / 1000; val h = s / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, s / 60 % 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
}

fun parse(s: String): Long? {
    val p = s.trim().split(":").map { it.toLongOrNull() ?: return null }
    if (p.size !in 1..3) return null
    return p.fold(0L) { a, b -> a * 60 + b } * 1000
}

fun nameOf(c: Context, u: Uri): String {
    val n = try {
        c.contentResolver.query(u, null, null, null, null)?.use {
            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
        }?.substringBeforeLast('.')
    } catch (e: Exception) { null }
    return n ?: "media"
}

fun clip(u: Uri, s: Long, e: Long, video: Boolean, h: Int): EditedMediaItem {
    val mi = MediaItem.Builder().setUri(u).setClippingConfiguration(
        MediaItem.ClippingConfiguration.Builder().setStartPositionMs(s).setEndPositionMs(e).build()).build()
    val fx = if (video && h > 0) listOf<Effect>(Presentation.createForHeight(h)) else listOf()
    return EditedMediaItem.Builder(mi).setRemoveVideo(!video).setEffects(Effects(listOf(), fx)).build()
}

fun export(ctx: Context, items: List<EditedMediaItem>, video: Boolean, folder: Uri?, base: String,
           hold: (Transformer) -> Unit, done: (Boolean, Uri?, String) -> Unit) {
    val ext = if (video) "mp4" else "m4a"
    val mime = if (video) "video/mp4" else "audio/mp4"
    val tmp = File(ctx.cacheDir, "t${System.currentTimeMillis()}.$ext")
    val t = Transformer.Builder(ctx).setAudioMimeType(MimeTypes.AUDIO_AAC)
        .addListener(object : Transformer.Listener {
            override fun onCompleted(c: Composition, r: ExportResult) {
                try {
                    val uri: Uri = if (folder != null)
                        DocumentFile.fromTreeUri(ctx, folder)!!.createFile(mime, base)!!.uri
                    else ctx.contentResolver.insert(
                        if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "$base.$ext")
                            put(MediaStore.MediaColumns.MIME_TYPE, mime)
                            put(MediaStore.MediaColumns.RELATIVE_PATH, (if (video) "Movies" else "Music") + "/MediaCutter")
                        })!!
                    ctx.contentResolver.openOutputStream(uri)!!.use { o -> tmp.inputStream().use { it.copyTo(o) } }
                    tmp.delete(); done(true, uri, "")
                } catch (e: Exception) { done(false, null, e.message ?: "") }
            }
            override fun onError(c: Composition, r: ExportResult, e: ExportException) { done(false, null, e.message ?: "") }
        }).build()
    hold(t)
    t.start(Composition.Builder(listOf(EditedMediaItemSequence(items))).build(), tmp.absolutePath)
}

@Composable
fun TimeField(label: String, v: Long, ver: Int, mod: Modifier, onSet: (Long) -> Unit) {
    var txt by remember(ver) { mutableStateOf(fmt(v)) }
    OutlinedTextField(txt, { txt = it; parse(it)?.let(onSet) }, label = { Text(label) }, singleLine = true, modifier = mod)
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("p", 0) }
    var lang by remember { mutableStateOf(prefs.getInt("lang", 0)) }
    fun t(i: Int) = (TR.getOrNull(lang) ?: TR[0])[i]
    var tab by remember { mutableStateOf(0) }
    var folder by remember { mutableStateOf<Uri?>(null) }
    var res by remember { mutableStateOf(0) } // 0 = original
    var busy by remember { mutableStateOf(false) }
    var sec by remember { mutableStateOf(0L) }
    var pct by remember { mutableStateOf(0f) }
    var tr by remember { mutableStateOf<Transformer?>(null) }
    var msg by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<Uri?>(null) }
    var file by remember { mutableStateOf<Uri?>(null) }
    var dur by remember { mutableStateOf(0L) }
    var isVideo by remember { mutableStateOf(true) }
    var startMs by remember { mutableStateOf(0L) }
    var endMs by remember { mutableStateOf(0L) }
    var ver by remember { mutableStateOf(0) }
    var files by remember { mutableStateOf(listOf<Uri>()) }
    var langMenu by remember { mutableStateOf(false) }

    val exo = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) { onDispose { exo.release() } }
    // Player plays ONLY the selected range
    LaunchedEffect(file, startMs, endMs) {
        val u = file ?: return@LaunchedEffect
        delay(350)
        exo.setMediaItem(MediaItem.Builder().setUri(u).setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder().setStartPositionMs(startMs)
                .setEndPositionMs(maxOf(endMs, startMs + 100)).build()).build())
        exo.prepare()
    }
    LaunchedEffect(busy) {
        val t0 = System.currentTimeMillis()
        while (busy) {
            sec = (System.currentTimeMillis() - t0) / 1000
            val h = ProgressHolder()
            if (tr?.getProgress(h) == Transformer.PROGRESS_STATE_AVAILABLE) pct = h.progress / 100f
            delay(300)
        }
    }

    val pickOne = rememberLauncherForActivityResult(OpenDocument()) { u ->
        if (u != null) {
            file = u; val (d, v) = info(ctx, u); dur = d; isVideo = v; startMs = 0; endMs = d; ver++
            msg = ""; saved = null
            exo.setMediaItem(MediaItem.fromUri(u)); exo.prepare()
        }
    }
    val pickMany = rememberLauncherForActivityResult(OpenMultipleDocuments()) { files = it }
    val pickDir = rememberLauncherForActivityResult(OpenDocumentTree()) { u ->
        if (u != null) { ctx.contentResolver.takePersistableUriPermission(u, 3); folder = u }
    }
    fun go(items: List<EditedMediaItem>, video: Boolean, base: String) {
        busy = true; pct = 0f; msg = ""; saved = null
        export(ctx, items, video, folder, base, { tr = it }) { ok, u, e ->
            busy = false
            if (ok) { saved = u; msg = t(18) } else msg = t(19) + ": " + e
        }
    }

    fun newProject() {
        exo.stop(); exo.clearMediaItems()
        file = null; files = listOf(); saved = null; msg = ""; pct = 0f; dur = 0; startMs = 0; endMs = 0; ver++
    }

    // scissors spin once on app open / resume / rotation
    val rot = remember { Animatable(0f) }
    var resumes by remember { mutableStateOf(0) }
    val orient = LocalConfiguration.current.orientation
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) resumes++ }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) }
    }
    LaunchedEffect(orient, resumes) { rot.snapTo(0f); rot.animateTo(360f, tween(900)) }

    CompositionLocalProvider(LocalLayoutDirection provides if (lang == 4) LayoutDirection.Rtl else LayoutDirection.Ltr) {
    Column(Modifier.fillMaxSize()) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✂️", fontSize = 30.sp, modifier = Modifier.graphicsLayer { rotationZ = rot.value })
            Text("  Simple Cutter", style = MaterialTheme.typography.headlineSmall)
        }
        TabRow(tab, containerColor = Color.Transparent) {
            Tab(tab == 0, { tab = 0 }, text = { Text(t(0)) })
            Tab(tab == 1, { tab = 1 }, text = { Text(t(1)) })
            Tab(tab == 2, { tab = 2 }, text = { Text(t(2)) })
        }
        if (tab == 2) {
            Text(t(21))
            Box {
                OutlinedButton({ langMenu = true }) { Text("🌐 ${LANGS[lang]}  ▾") }
                DropdownMenu(langMenu, { langMenu = false }) {
                    LANGS.forEachIndexed { i, n ->
                        DropdownMenuItem({ Text(n) }, { lang = i; prefs.edit().putInt("lang", i).apply(); langMenu = false })
                    }
                }
            }
            if (lang >= TR.size) Text("Translation for this language is not added yet — English is shown.",
                style = MaterialTheme.typography.bodySmall)
        } else {
            if (tab == 1 || isVideo) Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t(5))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 240, 360, 720).forEach {
                            FilterChip(res == it, { res = it }, label = { Text(if (it == 0) t(6) else "${it}p") })
                        }
                    }
                }
            }

            if (tab == 0) {
                Button({ pickOne.launch(arrayOf("video/*", "audio/*")) }) { Text(t(3)) }
                if (file != null && dur > 0) {
                    AndroidView({ PlayerView(it).apply { player = exo; useController = true } },
                        Modifier.fillMaxWidth().height(200.dp))
                    Text("${fmt(startMs)} — ${fmt(endMs)}   (${t(23)} ${fmt(dur)})")
                    RangeSlider(startMs.toFloat()..endMs.toFloat(),
                        { startMs = it.start.toLong(); endMs = it.endInclusive.toLong(); ver++ },
                        valueRange = 0f..dur.toFloat())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeField(t(11), startMs, ver, Modifier.weight(1f)) { startMs = it.coerceIn(0, endMs - 500) }
                        TimeField(t(12), endMs, ver, Modifier.weight(1f)) { endMs = it.coerceIn(startMs + 500, dur) }
                    }
                    OutlinedButton({ exo.seekTo(0); exo.play() }) { Text("▶ " + t(9)) }
                    val base = nameOf(ctx, file!!)
                    Button({ exo.pause()
                        go(listOf(clip(file!!, startMs, endMs, isVideo, res)), isVideo, base + "_cut")
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(t(13)) }
                    Button({ exo.pause()
                        val parts = mutableListOf<EditedMediaItem>()
                        if (startMs > 0) parts += clip(file!!, 0, startMs, isVideo, res)
                        if (endMs < dur) parts += clip(file!!, endMs, dur, isVideo, res)
                        if (parts.isEmpty()) msg = t(22) else go(parts, isVideo, base + "_edit")
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text(t(14)) }
                    if (isVideo) OutlinedButton({
                        go(listOf(clip(file!!, startMs, endMs, false, 0)), false, base + "_audio")
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("🎵 " + t(16)) }
                }
            } else {
                Button({ pickMany.launch(arrayOf("video/*", "audio/*")) }) { Text(t(4)) }
                files.forEachIndexed { i, u -> Text("${i + 1}. ${nameOf(ctx, u)}", maxLines = 1) }
                if (files.size >= 2) Button({
                    val v = info(ctx, files[0]).second
                    go(files.map { clip(it, 0, info(ctx, it).first, v, res) }, v, "joined_${System.currentTimeMillis() / 1000}")
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(t(15)) }
            }
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp)
                    Text("${t(17)}... ${(pct * 100).toInt()}%  •  ${fmt(sec * 1000)}")
                }
                LinearProgressIndicator({ pct }, Modifier.fillMaxWidth())
                OutlinedButton({ tr?.cancel(); tr = null; busy = false; msg = t(26) }) { Text("✖ " + t(24)) }
            }
            if (msg.isNotEmpty()) Text(msg)
            if (!busy && (saved != null || msg.isNotEmpty())) Button({ newProject() }) { Text("➕ " + t(25)) }
        }
    }
    if (tab == 2) Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("App Developed by Suresh Vyas", style = MaterialTheme.typography.bodyMedium)
        Text("Bikaner, Rajasthan", style = MaterialTheme.typography.bodySmall)
    } else Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton({ pickDir.launch(null) }, Modifier.weight(1f)) {
            Text("📁 " + t(27) + if (folder != null) " ✓" else "")
        }
        Button({
            saved?.let { u ->
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = ctx.contentResolver.getType(u) ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, u); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(Intent.createChooser(i, null))
            }
        }, enabled = saved != null) { Text("📤") }
    }
    }
    }
}
