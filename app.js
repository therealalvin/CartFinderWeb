/**
 * CartFinder Web App - Progressive Web App for Golf Cart GPS & Compass Guidance
 * Works on iPhone (iOS Safari), Android (Chrome), and Desktop
 * 
 * Storage behavior:
 * - Never saves a history or log of past locations.
 * - Only stores the single current parked location in case the user quits the browser or reboots the phone.
 * - Automatically forgets and clears the location the moment the user returns to the cart.
 */

// --- Application State ---
const state = {
  currentLocation: null,      // { lat, lng, accuracy, heading }
  parkedLocation: null,       // { id, lat, lng, accuracy, timestamp, spotLabel, note }
  preferredUnit: localStorage.getItem('cartfinder_unit') || 'Yards',
  deviceHeading: 0,           // 0..360 deg
  isCompassActive: false,
  isSatellite: false,
  isDemoMode: false,
  demoTimer: null,
  hasArrived: false,
  autoForgetTimer: null,
  watchId: null
};

// Purge any legacy history to ensure strict zero-tracking compliance
try {
  localStorage.removeItem('cartfinder_history');
} catch (e) {}

// Audio Context for Arrival Chime (synthesized, zero external audio dependency)
let audioCtx = null;

// Leaflet Map & Layer References
let map = null;
let userMarker = null;
let cartMarker = null;
let navigationLine = null;
let streetLayer = null;
let satelliteLayer = null;

// --- DOM Elements ---
const el = {
  gpsBadge: document.getElementById('gps-status-badge'),
  gpsStatusText: document.getElementById('gps-status-text'),
  unitToggleBtn: document.getElementById('unit-toggle-btn'),
  unitLabel: document.getElementById('unit-label'),
  layerToggleBtn: document.getElementById('layer-toggle-btn'),
  layerIcon: document.getElementById('layer-icon'),
  menuBtn: document.getElementById('menu-btn'),
  demoBanner: document.getElementById('demo-banner'),
  exitDemoBtn: document.getElementById('exit-demo-btn'),
  recenterUserBtn: document.getElementById('recenter-user-btn'),
  recenterCartBtn: document.getElementById('recenter-cart-btn'),
  fitBoundsBtn: document.getElementById('fit-bounds-btn'),
  navHud: document.getElementById('nav-hud'),
  hudDistanceVal: document.getElementById('hud-distance-val'),
  hudDistanceUnit: document.getElementById('hud-distance-unit'),
  hudAccuracyCaption: document.getElementById('hud-accuracy-caption'),
  compassNeedle: document.getElementById('compass-needle'),
  hudBearingText: document.getElementById('hud-bearing-text'),
  hudGuidanceText: document.getElementById('hud-guidance-text'),
  directionArrowIcon: document.getElementById('direction-arrow-icon'),
  extMapLink: document.getElementById('ext-map-link'),
  extMapText: document.getElementById('ext-map-text'),
  arrivalCard: document.getElementById('arrival-card'),
  arrivalSpotName: document.getElementById('arrival-spot-name'),
  dismissArrivalBtn: document.getElementById('dismiss-arrival-btn'),
  parkedInfoView: document.getElementById('parked-info-view'),
  notParkedView: document.getElementById('not-parked-view'),
  parkCartBtn: document.getElementById('park-cart-btn'),
  clearSpotBtn: document.getElementById('clear-spot-btn'),
  updateSpotBtn: document.getElementById('update-spot-btn'),
  parkedSpotLabel: document.getElementById('parked-spot-label'),
  parkedTimeAgo: document.getElementById('parked-time-ago'),
  parkedNote: document.getElementById('parked-note'),
  // Modals
  parkModal: document.getElementById('park-modal'),
  closeModalBtn: document.getElementById('close-modal-btn'),
  cancelParkBtn: document.getElementById('cancel-park-btn'),
  saveParkBtn: document.getElementById('save-park-btn'),
  spotNameInput: document.getElementById('spot-name-input'),
  spotNoteInput: document.getElementById('spot-note-input'),
  modalGpsCoord: document.getElementById('modal-gps-coord'),
  menuModal: document.getElementById('menu-modal'),
  closeMenuBtn: document.getElementById('close-menu-btn'),
  enableCompassBtn: document.getElementById('enable-compass-btn'),
  toggleDemoBtn: document.getElementById('toggle-demo-btn'),
  demoToggleTitle: document.getElementById('demo-toggle-title'),
  toast: document.getElementById('toast'),
  toastText: document.getElementById('toast-text')
};

// --- Initialization ---
window.addEventListener('DOMContentLoaded', () => {
  initMap();
  loadSavedParkedLocation();
  updateUnitDisplay();
  bindEvents();
  startGpsTracking();
  initCompassListener();
  registerServiceWorker();
  updateExternalMapProviderLabel();
});

// --- Register Service Worker for PWA Offline Cache ---
function registerServiceWorker() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js')
      .then(reg => console.log('CartFinder Service Worker registered:', reg.scope))
      .catch(err => console.log('Service Worker registration skipped/failed:', err));
  }
}

// --- Map Initialization ---
function initMap() {
  // Default to Pebble Beach Golf Links if no GPS yet
  const defaultPos = [36.5688, -121.9506];

  map = L.map('map', {
    zoomControl: false,
    attributionControl: true
  }).setView(defaultPos, 17);

  // Street Layer (OpenStreetMap)
  streetLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap'
  });

  // High-Resolution Satellite Layer (ESRI World Imagery - perfect for golf fairways!)
  satelliteLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
    maxZoom: 19,
    attribution: 'Tiles &copy; Esri &mdash; Earthstar Geographics'
  });

  // Start with Street Layer
  streetLayer.addTo(map);

  // Set up Custom User Pulsing Dot Marker
  const userIcon = L.divIcon({
    className: 'user-marker-icon',
    html: '<div class="user-marker-pulse"></div><div class="user-marker-dot"></div>',
    iconSize: [24, 24],
    iconAnchor: [12, 12]
  });

  userMarker = L.marker(defaultPos, { icon: userIcon, zIndexOffset: 1000 }).addTo(map);

  // Set up Cart Marker
  const cartIcon = L.divIcon({
    className: 'cart-marker-container',
    html: '<div class="cart-marker-pin"><span class="material-symbols-outlined">sports_golf</span></div>',
    iconSize: [44, 44],
    iconAnchor: [22, 44]
  });

  cartMarker = L.marker(defaultPos, { icon: cartIcon, zIndexOffset: 900 });

  // Navigation connecting line
  navigationLine = L.polyline([], {
    color: '#ffd54f',
    weight: 3,
    dashArray: '8, 8',
    opacity: 0.85
  }).addTo(map);
}

// --- GPS Geolocation Tracking ---
function startGpsTracking() {
  if (!('geolocation' in navigator)) {
    updateGpsBadge(false, 'GPS Unavailable');
    showToast('Geolocation is not supported by your browser');
    return;
  }

  updateGpsBadge(false, 'Acquiring GPS...');

  const options = {
    enableHighAccuracy: true,
    timeout: 15000,
    maximumAge: 1000
  };

  state.watchId = navigator.geolocation.watchPosition(
    onLocationSuccess,
    onLocationError,
    options
  );
}

function onLocationSuccess(position) {
  if (state.isDemoMode) return; // demo controls location

  const { latitude, longitude, accuracy, heading } = position.coords;
  const latLng = [latitude, longitude];

  state.currentLocation = {
    lat: latitude,
    lng: longitude,
    accuracy: accuracy || 10,
    heading: heading || null
  };

  // Update GPS Status UI
  const accFormatted = formatDistance(accuracy || 10);
  updateGpsBadge(true, `GPS (±${accFormatted})`);

  // Update Map User Marker
  if (userMarker) {
    userMarker.setLatLng(latLng);
  }

  // If first location fix and cart not parked, pan to user
  if (!state.parkedLocation && map) {
    map.setView(latLng, 18);
  }

  // Recalculate distance and compass needle
  updateNavigation();
}

function onLocationError(err) {
  console.warn('Geolocation error:', err.code, err.message);
  let msg = 'Acquiring GPS...';
  if (err.code === 1) {
    msg = 'Location Denied';
    showToast('Please enable Location Permissions in your browser settings');
  } else if (err.code === 2) {
    msg = 'GPS Signal Searching...';
  } else if (err.code === 3) {
    msg = 'GPS Timeout';
  }
  updateGpsBadge(false, msg);
}

function updateGpsBadge(isActive, text) {
  el.gpsStatusText.textContent = text;
  const dot = el.gpsBadge.querySelector('.status-dot');
  if (isActive) {
    dot.className = 'status-dot active';
  } else {
    dot.className = 'status-dot pulsing';
  }
}

// --- Compass & Device Orientation ---
function initCompassListener() {
  if (window.DeviceOrientationEvent) {
    if (typeof DeviceOrientationEvent.requestPermission === 'function') {
      el.enableCompassBtn.classList.remove('hidden');
    } else {
      window.addEventListener('deviceorientationabsolute', handleOrientation, true);
      window.addEventListener('deviceorientation', handleOrientation, true);
      state.isCompassActive = true;
    }
  }
}

function requestIosCompassPermission() {
  if (typeof DeviceOrientationEvent !== 'undefined' && typeof DeviceOrientationEvent.requestPermission === 'function') {
    DeviceOrientationEvent.requestPermission()
      .then(response => {
        if (response === 'granted') {
          window.addEventListener('deviceorientation', handleOrientation, true);
          state.isCompassActive = true;
          showToast('Compass calibrated successfully!');
          closeMenu();
        } else {
          showToast('Motion permission denied. Needle will use map heading.');
        }
      })
      .catch(err => {
        console.error('Error requesting motion permission:', err);
        showToast('Compass sensor error: ' + err.message);
      });
  } else {
    showToast('Compass sensors active');
  }
}

function handleOrientation(event) {
  let heading = null;

  // iOS Safari: webkitCompassHeading provides calibrated heading (0 = North)
  if (typeof event.webkitCompassHeading !== 'undefined') {
    heading = event.webkitCompassHeading;
  }
  // Android absolute orientation
  else if (event.absolute && event.alpha !== null) {
    heading = 360 - event.alpha;
  }
  // Standard fallback
  else if (event.alpha !== null) {
    heading = 360 - event.alpha;
  }

  if (heading !== null) {
    state.deviceHeading = (heading + 360) % 360;
    updateCompassNeedle();
  }
}

// --- Navigation & Geometry Calculations ---
function updateNavigation() {
  if (!state.parkedLocation || !state.currentLocation) {
    hideNavHud();
    if (navigationLine) navigationLine.setLatLngs([]);
    return;
  }

  showNavHud();

  const user = state.currentLocation;
  const cart = state.parkedLocation;

  // Calculate distance in meters using Haversine formula
  const distMeters = calculateDistanceMeters(user.lat, user.lng, cart.lat, cart.lng);

  // Bearing from user to cart
  const targetBearing = calculateBearing(user.lat, user.lng, cart.lat, cart.lng);

  // Update HUD text
  const formatted = formatDistance(distMeters);
  const parts = formatted.split(' ');
  el.hudDistanceVal.textContent = parts[0];
  el.hudDistanceUnit.textContent = parts[1];
  el.hudAccuracyCaption.textContent = `GPS accuracy: ±${formatDistance(user.accuracy || 10)}`;
  el.hudBearingText.textContent = `${Math.round(targetBearing)}° ${getCardinalDirection(targetBearing)}`;

  // Update navigation line
  if (navigationLine) {
    navigationLine.setLatLngs([
      [user.lat, user.lng],
      [cart.lat, cart.lng]
    ]);
  }

  // Update Compass Needle Rotation
  updateCompassNeedle(targetBearing);

  // Update External Navigation Links
  updateExternalNavLinks(cart.lat, cart.lng);

  // Check Arrival (<= 10 meters / ~11 yards)
  if (distMeters <= 10.0) {
    triggerArrival();
  } else {
    hideArrival();
  }
}

function updateCompassNeedle(bearingOverride) {
  if (!state.parkedLocation || !state.currentLocation) return;

  const targetBearing = bearingOverride !== undefined
    ? bearingOverride
    : calculateBearing(state.currentLocation.lat, state.currentLocation.lng, state.parkedLocation.lat, state.parkedLocation.lng);

  // Relative needle angle = Target Bearing - Device Heading
  let relativeAngle = (targetBearing - state.deviceHeading + 360) % 360;

  // Rotate Needle
  el.compassNeedle.style.transform = `rotate(${relativeAngle}deg)`;

  // Direction Guidance Text & Icon
  updateGuidanceMessage(relativeAngle);
}

function updateGuidanceMessage(angle) {
  let msg = 'Walk straight ahead';
  let icon = 'straight';

  if (angle > 340 || angle < 20) {
    msg = 'Walk straight ahead toward cart';
    icon = 'straight';
  } else if (angle >= 20 && angle < 70) {
    msg = 'Bear slight right';
    icon = 'turn_slight_right';
  } else if (angle >= 70 && angle < 120) {
    msg = 'Turn right';
    icon = 'turn_right';
  } else if (angle >= 120 && angle < 165) {
    msg = 'Turn hard right';
    icon = 'u_turn_right';
  } else if (angle >= 165 && angle <= 195) {
    msg = 'Turn around — cart is behind you';
    icon = 'south';
  } else if (angle > 195 && angle <= 240) {
    msg = 'Turn hard left';
    icon = 'u_turn_left';
  } else if (angle > 240 && angle <= 290) {
    msg = 'Turn left';
    icon = 'turn_left';
  } else if (angle > 290 && angle <= 340) {
    msg = 'Bear slight left';
    icon = 'turn_slight_left';
  }

  el.hudGuidanceText.textContent = msg;
  el.directionArrowIcon.textContent = icon;
}

// Haversine Great Circle Distance
function calculateDistanceMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000; // Earth radius in meters
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

// Forward Azimuth / Bearing
function calculateBearing(lat1, lon1, lat2, lon2) {
  const φ1 = lat1 * Math.PI / 180;
  const φ2 = lat2 * Math.PI / 180;
  const Δλ = (lon2 - lon1) * Math.PI / 180;

  const y = Math.sin(Δλ) * Math.cos(φ2);
  const x = Math.cos(φ1) * Math.sin(φ2) -
            Math.sin(φ1) * Math.cos(φ2) * Math.cos(Δλ);

  const θ = Math.atan2(y, x);
  return (θ * 180 / Math.PI + 360) % 360;
}

function getCardinalDirection(deg) {
  const directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
  const index = Math.round(deg / 45) % 8;
  return directions[index];
}

// Format distance based on preferred unit
function formatDistance(meters) {
  if (state.preferredUnit === 'Yards') {
    const yards = Math.round(meters * 1.09361);
    return `${yards} yd`;
  } else if (state.preferredUnit === 'Feet') {
    const feet = Math.round(meters * 3.28084);
    return `${feet} ft`;
  } else {
    const m = Math.round(meters);
    return `${m} m`;
  }
}

// --- Arrival Detection, Auto-Forget & Audio Chime ---
function triggerArrival() {
  if (state.hasArrived) return;
  state.hasArrived = true;

  // Immediately delete the stored location from localStorage so it's forgotten upon arrival
  try {
    localStorage.removeItem('cartfinder_parked');
  } catch (e) {}

  const spotName = state.parkedLocation ? (state.parkedLocation.spotLabel || 'Golf Cart') : 'Golf Cart';
  el.arrivalSpotName.textContent = `Back at ${spotName}! Location forgotten.`;
  el.arrivalCard.classList.remove('hidden');

  // Trigger device vibration
  if ('vibrate' in navigator) {
    try {
      navigator.vibrate([150, 80, 200, 80, 300]);
    } catch (e) {}
  }

  // Play pleasant arrival chime
  playArrivalChime();

  // Automatically reset map & bottom card after 5 seconds of arrival celebration
  clearTimeout(state.autoForgetTimer);
  state.autoForgetTimer = setTimeout(() => {
    forgetParkedLocation(false);
  }, 5000);
}

function hideArrival() {
  if (!state.hasArrived) return;
  state.hasArrived = false;
  el.arrivalCard.classList.add('hidden');
}

function playArrivalChime() {
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return;
    if (!audioCtx) audioCtx = new AudioContext();

    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }

    const now = audioCtx.currentTime;

    // Harmonic triumphant two-tone chime (587.33Hz D5 -> 880Hz A5)
    const osc1 = audioCtx.createOscillator();
    const osc2 = audioCtx.createOscillator();
    const gain = audioCtx.createGain();

    osc1.type = 'triangle';
    osc1.frequency.setValueAtTime(587.33, now); // D5
    osc1.frequency.setValueAtTime(880, now + 0.15); // A5

    osc2.type = 'sine';
    osc2.frequency.setValueAtTime(880, now); // A5
    osc2.frequency.setValueAtTime(1174.66, now + 0.15); // D6

    gain.gain.setValueAtTime(0.01, now);
    gain.gain.linearRampToValueAtTime(0.25, now + 0.05);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.8);

    osc1.connect(gain);
    osc2.connect(gain);
    gain.connect(audioCtx.destination);

    osc1.start(now);
    osc2.start(now);
    osc1.stop(now + 0.85);
    osc2.stop(now + 0.85);
  } catch (err) {
    console.warn('Audio chime skipped:', err);
  }
}

// --- External Navigation Deep Links ---
function isIosDevice() {
  return /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
}

function updateExternalMapProviderLabel() {
  if (isIosDevice()) {
    el.extMapText.textContent = 'Open Apple Maps';
  } else {
    el.extMapText.textContent = 'Open Google Maps';
  }
}

function updateExternalNavLinks(lat, lng) {
  if (isIosDevice()) {
    el.extMapLink.href = `https://maps.apple.com/?daddr=${lat},${lng}&dirflg=w`;
  } else {
    el.extMapLink.href = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}&travelmode=walking`;
  }
}

// --- Parking Spot Management & Single-Session Persistence ---
function loadSavedParkedLocation() {
  const saved = localStorage.getItem('cartfinder_parked');
  if (saved) {
    try {
      const data = JSON.parse(saved);
      applyParkedLocation(data);
    } catch (e) {
      console.error('Error loading saved location:', e);
    }
  } else {
    showNotParkedView();
  }
}

function applyParkedLocation(parked) {
  state.parkedLocation = parked;
  state.hasArrived = false;

  // Add marker to map
  if (cartMarker && map) {
    cartMarker.setLatLng([parked.lat, parked.lng]);
    if (!map.hasLayer(cartMarker)) {
      cartMarker.addTo(map);
    }
  }

  // Update UI Card
  el.parkedSpotLabel.textContent = parked.spotLabel || 'Golf Cart';
  el.parkedTimeAgo.textContent = formatTimeAgo(parked.timestamp);
  el.parkedNote.textContent = parked.note || 'No notes added';

  el.notParkedView.classList.add('hidden');
  el.parkedInfoView.classList.remove('hidden');

  updateNavigation();

  // If user location is available, fit bounds to show both
  if (state.currentLocation && map) {
    fitBothOnMap();
  } else if (map) {
    map.setView([parked.lat, parked.lng], 18);
  }
}

function openParkModal() {
  let lat = 36.5688;
  let lng = -121.9506;
  let acc = 10;

  if (state.currentLocation) {
    lat = state.currentLocation.lat;
    lng = state.currentLocation.lng;
    acc = state.currentLocation.accuracy;
  }

  el.modalGpsCoord.textContent = `Coordinates: ${lat.toFixed(6)}, ${lng.toFixed(6)} (±${formatDistance(acc)})`;
  el.spotNameInput.value = state.parkedLocation ? state.parkedLocation.spotLabel : '';
  el.spotNoteInput.value = state.parkedLocation ? state.parkedLocation.note : '';

  el.parkModal.classList.remove('hidden');
  setTimeout(() => el.spotNameInput.focus(), 150);
}

function saveParkedSpot() {
  let lat = 36.5688;
  let lng = -121.9506;
  let acc = 10;

  if (state.currentLocation) {
    lat = state.currentLocation.lat;
    lng = state.currentLocation.lng;
    acc = state.currentLocation.accuracy;
  }

  const label = el.spotNameInput.value.trim() || 'Golf Cart';
  const note = el.spotNoteInput.value.trim();

  const newSpot = {
    id: Date.now().toString(),
    lat,
    lng,
    accuracy: acc,
    timestamp: Date.now(),
    spotLabel: label,
    note
  };

  // Only store the current active location for recovery if phone reboots or browser closes
  localStorage.setItem('cartfinder_parked', JSON.stringify(newSpot));

  applyParkedLocation(newSpot);
  closeParkModal();
  showToast(`Spot marked! Saved until you return to your cart.`);

  // Haptic feedback
  if ('vibrate' in navigator) {
    try { navigator.vibrate(60); } catch (e) {}
  }
}

// Forgets and completely clears the parked spot
function forgetParkedLocation(showToastMsg = true) {
  clearTimeout(state.autoForgetTimer);
  state.autoForgetTimer = null;

  try {
    localStorage.removeItem('cartfinder_parked');
  } catch (e) {}

  state.parkedLocation = null;
  state.hasArrived = false;

  if (cartMarker && map && map.hasLayer(cartMarker)) {
    map.removeLayer(cartMarker);
  }

  if (navigationLine) {
    navigationLine.setLatLngs([]);
  }

  hideNavHud();
  hideArrival();
  showNotParkedView();

  if (showToastMsg) {
    showToast('Spot forgotten. Ready for your next round!');
  }
}

function clearParkedSpot() {
  if (!confirm('Forget this parked golf cart location?')) return;
  forgetParkedLocation(true);
}

function showNotParkedView() {
  el.parkedInfoView.classList.add('hidden');
  el.notParkedView.classList.remove('hidden');
  hideNavHud();
  hideArrival();
}

function showNavHud() {
  el.navHud.classList.remove('hidden');
}

function hideNavHud() {
  el.navHud.classList.add('hidden');
}

function closeParkModal() {
  el.parkModal.classList.add('hidden');
}

function closeMenu() {
  el.menuModal.classList.add('hidden');
}

// --- Map Center & Fit Controls ---
function centerOnUser() {
  if (state.currentLocation && map) {
    map.setView([state.currentLocation.lat, state.currentLocation.lng], 18, { animate: true });
    showToast('Centered on your position');
  } else {
    showToast('Waiting for GPS fix...');
  }
}

function centerOnCart() {
  if (state.parkedLocation && map) {
    map.setView([state.parkedLocation.lat, state.parkedLocation.lng], 18, { animate: true });
    showToast('Centered on Golf Cart');
  } else {
    showToast('No cart currently parked');
  }
}

function fitBothOnMap() {
  if (state.currentLocation && state.parkedLocation && map) {
    const bounds = L.latLngBounds([
      [state.currentLocation.lat, state.currentLocation.lng],
      [state.parkedLocation.lat, state.parkedLocation.lng]
    ]);
    map.fitBounds(bounds, { padding: [60, 60], maxZoom: 19 });
  } else if (state.currentLocation && map) {
    centerOnUser();
  }
}

// --- Map Tile Layer Toggle (Street vs Satellite) ---
function toggleMapLayer() {
  if (!map) return;
  state.isSatellite = !state.isSatellite;

  if (state.isSatellite) {
    map.removeLayer(streetLayer);
    satelliteLayer.addTo(map);
    el.layerIcon.textContent = 'map';
    showToast('Switched to Satellite Course Imagery');
  } else {
    map.removeLayer(satelliteLayer);
    streetLayer.addTo(map);
    el.layerIcon.textContent = 'satellite_alt';
    showToast('Switched to Standard Map');
  }
}

// --- Distance Unit Cycling ---
function cycleUnit() {
  const units = ['Yards', 'Meters', 'Feet'];
  const nextIdx = (units.indexOf(state.preferredUnit) + 1) % units.length;
  setUnit(units[nextIdx]);
}

function setUnit(unit) {
  state.preferredUnit = unit;
  localStorage.setItem('cartfinder_unit', unit);
  updateUnitDisplay();
  updateNavigation();
  showToast(`Distance units set to ${unit}`);
}

function updateUnitDisplay() {
  el.unitLabel.textContent = state.preferredUnit;
  document.querySelectorAll('.unit-opt-btn').forEach(btn => {
    if (btn.dataset.unit === state.preferredUnit) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });
}

// --- Demo & Simulation Mode (Test Navigation Indoors) ---
function toggleDemoMode() {
  if (state.isDemoMode) {
    exitDemoMode();
  } else {
    startDemoMode();
  }
}

function startDemoMode() {
  state.isDemoMode = true;
  el.demoBanner.classList.remove('hidden');
  el.demoToggleTitle.textContent = 'Stop Demo Simulation';
  closeMenu();

  const baseLat = state.currentLocation ? state.currentLocation.lat : 36.5688;
  const baseLng = state.currentLocation ? state.currentLocation.lng : -121.9506;

  // Set parked cart ~75 yards away
  const demoCart = {
    id: 'demo-cart',
    lat: baseLat + 0.00062,
    lng: baseLng + 0.00055,
    accuracy: 3.5,
    timestamp: Date.now() - (15 * 60 * 1000),
    spotLabel: 'Cart #42 (Clubhouse Turn)',
    note: 'Demo simulation — testing auto-forget on arrival'
  };

  applyParkedLocation(demoCart);

  let currentSimLat = baseLat;
  let currentSimLng = baseLng;
  let step = 0;
  const totalSteps = 8;

  state.currentLocation = {
    lat: currentSimLat,
    lng: currentSimLng,
    accuracy: 3.0,
    heading: 45
  };

  updateGpsBadge(true, 'DEMO GPS (±3 yd)');
  updateNavigation();
  fitBothOnMap();
  showToast('Demo simulation started: Simulating walk to cart');

  clearInterval(state.demoTimer);
  state.demoTimer = setInterval(() => {
    step++;
    const progress = Math.min(step / totalSteps, 1.0);

    currentSimLat = baseLat + (demoCart.lat - baseLat) * progress;
    currentSimLng = baseLng + (demoCart.lng - baseLng) * progress;

    state.currentLocation = {
      lat: currentSimLat,
      lng: currentSimLng,
      accuracy: 2.5,
      heading: 40 + Math.sin(step) * 10
    };

    if (userMarker) {
      userMarker.setLatLng([currentSimLat, currentSimLng]);
    }

    updateNavigation();

    if (step >= totalSteps) {
      clearInterval(state.demoTimer);
      showToast('Arrived at cart! Location automatically forgotten.');
    }
  }, 1800);
}

function exitDemoMode() {
  state.isDemoMode = false;
  clearInterval(state.demoTimer);
  el.demoBanner.classList.add('hidden');
  el.demoToggleTitle.textContent = 'Start Demo Simulation';
  showToast('Exited demo mode');
  startGpsTracking();
}

// --- Utilities & Formatters ---
function formatTimeAgo(timestamp) {
  if (!timestamp) return 'Just now';
  const diffSec = Math.floor((Date.now() - timestamp) / 1000);
  if (diffSec < 60) return 'Just now';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  return `${Math.floor(diffHr / 24)}d ago`;
}

function showToast(msg) {
  el.toastText.textContent = msg;
  el.toast.classList.remove('hidden');
  clearTimeout(el.toast._timer);
  el.toast._timer = setTimeout(() => {
    el.toast.classList.add('hidden');
  }, 3200);
}

// --- Event Listeners ---
function bindEvents() {
  // Unit toggle
  el.unitToggleBtn.addEventListener('click', cycleUnit);

  // Layer toggle
  el.layerToggleBtn.addEventListener('click', toggleMapLayer);

  // Recenter controls
  el.recenterUserBtn.addEventListener('click', centerOnUser);
  el.recenterCartBtn.addEventListener('click', centerOnCart);
  el.fitBoundsBtn.addEventListener('click', fitBothOnMap);

  // Park Spot Actions
  el.parkCartBtn.addEventListener('click', openParkModal);
  el.updateSpotBtn.addEventListener('click', openParkModal);
  el.clearSpotBtn.addEventListener('click', clearParkedSpot);

  // Arrival Card Dismiss button: immediately clear and forget
  if (el.dismissArrivalBtn) {
    el.dismissArrivalBtn.addEventListener('click', () => {
      forgetParkedLocation(true);
    });
  }

  // Modal Buttons
  el.closeModalBtn.addEventListener('click', closeParkModal);
  el.cancelParkBtn.addEventListener('click', closeParkModal);
  el.saveParkBtn.addEventListener('click', saveParkedSpot);

  // Quick chips in modal
  document.querySelectorAll('.tag-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      el.spotNameInput.value = chip.textContent;
      document.querySelectorAll('.tag-chip').forEach(c => c.classList.remove('selected'));
      chip.classList.add('selected');
    });
  });

  // Settings & Options Modal
  el.menuBtn.addEventListener('click', () => {
    el.menuModal.classList.remove('hidden');
  });
  el.closeMenuBtn.addEventListener('click', closeMenu);

  // Menu Unit Buttons
  document.querySelectorAll('.unit-opt-btn').forEach(btn => {
    btn.addEventListener('click', () => setUnit(btn.dataset.unit));
  });

  // iOS Compass Permission Button
  el.enableCompassBtn.addEventListener('click', requestIosCompassPermission);

  // Demo Simulation Toggle
  el.toggleDemoBtn.addEventListener('click', toggleDemoMode);
  el.exitDemoBtn.addEventListener('click', exitDemoMode);

  // Close modals on overlay backdrop click
  [el.parkModal, el.menuModal].forEach(modal => {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) modal.classList.add('hidden');
    });
  });

  // Tap anywhere on document initializes Web Audio Context on iOS
  document.addEventListener('touchstart', () => {
    if (audioCtx && audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
  }, { once: true });
}
