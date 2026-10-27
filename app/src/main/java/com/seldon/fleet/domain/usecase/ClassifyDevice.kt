package com.seldon.fleet.domain.usecase

import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceStatus

data class Classificacao(val status: DeviceStatus, val motivo: String)

class ClassifyDevice {

    operator fun invoke(device: Device): Classificacao = classify(device)

    fun classify(device: Device): Classificacao {
        // Ordem do protocolo: INDETERMINADO, depois CRITICO, ATENCAO e OK.
        if (device.batteryCapacityMah <= 0 || device.screenOnMinutes <= 0) {
            return Classificacao(DeviceStatus.INDETERMINADO, "capacidade ou tempo de tela zerado")
        }

        // Dreno (%/h) >= limiar  <=>  energia × 6000 >= limiar × capacidade × tempoTela
        // Sem divisão, então a fronteira de 7,00 é exata.
        val capacidadeTempo = device.batteryCapacityMah.toDouble() * device.screenOnMinutes

        fun drenoAtinge(limiarPctPorHora: Double): Boolean =
            device.energyMah * 6000.0 >= limiarPctPorHora * capacidadeTempo

        if (drenoAtinge(12.0) || device.crashCount7d >= 3 || device.jankFramesPct >= 15.0) {
            return Classificacao(DeviceStatus.CRITICO, "sinal crítico")
        }
        if (drenoAtinge(7.0) || device.crashCount7d >= 1 || device.jankFramesPct >= 8.0) {
            return Classificacao(DeviceStatus.ATENCAO, "sinal de atenção")
        }
        return Classificacao(DeviceStatus.OK, "nenhum sinal disparado")
    }
}