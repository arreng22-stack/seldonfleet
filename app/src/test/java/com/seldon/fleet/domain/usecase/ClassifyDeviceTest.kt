package com.seldon.fleet.domain.usecase

import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifyDeviceTest {

    private val classify = ClassifyDevice()

    // Capacidade 1000 mAh e 60 min de tela: energyMah igual ao dreno relativo em %/h.
    private fun device(
        capacity: Int = 1000,
        screenOnMinutes: Int = 60,
        energyMah: Double = 0.0,
        jankPct: Double = 0.0,
        crashes: Int = 0,
    ) = Device(
        id = "TESTE",
        batteryCapacityMah = capacity,
        screenOnMinutes = screenOnMinutes,
        energyMah = energyMah,
        jankFramesPct = jankPct,
        crashCount7d = crashes,
    )

    @Test fun dreno_exatamente_7_e_ATENCAO() =
        assertEquals(DeviceStatus.ATENCAO, classify(device(energyMah = 70.0)).status)

    @Test fun dreno_6_99_e_OK() =
        assertEquals(DeviceStatus.OK, classify(device(energyMah = 69.9)).status)

    @Test fun dreno_exatamente_12_e_CRITICO() =
        assertEquals(DeviceStatus.CRITICO, classify(device(energyMah = 120.0)).status)

    @Test fun dreno_11_99_e_ATENCAO() =
        assertEquals(DeviceStatus.ATENCAO, classify(device(energyMah = 119.9)).status)

    @Test fun um_crash_e_ATENCAO() =
        assertEquals(DeviceStatus.ATENCAO, classify(device(crashes = 1)).status)

    @Test fun tres_crashes_com_dreno_baixo_e_CRITICO() =
        assertEquals(DeviceStatus.CRITICO, classify(device(energyMah = 10.0, crashes = 3)).status)

    @Test fun jank_exatamente_8_e_ATENCAO() =
        assertEquals(DeviceStatus.ATENCAO, classify(device(jankPct = 8.0)).status)

    @Test fun jank_exatamente_15_e_CRITICO() =
        assertEquals(DeviceStatus.CRITICO, classify(device(jankPct = 15.0)).status)

    @Test fun capacidade_zero_e_INDETERMINADO() =
        assertEquals(DeviceStatus.INDETERMINADO, classify(device(capacity = 0, energyMah = 500.0)).status)

    @Test fun tela_zero_e_INDETERMINADO() =
        assertEquals(DeviceStatus.INDETERMINADO, classify(device(screenOnMinutes = 0, energyMah = 500.0)).status)

    @Test fun indeterminado_vence_sinal_critico() =
        assertEquals(DeviceStatus.INDETERMINADO, classify(device(capacity = 0, crashes = 5)).status)

    @Test fun critico_vence_atencao_simultanea() =
        assertEquals(DeviceStatus.CRITICO, classify(device(energyMah = 70.0, crashes = 3)).status)
}