package com.example.pdfviewer.utility

import android.content.Context
import androidx.compose.runtime.ReadOnlyComposable
import java.io.File
import java.io.IOException

object Utility {

    @ReadOnlyComposable
    @Throws(IOException::class)
    fun getFileFromAssets(context: Context, fileName: String): File = File(context.cacheDir, fileName)
            .also {
                if (!it.exists()) {
                    it.outputStream().use { cache ->
                        context.assets.open(fileName).use { inputStream ->
                            inputStream.copyTo(cache)
                        }
                    }
                }
            }

}