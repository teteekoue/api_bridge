package com.ialocalbridge.models

data class Provider(
    val name: String,
    val packageName: String,
    val coordinates: ProviderCoordinates
)

data class ProviderCoordinates(
    var textFieldX: Float = 0f,
    var textFieldY: Float = 0f,
    var sendButtonX: Float = 0f,
    var sendButtonY: Float = 0f,
    var copyButtonX: Float = 0f,
    var copyButtonY: Float = 0f,
    var copyButtonResourceId: String? = null,
    var copyButtonClassName: String? = null,
    var copyButtonDescription: String? = null,
    var delayAfterSendMs: Long = 7000
)
