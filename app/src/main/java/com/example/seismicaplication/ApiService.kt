package com.example.seismicaplication

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    // Sesuaikan endpoint ini dengan route Flask kamu nanti
    @GET("/documents")
    fun getDetectionLogs(): Call<List<DetectionLog>>
}