package com.cognilens.app.data.preferences

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cognilens.app.BuildConfig
import com.cognilens.app.data.local.CogniLensDatabase
import com.cognilens.app.data.local.dao.TimetableDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class TimetableRepository(
    private val dao: TimetableDao,
    private val extractor: TimetableExtractor
) {
    fun observeCount() = dao.observeCount()

    suspend fun importFrom(bytes: ByteArray, mimeType: String): Int {
        val rows = TimetableCsv.parse(extractor.extractCsv(bytes, mimeType))
        check(rows.isNotEmpty()) { "No classes were found. Try a clearer photo or a PDF." }
        dao.replaceAll(rows)
        return rows.size
    }
}

sealed interface ImportState {
    data object Idle : ImportState
    data object Working : ImportState
    data class Failed(val message: String) : ImportState
}

class TimetableImportViewModel(private val repo: TimetableRepository) : ViewModel() {
    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state
    val entryCount = repo.observeCount()

    fun import(uri: Uri, resolver: ContentResolver, onSuccess: () -> Unit) {
        if (_state.value is ImportState.Working) return
        _state.value = ImportState.Working
        viewModelScope.launch {
            runCatching {
                val (bytes, mime) = withContext(Dispatchers.IO) { readFile(uri, resolver) }
                repo.importFrom(bytes, mime)
            }.onSuccess {
                _state.value = ImportState.Idle
                onSuccess()
            }.onFailure {
                _state.value = ImportState.Failed(it.message ?: "Something went wrong. Please try again.")
            }
        }
    }

    private fun readFile(uri: Uri, resolver: ContentResolver): Pair<ByteArray, String> {
        val mime = resolver.getType(uri) ?: error("Unknown file type.")
        val raw = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not open that file.")
        require(raw.size <= 20 * 1024 * 1024) { "That file is too large (max 20 MB)." }
        return when {
            mime == "application/pdf" -> raw to mime
            mime.startsWith("image/") -> shrinkToJpeg(raw) to "image/jpeg"
            else -> error("Please choose a PDF or a photo.")
        }
    }

    private fun shrinkToJpeg(raw: ByteArray): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2400) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(
            raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: error("Could not read that image.")
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        bmp.recycle()
        return out.toByteArray()
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val db = CogniLensDatabase.getDatabase(context.applicationContext)
                TimetableImportViewModel(
                    TimetableRepository(db.timetableDao(), OpenAiTimetableExtractor(BuildConfig.AI_API_KEY))
                )
            }
        }
    }
}
