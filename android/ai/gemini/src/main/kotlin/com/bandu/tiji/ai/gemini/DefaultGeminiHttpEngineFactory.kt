package com.bandu.tiji.ai.gemini

import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.core.common.logging.AppLogger
import com.bandu.tiji.core.network.AiHttpClientFactory
import com.bandu.tiji.core.network.AiHttpOperation
import com.bandu.tiji.core.network.HttpEngine
import com.bandu.tiji.core.network.endpoint.EndpointPolicy

class DefaultGeminiHttpEngineFactory(
    private val endpointPolicy: EndpointPolicy,
    private val logger: AppLogger? = null,
) : GeminiHttpEngineFactory {
    override fun create(
        configuration: ResolvedAiConfiguration,
        operation: AiHttpOperation,
    ): HttpEngine = HttpEngine(
        AiHttpClientFactory.create(
            operation = operation,
            endpointPolicy = endpointPolicy,
            allowPrivateCleartext = { configuration.allowPrivateCleartext },
            logger = logger,
        ),
    )
}
