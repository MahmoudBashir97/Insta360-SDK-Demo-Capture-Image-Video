package com.example.sdkinstatest.camera_api

import javax.inject.Inject

class Repository @Inject constructor(val apiService: apiService) {
    suspend fun getResponse(body:ExcuteComandsBody) = apiService.excuteCommand(body)
}