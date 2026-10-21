package com.karvo.data

import android.content.Context
import android.net.Uri
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads a whole text file picked by the user; an interface so view models can be tested without Android. */
fun interface TextFileReader {
    suspend fun read(uri: String): String
}

class ContentResolverTextFileReader(context: Context) : TextFileReader {
    private val resolver = context.applicationContext.contentResolver

    override suspend fun read(uri: String): String = withContext(Dispatchers.IO) {
        val input = resolver.openInputStream(Uri.parse(uri)) ?: throw IOException("Can't open $uri")
        input.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
