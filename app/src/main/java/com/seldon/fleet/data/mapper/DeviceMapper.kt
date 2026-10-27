package com.seldon.fleet.data.mapper

object DeviceMapper {
    // Energia em mWh para mAh, usando a tensão nominal da bateria.
    fun mwhToMah(mwh: Double, tensaoV: Double = 3.85): Double = mwh / tensaoV
}