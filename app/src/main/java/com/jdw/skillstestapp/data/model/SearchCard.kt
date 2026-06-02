package com.jdw.skillstestapp.data.model

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng

data class SearchCard(
    val id: String,
    val title: String,
    val subtitle: String?,   // 주소, 설명 등
    val badge: String?,      // 평점, 가격대, 거리 등 한 줄 요약
    val thumbnail: Bitmap?,
    val latLng: LatLng?,     // 지도 연동용 — 있으면 Google Maps 오픈
    val actionUrl: String?   // 웹/딥링크용 — 있으면 브라우저 오픈
)
