package com.bandu.tiji.core.common.id

import java.util.UUID

interface UuidGenerator {
    fun newUuid(): String
}

class RandomUuidGenerator : UuidGenerator {
    override fun newUuid(): String = UUID.randomUUID().toString()
}
