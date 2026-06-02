package com.jdw.skillstestapp.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jdw.skillstestapp.BuildConfig
import com.jdw.skillstestapp.data.AppDatabase
import com.jdw.skillstestapp.data.ChatMessageDao
import com.jdw.skillstestapp.data.UserImgDao
import com.jdw.skillstestapp.data.network.WeatherApi
import com.jdw.skillstestapp.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @Singleton
    fun provideImgInfoDatabase(@ApplicationContext appContext: Context): AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "app_database").build()

    @Provides
    @Singleton
    fun provideUserImgDao(appDatabase: AppDatabase): UserImgDao = appDatabase.userImgDao()

    @Provides
    @Singleton
    fun provideChatDao(appDatabase: AppDatabase): ChatMessageDao = appDatabase.chatMessageDao()

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel =
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content {
                text("""
                You are a helpful AI assistant.
                IMPORTANT: Always respond with valid JSON only. Never use markdown, code blocks, or backticks.

                When the user is requesting to FIND or SEARCH for any place nearby (restaurants, cafes, stores, pharmacies, hospitals, parks, convenience stores, stationery shops, banks, gyms, etc.), respond with:
                {"intent":"PLACE_SEARCH","message":"<encouraging Korean response about starting the search>","keyword":"<specific place type in Korean, null if general>","radiusMeters":<500-2000, default 1000>}

                For all other conversations, respond with:
                {"intent":"GENERAL","message":"<helpful response in the user's language>"}
                """.trimIndent())
            }
        )

    @Provides
    @Singleton
    fun getFirebaseAuthInstance(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun getFirebaseFireStoreInstance(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun providePlacesClient(@ApplicationContext context: Context): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context, BuildConfig.GOOGLE_MAPS_API_KEY)
        }
        return Places.createClient(context)
    }

    @Provides
    @Singleton
    fun provideOpenWeatherApi(): WeatherApi =
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
}
