package com.seldon.fleet.domain.model

data class Device(
    val id: String,
    val batteryCapacityMah: Int,
    val screenOnMinutes: Int,
    val energyMah: Double,
    val jankFramesPct: Double,
    val crashCount7d: Int,
)