package com.newjourney.core.catalog

import com.newjourney.core.model.Catalog

/**
 * Category and sub-category names in the app's other languages, by id. The catalog itself is in
 * English; a name that is missing here (for example a new category added online) stays English.
 * Track titles are names of the music and are not translated.
 */
internal object CatalogNames {

    fun localize(catalog: Catalog, language: String): Catalog {
        val names = NAMES[language] ?: return catalog
        return catalog.copy(
            categories = catalog.categories.map { category ->
                category.copy(
                    name = names[category.id] ?: category.name,
                    subcategories = category.subcategories.map { sub -> sub.copy(name = names["${category.id}/${sub.id}"] ?: sub.name) },
                )
            },
        )
    }

    private val NAMES: Map<String, Map<String, String>> = mapOf(
        "nl" to mapOf(
            "chakras" to "Zeven chakra's",
            "chakras/root" to "Wortelchakra", "chakras/sacral" to "Sacraalchakra", "chakras/solar-plexus" to "Zonnevlechtchakra",
            "chakras/heart" to "Hartchakra", "chakras/throat" to "Keelchakra", "chakras/third-eye" to "Derde-oogchakra", "chakras/crown" to "Kruinchakra",
            "sleep" to "Slaap",
            "sleep/deep-sleep" to "Diepe slaap", "sleep/before-bed" to "Ontspannen voor het slapengaan",
            "sleep/sleep-sounds" to "Slaapgeluiden", "sleep/night-meditation" to "Avondmeditatie",
            "relaxation" to "Ontspanning",
            "relaxation/stress" to "Stressverlichting", "relaxation/anxiety" to "Ontspanning bij angst",
            "relaxation/emotional-release" to "Emotioneel loslaten", "relaxation/inner-peace" to "Innerlijke rust",
            "focus" to "Focus & productiviteit",
            "focus/deep-focus" to "Diepe focus", "focus/concentration" to "Concentratie",
            "focus/clarity" to "Mentale helderheid", "focus/creative" to "Creatief denken",
            "deep" to "Diepe meditatie",
            "deep/consciousness" to "Diep bewustzijn", "deep/exploration" to "Innerlijke verkenning",
            "deep/mindfulness" to "Mindfulness", "deep/spiritual" to "Spirituele meditatie",
            "healing" to "Heling & energie",
            "healing/balance" to "Emotionele balans", "healing/self-love" to "Zelfliefde",
            "healing/positive-energy" to "Positieve energie", "healing/inner-healing" to "Innerlijke heling",
        ),
        "de" to mapOf(
            "chakras" to "Sieben Chakren",
            "chakras/root" to "Wurzelchakra", "chakras/sacral" to "Sakralchakra", "chakras/solar-plexus" to "Solarplexuschakra",
            "chakras/heart" to "Herzchakra", "chakras/throat" to "Halschakra", "chakras/third-eye" to "Stirnchakra", "chakras/crown" to "Kronenchakra",
            "sleep" to "Schlaf",
            "sleep/deep-sleep" to "Tiefschlaf", "sleep/before-bed" to "Entspannen vor dem Schlafen",
            "sleep/sleep-sounds" to "Schlafklänge", "sleep/night-meditation" to "Abendmeditation",
            "relaxation" to "Entspannung",
            "relaxation/stress" to "Stressabbau", "relaxation/anxiety" to "Entspannung bei Angst",
            "relaxation/emotional-release" to "Emotionales Loslassen", "relaxation/inner-peace" to "Innerer Frieden",
            "focus" to "Fokus & Produktivität",
            "focus/deep-focus" to "Tiefer Fokus", "focus/concentration" to "Konzentration",
            "focus/clarity" to "Geistige Klarheit", "focus/creative" to "Kreatives Denken",
            "deep" to "Tiefe Meditation",
            "deep/consciousness" to "Tiefes Bewusstsein", "deep/exploration" to "Innere Erkundung",
            "deep/mindfulness" to "Achtsamkeit", "deep/spiritual" to "Spirituelle Meditation",
            "healing" to "Heilung & Energie",
            "healing/balance" to "Emotionale Balance", "healing/self-love" to "Selbstliebe",
            "healing/positive-energy" to "Positive Energie", "healing/inner-healing" to "Innere Heilung",
        ),
        "fr" to mapOf(
            "chakras" to "Les sept chakras",
            "chakras/root" to "Chakra racine", "chakras/sacral" to "Chakra sacré", "chakras/solar-plexus" to "Chakra du plexus solaire",
            "chakras/heart" to "Chakra du cœur", "chakras/throat" to "Chakra de la gorge", "chakras/third-eye" to "Chakra du troisième œil", "chakras/crown" to "Chakra couronne",
            "sleep" to "Sommeil",
            "sleep/deep-sleep" to "Sommeil profond", "sleep/before-bed" to "Se détendre avant le coucher",
            "sleep/sleep-sounds" to "Sons pour dormir", "sleep/night-meditation" to "Méditation du soir",
            "relaxation" to "Relaxation",
            "relaxation/stress" to "Soulager le stress", "relaxation/anxiety" to "Apaiser l'anxiété",
            "relaxation/emotional-release" to "Libération émotionnelle", "relaxation/inner-peace" to "Paix intérieure",
            "focus" to "Concentration & productivité",
            "focus/deep-focus" to "Concentration profonde", "focus/concentration" to "Concentration",
            "focus/clarity" to "Clarté mentale", "focus/creative" to "Pensée créative",
            "deep" to "Méditation profonde",
            "deep/consciousness" to "Conscience profonde", "deep/exploration" to "Exploration intérieure",
            "deep/mindfulness" to "Pleine conscience", "deep/spiritual" to "Méditation spirituelle",
            "healing" to "Guérison & énergie",
            "healing/balance" to "Équilibre émotionnel", "healing/self-love" to "Amour de soi",
            "healing/positive-energy" to "Énergie positive", "healing/inner-healing" to "Guérison intérieure",
        ),
        "es" to mapOf(
            "chakras" to "Los siete chakras",
            "chakras/root" to "Chakra raíz", "chakras/sacral" to "Chakra sacro", "chakras/solar-plexus" to "Chakra del plexo solar",
            "chakras/heart" to "Chakra del corazón", "chakras/throat" to "Chakra de la garganta", "chakras/third-eye" to "Chakra del tercer ojo", "chakras/crown" to "Chakra corona",
            "sleep" to "Sueño",
            "sleep/deep-sleep" to "Sueño profundo", "sleep/before-bed" to "Relajarse antes de dormir",
            "sleep/sleep-sounds" to "Sonidos para dormir", "sleep/night-meditation" to "Meditación nocturna",
            "relaxation" to "Relajación",
            "relaxation/stress" to "Alivio del estrés", "relaxation/anxiety" to "Calmar la ansiedad",
            "relaxation/emotional-release" to "Liberación emocional", "relaxation/inner-peace" to "Paz interior",
            "focus" to "Concentración y productividad",
            "focus/deep-focus" to "Concentración profunda", "focus/concentration" to "Concentración",
            "focus/clarity" to "Claridad mental", "focus/creative" to "Pensamiento creativo",
            "deep" to "Meditación profunda",
            "deep/consciousness" to "Conciencia profunda", "deep/exploration" to "Exploración interior",
            "deep/mindfulness" to "Atención plena", "deep/spiritual" to "Meditación espiritual",
            "healing" to "Sanación y energía",
            "healing/balance" to "Equilibrio emocional", "healing/self-love" to "Amor propio",
            "healing/positive-energy" to "Energía positiva", "healing/inner-healing" to "Sanación interior",
        ),
    )
}
