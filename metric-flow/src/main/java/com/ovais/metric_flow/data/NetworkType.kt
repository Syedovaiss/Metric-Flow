package com.ovais.metric_flow.data

sealed interface NetworkClientType {
    data object OkHttp : NetworkClientType
    data object Ktor : NetworkClientType
    data object Retrofit : NetworkClientType  // Uses OkHttp under the hood
    data object HttpURLConnection : NetworkClientType
    data object Volley : NetworkClientType
    data object Auto : NetworkClientType  // Automatically detect network type
}