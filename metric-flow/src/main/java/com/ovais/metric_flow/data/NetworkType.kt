package com.ovais.metric_flow.data

sealed interface NetworkClientType {
    data object OkHttp : NetworkClientType
    data object Ktor : NetworkClientType
}