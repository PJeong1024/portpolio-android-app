package com.jdw.skillstestapp.repository

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PlacesRepository @Inject constructor(
    private val placesClient: PlacesClient
) {
    private val placeFields = listOf(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.LAT_LNG,
        Place.Field.RATING,
        Place.Field.ADDRESS,
        Place.Field.TYPES,
        Place.Field.PHOTO_METADATAS,
        Place.Field.BUSINESS_STATUS,
        Place.Field.CURRENT_OPENING_HOURS,
        Place.Field.OPENING_HOURS,
        Place.Field.PRICE_LEVEL,
        Place.Field.REVIEWS,
    )

    private fun keywordToPlaceTypes(keyword: String?): List<String>? {
        if (keyword == null) return null
        val k = keyword.lowercase()
        return when {
            // 음식/식당
            k.contains("sushi") || k.contains("japanese") -> listOf("japanese_restaurant")
            k.contains("korean") -> listOf("korean_restaurant")
            k.contains("chinese") -> listOf("chinese_restaurant")
            k.contains("italian") || k.contains("pasta") || k.contains("pizza") -> listOf("italian_restaurant")
            k.contains("cafe") || k.contains("coffee") -> listOf("cafe")
            k.contains("burger") || k.contains("fast food") || k.contains("fastfood") -> listOf("fast_food_restaurant")
            k.contains("bakery") || k.contains("bread") -> listOf("bakery")
            k.contains("restaurant") || k.contains("food") || k.contains("dining") -> listOf("restaurant")
            // 쇼핑/생활
            k.contains("stationery") -> listOf("stationery")
            k.contains("pharmacy") || k.contains("drugstore") -> listOf("pharmacy")
            k.contains("convenience") -> listOf("convenience_store")
            k.contains("supermarket") || k.contains("grocery") -> listOf("supermarket")
            k.contains("bookstore") || k.contains("book store") || k.contains("book shop") -> listOf("book_store")
            k.contains("florist") || k.contains("flower") -> listOf("florist")
            k.contains("laundry") -> listOf("laundry")
            k.contains("hair") || k.contains("salon") || k.contains("barber") -> listOf("hair_care")
            // 시설/서비스
            k.contains("hospital") || k.contains("clinic") -> listOf("hospital")
            k.contains("bank") -> listOf("bank")
            k.contains("gas station") || k.contains("gas_station") || k.contains("fuel") -> listOf("gas_station")
            k.contains("gym") || k.contains("fitness") -> listOf("gym")
            k.contains("movie") || k.contains("cinema") || k.contains("theater") -> listOf("movie_theater")
            k.contains("library") -> listOf("library")
            k.contains("park") -> listOf("park")
            k.contains("hotel") || k.contains("lodging") -> listOf("lodging")
            k.contains("school") -> listOf("school")
            else -> null  // 매핑 안 되면 타입 필터 없이 검색
        }
    }

    suspend fun searchNearbyPlaces(
        location: LatLng,
        radiusMeters: Double = 1000.0,
        maxCount: Int = 20,
        keyword: String? = null
    ): Result<List<Place>> = suspendCancellableCoroutine { cont ->
        val bounds = CircularBounds.newInstance(location, radiusMeters)
        val builder = SearchNearbyRequest.builder(bounds, placeFields)
            .setMaxResultCount(maxCount)
        keywordToPlaceTypes(keyword)?.let { builder.setIncludedTypes(it) }
        val request = builder.build()

        placesClient.searchNearby(request)
            .addOnSuccessListener { response ->
                if (cont.isActive) cont.resume(Result.success(response.places))
            }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resume(Result.failure(e))
            }
    }

    suspend fun fetchPlacePhoto(
        photoMetadata: PhotoMetadata,
        maxWidth: Int = 400,
        maxHeight: Int = 400
    ): Result<Bitmap> = suspendCancellableCoroutine { cont ->
        val request = FetchPhotoRequest.builder(photoMetadata)
            .setMaxWidth(maxWidth)
            .setMaxHeight(maxHeight)
            .build()

        placesClient.fetchPhoto(request)
            .addOnSuccessListener { response ->
                if (cont.isActive) cont.resume(Result.success(response.bitmap))
            }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resume(Result.failure(e))
            }
    }
}
