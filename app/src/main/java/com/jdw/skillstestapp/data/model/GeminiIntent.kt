package com.jdw.skillstestapp.data.model

data class GeminiIntent(
    val intent: String = "GENERAL",
    val message: String = "",
    val keyword: String? = null,
    val radiusMeters: Int = 1000
)
