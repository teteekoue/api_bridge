package com.ialocalbridge.utils

object FileMessageBuilder {
    fun build(filename: String, url: String, question: String? = null): String {
        val baseMessage = "[FICHIER: $filename] [LIEN: $url]"
        return if (!question.isNullOrBlank()) {
            "$baseMessage\n\nQuestion: $question"
        } else {
            baseMessage
        }
    }
}
