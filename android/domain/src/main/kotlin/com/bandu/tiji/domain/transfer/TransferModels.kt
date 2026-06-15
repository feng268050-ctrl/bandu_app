package com.bandu.tiji.domain.transfer

enum class DiscoveryMode {
    PAIR,
    TRUSTED,
}

data class NearbyDevice(
    val discoveryId: String,
    val displayName: String,
    val mode: DiscoveryMode,
)

data class TrustedDevice(
    val deviceId: String,
    val displayName: String,
    val publicKeyFingerprint: String,
)

data class PairingCode(
    val code: String,
    val expiresAtEpochMillis: Long,
)

sealed interface PairingResult {
    data object Success : PairingResult

    data class Failure(val code: TransferFailureCode) : PairingResult
}

data class TransferOfferSummary(
    val sessionId: String,
    val sourceDeviceName: String,
    val totalBytes: Long,
    val totalFiles: Int,
    val totalRecords: Int,
)

data class TransferSummary(
    val sessionId: String,
    val transferredBytes: Long,
    val durationMillis: Long,
)

enum class TransferFailureCode {
    PAIRING_FAILED,
    PAIRING_EXPIRED,
    OFFER_REJECTED,
    CHECKSUM_FAILED,
    NETWORK_INTERRUPTED,
    PROTOCOL_ERROR,
    COMMIT_FAILED,
    CANCELLED,
}
