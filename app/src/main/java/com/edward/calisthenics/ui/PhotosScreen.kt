package com.edward.calisthenics.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.edward.calisthenics.data.AppState
import com.edward.calisthenics.data.Program
import java.io.File
import java.time.LocalDate

fun photoFile(context: Context, week: Int) = File(context.filesDir, "photos/week_$week.jpg")

/** Читает фото с учётом поворота из EXIF, уменьшая до ~1200 px. */
private fun loadPhoto(file: File, maxSide: Int = 1200): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
    val bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val deg = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val out: Bitmap = if (deg == 0f) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
    out.asImageBitmap()
}.getOrNull()

private fun copyInto(context: Context, uri: Uri, target: File): Boolean = runCatching {
    target.parentFile?.mkdirs()
    context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } != null
}.getOrDefault(false)

/** Экран «Фото прогресса»: сравнение было/стало ползунком и фото по неделям. */
@Composable
fun PhotosScreen(state: AppState, onBack: () -> Unit, onSaved: (Int) -> Unit, onDelete: (Int) -> Unit) {
    val context = LocalContext.current
    val currentWeek = Program.weekOf(state.settings, LocalDate.now()).coerceIn(1, Program.WEEKS)
    var target by rememberSaveable { mutableIntStateOf(currentWeek) }
    var askSource by remember { mutableStateOf(false) }
    var version by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf<Int?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val tmp = File(context.filesDir, "photos/tmp.jpg")
        if (ok && tmp.exists() && tmp.length() > 0) {
            tmp.renameTo(photoFile(context, target).also { it.delete() })
            onSaved(target)
            version++
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && copyInto(context, uri, photoFile(context, target))) {
            onSaved(target)
            version++
        }
    }
    fun takePhoto() {
        val tmp = File(context.filesDir, "photos/tmp.jpg").apply { parentFile?.mkdirs() }
        camera.launch(FileProvider.getUriForFile(context, context.packageName + ".files", tmp))
    }

    val weeks = state.photos.keys.sorted()
    val first = weeks.firstOrNull()
    val last = weeks.lastOrNull()
    val firstImg = remember(first, version) { first?.let { loadPhoto(photoFile(context, it)) } }
    val lastImg = remember(last, version) { last?.let { loadPhoto(photoFile(context, it)) } }

    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar("Фото прогресса", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Раз в неделю, лучше в воскресенье утром: у одного зеркала, при одном свете, в одной позе. Фото хранятся только в телефоне.", color = Muted)
            if (firstImg != null && lastImg != null && first != last) {
                CompareSlider(firstImg, lastImg, "Неделя $first", "Неделя $last")
            } else if (lastImg != null) {
                Box(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(24.dp)).background(Surface1)) {
                    Image(lastImg, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Pill("Неделя $last", Color.White, Modifier.padding(12.dp))
                }
                Text("Через неделю сделайте второе фото: появится сравнение «было / стало».", style = Caption)
            } else {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(24.dp)).background(Surface1),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MoveFigure("arm_circles", Modifier.size(150.dp), animate = false, muscles = false)
                        Text("Здесь будет сравнение «было / стало»", color = Muted, textAlign = TextAlign.Center)
                    }
                }
            }

            SectionTitle("Все недели")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1..Program.WEEKS).toList()) { w ->
                    val has = w in state.photos
                    val thumb = remember(w, has, version) { if (has) loadPhoto(photoFile(context, w), 300) else null }
                    Box(
                        Modifier
                            .size(72.dp, 96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (has) Surface2 else Surface1)
                            .border(2.dp, if (w == target) Accent else Color.Transparent, RoundedCornerShape(14.dp))
                            .pointerInput(w, has) {
                                detectTapGestures(
                                    onTap = { target = w },
                                    onLongPress = { if (has) confirmDelete = w },
                                )
                            },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Text(
                            "нед $w", fontSize = 12.sp, color = if (has) Color.White else Muted,
                            modifier = Modifier.background(if (has) Color.Black.copy(alpha = 0.45f) else Color.Transparent).padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text("Нажмите на неделю, чтобы выбрать её. Долгое нажатие удаляет фото.", style = Caption)
            Spacer(Modifier.height(4.dp))
        }
        Button(
            onClick = { askSource = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Filled.PhotoCamera, null)
            Text(if (target in state.photos) "  Переснять фото недели $target" else "  Фото недели $target", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (askSource) {
        AlertDialog(
            onDismissRequest = { askSource = false },
            title = { Text("Фото недели $target") },
            text = { Text("Сфотографируйтесь в зеркало в полный рост или выберите готовое фото.") },
            confirmButton = {
                TextButton(onClick = { askSource = false; takePhoto() }) {
                    Icon(Icons.Filled.PhotoCamera, null); Text("  Камера")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    askSource = false
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Icon(Icons.Filled.PhotoLibrary, null); Text("  Галерея") }
            },
        )
    }
    confirmDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Удалить фото недели $w?") },
            confirmButton = { TextButton(onClick = { onDelete(w); version++; confirmDelete = null }) { Text("Удалить", color = Danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Отмена") } },
        )
    }
}

/** Два фото друг на друге; ползунок открывает «стало» поверх «было». */
@Composable
private fun CompareSlider(before: ImageBitmap, after: ImageBitmap, beforeLabel: String, afterLabel: String) {
    var frac by remember { mutableFloatStateOf(0.5f) }
    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(24.dp)).background(Surface1)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> frac = (change.position.x / size.width).coerceIn(0f, 1f) }
            }
            .pointerInput(Unit) { detectTapGestures { frac = (it.x / size.width).coerceIn(0f, 1f) } },
    ) {
        Image(after, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Image(
            before, null,
            Modifier.fillMaxSize().drawWithContent {
                clipRect(right = size.width * frac) { this@drawWithContent.drawContent() }
            },
            contentScale = ContentScale.Crop,
        )
        val x = maxWidth * frac
        Box(Modifier.fillMaxHeight().width(3.dp).offset(x = x - 1.5.dp).background(Color.White))
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = x - 20.dp).size(40.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) { Text("⇆", color = Color(0xFF111111), fontWeight = FontWeight.Black) }
        Pill(beforeLabel, Color.White, Modifier.padding(12.dp))
        Pill(afterLabel, Success, Modifier.align(Alignment.TopEnd).padding(12.dp))
    }
}
