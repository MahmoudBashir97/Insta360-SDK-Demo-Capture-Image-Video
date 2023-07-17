package com.example.sdkinstatest.camera_api

data class ExcuteComandsBody(
    val name:String,
    val parameters: Parameters,
)

data class Parameters(
    val sessionId:String,
    val options: Options
)
data class Options(
    val iso:Int,
    val exposureCompensation:Int
)

data class ExcuteCommmandsResponse(
    val name:String,
    val state:String,
    val results:Results
)
data class Results(
    val fileUri:String
)