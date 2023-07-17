package com.example.sdkinstatest.camera_api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface apiService {

    @POST("osc/commands/status")
    suspend fun excuteCommand(@Body body:ExcuteComandsBody):Response<ExcuteCommmandsResponse>
}