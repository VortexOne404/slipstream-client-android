package com.kmk.slipstream.vpn.data

data class SlipstreamConfig(
    val id: String,
    val name: String,
    val domain: String,
    val socksAuthEnabled: Boolean,
    val username: String,
    val password: String
)
