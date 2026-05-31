package com.example.data

class DictionaryManager(private val geminiService: GeminiService) {
    // Offline quick entries for academic and office terms
    private val offlineDict = mapOf(
        "abstract" to DictionaryEntry(
            word = "Abstract",
            explanation = "A concise summary of a research paper, article, or document outline.",
            translations = mapOf("ar" to "ملخص / خلاصة المستند", "de" to "Zusammenfassung")
        ),
        "introduction" to DictionaryEntry(
            word = "Introduction",
            explanation = "The opening section of a document that introduces the core topic, objectives, and scope.",
            translations = mapOf("ar" to "مقدمة", "de" to "Einleitung")
        ),
        "conclusion" to DictionaryEntry(
            word = "Conclusion",
            explanation = "The final part of a research or business document summarizing the main findings and decisions.",
            translations = mapOf("ar" to "خاتمة / خلاصة القول", "de" to "Schlussfolgerung")
        ),
        "methodology" to DictionaryEntry(
            word = "Methodology",
            explanation = "The theoretical and systematic analysis of the methods or scientific principles applied to a field of study.",
            translations = mapOf("ar" to "منهجية البحث", "de" to "Methodik")
        ),
        "results" to DictionaryEntry(
            word = "Results",
            explanation = "The direct outcomes, empirical findings, or output of an analysis, paper, or research.",
            translations = mapOf("ar" to "النتائج", "de" to "Ergebnisse")
        ),
        "bibliography" to DictionaryEntry(
            word = "Bibliography",
            explanation = "A list of books, articles, and other sources used as academic references in a document.",
            translations = mapOf("ar" to "قائمة المراجع", "de" to "Literaturverzeichnis")
        ),
        "appendix" to DictionaryEntry(
            word = "Appendix",
            explanation = "Supplementary material attached at the end of a document containing tables, datasets, or extra explanations.",
            translations = mapOf("ar" to "ملحق / ضميمية", "de" to "Anhang")
        )
    )

    fun getOfflineEntry(query: String): DictionaryEntry? {
        return offlineDict[query.lowercase().trim()]
    }

    suspend fun lookupWord(word: String, customKey: String?): DictionaryEntry {
        val wordTrimmed = word.trim()
        val offline = getOfflineEntry(wordTrimmed)
        if (offline != null) return offline

        val systemPrompt = "You are a professional dictionary database. Describe the term concisely and translate it into Arabic and German."
        val prompt = "Provide a clean definition of the word '$wordTrimmed'. Please format your response to include a 1-sentence definition in English, a direct translation labeled 'ar: [translation]' in Arabic, and a direct translation labeled 'de: [translation]' in German."
        
        val response = geminiService.getAiResponse(prompt, userCustomKey = customKey, systemPrompt = systemPrompt)
        
        val arTranslation = parsePrefix(response, "ar:") ?: "ترجمة بالذكاء الاصطناعي"
        val deTranslation = parsePrefix(response, "de:") ?: "KI-Übersetzung"

        return DictionaryEntry(
            word = wordTrimmed,
            explanation = response,
            translations = mapOf("ar" to arTranslation, "de" to deTranslation)
        )
    }

    private fun parsePrefix(text: String, prefix: String): String? {
        return text.lines()
            .firstOrNull { it.contains(prefix, ignoreCase = true) }
            ?.substringAfter(prefix)
            ?.trim()
    }
}

data class DictionaryEntry(
    val word: String,
    val explanation: String,
    val translations: Map<String, String>
)
