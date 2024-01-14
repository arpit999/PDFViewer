package com.example.pdfviewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.example.pdfviewer.model.PdfRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun Render1(
    modifier: Modifier = Modifier,
    file: File,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
) {

    val scaleState = remember { mutableStateOf(1f) }

    val rendererScope = rememberCoroutineScope()
    val mutex = remember { Mutex() }
    val renderer by produceState<PdfRenderer?>(null, file) {
        rendererScope.launch(Dispatchers.IO) {
            val input = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            value = PdfRenderer(input)
        }
        awaitDispose {
            val currentRenderer = value
            rendererScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    currentRenderer?.close()
                }
            }
        }
    }
    val context = LocalContext.current
    val imageLoader = LocalContext.current.imageLoader
    val imageLoadingScope = rememberCoroutineScope()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
        val height = (width * sqrt(2f)).toInt()
        val pageCount by remember(renderer) { derivedStateOf { renderer?.pageCount ?: 0 } }
        LazyColumn(
            verticalArrangement = verticalArrangement
        ) {
            items(
                count = pageCount,
                key = { index -> "$file-$index" }
            ) { index ->
                val cacheKey = MemoryCache.Key("$file-$index")
                val cacheValue: Bitmap? = imageLoader.memoryCache?.get(cacheKey)?.bitmap

                var bitmap: Bitmap? by remember { mutableStateOf(cacheValue) }
                if (bitmap == null) {
                    DisposableEffect(file, index) {
                        val job = imageLoadingScope.launch(Dispatchers.IO) {
                            val destinationBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            mutex.withLock {
                                Log.d("PdfGenerator", "Loading PDF $file - page $index/$pageCount")
                                if (!coroutineContext.isActive) return@launch
                                try {
                                    renderer?.let {
                                        it.openPage(index).use { page ->
                                            page.render(
                                                destinationBitmap,
                                                null,
                                                null,
                                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    //Just catch and return in case the renderer is being closed
                                    return@launch
                                }
                            }
                            bitmap = destinationBitmap
                        }
                        onDispose {
                            job.cancel()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .aspectRatio(1f / sqrt(2f))
                            .fillMaxWidth()
                    )
                } else { //bitmap != null
                    val request = ImageRequest.Builder(context)
                        .size(width, height)
                        .memoryCacheKey(cacheKey)
                        .data(bitmap)
                        .build()

                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .aspectRatio(1f / sqrt(2f))
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scaleState.value *= zoom
                                    scaleState.value = max(1f, min(scaleState.value, 3f))
                                }
                            }
                            .graphicsLayer(scaleX = scaleState.value, scaleY = scaleState.value)
                    ) {
                        Image(
                            modifier = Modifier
                                .background(Color.White)
                                .aspectRatio(1f / sqrt(2f))
                                .fillMaxWidth(),
                            contentScale = ContentScale.Fit,
                            painter = rememberAsyncImagePainter(request),
                            contentDescription = "Page ${index + 1} of $pageCount"
                        )
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Render2(file: File) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val pdfRender = PdfRender(file)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(count = pdfRender.pdfRenderer.pageCount) { index ->
                val page = pdfRender.pageLists[index]
                LaunchedEffect(key1 = Unit) {
                    page.load()
                }
                page.pageContent.collectAsState().value?.asImageBitmap()?.let {
                    Image(
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .background(Color.White)
                            .aspectRatio(1f / sqrt(2f))
                            .fillMaxWidth()
                            .clipToBounds()
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onDoubleClick = {
                                    page.scale = if (page.scale > 1f) 1f
                                    else 3f
                                }
                            )
                            .graphicsLayer {
                                scaleX = page.scale
                                scaleY = page.scale
                            }
                            .pointerInput(Unit) {
                                forEachGesture {
                                    awaitPointerEventScope {
                                        // Wait for at least one pointer to press down
                                        awaitFirstDown()
                                        do {

                                            val event = awaitPointerEvent()
                                            // Calculate gestures and consume pointerInputChange
                                            // only size of pointers down is 2
                                            if (event.changes.size == 2) {
                                                var zoom = page.scale
                                                zoom *= event.calculateZoom()
                                                // Limit zoom between 100% and 300%
                                                zoom = zoom.coerceIn(1f, 3f)
                                                page.scale = zoom

                                                /*
                                                    Consumes position change if there is any
                                                    This stops scrolling if there is one set to any parent Composable
                                                 */
                                                event.changes.forEach { pointerInputChange: PointerInputChange ->
                                                    pointerInputChange.consume()
                                                }
                                            }
                                        } while (event.changes.any { it.pressed })
                                    }
                                }
                            },
                        bitmap = it,
                        contentDescription = "Pdf page number: $index"
                    )
                }
            }
        }

    }


}