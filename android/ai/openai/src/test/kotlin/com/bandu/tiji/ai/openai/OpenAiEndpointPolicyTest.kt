package com.bandu.tiji.ai.openai

import com.bandu.tiji.ai.api.config.ProviderValidation
import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.network.endpoint.DefaultEndpointPolicy
import com.bandu.tiji.core.network.endpoint.HostResolver
import com.google.common.truth.Truth.assertThat
import java.net.InetAddress
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class OpenAiEndpointPolicyTest {
    @Test
    fun `cleartext private endpoint is rejected until user confirms risk`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"choices":[]}"""))
            val provider = provider(DefaultEndpointPolicy())

            val rejected = provider.validate(
                configuration(server.url("/v1").toString(), allowPrivateCleartext = false),
            )
            val allowed = provider.validate(
                configuration(server.url("/v1").toString(), allowPrivateCleartext = true),
            )

            assertThat(rejected).isEqualTo(ProviderValidation.Failure("endpoint_rejected"))
            assertThat(allowed).isEqualTo(ProviderValidation.Success)
            assertThat(server.requestCount).isEqualTo(1)
        }
    }

    @Test
    fun `confirmed public cleartext endpoint is rejected before connection`() = runTest {
        val policy = DefaultEndpointPolicy(
            hostResolver = HostResolver {
                listOf(InetAddress.getByName("203.0.113.10"))
            },
        )

        val result = provider(policy).validate(
            configuration("http://public.example/v1", allowPrivateCleartext = true),
        )

        assertThat(result).isEqualTo(ProviderValidation.Failure("endpoint_rejected"))
    }

    @Test
    fun `mixed private and public DNS answers are rejected`() = runTest {
        val policy = DefaultEndpointPolicy(
            hostResolver = HostResolver {
                listOf(
                    InetAddress.getByName("192.168.1.20"),
                    InetAddress.getByName("198.51.100.20"),
                )
            },
        )

        val result = provider(policy).validate(
            configuration("http://mixed.example/v1", allowPrivateCleartext = true),
        )

        assertThat(result).isEqualTo(ProviderValidation.Failure("endpoint_rejected"))
    }

    @Test
    fun `redirect from private endpoint to public cleartext is rejected`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader(
                        "Location",
                        "http://redirect.example/v1/chat/completions",
                    ),
            )
            val policy = DefaultEndpointPolicy(
                hostResolver = HostResolver { host ->
                    when (host) {
                        "redirect.example" -> listOf(
                            InetAddress.getByName("203.0.113.30"),
                        )
                        else -> InetAddress.getAllByName(host).toList()
                    }
                },
            )

            val result = provider(policy).validate(
                configuration(server.url("/v1").toString(), allowPrivateCleartext = true),
            )

            assertThat(result).isEqualTo(ProviderValidation.Failure("endpoint_rejected"))
            assertThat(server.requestCount).isEqualTo(1)
        }
    }

    private fun provider(policy: DefaultEndpointPolicy) =
        OpenAiCompatibleProvider(
            httpEngineFactory = DefaultOpenAiHttpEngineFactory(policy),
        )

    private fun configuration(
        baseUrl: String,
        allowPrivateCleartext: Boolean,
    ) = ResolvedAiConfiguration(
        id = "openai",
        displayName = "OpenAI compatible",
        providerType = AiProviderType.OPENAI_COMPATIBLE,
        baseUrl = baseUrl.removeSuffix("/"),
        apiKey = "test-secret",
        analysisModel = "vision-model",
        tutorModel = "tutor-model",
        allowPrivateCleartext = allowPrivateCleartext,
    )
}
