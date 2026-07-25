package com.s14p31s207.android.data.comm.tcp

data class WifiConnectionDto(
    val ip: String,
    val port: Int,
    val deviceName: String // 나중에 장비 구분을 위해 이름을 붙일 수도 있음.
)