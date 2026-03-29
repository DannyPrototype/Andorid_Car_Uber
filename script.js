// ===== Estado de la aplicación =====
const state = {
    mode: 0, // 0: Personal, 1: Uber con pasajero, 2: Uber sin pasajero
    modes: [
        { name: 'Personal', icon: '\u{1F698}', class: 'personal' },
        { name: 'Uber con Pasajero', icon: '\u{1F7E2}', class: 'uber-passenger' },
        { name: 'Uber sin Pasajero', icon: '\u{1F7E1}', class: 'uber-empty' }
    ],
    tripActive: false,
    speed: 0,
    tripDistance: 0,
    dayKm: 12.45,          // Datos simulados previos del día
    fuelBlockKm: 100,
    fuelCurrent: 34.7,      // km recorridos en el bloque actual
    fuelCostPerBlock: 8.50,  // costo por bloque de 100km
    uberIncome: 45.00,      // ingresos simulados
    estimatedCost: 18.30,
    tripTimeSeconds: 0,
    stoppedTimeSeconds: 0,
    tripStartTime: null,
    simulationInterval: null,
    routePoints: [],
    carX: 50,
    carY: 45,
    trips: [],              // historial para CSV
    modalCallback: null
};

// ===== Elementos DOM =====
const dom = {
    speed: document.getElementById('speed'),
    tripDistance: document.getElementById('tripDistance'),
    dayKm: document.getElementById('dayKm'),
    tripTime: document.getElementById('tripTime'),
    stoppedTime: document.getElementById('stoppedTime'),
    fuelBar: document.getElementById('fuelBar'),
    fuelBarText: document.getElementById('fuelBarText'),
    fuelCurrent: document.getElementById('fuelCurrent'),
    fuelExcess: document.getElementById('fuelExcess'),
    uberIncome: document.getElementById('uberIncome'),
    estimatedCost: document.getElementById('estimatedCost'),
    profit: document.getElementById('profit'),
    modeBar: document.getElementById('modeBar'),
    modeLabel: document.getElementById('modeLabel'),
    modeIcon: document.getElementById('modeIcon'),
    btnStart: document.getElementById('btnStart'),
    btnStop: document.getElementById('btnStop'),
    carMarker: document.getElementById('carMarker'),
    routePath: document.getElementById('routePath'),
    modalOverlay: document.getElementById('modalOverlay'),
    modalTitle: document.getElementById('modalTitle'),
    modalMessage: document.getElementById('modalMessage'),
    modalInput: document.getElementById('modalInput'),
    modalOk: document.getElementById('modalOk'),
    toast: document.getElementById('toast')
};

// ===== Inicialización =====
function init() {
    updateModeDisplay();
    updateAllDisplays();
}

// ===== Actualizar pantalla =====
function updateAllDisplays() {
    dom.speed.textContent = state.speed;
    dom.tripDistance.textContent = state.tripDistance.toFixed(2);
    dom.dayKm.textContent = state.dayKm.toFixed(2);
    dom.tripTime.textContent = formatTime(state.tripTimeSeconds);
    dom.stoppedTime.textContent = formatTime(state.stoppedTimeSeconds);

    // Combustible
    const fuelPercent = Math.min((state.fuelCurrent / state.fuelBlockKm) * 100, 100);
    const excess = Math.max(0, state.fuelCurrent - state.fuelBlockKm);
    dom.fuelBar.style.width = fuelPercent + '%';
    dom.fuelBarText.textContent = state.fuelCurrent.toFixed(1) + ' / ' + state.fuelBlockKm + ' km';
    dom.fuelCurrent.textContent = state.fuelCurrent.toFixed(2);
    dom.fuelExcess.textContent = (excess * 1000).toFixed(0);

    // Color de la barra de combustible
    dom.fuelBar.classList.remove('warning', 'danger');
    if (fuelPercent >= 100) {
        dom.fuelBar.classList.add('danger');
    } else if (fuelPercent >= 80) {
        dom.fuelBar.classList.add('warning');
    }

    // Finanzas
    dom.uberIncome.textContent = '$' + state.uberIncome.toFixed(2);
    dom.estimatedCost.textContent = '$' + state.estimatedCost.toFixed(2);
    const profit = state.uberIncome - state.estimatedCost;
    dom.profit.textContent = '$' + profit.toFixed(2);
    dom.profit.className = 'finance-value ' + (profit >= 0 ? 'green' : 'red');
}

function updateModeDisplay() {
    const mode = state.modes[state.mode];
    dom.modeLabel.textContent = mode.name;
    dom.modeIcon.textContent = mode.icon;
    dom.modeBar.className = 'mode-bar ' + mode.class;
}

function formatTime(totalSeconds) {
    const h = Math.floor(totalSeconds / 3600);
    const m = Math.floor((totalSeconds % 3600) / 60);
    const s = totalSeconds % 60;
    if (h > 0) {
        return h + ':' + String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
    }
    return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
}

// ===== Simulación de viaje =====
function startTrip() {
    if (state.tripActive) return;

    state.tripActive = true;
    state.tripDistance = 0;
    state.tripTimeSeconds = 0;
    state.stoppedTimeSeconds = 0;
    state.tripStartTime = Date.now();
    state.routePoints = [{ x: state.carX, y: state.carY }];

    dom.btnStart.disabled = true;
    dom.btnStop.disabled = false;

    showToast('Viaje iniciado - Modo: ' + state.modes[state.mode].name);

    // Simulación cada segundo
    state.simulationInterval = setInterval(simulateMovement, 1000);
}

function simulateMovement() {
    // Simular velocidad variable (ciudad)
    const rand = Math.random();
    if (rand < 0.15) {
        // Detenido (semáforo, tráfico)
        state.speed = 0;
        state.stoppedTimeSeconds++;
    } else if (rand < 0.3) {
        // Velocidad baja
        state.speed = Math.floor(Math.random() * 20) + 5;
    } else if (rand < 0.7) {
        // Velocidad media
        state.speed = Math.floor(Math.random() * 25) + 25;
    } else {
        // Velocidad alta (avenida)
        state.speed = Math.floor(Math.random() * 20) + 50;
    }

    state.tripTimeSeconds++;

    // Calcular distancia recorrida en este segundo (km)
    const distThisSecond = state.speed / 3600;
    state.tripDistance += distThisSecond;
    state.dayKm += distThisSecond;
    state.fuelCurrent += distThisSecond;

    // Costo estimado basado en distancia
    state.estimatedCost = (state.dayKm / state.fuelBlockKm) * state.fuelCostPerBlock;

    // Mover el carro en el mapa
    moveCarOnMap();

    updateAllDisplays();
}

function moveCarOnMap() {
    if (state.speed === 0) return;

    // Movimiento aleatorio suave
    const angle = (Math.random() - 0.5) * 0.8 + (state.lastAngle || 0) * 0.7;
    state.lastAngle = angle;

    const step = state.speed * 0.015;
    state.carX += Math.cos(angle) * step;
    state.carY += Math.sin(angle) * step;

    // Mantener dentro del mapa
    state.carX = Math.max(8, Math.min(92, state.carX));
    state.carY = Math.max(8, Math.min(92, state.carY));

    dom.carMarker.style.left = state.carX + '%';
    dom.carMarker.style.top = state.carY + '%';

    // Agregar punto a la ruta
    state.routePoints.push({ x: state.carX, y: state.carY });
    if (state.routePoints.length > 200) {
        state.routePoints.shift();
    }

    // Dibujar ruta
    const points = state.routePoints.map(p => p.x + ',' + p.y).join(' ');
    dom.routePath.setAttribute('points', points);
}

function endTrip() {
    if (!state.tripActive) return;

    clearInterval(state.simulationInterval);
    state.tripActive = false;
    state.speed = 0;

    dom.btnStart.disabled = false;
    dom.btnStop.disabled = true;

    updateAllDisplays();

    // Si es modo Uber con pasajero, pedir ingreso
    if (state.mode === 1) {
        openModal(
            'Ingreso del Pasajero',
            'Ingresa el monto cobrado por este viaje:',
            function (value) {
                const amount = parseFloat(value);
                if (!isNaN(amount) && amount > 0) {
                    state.uberIncome += amount;
                    saveTripRecord(amount);
                    updateAllDisplays();
                    showToast('Viaje registrado: $' + amount.toFixed(2));
                }
            }
        );
    } else {
        saveTripRecord(0);
        showToast('Viaje finalizado - ' + state.tripDistance.toFixed(2) + ' km');
    }
}

function saveTripRecord(income) {
    state.trips.push({
        fecha: new Date().toLocaleString('es'),
        modo: state.modes[state.mode].name,
        distancia: state.tripDistance.toFixed(2),
        tiempoViaje: formatTime(state.tripTimeSeconds),
        tiempoDetenido: formatTime(state.stoppedTimeSeconds),
        ingreso: income.toFixed(2),
        costoEstimado: state.estimatedCost.toFixed(2)
    });
}

// ===== Registrar combustible =====
function registerFuel() {
    openModal(
        'Registrar Combustible',
        'Ingresa el costo del bloque de 100 km:',
        function (value) {
            const cost = parseFloat(value);
            if (!isNaN(cost) && cost > 0) {
                state.fuelCostPerBlock = cost;
                state.fuelCurrent = 0; // Reset del bloque
                state.estimatedCost = (state.dayKm / state.fuelBlockKm) * state.fuelCostPerBlock;
                updateAllDisplays();
                showToast('Combustible registrado: $' + cost.toFixed(2) + ' / 100 km');
            }
        }
    );
}

// ===== Cambiar modo =====
function changeMode() {
    state.mode = (state.mode + 1) % state.modes.length;
    updateModeDisplay();
    showToast('Modo: ' + state.modes[state.mode].name);
}

// ===== Exportar CSV =====
function exportCSV() {
    if (state.trips.length === 0) {
        showToast('No hay viajes para exportar');
        return;
    }

    const headers = ['Fecha', 'Modo', 'Distancia (km)', 'Tiempo Viaje', 'Tiempo Detenido', 'Ingreso ($)', 'Costo Estimado ($)'];
    const rows = state.trips.map(t => [
        t.fecha, t.modo, t.distancia, t.tiempoViaje, t.tiempoDetenido, t.ingreso, t.costoEstimado
    ]);

    let csv = headers.join(',') + '\n';
    rows.forEach(row => {
        csv += row.map(cell => '"' + cell + '"').join(',') + '\n';
    });

    // Descargar
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'viajes_uber_' + new Date().toISOString().slice(0, 10) + '.csv';
    link.click();
    URL.revokeObjectURL(url);

    showToast('CSV exportado: ' + state.trips.length + ' viajes');
}

// ===== Modal =====
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
    if (state.modalCallback && value) {
        state.modalCallback(value);
    }
}

function closeModal() {
    dom.modalOverlay.classList.remove('active');
    state.modalCallback = null;
}

// Cerrar modal con Enter
document.addEventListener('keydown', function (e) {
    if (e.key === 'Enter' && dom.modalOverlay.classList.contains('active')) {
        confirmModal();
    }
    if (e.key === 'Escape' && dom.modalOverlay.classList.contains('active')) {
        closeModal();
    }
});

// ===== Toast =====
function showToast(message) {
    dom.toast.textContent = message;
    dom.toast.classList.add('show');
    setTimeout(() => dom.toast.classList.remove('show'), 2500);
}

// ===== Cargar datos simulados iniciales =====
function loadSimulatedData() {
    state.dayKm = 47.83;
    state.fuelCurrent = 34.7;
    state.uberIncome = 125.50;
    state.estimatedCost = 42.30;
    state.fuelCostPerBlock = 8.50;

    // Viajes previos simulados
    state.trips = [
        {
            fecha: '29/03/2026 08:15:00',
            modo: 'Uber con Pasajero',
            distancia: '12.34',
            tiempoViaje: '18:45',
            tiempoDetenido: '04:12',
            ingreso: '45.00',
            costoEstimado: '10.50'
        },
        {
            fecha: '29/03/2026 09:30:00',
            modo: 'Uber con Pasajero',
            distancia: '8.76',
            tiempoViaje: '14:20',
            tiempoDetenido: '03:05',
            ingreso: '35.50',
            costoEstimado: '7.45'
        },
        {
            fecha: '29/03/2026 10:45:00',
            modo: 'Uber sin Pasajero',
            distancia: '5.20',
            tiempoViaje: '08:10',
            tiempoDetenido: '01:30',
            ingreso: '0.00',
            costoEstimado: '4.42'
        },
        {
            fecha: '29/03/2026 11:20:00',
            modo: 'Uber con Pasajero',
            distancia: '15.90',
            tiempoViaje: '22:30',
            tiempoDetenido: '05:45',
            ingreso: '45.00',
            costoEstimado: '13.52'
        }
    ];

    updateAllDisplays();
}

// ===== Arrancar =====
loadSimulatedData();
init();
