package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Models ---

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String,
    @Json(name = "imageSize") val imageSize: String = "1K"
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "imageConfig") val imageConfig: ImageConfig? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// --- Veo Video Generation Models ---

@JsonClass(generateAdapter = true)
data class VeoConfig(
    @Json(name = "numberOfVideos") val numberOfVideos: Int = 1,
    @Json(name = "resolution") val resolution: String = "720p",
    @Json(name = "aspectRatio") val aspectRatio: String = "16:9"
)

@JsonClass(generateAdapter = true)
data class GenerateVideosRequest(
    @Json(name = "prompt") val prompt: String,
    @Json(name = "config") val config: VeoConfig? = null
)

@JsonClass(generateAdapter = true)
data class VeoResponse(
    @Json(name = "name") val name: String? = null,
    @Json(name = "error") val error: String? = null
)

// --- Retrofit Interface ---

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/{model}:generateVideos")
    suspend fun generateVideos(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateVideosRequest
    ): Map<String, Any> // Flexible response map for async operations
}

object GeminiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: GeminiApi = retrofit.create(GeminiApi::class.java)

    /**
     * Helper to get API Key safely.
     */
    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Executes general AI text commands (Summarize, grammar, translations, rewrite).
     * Uses gemini-3.1-flash-lite-preview for low latency, or gemini-3.5-flash by default.
     */
    suspend fun processText(
        prompt: String,
        model: String = "gemini-3.1-flash-lite-preview",
        systemInstruction: String? = null
    ): String {
        val apiKey = getApiKey()
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemInstruction?.let {
                Content(parts = listOf(Part(text = it)))
            }
        )
        return try {
            val response = api.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No text response generated."
        } catch (e: Exception) {
            Log.e("GeminiService", "Text API call failed", e)
            "AI Error: ${e.localizedMessage ?: "Connection timed out"}"
        }
    }

    /**
     * Multi-turn chat assistant.
     */
    suspend fun chat(
        history: List<Content>,
        systemInstruction: String = "You are MaxOffice AI, a highly professional, privacy-respecting chatbot assistant. Helping with office editing, summaries, structures, or any guidance.",
        model: String = "gemini-3.5-flash"
    ): String {
        val apiKey = getApiKey()
        val request = GenerateContentRequest(
            contents = history,
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        return try {
            val response = api.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Empty chat response."
        } catch (e: Exception) {
            Log.e("GeminiService", "Chat API call failed", e)
            "Error: ${e.localizedMessage ?: "Failed to connect to AI assistant"}"
        }
    }

    /**
     * Generates an image using gemini-3.1-flash-image-preview with aspect ratio control.
     * Returns Base64 jpeg string if successful.
     */
    suspend fun generateImage(
        prompt: String,
        aspectRatio: String = "1:1",
        model: String = "gemini-3.1-flash-image-preview"
    ): String? {
        val apiKey = getApiKey()
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )
        return try {
            val response = api.generateContent(model, apiKey, request)
            val parts = response.candidates?.firstOrNull()?.content?.parts
            val inlinePart = parts?.firstOrNull { it.inlineData != null }
            inlinePart?.inlineData?.data
        } catch (e: Exception) {
            Log.e("GeminiService", "Image generation failed", e)
            null
        }
    }

    /**
     * Triggers Veo Video Generation using model veo-3.1-fast-generate-preview and 16:9 / 9:16 aspect ratio.
     * Returns the name of the operation or details.
     */
    suspend fun generateVideo(
        prompt: String,
        aspectRatio: String = "16:9",
        model: String = "veo-3.1-fast-generate-preview"
    ): String {
        val apiKey = getApiKey()
        val request = GenerateVideosRequest(
            prompt = prompt,
            config = VeoConfig(numberOfVideos = 1, resolution = "720p", aspectRatio = aspectRatio)
        )
        return try {
            val responseMap = api.generateVideos(model, apiKey, request)
            responseMap["name"]?.toString() ?: "Operation started successfully."
        } catch (e: Exception) {
            Log.e("GeminiService", "Veo video generation failed", e)
            "Error: ${e.localizedMessage ?: "Veo synthesis error (endpoint offline or rate-limited)"}"
        }
    }
}
