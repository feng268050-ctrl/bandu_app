package com.bandu.tiji.core.testing.id

import com.bandu.tiji.core.common.id.UuidGenerator

class FixedUuidGenerator(
    private val value: String,
) : UuidGenerator {
    init {
        require(value.isNotBlank()) { "value must not be blank" }
    }

    override fun newUuid(): String = value
}
