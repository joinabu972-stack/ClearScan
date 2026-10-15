package com.example.data.model

enum class FilterType(val displayName: String, val description: String) {
    MAGIC("Magic Color", "Enhances colors and removes paper shadows"),
    BLACK_WHITE("B & W", "High-contrast document mode for crisp black & white text"),
    GRAYSCALE("Grayscale", "Smooth professional grayscale scan"),
    SHARP("Sharp Clear", "High clarity and text edge sharpening"),
    VINTAGE("Warm Paper", "Soft warm tone for notes and books"),
    ORIGINAL("Original", "Natural camera capture without filters")
}
