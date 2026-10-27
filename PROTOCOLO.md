# Protocolo de decisões: Seldon Fleet

## 1. Regra de classificação

Avaliada nesta ordem. O primeiro critério que casar define o status.

- **a) INDETERMINADO:** batteryCapacityMah ≤ 0 ou ausente; ou screenOnMinutes ≤ 0;
  ou energyUnit = mWh sem nominalVoltage.
- **b) CRITICO:** drenoRelativo ≥ 12 %/h; ou crashCount7d ≥ 3; ou jankFramesPct ≥ 15.
- **c) ATENCAO:** drenoRelativo ≥ 7 %/h; ou crashCount7d ≥ 1; ou jankFramesPct ≥ 8.
- **d) OK:** nenhuma das condições anteriores.

Derivadas (após normalização para mAh):
- dreno absoluto (mAh/h) = energyMah ÷ (screenOnMinutes ÷ 60)
- dreno relativo (%/h) = (dreno absoluto ÷ batteryCapacityMah) × 100

## 2. Justificativa

**Por que o dreno relativo.** O dreno absoluto (mAh/h) não é comparável entre
aparelhos de capacidades diferentes. O mesmo consumo de 900 mAh/h pesa mais numa
bateria de 3.000 mAh do que numa de 5.000 mAh. Ao normalizar pela capacidade (%/h),
a métrica indica a fração da bateria consumida por hora, e o mesmo limiar passa a
ter o mesmo significado para qualquer modelo.

**Exemplo.** O DEV-0137 (bateria de 4.410 mAh) consumiu 274,8 mAh/h com a tela
ligada, o que corresponde a um dreno relativo de 6,2 %/h. Esse valor fica abaixo
do limiar de ATENCAO por dreno (7 %/h), mas o registro é classificado como ATENCAO
porque crashCount7d = 2 atinge o limiar de 1 falha. Cada sinal basta por si só.

**Origem dos limiares.** Adotei integralmente os limiares de referência da seção 4.2
do desafio: dreno relativo de 12 %/h e 7 %/h; falhas em 7 dias de 3 e 1; quadros
perdidos de 15 % e 8 %. A regra foi registrada neste arquivo antes da implementação
de `domain/`.

## 3. Tratamento dos defeitos

**Defeito 1: unidade de energia inconsistente.** Registros com energyUnit = "mWh"
são convertidos para mAh antes de qualquer cálculo: energyMah = energyConsumed ÷ nominalVoltage.
Se nominalVoltage estiver ausente nesse caso, o registro é INDETERMINADO.
A conversão fica na camada data/ (DeviceMapper), e o domínio só recebe mAh.

Efeito no DEV-0204 (5000 mAh, 186 min de tela ligada, 3773 mWh):
- sem conversão: 3773 ÷ (186/60) = 1216,4 mAh/h → 24,3 %/h → CRITICO (incorreto);
- com conversão: 980 mAh → 316,1 mAh/h → 6,3 %/h, sem outros sinais → OK (correto).

**Defeito 2: registros incompletos.**
- batteryCapacityMah = 0 ou ausente → INDETERMINADO (ex.: DEV-0311).
- screenOnMinutes = 0 → INDETERMINADO.
- Nessas situações, nenhuma divisão é executada. Logo, não há NaN, Infinity
  nem encerramento do app (REQ-03).
- Motivo: manter o registro como INDETERMINADO evita inventar um veredito e
  evita esconder um dispositivo da frota.

## 4. Ameaças à validade

- **Sinais independentes combinados por OR.** Basta um sinal para o status ser
  ATENCAO ou CRITICO. O DEV-0137 tem dreno de 6,2 %/h, abaixo de 7 %/h, mas é
  ATENCAO por crashCount7d = 2. Logo, o status reflete a saúde geral do aparelho,
  e não só a bateria. Um leitor que interprete ATENCAO como problema de bateria
  pode agir sobre a causa errada.
- **Premissa de tela ligada.** O dreno relativo divide todo o consumo registrado
  pelo tempo de tela ligada. Se parte do consumo ocorreu com a tela desligada
  (serviço em primeiro plano, por exemplo), a métrica superestima o dreno e pode
  gerar falso CRITICO.
