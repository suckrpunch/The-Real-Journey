package com.newjourney.core.model

data class Category(
    val id: String,
    val name: String,
    val subcategories: List<Subcategory>,
)

data class Subcategory(val id: String, val name: String)

/**
 * The six launch categories. This list is a stand-in: in Phase 3 categories come from the
 * online catalog, so you can add, rename or reorder them without an app update.
 */
val DefaultCategories: List<Category> = listOf(
    Category(
        "chakras", "Seven Chakras",
        listOf(
            Subcategory("root", "Root Chakra"),
            Subcategory("sacral", "Sacral Chakra"),
            Subcategory("solar-plexus", "Solar Plexus Chakra"),
            Subcategory("heart", "Heart Chakra"),
            Subcategory("throat", "Throat Chakra"),
            Subcategory("third-eye", "Third Eye Chakra"),
            Subcategory("crown", "Crown Chakra"),
        ),
    ),
    Category(
        "sleep", "Sleep",
        listOf(
            Subcategory("deep-sleep", "Deep sleep"),
            Subcategory("before-bed", "Relaxing before bedtime"),
            Subcategory("sleep-sounds", "Sleep sounds"),
            Subcategory("night-meditation", "Night meditation"),
        ),
    ),
    Category(
        "relaxation", "Relaxation",
        listOf(
            Subcategory("stress", "Stress relief"),
            Subcategory("anxiety", "Anxiety relaxation"),
            Subcategory("emotional-release", "Emotional release"),
            Subcategory("inner-peace", "Inner peace"),
        ),
    ),
    Category(
        "focus", "Focus & Productivity",
        listOf(
            Subcategory("deep-focus", "Deep focus"),
            Subcategory("concentration", "Concentration"),
            Subcategory("clarity", "Mental clarity"),
            Subcategory("creative", "Creative thinking"),
        ),
    ),
    Category(
        "deep", "Deep Meditation",
        listOf(
            Subcategory("consciousness", "Deep consciousness"),
            Subcategory("exploration", "Inner exploration"),
            Subcategory("mindfulness", "Mindfulness"),
            Subcategory("spiritual", "Spiritual meditation"),
        ),
    ),
    Category(
        "healing", "Healing & Energy",
        listOf(
            Subcategory("balance", "Emotional balance"),
            Subcategory("self-love", "Self-love"),
            Subcategory("positive-energy", "Positive energy"),
            Subcategory("inner-healing", "Inner healing"),
        ),
    ),
)
