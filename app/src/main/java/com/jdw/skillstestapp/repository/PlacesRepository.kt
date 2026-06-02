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
        return when {
            // 음식/식당
            keyword.contains("일식") || keyword.contains("스시") || keyword.contains("라멘") -> listOf("japanese_restaurant")
            keyword.contains("한식") -> listOf("korean_restaurant")
            keyword.contains("중식") -> listOf("chinese_restaurant")
            keyword.contains("이탈리안") || keyword.contains("파스타") || keyword.contains("피자") -> listOf("italian_restaurant")
            keyword.contains("카페") || keyword.contains("커피") -> listOf("cafe")
            keyword.contains("버거") || keyword.contains("패스트푸드") -> listOf("fast_food_restaurant")
            keyword.contains("베이커리") || keyword.contains("빵집") -> listOf("bakery")
            keyword.contains("식당") || keyword.contains("레스토랑") || keyword.contains("음식점") || keyword.contains("맛집") -> listOf("restaurant")
            // 쇼핑/생활
            keyword.contains("문구") -> listOf("stationery")
            keyword.contains("약국") -> listOf("pharmacy")
            keyword.contains("편의점") -> listOf("convenience_store")
            keyword.contains("마트") || keyword.contains("슈퍼") -> listOf("supermarket")
            keyword.contains("서점") || keyword.contains("책방") -> listOf("book_store")
            keyword.contains("꽃집") || keyword.contains("꽃") -> listOf("florist")
            keyword.contains("세탁") -> listOf("laundry")
            keyword.contains("미용실") || keyword.contains("헤어") -> listOf("hair_care")
            // 시설/서비스
            keyword.contains("병원") -> listOf("hospital")
            keyword.contains("은행") -> listOf("bank")
            keyword.contains("주유소") -> listOf("gas_station")
            keyword.contains("헬스") || keyword.contains("헬스장") || keyword.contains("피트니스") -> listOf("gym")
            keyword.contains("영화관") || keyword.contains("영화") -> listOf("movie_theater")
            keyword.contains("도서관") -> listOf("library")
            keyword.contains("공원") -> listOf("park")
            keyword.contains("호텔") -> listOf("lodging")
            keyword.contains("학교") -> listOf("school")
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
