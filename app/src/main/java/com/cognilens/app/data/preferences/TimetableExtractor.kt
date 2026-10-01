package com.cognilens.app.data.preferences

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

interface TimetableExtractor {
    suspend fun extractCsv(bytes: ByteArray, mimeType: String): String
}

class OpenAiTimetableExtractor(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .build()
) : TimetableExtractor {

    override suspend fun extractCsv(bytes: ByteArray, mimeType: String): String =
        withContext(Dispatchers.IO) {
            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
            
            // OpenAI Vision API format
            val contentArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", PROMPT)
                })
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().put("url", "data:$mimeType;base64,$base64Image"))
                })
            }

            val body = JSONObject().apply {
                put("model", "gpt-4o")
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", contentArray)
                }))
                put("max_tokens", 4000)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    throw IOException("OpenAI Error (${response.code}): $errorBody")
                }
                val json = JSONObject(response.body?.string().orEmpty())
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            }
        }

    private companion object {
        val PROMPT = """
            You convert a student's class timetable into CSV.
            Output ONLY CSV. No commentary and no code fences.
            The first line must be exactly: day,start,end,course_code,course_name,location
            One row per class session. day is one of MON,TUE,WED,THU,FRI,SAT,SUN.
            start and end are 24-hour HH:mm. Leave a value empty if it is missing.
            Wrap any value containing a comma in double quotes.
            If the file contains no timetable, output only the header line.
        """.trimIndent()
    }
}
