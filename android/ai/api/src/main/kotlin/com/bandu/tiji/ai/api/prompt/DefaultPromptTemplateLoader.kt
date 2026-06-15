package com.bandu.tiji.ai.api.prompt

interface DefaultPromptTemplateLoader {
    fun schemaVersion(): Int

    fun loadDefault(type: PromptType): String
}
