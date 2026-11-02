package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class JamendoResponse(
    @Json(name = "results") val results: List<JamendoTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JamendoTrackDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "duration") val duration: Int? = 0,
    @Json(name = "artist_name") val artistName: String,
    @Json(name = "album_name") val albumName: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "audio") val audio: String? = null
)

interface JamendoApiService {
    @GET("v3.0/tracks/")
    suspend fun getTracks(
        @Query("client_id") clientId: String = "56d30c95",
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 30,
        @Query("order") order: String = "popularity_total",
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("namesearch") query: String? = null,
        @Query("tags") tags: String? = null
    ): JamendoResponse

    companion object {
        private const val BASE_URL = "https://api.jamendo.com/"

        fun create(): JamendoApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(JamendoApiService::class.java)
        }
    }
}
