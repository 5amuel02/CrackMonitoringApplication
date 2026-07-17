package com.example.seismicaplication

data class DetectionLog(
    val _id: String,
    val data: DetectionData
)

data class DetectionData(
    val confidence: Double,
    val image_url: String?,   // ✅ nullable — bisa null dari server
    val label: String,
    val timestamp: String,
    val status: String?        // ✅ nullable — bisa null dari server
)