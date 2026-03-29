// ============================================================
//  ARQUITECTURA DEL ESTADO
// ============================================================
// El estado central (state) contiene TODO lo que la app necesita.
// Se divide en:
//   - Configuración de modos
//   - Datos del viaje activo (trip*)
//   - Acumuladores por modo (modeData[])
//   - Bloque de combustible (fuel*)
//   - Finanzas globales
//   - Historial de viajes (trips[]) para CSV
//   - Estado visual/mapa
// ============================================================

const state = {
    // --- Modos ---
    mode: 0,  // 0=Personal, 1=Uber con pasajero, 2=Uber sin pasajero
    modes: [
        { name: 'Personal',          icon: '\u{1F698}', class: 'personal' },
        { name: 'Uber con Pasajero', icon: '\u{1F7E2}', class: 'uber-passenger' },
        { name: 'Uber sin Pasajero', icon: '\u{1F7E1}', class: 'uber-empty' }
    ],

    // Acumuladores separados por modo (km totales y viajes)
    modeData: [
        { km: 0, trips: 0, income: 0, cost: 0 },   // Personal
        { km: 0, trips: 0, income: 0, cost: 0 },   // Uber con pasajero
        { km: 0, trips: 0, income: 0, cost: 0 }    // Uber sin pasajero
    ],

    // --- Viaje activo ---
    tripActive: false,
    speed: 0,
    targetSpeed: 0,           // Velocidad objetivo para transiciones suaves
    tripDistance: 0,           // km del viaje actual
    dayKm: 0,                 // km totales del día (todos los modos)
    tripTimeSeconds: 0,
    stoppedTimeSeconds: 0,
    consecutiveStoppedSec: 0, // segundos seguidos detenido (para detectar parada)
    tripStartTime: null,
    simulationInterval: null,
    tripModeAtStart: 0,       // modo con el que se inició el viaje

    // --- Combustible ---
    fuelBlockKm: 100,         // meta del bloque
    fuelCurrent: 0,           // km recorridos en el bloque actual
    fuelCostPerBlock: 0,      // costo del bloque actual ($)
    fuelAlertShown: false,    // si ya se mostró la alerta de 100 km
    fuelBlocks: [],           // historial de bloques {cost, km}

    // --- Finanzas ---
    uberIncome: 0,            // ingresos totales Uber del día
    tripCost: 0,              // costo estimado del viaje actual
    totalCost: 0,             // costo total del día

    // --- Mapa ---
    routePoints: [],
    carX: 50,
    carY: 45,
    lastAngle: 0,

    // --- Historial para CSV ---
    trips: [],

    // --- UI ---
    modalCallback: null,
    toastTimer: null
};

// ============================================================
//  REFERENCIAS DOM
// ============================================================
const dom = {};
function cacheDom() {
    const ids = [
        'speed', 'tripDistance', 'dayKm', 'tripTime', 'stoppedTime',
        'stoppedPercent', 'stoppedCard', 'fuelBar', 'fuelBarText',
        'fuelCurrent', 'fuelExcess', 'fuelCostPerKm', 'fuelBlockCost',
        'fuelCard', 'fuelAlert', 'uberIncome', 'tripCost', 'estimatedCost',
        'profit', 'modeBar', 'modeLabel', 'modeIcon', 'modeStats',
        'btnStart', 'btnStop', 'btnMode', 'carMarker', 'carDot', 'carPulse',
        'routePath', 'modalOverlay', 'modalTitle', 'modalMessage',
        'modalInput', 'modalOk', 'toast', 'speedIndicatorFill',
        'mapSpeed', 'mapSpeedOverlay',
        'sumPersonalKm', 'sumUberPassKm', 'sumUberPassTrips', 'sumUberEmptyKm'
    ];
    ids.forEach(id => { dom[id] = document.getElementById(id); });
}

// ============================================================
//  CÁLCULO DE COSTO POR KM
// ============================================================
// Se basa en el costo del último bloque de combustible registrado.
// costoPorKm = fuelCostPerBlock / fuelBlockKm
// Si no se ha registrado bloque, costoPorKm = 0 (no se puede estimar).
function getCostPerKm() {
    if (state.fuelCostPerBlock <= 0) return 0;
    return state.fuelCostPerBlock / state.fuelBlockKm;
}

// ============================================================
//  SIMULACIÓN DE VELOCIDAD
// ============================================================
// La velocidad no salta instantáneamente. Se define una "targetSpeed"
// y la velocidad real converge hacia ella con aceleración/frenado.
// Cada ~5 segundos se elige un nuevo targetSpeed según probabilidad
// que simula tráfico urbano.

let tickCounter = 0;

function pickTargetSpeed() {
    const r = Math.random();
    if (r < 0.12)      return 0;                             // Semáforo/parada
    if (r < 0.25)      return Math.floor(Math.random() * 15) + 5;   // Muy lento
    if (r < 0.55)      return Math.floor(Math.random() * 20) + 20;  // Ciudad
    if (r < 0.80)      return Math.floor(Math.random() * 20) + 40;  // Avenida
    return Math.floor(Math.random() * 20) + 55;                     // Rápido
}

function smoothSpeed() {
    const diff = state.targetSpeed - state.speed;
    if (Math.abs(diff) <= 2) {
        state.speed = state.targetSpeed;
    } else if (diff > 0) {
        // Acelerando: +3~6 km/h por segundo
        state.speed += Math.min(diff, Math.floor(Math.random() * 4) + 3);
    } else {
        // Frenando: -5~10 km/h por segundo (frena más rápido)
        state.speed += Math.max(diff, -(Math.floor(Math.random() * 6) + 5));
    }
    state.speed = Math.max(0, state.speed);
}

// ============================================================
//  DETECCIÓN DE VEHÍCULO DETENIDO
// ============================================================
// Velocidad <= 2 km/h = detenido.
// Se acumula stoppedTimeSeconds y consecutiveStoppedSec.
// Después de 5 seg seguidos detenido se resalta visualmente.

function isStopped() {
    return state.speed <= 2;
}

// ============================================================
//  SIMULACIÓN PRINCIPAL (cada segundo)
// ============================================================
function simulateTick() {
    tickCounter++;

    // Cada 4-7 seg, nuevo objetivo de velocidad
    if (tickCounter % (Math.floor(Math.random() * 4) + 4) === 0) {
        state.targetSpeed = pickTargetSpeed();
    }

    smoothSpeed();

    state.tripTimeSeconds++;

    // Detección detenido
    if (isStopped()) {
        state.stoppedTimeSeconds++;
        state.consecutiveStoppedSec++;
    } else {
        state.consecutiveStoppedSec = 0;
    }

    // Distancia recorrida este segundo (km)
    const distKm = state.speed / 3600;

    if (distKm > 0) {
        state.tripDistance += distKm;
        state.dayKm += distKm;
        state.fuelCurrent += distKm;

        // Acumular en el modo actual
        state.modeData[state.mode].km += distKm;

        // Costo del viaje actual
        state.tripCost = state.tripDistance * getCostPerKm();

        // Costo total del día
        state.totalCost = state.dayKm * getCostPerKm();
    }

    // Verificar alerta de combustible (al llegar a 100 km)
    checkFuelAlert();

    // Mover carro en mapa
    moveCarOnMap();

    // Actualizar pantalla
    updateAllDisplays();
}

// ============================================================
//  ALERTA DE COMBUSTIBLE
// ============================================================
function checkFuelAlert() {
    if (state.fuelCurrent >= state.fuelBlockKm && !state.fuelAlertShown) {
        state.fuelAlertShown = true;
        dom.fuelAlert.classList.add('visible');
    }
}

function dismissFuelAlert() {
    dom.fuelAlert.classList.remove('visible');
}

// ============================================================
//  MOVIMIENTO DEL CARRO EN EL MAPA
// ============================================================
function moveCarOnMap() {
    if (isStopped()) return;

    const angle = (Math.random() - 0.5) * 0.6 + state.lastAngle * 0.75;
    state.lastAngle = angle;

    const step = state.speed * 0.012;
    state.carX += Math.cos(angle) * step;
    state.carY += Math.sin(angle) * step;

    state.carX = Math.max(6, Math.min(94, state.carX));
    state.carY = Math.max(6, Math.min(94, state.carY));

    dom.carMarker.style.left = state.carX + '%';
    dom.carMarker.style.top = state.carY + '%';

    state.routePoints.push({ x: state.carX, y: state.carY });
    if (state.routePoints.length > 300) state.routePoints.shift();

    const pts = state.routePoints.map(p => p.x + ',' + p.y).join(' ');
    dom.routePath.setAttribute('points', pts);
}

// ============================================================
//  ACTUALIZACIÓN DE PANTALLA (en tiempo real)
// ============================================================
function updateAllDisplays() {
    // Velocidad
    dom.speed.textContent = state.speed;
    dom.mapSpeed.textContent = state.speed;

    // Color de velocidad en speed card
    const speedCard = dom.speed.closest('.speed-card');
    speedCard.classList.remove('stopped', 'slow', 'fast');
    if (state.tripActive) {
        if (isStopped())           speedCard.classList.add('stopped');
        else if (state.speed < 20) speedCard.classList.add('slow');
        else if (state.speed > 50) speedCard.classList.add('fast');
    }

    // Barra indicadora de velocidad (max 80 km/h para la barra)
    const speedPct = Math.min((state.speed / 80) * 100, 100);
    dom.speedIndicatorFill.style.width = speedPct + '%';
    if (isStopped())           dom.speedIndicatorFill.style.background = '#ef5350';
    else if (state.speed < 20) dom.speedIndicatorFill.style.background = '#ffb74d';
    else if (state.speed > 50) dom.speedIndicatorFill.style.background = '#4caf50';
    else                       dom.speedIndicatorFill.style.background = '#00b4d8';

    // Car dot color
    dom.carDot.classList.toggle('stopped', state.tripActive && isStopped());
    dom.carPulse.classList.toggle('stopped', state.tripActive && isStopped());

    // Detenido visual
    const stoppedCard = dom.stoppedCard;
    stoppedCard.classList.toggle('stopped-highlight',
        state.tripActive && state.consecutiveStoppedSec >= 5);

    // Distancias
    dom.tripDistance.textContent = state.tripDistance.toFixed(2);
    dom.dayKm.textContent = state.dayKm.toFixed(2);

    // Tiempos
    dom.tripTime.textContent = formatTime(state.tripTimeSeconds);
    dom.stoppedTime.textContent = formatTime(state.stoppedTimeSeconds);
    const stoppedPct = state.tripTimeSeconds > 0
        ? Math.round((state.stoppedTimeSeconds / state.tripTimeSeconds) * 100)
        : 0;
    dom.stoppedPercent.textContent = stoppedPct + '%';

    // Combustible
    const fuelPct = Math.min((state.fuelCurrent / state.fuelBlockKm) * 100, 100);
    const fuelExcess = Math.max(0, state.fuelCurrent - state.fuelBlockKm);
    dom.fuelBar.style.width = fuelPct + '%';
    dom.fuelBarText.textContent = state.fuelCurrent.toFixed(1) + ' / ' + state.fuelBlockKm + ' km';
    dom.fuelCurrent.textContent = state.fuelCurrent.toFixed(2);
    dom.fuelExcess.textContent = (fuelExcess * 1000).toFixed(0);
    dom.fuelBlockCost.textContent = '$' + state.fuelCostPerBlock.toFixed(2);
    dom.fuelCostPerKm.textContent = '$' + getCostPerKm().toFixed(3);

    dom.fuelBar.classList.remove('warning', 'danger');
    dom.fuelCard.classList.remove('fuel-warning', 'fuel-danger');
    if (fuelPct >= 100) {
        dom.fuelBar.classList.add('danger');
        dom.fuelCard.classList.add('fuel-danger');
    } else if (fuelPct >= 80) {
        dom.fuelBar.classList.add('warning');
        dom.fuelCard.classList.add('fuel-warning');
    }

    // Finanzas
    dom.uberIncome.textContent = '$' + state.uberIncome.toFixed(2);
    dom.tripCost.textContent = '$' + state.tripCost.toFixed(2);
    dom.estimatedCost.textContent = '$' + state.totalCost.toFixed(2);

    const profit = state.uberIncome - state.totalCost;
    dom.profit.textContent = (profit >= 0 ? '+$' : '-$') + Math.abs(profit).toFixed(2);
    dom.profit.className = 'finance-value ' + (profit >= 0 ? 'green' : 'red');

    // Stats en barra de modo
    const md = state.modeData[state.mode];
    dom.modeStats.textContent = md.km.toFixed(1) + ' km | ' + md.trips + ' viajes';

    // Resumen por modo
    dom.sumPersonalKm.textContent = state.modeData[0].km.toFixed(1);
    dom.sumUberPassKm.textContent = state.modeData[1].km.toFixed(1);
    dom.sumUberPassTrips.textContent = state.modeData[1].trips;
    dom.sumUberEmptyKm.textContent = state.modeData[2].km.toFixed(1);
}

function updateModeDisplay() {
    const m = state.modes[state.mode];
    dom.modeLabel.textContent = m.name;
    dom.modeIcon.textContent = m.icon;
    dom.modeBar.className = 'mode-bar ' + m.class;
}

function formatTime(sec) {
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    if (h > 0) return h + ':' + String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
    return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
}

// ============================================================
//  ACCIONES DE VIAJE
// ============================================================
function startTrip() {
    if (state.tripActive) return;

    state.tripActive = true;
    state.tripDistance = 0;
    state.tripCost = 0;
    state.tripTimeSeconds = 0;
    state.stoppedTimeSeconds = 0;
    state.consecutiveStoppedSec = 0;
    state.tripStartTime = Date.now();
    state.tripModeAtStart = state.mode;
    state.speed = 0;
    state.targetSpeed = pickTargetSpeed();
    state.routePoints = [{ x: state.carX, y: state.carY }];
    tickCounter = 0;

    dom.btnStart.disabled = true;
    dom.btnStop.disabled = false;
    dom.btnMode.disabled = true;  // No cambiar modo durante viaje

    showToast('Viaje iniciado - ' + state.modes[state.mode].name);

    state.simulationInterval = setInterval(simulateTick, 1000);
}

function endTrip() {
    if (!state.tripActive) return;

    clearInterval(state.simulationInterval);
    state.tripActive = false;
    state.speed = 0;
    state.targetSpeed = 0;
    state.consecutiveStoppedSec = 0;

    // Registrar viaje en el modo
    state.modeData[state.tripModeAtStart].trips++;

    dom.btnStart.disabled = false;
    dom.btnStop.disabled = true;
    dom.btnMode.disabled = false;

    updateAllDisplays();

    // Si es Uber con pasajero, pedir ingreso
    if (state.tripModeAtStart === 1) {
        openModal(
            'Ingreso del Pasajero',
            'Ingresa el monto cobrado por este viaje (' +
                state.tripDistance.toFixed(2) + ' km, costo estimado: $' +
                state.tripCost.toFixed(2) + '):',
            function (value) {
                const amount = parseFloat(value);
                if (!isNaN(amount) && amount > 0) {
                    state.uberIncome += amount;
                    state.modeData[1].income += amount;
                    const tripProfit = amount - state.tripCost;
                    saveTripRecord(amount, tripProfit);
                    updateAllDisplays();
                    const sign = tripProfit >= 0 ? '+' : '';
                    showToast('Viaje: $' + amount.toFixed(2) +
                        ' | Ganancia: ' + sign + '$' + tripProfit.toFixed(2));
                } else {
                    saveTripRecord(0, -state.tripCost);
                    showToast('Viaje finalizado sin registrar ingreso');
                }
            }
        );
    } else {
        saveTripRecord(0, 0);
        showToast('Viaje finalizado - ' + state.tripDistance.toFixed(2) + ' km');
    }
}

// ============================================================
//  REGISTRO DE VIAJE (estructura para CSV)
// ============================================================
function saveTripRecord(income, profit) {
    state.trips.push({
        id: state.trips.length + 1,
        fecha: new Date().toISOString(),
        fechaLocal: new Date().toLocaleString('es'),
        modo: state.modes[state.tripModeAtStart].name,
        modoId: state.tripModeAtStart,
        distanciaKm: parseFloat(state.tripDistance.toFixed(3)),
        tiempoViajeSeg: state.tripTimeSeconds,
        tiempoViaje: formatTime(state.tripTimeSeconds),
        tiempoDetenidoSeg: state.stoppedTimeSeconds,
        tiempoDetenido: formatTime(state.stoppedTimeSeconds),
        pctDetenido: state.tripTimeSeconds > 0
            ? Math.round((state.stoppedTimeSeconds / state.tripTimeSeconds) * 100) : 0,
        velocidadPromedio: state.tripTimeSeconds > 0
            ? parseFloat(((state.tripDistance / state.tripTimeSeconds) * 3600).toFixed(1)) : 0,
        costoPorKm: parseFloat(getCostPerKm().toFixed(4)),
        costoViaje: parseFloat(state.tripCost.toFixed(2)),
        ingreso: parseFloat(income.toFixed(2)),
        ganancia: parseFloat(profit.toFixed(2)),
        kmDiaAcumulado: parseFloat(state.dayKm.toFixed(2)),
        fuelBlockKm: parseFloat(state.fuelCurrent.toFixed(2))
    });
}

// ============================================================
//  COMBUSTIBLE
// ============================================================
function registerFuel() {
    dismissFuelAlert();

    const currentKm = state.fuelCurrent.toFixed(1);
    openModal(
        'Registrar Combustible',
        'Bloque actual: ' + currentKm + ' km recorridos.\n' +
        'Ingresa el costo de este bloque de ' + state.fuelBlockKm + ' km:',
        function (value) {
            const cost = parseFloat(value);
            if (!isNaN(cost) && cost > 0) {
                // Guardar bloque anterior
                state.fuelBlocks.push({
                    km: state.fuelCurrent,
                    cost: cost,
                    costPerKm: cost / state.fuelBlockKm,
                    fecha: new Date().toISOString()
                });

                state.fuelCostPerBlock = cost;
                state.fuelCurrent = 0;
                state.fuelAlertShown = false;

                // Recalcular costos con nuevo precio
                state.totalCost = state.dayKm * getCostPerKm();
                if (state.tripActive) {
                    state.tripCost = state.tripDistance * getCostPerKm();
                }

                updateAllDisplays();
                showToast('Combustible: $' + cost.toFixed(2) +
                    ' (' + getCostPerKm().toFixed(3) + '$/km)');
            }
        }
    );
}

// ============================================================
//  CAMBIO DE MODO
// ============================================================
function changeMode() {
    if (state.tripActive) {
        showToast('Finaliza el viaje antes de cambiar modo');
        return;
    }
    state.mode = (state.mode + 1) % state.modes.length;
    updateModeDisplay();
    updateAllDisplays();
    showToast('Modo: ' + state.modes[state.mode].name);
}

// ============================================================
//  EXPORTAR CSV
// ============================================================
function exportCSV() {
    if (state.trips.length === 0) {
        showToast('No hay viajes para exportar');
        return;
    }

    const headers = [
        'ID', 'Fecha', 'Modo', 'Distancia_km', 'Tiempo_Viaje',
        'Tiempo_Detenido', '%_Detenido', 'Vel_Promedio_kmh',
        'Costo_por_km', 'Costo_Viaje', 'Ingreso', 'Ganancia',
        'Km_Dia_Acumulado', 'Fuel_Block_Km'
    ];

    const rows = state.trips.map(t => [
        t.id, t.fechaLocal, t.modo, t.distanciaKm, t.tiempoViaje,
        t.tiempoDetenido, t.pctDetenido, t.velocidadPromedio,
        t.costoPorKm, t.costoViaje, t.ingreso, t.ganancia,
        t.kmDiaAcumulado, t.fuelBlockKm
    ]);

    let csv = headers.join(',') + '\n';
    rows.forEach(row => {
        csv += row.map(c => '"' + c + '"').join(',') + '\n';
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'viajes_uber_' + new Date().toISOString().slice(0, 10) + '.csv';
    a.click();
    URL.revokeObjectURL(url);

    showToast('CSV exportado: ' + state.trips.length + ' viajes');
}

// ============================================================
//  MODAL
// ============================================================
function openModal(title, message, callback) {
    dom.modalTitle.textContent = title;
    dom.modalMessage.textContent = message;
    dom.modalInput.value = '';
    dom.modalOverlay.classList.add('active');
    state.modalCallback = callback;
    setTimeout(() => dom.modalInput.focus(), 100);
}

function confirmModal() {
    const value = dom.modalInput.value;
    closeModal();
    if (state.modalCallback) {
        state.modalCallback(value);
    }
}

function closeModal() {
    dom.modalOverlay.classList.remove('active');
    state.modalCallback = null;
}

document.addEventListener('keydown', function (e) {
    if (!dom.modalOverlay.classList.contains('active')) return;
    if (e.key === 'Enter') confirmModal();
    if (e.key === 'Escape') closeModal();
});

// ============================================================
//  TOAST
// ============================================================
function showToast(msg) {
    clearTimeout(state.toastTimer);
    dom.toast.textContent = msg;
    dom.toast.classList.add('show');
    state.toastTimer = setTimeout(() => dom.toast.classList.remove('show'), 3000);
}

// ============================================================
//  DATOS SIMULADOS INICIALES
// ============================================================
// Precarga datos para que el prototipo se vea real al abrir.
function loadSimulatedData() {
    // Día parcialmente recorrido
    state.dayKm = 47.83;
    state.fuelCurrent = 47.83;
    state.fuelCostPerBlock = 8.50;
    state.uberIncome = 125.50;
    state.totalCost = state.dayKm * getCostPerKm();

    // Km por modo
    state.modeData[0].km = 5.53;   state.modeData[0].trips = 1;
    state.modeData[1].km = 37.10;  state.modeData[1].trips = 3;
    state.modeData[1].income = 125.50;
    state.modeData[2].km = 5.20;   state.modeData[2].trips = 1;

    // Viajes previos
    state.trips = [
        {
            id: 1, fecha: '2026-03-29T08:15:00', fechaLocal: '29/3/2026, 8:15:00',
            modo: 'Personal', modoId: 0, distanciaKm: 5.53,
            tiempoViajeSeg: 720, tiempoViaje: '12:00',
            tiempoDetenidoSeg: 145, tiempoDetenido: '02:25', pctDetenido: 20,
            velocidadPromedio: 27.7, costoPorKm: 0.085, costoViaje: 0.47,
            ingreso: 0, ganancia: 0, kmDiaAcumulado: 5.53, fuelBlockKm: 5.53
        },
        {
            id: 2, fecha: '2026-03-29T08:45:00', fechaLocal: '29/3/2026, 8:45:00',
            modo: 'Uber con Pasajero', modoId: 1, distanciaKm: 12.34,
            tiempoViajeSeg: 1125, tiempoViaje: '18:45',
            tiempoDetenidoSeg: 252, tiempoDetenido: '04:12', pctDetenido: 22,
            velocidadPromedio: 39.5, costoPorKm: 0.085, costoViaje: 1.05,
            ingreso: 45.00, ganancia: 43.95, kmDiaAcumulado: 17.87, fuelBlockKm: 17.87
        },
        {
            id: 3, fecha: '2026-03-29T09:30:00', fechaLocal: '29/3/2026, 9:30:00',
            modo: 'Uber con Pasajero', modoId: 1, distanciaKm: 8.76,
            tiempoViajeSeg: 860, tiempoViaje: '14:20',
            tiempoDetenidoSeg: 185, tiempoDetenido: '03:05', pctDetenido: 22,
            velocidadPromedio: 36.7, costoPorKm: 0.085, costoViaje: 0.74,
            ingreso: 35.50, ganancia: 34.76, kmDiaAcumulado: 26.63, fuelBlockKm: 26.63
        },
        {
            id: 4, fecha: '2026-03-29T10:45:00', fechaLocal: '29/3/2026, 10:45:00',
            modo: 'Uber sin Pasajero', modoId: 2, distanciaKm: 5.20,
            tiempoViajeSeg: 490, tiempoViaje: '08:10',
            tiempoDetenidoSeg: 90, tiempoDetenido: '01:30', pctDetenido: 18,
            velocidadPromedio: 38.2, costoPorKm: 0.085, costoViaje: 0.44,
            ingreso: 0, ganancia: -0.44, kmDiaAcumulado: 31.83, fuelBlockKm: 31.83
        },
        {
            id: 5, fecha: '2026-03-29T11:20:00', fechaLocal: '29/3/2026, 11:20:00',
            modo: 'Uber con Pasajero', modoId: 1, distanciaKm: 16.00,
            tiempoViajeSeg: 1350, tiempoViaje: '22:30',
            tiempoDetenidoSeg: 345, tiempoDetenido: '05:45', pctDetenido: 26,
            velocidadPromedio: 42.7, costoPorKm: 0.085, costoViaje: 1.36,
            ingreso: 45.00, ganancia: 43.64, kmDiaAcumulado: 47.83, fuelBlockKm: 47.83
        }
    ];
}

// ============================================================
//  ARRANQUE
// ============================================================
cacheDom();
loadSimulatedData();
updateModeDisplay();
updateAllDisplays();
