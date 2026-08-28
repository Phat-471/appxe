/**
 * VIETNAM TRAFFIC CAMERA INSPECTOR — APPLICATION ENGINE
 * High-performance interactive map for inspecting, filtering, verifying,
 * fetching Overpass OSM live cameras, and simulating speed radar alerts.
 */

// =========================================================
// 1. STATE & DATA INITIALIZATION
// =========================================================
let allCameras = [];
let filteredCameras = [];
let selectedCamera = null;
let currentRegion = 'ALL';
let currentSpeedFilter = 'ALL';
let isPickingOnMap = false;

// Map & Layer references
let map = null;
let markerClusterGroup = null;
let markersMap = new Map(); // id -> L.Marker
let radarCirclesGroup = null;
let userLocationMarker = null;

// Simulator State
let simInterval = null;
let simMarker = null;
let simRouteIndex = 0;
let simSpeedKmh = 65;

// Sample simulation path along Vo Van Kiet & QL1A (TP.HCM)
const SIM_ROUTE = [
  [10.7410, 106.6430], // An Duong Vuong
  [10.7450, 106.6520],
  [10.7480, 106.6620],
  [10.7523, 106.6712], // Cau Chu Y (Cam speed 60)
  [10.7580, 106.6850],
  [10.7620, 106.6950],
  [10.7650, 106.7020], // Ham Thu Thiem
  [10.7720, 106.7150],
  [10.7850, 106.7350],
  [10.7960, 106.7620]  // An Phu / Cao Toc
];

// =========================================================
// 2. DOM ELEMENTS
// =========================================================
const dom = {
  sidebar: document.getElementById('sidebar'),
  toggleSidebarBtn: document.getElementById('toggleSidebarBtn'),
  openSidebarBtn: document.getElementById('openSidebarBtn'),
  
  // Stats
  statTotalCam: document.getElementById('statTotalCam'),
  statFilteredCam: document.getElementById('statFilteredCam'),
  statSpeedCam: document.getElementById('statSpeedCam'),
  statRedLightCam: document.getElementById('statRedLightCam'),
  statSouthCam: document.getElementById('statSouthCam'),

  // Tabs
  navTabs: document.querySelectorAll('.nav-tab'),
  tabPanes: document.querySelectorAll('.tab-pane'),

  // Search & List
  searchInput: document.getElementById('searchInput'),
  clearSearchBtn: document.getElementById('clearSearchBtn'),
  cameraList: document.getElementById('cameraList'),
  regionPills: document.querySelectorAll('.region-pill'),

  // Filters
  filterSpeedCam: document.getElementById('filterSpeedCam'),
  filterRedLightCam: document.getElementById('filterRedLightCam'),
  filterSurveillanceCam: document.getElementById('filterSurveillanceCam'),
  filterProhibitCam: document.getElementById('filterProhibitCam'),
  filterSpeedSign: document.getElementById('filterSpeedSign'),
  speedFilterBtns: document.querySelectorAll('.speed-filter-btn'),
  toggleClustering: document.getElementById('toggleClustering'),
  toggleRadarRings: document.getElementById('toggleRadarRings'),

  // Add Form
  addCameraForm: document.getElementById('addCameraForm'),
  pickOnMapBtn: document.getElementById('pickOnMapBtn'),
  newLat: document.getElementById('newLat'),
  newLng: document.getElementById('newLng'),
  newRoadName: document.getElementById('newRoadName'),
  newType: document.getElementById('newType'),
  newSpeedLimit: document.getElementById('newSpeedLimit'),
  newDistrict: document.getElementById('newDistrict'),
  newDescription: document.getElementById('newDescription'),

  // Simulator
  simSpeedSlider: document.getElementById('simSpeedSlider'),
  simSpeedDisplay: document.getElementById('simSpeedDisplay'),
  startSimBtn: document.getElementById('startSimBtn'),
  stopSimBtn: document.getElementById('stopSimBtn'),
  simStatusBox: document.getElementById('simStatusBox'),
  simStatusText: document.getElementById('simStatusText'),

  // Footer & Header actions
  syncOsmBtn: document.getElementById('syncOsmBtn'),
  syncAllNationBtn: document.getElementById('syncAllNationBtn'),
  exportBtn: document.getElementById('exportBtn'),
  locateUserBtn: document.getElementById('locateUserBtn'),

  // Detail card
  cameraDetailCard: document.getElementById('cameraDetailCard'),
  closeDetailCardBtn: document.getElementById('closeDetailCardBtn'),
  cardBadge: document.getElementById('cardBadge'),
  cardRoadName: document.getElementById('cardRoadName'),
  cardDescription: document.getElementById('cardDescription'),
  cardDistrict: document.getElementById('cardDistrict'),
  cardDirection: document.getElementById('cardDirection'),
  cardCoords: document.getElementById('cardCoords'),
  openGoogleMapsBtn: document.getElementById('openGoogleMapsBtn'),
  copyCoordsBtn: document.getElementById('copyCoordsBtn'),
  testVoicePromptBtn: document.getElementById('testVoicePromptBtn'),

  // Toast & Loading
  toast: document.getElementById('toast'),
  loadingOverlay: document.getElementById('loadingOverlay'),
  loadingText: document.getElementById('loadingText')
};

// =========================================================
// 3. INITIALIZE LEAFLET MAP
// =========================================================
function initMap() {
  // Center on Ho Chi Minh City
  map = L.map('map', {
    center: [10.7769, 106.6958],
    zoom: 12,
    zoomControl: false
  });

  // Zoom control top right
  L.control.zoom({ position: 'topright' }).addTo(map);

  // BASE TILE LAYERS
  const googleHD = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
    maxZoom: 20,
    attribution: '© Google Maps HD'
  });

  const googleSatellite = L.tileLayer('https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
    maxZoom: 20,
    attribution: '© Google Hybrid'
  });

  const cartoDark = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    maxZoom: 19,
    attribution: '© CartoDB Dark Matter'
  });

  const osmStandard = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors'
  });

  // Default layer
  googleHD.addTo(map);

  // Layer Switcher Control
  const baseLayers = {
    "Google Maps HD": googleHD,
    "Google Vệ Tinh (Satellite)": googleSatellite,
    "Giao Diện Tối (Carto Dark)": cartoDark,
    "OpenStreetMap Bản Gốc": osmStandard
  };
  L.control.layers(baseLayers, null, { position: 'topright' }).addTo(map);

  // Marker cluster and circle overlay groups
  markerClusterGroup = L.markerClusterGroup({
    maxClusterRadius: 40,
    spiderfyOnMaxZoom: true,
    showCoverageOnHover: false
  });
  map.addLayer(markerClusterGroup);

  radarCirclesGroup = L.layerGroup().addTo(map);

  // Map Click Listener
  map.on('click', (e) => {
    if (isPickingOnMap) {
      dom.newLat.value = e.latlng.lat.toFixed(5);
      dom.newLng.value = e.latlng.lng.toFixed(5);
      isPickingOnMap = false;
      dom.pickOnMapBtn.innerHTML = '<i class="fa-solid fa-map-pin"></i> Chọn Điểm Trên Bản Đồ';
      showToast(`Đã chọn toạ độ: ${e.latlng.lat.toFixed(4)}, ${e.latlng.lng.toFixed(4)}`);
      
      // Auto reverse-geocode street name if possible
      reverseGeocode(e.latlng.lat, e.latlng.lng);
    } else {
      hideCameraDetail();
    }
  });
}

// =========================================================
// 4. LOAD & RENDER CAMERA DATA
// =========================================================
function loadInitialData() {
  if (typeof INITIAL_CAMERAS !== 'undefined' && Array.isArray(INITIAL_CAMERAS)) {
    allCameras = [...INITIAL_CAMERAS];
  } else {
    allCameras = [];
  }
  applyFilters();
}

function createCameraIcon(camera) {
  let innerHtml = '';
  const isSpeed = camera.type === 'SPEED_CAMERA';
  const isRedLight = camera.type === 'RED_LIGHT_CAMERA';
  const isSurveillance = camera.type === 'COLD_FINE_SURVEILLANCE';
  const isProhibit = camera.type === 'MOTORBIKE_PROHIBITED_ZONE';
  const isSpeedSign = camera.type === 'SPEED_LIMIT_SIGN';

  if (isSpeed) {
    // 🔴 CAMERA BẮN TỐC ĐỘ: Biển tròn đỏ viền trắng, icon camera chuyên dụng + số tốc độ
    innerHtml = `
      <div class="vietnam-camera-badge speed-badge" title="Camera bắn tốc độ ${camera.speedLimit} km/h">
        <div class="badge-icon-box">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="white">
            <path d="M4 4h3l2-2h6l2 2h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zm8 3a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm0 2a3 3 0 1 1 0 6 3 3 0 0 1 0-6z"/>
          </svg>
        </div>
        ${camera.speedLimit > 0 ? `<div class="badge-speed-pill">${camera.speedLimit}</div>` : ''}
        <div class="pulse-ring red"></div>
      </div>
    `;
  } else if (isRedLight) {
    // 🚦 CAMERA PHẠT NGUỘI VƯỢT ĐÈN ĐỎ: Cột đèn giao thông 3 màu + CCTV
    innerHtml = `
      <div class="vietnam-camera-badge redlight-badge" title="Camera phạt nguội vượt đèn đỏ">
        <div class="traffic-light-mini">
          <span class="light red"></span>
          <span class="light yellow"></span>
          <span class="light green"></span>
        </div>
        <div class="cctv-lens">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="#FBBF24">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
          </svg>
        </div>
        <div class="pulse-ring amber"></div>
      </div>
    `;
  } else if (isProhibit) {
    // ⛔ BIỂN CẤM XE MÁY: Biển P.111 gạch chéo xe máy
    innerHtml = `
      <div class="vietnam-camera-badge prohibit-badge" title="Cảnh báo: Cấm xe máy!">
        <div class="prohibit-circle">
          <span class="slash"></span>
          <i class="fa-solid fa-motorcycle"></i>
        </div>
        <div class="pulse-ring purple"></div>
      </div>
    `;
  } else {
    // 📹 CAMERA GIÁM SÁT GIAO THÔNG / LÀN ĐƯỜNG: Khiên xanh CCTV
    innerHtml = `
      <div class="vietnam-camera-badge surveillance-badge" title="Camera giám sát giao thông">
        <div class="badge-icon-box">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="white">
            <path d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"/>
          </svg>
        </div>
        <div class="pulse-ring blue"></div>
      </div>
    `;
  }

  return L.divIcon({
    className: 'custom-leaflet-div-icon',
    html: innerHtml,
    iconSize: [44, 44],
    iconAnchor: [22, 22]
  });
}

function renderMapMarkers() {
  markerClusterGroup.clearLayers();
  radarCirclesGroup.clearLayers();
  markersMap.clear();

  const useClustering = dom.toggleClustering.checked;
  const showRadarRings = dom.toggleRadarRings.checked;

  filteredCameras.forEach(cam => {
    const marker = L.marker([cam.latitude, cam.longitude], {
      icon: createCameraIcon(cam),
      title: cam.roadName
    });

    marker.on('click', () => {
      selectCamera(cam, true);
    });

    markersMap.set(cam.id, marker);

    if (useClustering) {
      markerClusterGroup.addLayer(marker);
    } else {
      marker.addTo(map);
    }

    // Add 500m warning radar circle
    if (showRadarRings && (cam.type === 'SPEED_CAMERA' || cam.type === 'RED_LIGHT_CAMERA')) {
      const circle = L.circle([cam.latitude, cam.longitude], {
        radius: 450,
        color: cam.type === 'SPEED_CAMERA' ? '#EF4444' : '#F59E0B',
        fillColor: cam.type === 'SPEED_CAMERA' ? '#EF4444' : '#F59E0B',
        fillOpacity: 0.06,
        weight: 1.2,
        dashArray: '4, 6'
      });
      radarCirclesGroup.addLayer(circle);
    }
  });

  if (!useClustering) {
    map.removeLayer(markerClusterGroup);
  } else {
    if (!map.hasLayer(markerClusterGroup)) {
      map.addLayer(markerClusterGroup);
    }
  }
}

function renderCameraList() {
  dom.cameraList.innerHTML = '';

  if (filteredCameras.length === 0) {
    dom.cameraList.innerHTML = `
      <div style="text-align:center; padding: 40px 20px; color: var(--text-muted);">
        <i class="fa-solid fa-camera-slash" style="font-size: 32px; margin-bottom: 10px; display:block;"></i>
        Không tìm thấy camera phù hợp với bộ lọc.
      </div>
    `;
    return;
  }

  filteredCameras.forEach(cam => {
    const card = document.createElement('div');
    card.className = `camera-item-card ${selectedCamera && selectedCamera.id === cam.id ? 'selected' : ''}`;
    card.dataset.id = cam.id;

    let badgeText = 'BẮN TỐC ĐỘ';
    let badgeClass = 'speed';
    if (cam.type === 'RED_LIGHT_CAMERA') { badgeText = 'VƯỢT ĐÈN ĐỎ'; badgeClass = 'redlight'; }
    else if (cam.type === 'COLD_FINE_SURVEILLANCE') { badgeText = 'PHẠT NGUỘI'; badgeClass = 'surveillance'; }
    else if (cam.type === 'MOTORBIKE_PROHIBITED_ZONE') { badgeText = 'CẤM XE MÁY'; badgeClass = 'prohibit'; }

    card.innerHTML = `
      <div class="item-header">
        <span class="item-badge ${badgeClass}">${badgeText}</span>
        ${cam.speedLimit > 0 ? `<span class="item-speed-pill">${cam.speedLimit} km/h</span>` : ''}
      </div>
      <div class="item-road">${cam.roadName}</div>
      <div class="item-meta">
        <span><i class="fa-solid fa-location-dot"></i> ${cam.districtCity}</span>
        <span><i class="fa-solid fa-compass"></i> ${cam.directionName || 'Hai chiều'}</span>
      </div>
    `;

    card.addEventListener('click', () => {
      selectCamera(cam, true);
    });

    dom.cameraList.appendChild(card);
  });
}

function updateStats() {
  const total = allCameras.length;
  const speed = allCameras.filter(c => c.type === 'SPEED_CAMERA' || c.type === 'SPEED_LIMIT_SIGN').length;
  const redlight = allCameras.filter(c => c.type === 'RED_LIGHT_CAMERA' || c.type === 'COLD_FINE_SURVEILLANCE').length;
  const south = allCameras.filter(c => {
    const dist = (c.districtCity || '').toLowerCase();
    const road = (c.roadName || '').toLowerCase();
    return dist.includes('hcm') || dist.includes('hồ chí minh') || dist.includes('bình dương') || dist.includes('đồng nai') || dist.includes('vũng tàu') || dist.includes('cần thơ') || dist.includes('long an') || c.latitude < 12.0;
  }).length;

  dom.statTotalCam.textContent = total;
  dom.statFilteredCam.textContent = `Hiển thị: ${filteredCameras.length}`;
  dom.statSpeedCam.textContent = speed;
  dom.statRedLightCam.textContent = redlight;
  dom.statSouthCam.textContent = south;
}

// =========================================================
// 5. FILTERING ENGINE
// =========================================================
function applyFilters() {
  const query = dom.searchInput.value.toLowerCase().trim();

  const allowSpeed = dom.filterSpeedCam.checked;
  const allowRedLight = dom.filterRedLightCam.checked;
  const allowSurveillance = dom.filterSurveillanceCam.checked;
  const allowProhibit = dom.filterProhibitCam.checked;
  const allowSpeedSign = dom.filterSpeedSign.checked;

  filteredCameras = allCameras.filter(cam => {
    // 1. Type filter
    if (cam.type === 'SPEED_CAMERA' && !allowSpeed) return false;
    if (cam.type === 'RED_LIGHT_CAMERA' && !allowRedLight) return false;
    if (cam.type === 'COLD_FINE_SURVEILLANCE' && !allowSurveillance) return false;
    if (cam.type === 'MOTORBIKE_PROHIBITED_ZONE' && !allowProhibit) return false;
    if (cam.type === 'SPEED_LIMIT_SIGN' && !allowSpeedSign) return false;

    // 2. Speed range filter
    if (currentSpeedFilter !== 'ALL') {
      const speedVal = parseInt(currentSpeedFilter, 10);
      if (speedVal === 90 && cam.speedLimit < 90) return false;
      if (speedVal !== 90 && cam.speedLimit !== speedVal) return false;
    }

    // 3. Region filter
    if (currentRegion === 'HCM') {
      const d = (cam.districtCity || '').toLowerCase();
      if (!d.includes('hcm') && !d.includes('hồ chí minh') && !d.includes('sài gòn') && !d.includes('thủ đức') && !d.includes('quận') && !d.includes('huyện')) return false;
    } else if (currentRegion === 'SOUTH') {
      if (cam.latitude > 12.5) return false;
    } else if (currentRegion === 'CENTRAL') {
      if (cam.latitude <= 12.5 || cam.latitude >= 18.0) return false;
    } else if (currentRegion === 'NORTH') {
      if (cam.latitude < 18.0) return false;
    }

    // 4. Keyword search
    if (query) {
      const matchText = `${cam.roadName} ${cam.districtCity} ${cam.description} ${cam.id} ${cam.latitude} ${cam.longitude}`.toLowerCase();
      if (!matchText.includes(query)) return false;
    }

    return true;
  });

  updateStats();
  renderMapMarkers();
  renderCameraList();
}

// =========================================================
// 6. CAMERA SELECTION & DETAILS MODAL
// =========================================================
function selectCamera(camera, panToMap = false) {
  selectedCamera = camera;

  // Highlight list item
  document.querySelectorAll('.camera-item-card').forEach(el => {
    el.classList.toggle('selected', el.dataset.id === camera.id);
  });

  // Pan to map
  if (panToMap) {
    map.flyTo([camera.latitude, camera.longitude], 16, {
      animate: true,
      duration: 0.8
    });
  }

  // Populate Details HUD
  let badgeText = `BẮN TỐC ĐỘ ${camera.speedLimit} KM/H`;
  dom.cardBadge.className = 'card-badge';
  if (camera.type === 'RED_LIGHT_CAMERA') {
    badgeText = 'PHẠT NGUỘI VƯỢT ĐÈN ĐỎ';
    dom.cardBadge.style.background = '#D97706';
  } else if (camera.type === 'COLD_FINE_SURVEILLANCE') {
    badgeText = 'GIÁM SÁT GIAO THÔNG & TỐC ĐỘ';
    dom.cardBadge.style.background = '#0284C7';
  } else if (camera.type === 'MOTORBIKE_PROHIBITED_ZONE') {
    badgeText = 'CẢNH BÁO ĐƯỜNG CẤM XE MÁY';
    dom.cardBadge.style.background = '#7E22CE';
  } else {
    dom.cardBadge.style.background = '#DC2626';
  }

  dom.cardBadge.textContent = badgeText;
  dom.cardRoadName.textContent = camera.roadName;
  dom.cardDescription.textContent = camera.description;
  dom.cardDistrict.textContent = camera.districtCity;
  dom.cardDirection.textContent = camera.directionName || 'Hai chiều';
  dom.cardCoords.textContent = `${camera.latitude.toFixed(4)}, ${camera.longitude.toFixed(4)}`;

  dom.cameraDetailCard.classList.add('active');
}

function hideCameraDetail() {
  selectedCamera = null;
  dom.cameraDetailCard.classList.remove('active');
  document.querySelectorAll('.camera-item-card').forEach(el => el.classList.remove('selected'));
}

// =========================================================
// 7. OPENSTREETMAP OVERPASS API LIVE SYNC
// =========================================================
async function fetchOverpassCameras(regionType = 'SOUTH') {
  showLoading(`Đang kết nối Overpass API để kéo camera ${regionType === 'SOUTH' ? 'Miền Nam' : 'Toàn Quốc'}...`);

  // Bounding box: [south, west, north, east]
  const bbox = regionType === 'SOUTH' 
    ? '8.5,104.5,12.2,108.5' 
    : '8.0,102.0,23.5,110.0';

  const query = `
    [out:json][timeout:35];
    (
      node["highway"="speed_camera"](${bbox});
      node["traffic_signals:camera"="yes"](${bbox});
      node["man_made"="surveillance"](${bbox});
      node["enforcement"="maxspeed"](${bbox});
    );
    out body;
  `;

  const endpoints = [
    'https://overpass-api.de/api/interpreter',
    'https://maps.mail.ru/osm/tools/overpass/api/interpreter',
    'https://overpass.kumi.systems/api/interpreter'
  ];

  let data = null;
  for (const url of endpoints) {
    try {
      const resp = await fetch(url, {
        method: 'POST',
        body: query
      });
      if (resp.ok) {
        data = await resp.json();
        break;
      }
    } catch (e) {
      console.warn(`Endpoint ${url} failed, trying next...`);
    }
  }

  hideLoading();

  if (!data || !data.elements) {
    showToast('Không thể kết nối đến máy chủ Overpass. Vui lòng thử lại sau!', true);
    return;
  }

  let addedCount = 0;
  data.elements.forEach(node => {
    const id = `osm_cam_${node.id}`;
    if (!allCameras.some(c => c.id === id || (Math.abs(c.latitude - node.lat) < 0.0003 && Math.abs(c.longitude - node.lon) < 0.0003))) {
      const tags = node.tags || {};
      const maxspeed = parseInt(tags.maxspeed || tags['maxspeed:motorcycle'] || '60', 10);
      const isSpeed = tags.highway === 'speed_camera' || tags.enforcement === 'maxspeed';
      const isRedLight = tags['traffic_signals:camera'] === 'yes';

      allCameras.push({
        id: id,
        latitude: node.lat,
        longitude: node.lon,
        type: isSpeed ? 'SPEED_CAMERA' : (isRedLight ? 'RED_LIGHT_CAMERA' : 'COLD_FINE_SURVEILLANCE'),
        roadName: tags.name || tags.road || tags['addr:street'] || `Camera OSM #${node.id}`,
        speedLimit: isNaN(maxspeed) ? 60 : maxspeed,
        description: tags.description || `Camera giám sát trích xuất từ OpenStreetMap (Node ID: ${node.id})`,
        districtCity: tags['addr:city'] || tags['addr:district'] || (node.lat < 11.2 ? 'TP.HCM / Nam Bộ' : 'Việt Nam'),
        bearingDegrees: tags.direction ? parseFloat(tags.direction) : null,
        directionName: tags.direction ? `${tags.direction}°` : 'Hai chiều'
      });
      addedCount++;
    }
  });

  applyFilters();
  showToast(`Đã đồng bộ thành công thêm ${addedCount} camera mới từ OpenStreetMap!`);
}

// =========================================================
// 8. RADAR SIMULATOR WITH VIETNAMESE TTS
// =========================================================
function startRadarSimulation() {
  if (simInterval) clearInterval(simInterval);
  simRouteIndex = 0;

  dom.startSimBtn.disabled = true;
  dom.stopSimBtn.disabled = false;

  const startPos = SIM_ROUTE[0];
  map.flyTo(startPos, 16);

  if (!simMarker) {
    const icon = L.divIcon({
      className: 'sim-vehicle-marker',
      html: '<i class="fa-solid fa-motorcycle"></i>',
      iconSize: [44, 44],
      iconAnchor: [22, 22]
    });
    simMarker = L.marker(startPos, { icon }).addTo(map);
  } else {
    simMarker.setLatLng(startPos);
  }

  showToast('Đang khởi chạy giả lập xe máy dọc đường Võ Văn Kiệt...');

  simInterval = setInterval(() => {
    if (simRouteIndex >= SIM_ROUTE.length - 1) {
      stopRadarSimulation();
      showToast('Đã hoàn thành hành trình mô phỏng!');
      return;
    }

    simRouteIndex++;
    const pos = SIM_ROUTE[simRouteIndex];
    simMarker.setLatLng(pos);
    map.panTo(pos);

    // Check approaching cameras within 600m
    const currentSpeed = parseInt(dom.simSpeedSlider.value, 10);
    checkRadarThreats(pos[0], pos[1], currentSpeed);
  }, 2500);
}

function stopRadarSimulation() {
  if (simInterval) {
    clearInterval(simInterval);
    simInterval = null;
  }
  dom.startSimBtn.disabled = false;
  dom.stopSimBtn.disabled = true;
  dom.simStatusText.textContent = 'Mô phỏng đã dừng.';
}

function checkRadarThreats(lat, lng, speed) {
  let nearest = null;
  let minDistance = 999999;

  allCameras.forEach(cam => {
    const dist = calculateDistance(lat, lng, cam.latitude, cam.longitude);
    if (dist < minDistance) {
      minDistance = dist;
      nearest = cam;
    }
  });

  if (nearest && minDistance <= 600) {
    const isOver = speed > nearest.speedLimit && nearest.speedLimit > 0;
    dom.simStatusText.innerHTML = `
      <div style="color: ${isOver ? 'var(--red-accent)' : 'var(--cyan-accent)'}; font-weight: 700;">
        <i class="fa-solid fa-triangle-exclamation"></i> CẢNH BÁO: ${nearest.roadName} (${Math.round(minDistance)}m)
      </div>
      <div>Tốc độ hiện tại: <b>${speed} km/h</b> / Cho phép: <b>${nearest.speedLimit} km/h</b></div>
      ${isOver ? '<div style="color:#EF4444; font-weight:800; margin-top:2px;">⚠️ BẠN ĐANG VƯỢT QUÁ TỐC ĐỘ!</div>' : ''}
    `;

    // Trigger Voice Prompt
    speakVoiceWarning(nearest, Math.round(minDistance), speed);
  } else {
    dom.simStatusText.textContent = `Tốc độ: ${speed} km/h. Không có camera trong phạm vi 600m.`;
  }
}

// Calculate distance in meters (Haversine formula)
function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

// Voice synthesis in Vietnamese
function speakVoiceWarning(camera, distanceMeters, speed) {
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();

  let text = '';
  if (camera.type === 'SPEED_CAMERA') {
    text = `Phía trước ${distanceMeters} mét, có camera bắn tốc độ, giới hạn ${camera.speedLimit} kilomet trên giờ.`;
    if (speed > camera.speedLimit) {
      text += ' Vui lòng giảm tốc độ!';
    }
  } else if (camera.type === 'RED_LIGHT_CAMERA') {
    text = `Phía trước ${distanceMeters} mét, có camera phạt nguội vượt đèn đỏ.`;
  } else if (camera.type === 'MOTORBIKE_PROHIBITED_ZONE') {
    text = `Chú ý! Đoạn đường phía trước cấm xe máy!`;
  } else {
    text = `Phía trước ${distanceMeters} mét, có camera giám sát giao thông.`;
  }

  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'vi-VN';
  utterance.rate = 1.1;
  window.speechSynthesis.speak(utterance);
}

// =========================================================
// 9. EXPORT & UTILITIES
// =========================================================
function exportData() {
  const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(allCameras, null, 2));
  const downloadAnchor = document.createElement('a');
  downloadAnchor.setAttribute("href", dataStr);
  downloadAnchor.setAttribute("download", `vietnam_cameras_${Date.now()}.json`);
  document.body.appendChild(downloadAnchor);
  downloadAnchor.click();
  downloadAnchor.remove();
  showToast('Đã xuất thành công file JSON toàn bộ camera!');
}

function showToast(msg, isError = false) {
  dom.toast.textContent = msg;
  dom.toast.style.borderColor = isError ? 'var(--red-accent)' : 'var(--border-highlight)';
  dom.toast.classList.add('show');
  setTimeout(() => dom.toast.classList.remove('show'), 3500);
}

function showLoading(text) {
  dom.loadingText.textContent = text;
  dom.loadingOverlay.classList.add('active');
}

function hideLoading() {
  dom.loadingOverlay.classList.remove('active');
}

async function reverseGeocode(lat, lng) {
  try {
    const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`);
    if (res.ok) {
      const data = await res.json();
      if (data && data.display_name) {
        const road = data.address.road || data.address.suburb || data.display_name.split(',')[0];
        const district = [data.address.suburb, data.address.city || data.address.state].filter(Boolean).join(', ');
        dom.newRoadName.value = road;
        dom.newDistrict.value = district;
      }
    }
  } catch (e) {
    // Ignore offline/throttled reverse geocode
  }
}

// =========================================================
// 10. ATTACH EVENT LISTENERS
// =========================================================
function setupEventListeners() {
  // Sidebar toggles
  dom.toggleSidebarBtn.addEventListener('click', () => {
    dom.sidebar.classList.add('collapsed');
    dom.openSidebarBtn.style.display = 'flex';
  });

  dom.openSidebarBtn.addEventListener('click', () => {
    dom.sidebar.classList.remove('collapsed');
    dom.openSidebarBtn.style.display = 'none';
  });

  // Tabs
  dom.navTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      dom.navTabs.forEach(t => t.classList.remove('active'));
      dom.tabPanes.forEach(p => p.classList.remove('active'));
      tab.classList.add('active');
      document.getElementById(tab.dataset.tab).classList.add('active');
    });
  });

  // Search input & clear
  dom.searchInput.addEventListener('input', () => {
    dom.clearSearchBtn.style.display = dom.searchInput.value ? 'block' : 'none';
    applyFilters();
  });

  dom.clearSearchBtn.addEventListener('click', () => {
    dom.searchInput.value = '';
    dom.clearSearchBtn.style.display = 'none';
    applyFilters();
  });

  // Region pills
  dom.regionPills.forEach(pill => {
    pill.addEventListener('click', () => {
      dom.regionPills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      currentRegion = pill.dataset.region;
      applyFilters();

      // Pan to region center
      if (currentRegion === 'HCM') map.flyTo([10.7769, 106.6958], 12);
      else if (currentRegion === 'SOUTH') map.flyTo([10.5, 106.5], 9);
      else if (currentRegion === 'CENTRAL') map.flyTo([16.0544, 108.2022], 9);
      else if (currentRegion === 'NORTH') map.flyTo([21.0285, 105.8542], 9);
      else if (currentRegion === 'ALL') map.flyTo([16.0, 106.0], 6);
    });
  });

  // Filter checkboxes
  [dom.filterSpeedCam, dom.filterRedLightCam, dom.filterSurveillanceCam, dom.filterProhibitCam, dom.filterSpeedSign].forEach(cb => {
    cb.addEventListener('change', applyFilters);
  });

  // Speed filter buttons
  dom.speedFilterBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      dom.speedFilterBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      currentSpeedFilter = btn.dataset.speed;
      applyFilters();
    });
  });

  dom.toggleClustering.addEventListener('change', renderMapMarkers);
  dom.toggleRadarRings.addEventListener('change', renderMapMarkers);

  // Close detail card
  dom.closeDetailCardBtn.addEventListener('click', hideCameraDetail);

  // Open Google Maps
  dom.openGoogleMapsBtn.addEventListener('click', () => {
    if (selectedCamera) {
      window.open(`https://www.google.com/maps?q=${selectedCamera.latitude},${selectedCamera.longitude}`, '_blank');
    }
  });

  // Copy coords
  dom.copyCoordsBtn.addEventListener('click', () => {
    if (selectedCamera) {
      const text = `${selectedCamera.latitude.toFixed(5)}, ${selectedCamera.longitude.toFixed(5)}`;
      navigator.clipboard.writeText(text);
      showToast(`Đã sao chép toạ độ: ${text}`);
    }
  });

  // Test voice prompt
  dom.testVoicePromptBtn.addEventListener('click', () => {
    if (selectedCamera) {
      speakVoiceWarning(selectedCamera, 350, 75);
    }
  });

  // Add camera form
  dom.pickOnMapBtn.addEventListener('click', () => {
    isPickingOnMap = true;
    dom.pickOnMapBtn.innerHTML = '<i class="fa-solid fa-hand-pointer text-amber"></i> Hãy click một điểm trên bản đồ...';
    showToast('Hãy click vào một vị trí trên bản đồ để ghim toạ độ!');
  });

  dom.addCameraForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const newCam = {
      id: `custom_cam_${Date.now()}`,
      latitude: parseFloat(dom.newLat.value),
      longitude: parseFloat(dom.newLng.value),
      type: dom.newType.value,
      roadName: dom.newRoadName.value,
      speedLimit: parseInt(dom.newSpeedLimit.value, 10) || 50,
      description: dom.newDescription.value || 'Camera người dùng thêm vào',
      districtCity: dom.newDistrict.value || 'Việt Nam',
      bearingDegrees: 0,
      directionName: 'Hai chiều'
    };

    // Validate coords
    if (isNaN(newCam.latitude) || isNaN(newCam.longitude)) {
      showToast('❌ Vui lòng chọn vị trí trên bản đồ hoặc nhập toạ độ hợp lệ!', true);
      return;
    }

    // Show loading
    const submitBtn = dom.addCameraForm.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang lưu...';

    try {
      // Call server API to persist
      const resp = await fetch('/api/add-camera', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newCam)
      });
      const result = await resp.json();

      if (result.ok) {
        allCameras.unshift(newCam);
        applyFilters();
        selectCamera(newCam, true);
        showToast(`✅ Đã lưu camera "${newCam.roadName}" (${result.count} camera tổng). Đã sync Android!`);
        dom.addCameraForm.reset();
        dom.pickOnMapBtn.innerHTML = '<i class="fa-solid fa-map-pin"></i> Chọn Điểm Trên Bản Đồ';
      } else if (resp.status === 409) {
        showToast(`⚠️ Camera tại vị trí này đã tồn tại: ${result.existing.roadName}`, true);
      } else {
        throw new Error(result.error || 'Unknown error');
      }
    } catch (err) {
      // Fallback: add to memory only
      allCameras.unshift(newCam);
      applyFilters();
      selectCamera(newCam, true);
      showToast(`⚠️ Thêm thành công (chỉ trong phiên làm việc - server không lưu được: ${err.message})`, true);
      dom.addCameraForm.reset();
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> Lưu Camera Vào Database';
    }
  });

  // Simulator controls
  dom.simSpeedSlider.addEventListener('input', () => {
    dom.simSpeedDisplay.textContent = dom.simSpeedSlider.value;
  });

  dom.startSimBtn.addEventListener('click', startRadarSimulation);
  dom.stopSimBtn.addEventListener('click', stopRadarSimulation);

  // Sync Buttons
  dom.syncOsmBtn.addEventListener('click', () => fetchOverpassCameras('SOUTH'));
  dom.syncAllNationBtn.addEventListener('click', () => fetchOverpassCameras('ALL'));
  dom.exportBtn.addEventListener('click', exportData);

  // Geolocation
  dom.locateUserBtn.addEventListener('click', () => {
    if (!navigator.geolocation) {
      showToast('Trình duyệt không hỗ trợ Geolocation!', true);
      return;
    }
    showLoading('Đang lấy vị trí GPS của bạn...');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        hideLoading();
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        map.flyTo([lat, lng], 16);

        if (!userLocationMarker) {
          const userIcon = L.divIcon({
            className: 'sim-vehicle-marker',
            html: '<i class="fa-solid fa-location-arrow"></i>',
            iconSize: [36, 36],
            iconAnchor: [18, 18]
          });
          userLocationMarker = L.marker([lat, lng], { icon: userIcon }).addTo(map);
        } else {
          userLocationMarker.setLatLng([lat, lng]);
        }
        showToast('Đã định vị thành công vị trí của bạn!');
      },
      (err) => {
        hideLoading();
        showToast('Không thể lấy toạ độ GPS: ' + err.message, true);
      }
    );
  });
}

// =========================================================
// 11. BOOTSTRAP APP
// =========================================================
document.addEventListener('DOMContentLoaded', () => {
  initMap();
  setupEventListeners();
  loadInitialData();
});
