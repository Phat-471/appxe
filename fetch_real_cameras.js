/**
 * FETCH REAL VIETNAM TRAFFIC CAMERAS FROM OPENSTREETMAP OVERPASS API
 * Kéo dữ liệu camera giao thông THỰC TẾ từ OSM - có tọa độ GPS chính xác
 * Coverage: TP.HCM + Bình Dương + Đồng Nai + Long An + Bà Rịa-Vũng Tàu
 */
const https = require('https');
const fs = require('fs');
const path = require('path');

// Overpass API query - lấy TẤT CẢ camera giao thông ở miền Nam VN
const OVERPASS_QUERY = `
[out:json][timeout:60];
(
  // Camera giao thông - speed camera
  node["highway"="speed_camera"](9.0,105.0,11.5,107.5);
  // Camera giám sát giao thông
  node["man_made"="surveillance"]["surveillance:type"="camera"](9.0,105.0,11.5,107.5);
  // Camera enforcement
  node["enforcement"="maxspeed"](9.0,105.0,11.5,107.5);
  node["enforcement"="traffic_signals"](9.0,105.0,11.5,107.5);
  node["enforcement"="check"](9.0,105.0,11.5,107.5);
  // Red light camera
  node["camera:type"="fixed"](9.0,105.0,11.5,107.5);
);
out body;
`;

function fetchOverpass(query) {
  return new Promise((resolve, reject) => {
    const body = 'data=' + encodeURIComponent(query);
    const options = {
      hostname: 'overpass-api.de',
      path: '/api/interpreter',
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(body),
        'User-Agent': 'VNTrafficRadar/2.0 (educational project)'
      }
    };

    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          reject(new Error('JSON parse error: ' + e.message));
        }
      });
    });

    req.on('error', reject);
    req.setTimeout(65000, () => { req.destroy(); reject(new Error('Timeout')); });
    req.write(body);
    req.end();
  });
}

function mapOsmToCamera(node, index) {
  const tags = node.tags || {};
  
  // Xác định loại camera
  let type = 'COLD_FINE_SURVEILLANCE';
  if (tags.highway === 'speed_camera' || tags.enforcement === 'maxspeed') {
    type = 'SPEED_CAMERA';
  } else if (tags.enforcement === 'traffic_signals' || tags['camera:type'] === 'red_light') {
    type = 'RED_LIGHT_CAMERA';
  } else if (tags['surveillance:type'] === 'camera') {
    type = 'COLD_FINE_SURVEILLANCE';
  }

  // Speed limit
  const maxspeedRaw = tags.maxspeed || tags['maxspeed:advisory'] || '50';
  const speedLimit = parseInt(maxspeedRaw) || 50;

  // Tên đường
  const roadName = tags.name || tags['addr:street'] || tags.description || 
                   `Camera OSM #${node.id}`;
  
  // Mô tả
  const typeLabel = {
    'SPEED_CAMERA': 'Camera bắn tốc độ',
    'RED_LIGHT_CAMERA': 'Camera phạt nguội vượt đèn đỏ',
    'COLD_FINE_SURVEILLANCE': 'Camera giám sát giao thông'
  }[type] || 'Camera';

  return {
    id: `osm_${node.id}`,
    latitude: node.lat,
    longitude: node.lon,
    type,
    roadName,
    speedLimit,
    description: `${typeLabel} - Nguồn: OpenStreetMap (ID: ${node.id})`,
    districtCity: tags['addr:city'] || tags['is_in:city'] || 'TP.HCM & Miền Nam',
    bearingDegrees: parseFloat(tags.direction || '0'),
    directionName: tags.direction || 'Không rõ hướng',
    osmId: node.id,
    osmTags: tags
  };
}

async function main() {
  console.log('🔍 Đang kéo dữ liệu camera thật từ OpenStreetMap Overpass API...');
  console.log('📍 Khu vực: TP.HCM + Bình Dương + Đồng Nai + Long An + BR-VT');
  console.log('⏳ Vui lòng chờ 30-60 giây...\n');

  let osmCameras = [];
  
  try {
    const result = await fetchOverpass(OVERPASS_QUERY);
    const nodes = result.elements || [];
    console.log(`✅ OSM trả về ${nodes.length} điểm camera thực tế`);
    
    osmCameras = nodes.map((node, i) => mapOsmToCamera(node, i));
  } catch (err) {
    console.error('❌ Không kết nối được Overpass API:', err.message);
    console.log('⚠️  Sử dụng dữ liệu fallback đã verify bằng Google Maps...');
  }

  // Luôn include các camera đã verify thủ công bằng Google Maps
  const verifiedCameras = [
    // === NGÃ TƯ LŨY BÁN BÍCH - HÒA BÌNH (Google Maps verified 28/8/2026) ===
    {
      id: 'verified_lbb_hb_01',
      latitude: 10.770266,
      longitude: 106.631760,
      type: 'RED_LIGHT_CAMERA',
      roadName: 'Ngã 4 Lũy Bán Bích giao Hòa Bình',
      speedLimit: 50,
      description: 'Camera phạt nguội vượt đèn đỏ - Tọa độ xác minh Google Maps',
      districtCity: 'Quận Tân Phú, TP.HCM',
      bearingDegrees: 0,
      directionName: 'Ngã 4 Lũy Bán Bích - Hòa Bình'
    },
    {
      id: 'verified_lbb_hb_02',
      latitude: 10.770560,
      longitude: 106.631900,
      type: 'SPEED_CAMERA',
      roadName: 'Lũy Bán Bích (Highlands Coffee / ILA)',
      speedLimit: 50,
      description: 'Camera bắn tốc độ - Tọa độ xác minh Google Maps',
      districtCity: 'Quận Tân Phú, TP.HCM',
      bearingDegrees: 345,
      directionName: 'Hướng Thoại Ngọc Hầu'
    }
  ];

  // Gộp: OSM thực tế + verified thủ công, loại trùng
  const allCameras = [...osmCameras, ...verifiedCameras].filter((cam, idx, arr) => {
    // Loại camera quá gần nhau (< 20m)
    return !arr.slice(0, idx).some(prev => {
      const dlat = (prev.latitude - cam.latitude) * 111000;
      const dlng = (prev.longitude - cam.longitude) * 98000;
      return Math.sqrt(dlat*dlat + dlng*dlng) < 20;
    });
  });

  console.log(`\n📊 Tổng camera hợp lệ: ${allCameras.length}`);
  console.log(`   - OSM thực tế: ${osmCameras.length}`);
  console.log(`   - Verified thủ công: ${verifiedCameras.length}`);

  const types = {};
  allCameras.forEach(c => types[c.type] = (types[c.type] || 0) + 1);
  console.log('   Phân loại:', JSON.stringify(types));

  // Ghi camera_data.json
  const jsonPath = path.join(__dirname, 'web_camera_inspector', 'camera_data.json');
  fs.writeFileSync(jsonPath, JSON.stringify(allCameras, null, 2), 'utf-8');
  console.log(`\n✅ Đã ghi ${allCameras.length} cameras → camera_data.json`);

  // Ghi camera_data.js (dùng cho web inspector)
  const jsPath = path.join(__dirname, 'web_camera_inspector', 'camera_data.js');
  const cleanCameras = allCameras.map(c => {
    const { osmTags, osmId, ...clean } = c;
    return clean;
  });
  const jsContent = `// VIETNAM TRAFFIC CAMERAS - Real OSM Data fetched ${new Date().toISOString()}\n// Total: ${cleanCameras.length} cameras\nconst INITIAL_CAMERAS = ${JSON.stringify(cleanCameras, null, 2)};\n`;
  fs.writeFileSync(jsPath, jsContent, 'utf-8');
  console.log(`✅ Đã ghi ${cleanCameras.length} cameras → camera_data.js`);

  // Ghi VietnamTrafficData.kt
  const ktCameras = cleanCameras.map(c => {
    const typeMap = {
      'SPEED_CAMERA': 'CameraType.SPEED_CAMERA',
      'RED_LIGHT_CAMERA': 'CameraType.RED_LIGHT_CAMERA',
      'COLD_FINE_SURVEILLANCE': 'CameraType.COLD_FINE_SURVEILLANCE',
      'MOTORBIKE_PROHIBITED_ZONE': 'CameraType.MOTORBIKE_PROHIBITED_ZONE'
    };
    const ktType = typeMap[c.type] || 'CameraType.COLD_FINE_SURVEILLANCE';
    const esc = s => String(s || '').replace(/"/g, '\\"').replace(/\\/g, '\\\\');
    return `    TrafficCamera(\n      id = "${esc(c.id)}",\n      latitude = ${c.latitude},\n      longitude = ${c.longitude},\n      type = ${ktType},\n      roadName = "${esc(c.roadName)}",\n      speedLimit = ${c.speedLimit || 50},\n      description = "${esc(c.description)}",\n      districtCity = "${esc(c.districtCity)}",\n      bearingDegrees = ${c.bearingDegrees || 0}f,\n      directionName = "${esc(c.directionName || '')}"\n    )`;
  }).join(',\n');

  const ktPath = path.join(__dirname, 'app', 'src', 'main', 'java', 'com', 'example', 'data', 'VietnamTrafficData.kt');
  const existingKt = fs.readFileSync(ktPath, 'utf-8');
  const startMarker = 'val ALL_CAMERAS: List<TrafficCamera> = listOf(';
  const endMarker = '  ) // END_ALL_CAMERAS';
  const startIdx = existingKt.indexOf(startMarker);
  const endIdx = existingKt.indexOf(endMarker);

  if (startIdx !== -1 && endIdx !== -1) {
    const newKt = existingKt.substring(0, startIdx + startMarker.length + 1) +
      ktCameras + '\n' +
      existingKt.substring(endIdx);
    fs.writeFileSync(ktPath, newKt, 'utf-8');
    console.log(`✅ Đã sync ${cleanCameras.length} cameras → VietnamTrafficData.kt`);
  } else {
    console.log('⚠️  Không tìm thấy marker trong VietnamTrafficData.kt, bỏ qua sync Kotlin');
  }

  console.log('\n🎉 HOÀN TẤT! Reload http://localhost:3000 để xem dữ liệu mới.');
}

main().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
