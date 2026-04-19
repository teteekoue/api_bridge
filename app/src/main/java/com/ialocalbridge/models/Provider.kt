package com.ialocalbridge.models

data class Provider(
    val name: String,
    val packageName: String,
    val coordinates: ProviderCoordinates
)

data class ProviderCoordinates(
    var textFieldX: Float = 0f,
    var textFieldY: Float = 0f,
    var backButtonX: Float = 0f,
    var backButtonY: Float = 0f,
    var sendButtonX: Float = 0f,
    var sendButtonY: Float = 0f,
    var scrollDownButtonX: Float = 0f,
    var scrollDownButtonY: Float = 0f,
    var copyButtonX: Float = 0f,
    var copyButtonY: Float = 0f,
    var delayAfterSendMs: Long = 7000
)
