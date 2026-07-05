package com.example.fxsoundreplica

data class AudioSettings(
    val id: String = "default",
    val name: String = "Default",
    val isEnabled: Boolean = false,
    val clarity: Float = 5f,
    val ambience: Float = 0f,
    val surround: Float = 0f,
    val dynamicBoost: Float = 0f,
    val bassBoost: Float = 0f,
    val reverb: Float = 0f,
    val eqBands: List<Float> = List(10) { 0f }
) {
    companion object {
        val Music = AudioSettings(
            id = "music",
            name = "MUSIC",
            isEnabled = true,
            clarity = 6f,
            ambience = 3f,
            surround = 4f,
            dynamicBoost = 4f,
            bassBoost = 6f,
            reverb = 2f,
            eqBands = listOf(5f, 4f, 2f, 0f, 1f, 2f, 3f, 5f, 6f, 7f)
        )
        val Gaming = AudioSettings(
            id = "gaming",
            name = "GAMING",
            isEnabled = true,
            clarity = 8f,
            ambience = 2f,
            surround = 9f,
            dynamicBoost = 6f,
            bassBoost = 5f,
            reverb = 1f,
            eqBands = listOf(3f, 2f, 1f, 2f, 4f, 6f, 7f, 6f, 5f, 4f)
        )
        val Movie = AudioSettings(
            id = "movie",
            name = "MOVIE",
            isEnabled = true,
            clarity = 4f,
            ambience = 7f,
            surround = 8f,
            dynamicBoost = 5f,
            bassBoost = 5f,
            reverb = 5f,
            eqBands = listOf(6f, 5f, 3f, 1f, 2f, 3f, 4f, 5f, 5f, 4f)
        )
        val CarDSP = AudioSettings(
            id = "car_dsp",
            name = "CAR DSP",
            isEnabled = true,
            clarity = 5f,
            ambience = 4f,
            surround = 6f,
            dynamicBoost = 7f,
            bassBoost = 8f,
            reverb = 2f,
            eqBands = listOf(8f, 6f, 4f, 2f, 1f, 2f, 4f, 6f, 7f, 5f)
        )
        val DeepFieldKTV = AudioSettings(
            id = "ktv",
            name = "KTV",
            isEnabled = true,
            clarity = 0f, // Zero as requested
            ambience = 10f, // Max for hall effect
            surround = 5f,
            dynamicBoost = 6f,
            bassBoost = 0f, // Zero as requested
            reverb = 10f, // Max wetness
            // EQ tuned for Karaoke Hall (Mids and Highs emphasis)
            eqBands = listOf(2f, 1f, 0f, 2f, 5f, 8f, 9f, 8f, 7f, 6f)
        )
        val Panoramic = AudioSettings(
            id = "panoramic",
            name = "PANORAMIC",
            isEnabled = true,
            clarity = 0f, // Zero as requested
            ambience = 6f,
            surround = 10f, // Absolute Max
            dynamicBoost = 5f,
            bassBoost = 0f, // Zero as requested
            reverb = 4f,
            // EQ tuned for width and "airy" presence
            eqBands = listOf(0f, 0f, 0f, 1f, 3f, 5f, 7f, 9f, 10f, 12f)
        )
    }
}
