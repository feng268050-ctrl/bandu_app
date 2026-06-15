package com.bandu.tiji.ai.api.parser

internal object TagExtractor {
    fun extractTag(text: String, tagName: String): String? {
        val startTag = "<$tagName>"
        val endTag = "</$tagName>"
        val startIndex = text.indexOf(startTag, ignoreCase = false)
        if (startIndex < 0) return null
        val contentStart = startIndex + startTag.length
        val endIndex = text.indexOf(endTag, contentStart, ignoreCase = false)
        if (endIndex < 0) return null
        return text.substring(contentStart, endIndex).trim()
    }
}
