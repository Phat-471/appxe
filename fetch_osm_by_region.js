/**
 * Fetch Vietnam cameras by region - chia nhỏ tránh timeout
 */
const https = require('https');
const fs = require('fs');
const path = require('path');

// Chia nhỏ theo khu vực để tránh timeout
const REGIONS = [
  { name: 'TP.HCM nội thành',     bbox: [10.65,106.55,10.90,106.85] },
  { name: 'TP.HCM ngoại thành',   bbox: [10.40,106.45,10.65,106.95] },
  { name: 'Bình Dương',            bbox: [10.90,106.55,11.30,106.95] },
  { name: 'Đồng Nai',              bbox: [10.70,106.85,11.20,107.40] },
  { name: 'Long An + Tiền Giang',  bbox: [10.10,105.80,10.65,106.55] },
  { name: 'Bà Rịa-Vũng Tàu',     bbox: [10.20,107.00,10.65,107.50] },
  { name: 'Bình Thuận + Lâm Đồng',bbox: [10.80,107.40,11.80,108.50] },
  { name: 'Đắk Lắk + Đắk Nông',  bbox: [11.50,107.50,13.00,108.50] },
  { name: 'Khánh Hòa + Phú Yên',  bbox: [12.50,108.80,13.50,109.50] },
  { name: 'Đà Nẵng + Quảng Nam',  bbox: [15.50,107.90,16.20,108.60] },
  { name: 'Hà Nội',               bbox: [20.90,105.60,21.25,106.00] },
  { name: 'Hải Phòng',            bbox: [20.70,106.50,20.95,106.95] },
];

function buildQuery(bbox) {
  const [s,w,n,e] = bbox;
  return `[out:json][timeout:30];
(
  node["highway"="speed_camera"](${s},${w},${n},${e});
  node["enforcement"="maxspeed"](${s},${w},${n},${e});
  node["enforcement"="traffic_signals"](${s},${w},${n},${e});
  node["camera:type"="fixed"](${s},${w},${n},${e});
  node["man_made"="surveillance"]["surveillance"="traffic"](${s},${w},${n},${e});
);
out body;`;
}

function post(query) {
  return new Promise((resolve, reject) => {
    const body = 'data=' + encodeURIComponent(query);
    const opts = {
      hostname: 'overpass-api.de',
      path: '/api/interpreter',
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(body),
        'User-Agent': 'VNTrafficApp/2.0'
      }
    };
    const req = https.request(opts, res => {
      let d = '';
      res.on('data', c => d += c);
      res.on('end', () => {
        if (d.trim().startsWith('<')) {
          reject(new Error('HTML response (server error)'));
        } else {
          try { resolve(JSON.parse(d)); }
          catch (e) { reject(new Error('JSON parse error: ' + d.slice(0,100))); }
        }
      });
    });
    req.setTimeout(35000, () => { req.destroy(); reject(new Error('Timeout')); });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

function mapNode(node) {
  const tags = node.tags || {};
  let type = 'COLD_FINE_SURVEILLANCE';
  if (tags.highway === 'speed_camera' || tags.enforcement === 'maxspeed') type = 'SPEED_CAMERA';
  else if (tags.enforcement === 'traffic_signals') type = 'RED_LIGHT_CAMERA';
  const speed = parseInt(tags.maxspeed) || 50;
  const name = tags.name || tags.description || tags['addr:street'] || `OSM #${node.id}`;
  return {
    id: `osm_${node.id}`,
    latitude: node.lat,
    longitude: node.lon,
    type,
    roadName: name,
    speedLimit: speed,
    description: `Camera giao thông (OpenStreetMap ID: ${node.id})`,
    districtCity: tags['addr:city'] || tags['is_in'] || 'Việt Nam',
    bearingDegrees: parseFloat(tags.direction || '0'),
    directionName: tags.direction || ''
  };
}

async function main() {
  console.log('🇻🇳 Fetching cameras from ALL Vietnam regions...\n');
  
  const allNodes = new Map(); // dedup by OSM id
  
  for (const region of REGIONS) {
    process.stdout.write(`  📍 ${region.name}... `);
    try {
      const result = await post(buildQuery(region.bbox));
      const nodes = result.elements || [];
      nodes.forEach(n => allNodes.set(n.id, n));
      console.log(`✅ ${nodes.length} cameras`);
    } catch (err) {
      console.log(`❌ ${err.message}`);
    }
    // throttle
    await new Promise(r => setTimeout(r, 1500));
  }

  console.log(`\n📊 Tổng cộng: ${allNodes.size} cameras từ OSM`);

  const cameras = Array.from(allNodes.values()).map(mapNode);
  
  // Count by type
  const types = {};
  cameras.forEach(c => types[c.type] = (types[c.type]||0)+1);
  console.log('Phân loại:', JSON.stringify(types));
  
  // Save
  fs.writeFileSync(path.join(__dirname, 'osm_vietnam_cameras_raw.json'), JSON.stringify(cameras, null, 2));
  console.log('✅ Saved to osm_vietnam_cameras_raw.json');
  
  // Update web inspector files
  const jsContent = `// Vietnam Traffic Cameras from OpenStreetMap - ${new Date().toISOString()}\n// Total: ${cameras.length}\nconst INITIAL_CAMERAS = ${JSON.stringify(cameras, null, 2)};\n`;
  fs.writeFileSync(path.join(__dirname, 'web_camera_inspector', 'camera_data.js'), jsContent);
  fs.writeFileSync(path.join(__dirname, 'web_camera_inspector', 'camera_data.json'), JSON.stringify(cameras, null, 2));
  console.log(`✅ Updated camera_data.js & camera_data.json with ${cameras.length} real cameras`);
}

main().catch(e => { console.error('FATAL:', e.message); });
