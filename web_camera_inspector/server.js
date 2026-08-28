const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const DATA_JSON = path.join(__dirname, 'camera_data.json');
const DATA_JS   = path.join(__dirname, 'camera_data.js');
const KOTLIN_FILE = path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'example', 'data', 'VietnamTrafficData.kt');

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.css':  'text/css; charset=utf-8',
  '.js':   'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png':  'image/png',
  '.jpg':  'image/jpeg',
  '.svg':  'image/svg+xml'
};

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', c => body += c);
    req.on('end', () => {
      try { resolve(JSON.parse(body)); }
      catch (e) { reject(e); }
    });
    req.on('error', reject);
  });
}

function writeCameraFiles(cameras) {
  // Save JSON
  fs.writeFileSync(DATA_JSON, JSON.stringify(cameras, null, 2), 'utf-8');

  // Save JS for web
  const jsContent = `// VN Traffic Cameras - Updated ${new Date().toISOString()}\n// Total: ${cameras.length}\nconst INITIAL_CAMERAS = ${JSON.stringify(cameras, null, 2)};\n`;
  fs.writeFileSync(DATA_JS, jsContent, 'utf-8');

  return cameras.length;
}

function syncToKotlin(cameras) {
  try {
    const typeMap = {
      'SPEED_CAMERA': 'CameraType.SPEED_CAMERA',
      'RED_LIGHT_CAMERA': 'CameraType.RED_LIGHT_CAMERA',
      'COLD_FINE_SURVEILLANCE': 'CameraType.COLD_FINE_SURVEILLANCE',
      'MOTORBIKE_PROHIBITED_ZONE': 'CameraType.MOTORBIKE_PROHIBITED_ZONE',
      'SPEED_LIMIT_SIGN': 'CameraType.COLD_FINE_SURVEILLANCE',
      'COMMUNITY_REPORT': 'CameraType.COLD_FINE_SURVEILLANCE'
    };
    const esc = s => String(s||'').replace(/\\/g,'\\\\').replace(/"/g,'\\"');
    const ktEntries = cameras.map(c => {
      const t = typeMap[c.type] || 'CameraType.COLD_FINE_SURVEILLANCE';
      return `    TrafficCamera(\n      id = "${esc(c.id)}",\n      latitude = ${c.latitude},\n      longitude = ${c.longitude},\n      type = ${t},\n      roadName = "${esc(c.roadName)}",\n      speedLimit = ${c.speedLimit||50},\n      description = "${esc(c.description||'')}",\n      districtCity = "${esc(c.districtCity||'')}",\n      bearingDegrees = ${c.bearingDegrees||0}f,\n      directionName = "${esc(c.directionName||'')}"\n    )`;
    }).join(',\n');

    if (!fs.existsSync(KOTLIN_FILE)) return 'Kotlin file not found';
    let kt = fs.readFileSync(KOTLIN_FILE, 'utf-8');
    
    // Replace the camera list between listOf( and end
    const startToken = 'val ALL_CAMERAS: List<TrafficCamera> = listOf(\n';
    const endToken = '\n  ) // END_ALL_CAMERAS';
    const si = kt.indexOf(startToken);
    const ei = kt.indexOf(endToken);
    
    if (si === -1 || ei === -1) {
      // Fallback: try simple replace between listOf( and first )
      const si2 = kt.indexOf('val ALL_CAMERAS: List<TrafficCamera> = listOf(');
      if (si2 === -1) return 'Marker not found in Kotlin file';
    }
    
    const before = kt.substring(0, si + startToken.length);
    const after = kt.substring(ei);
    kt = before + ktEntries + after;
    fs.writeFileSync(KOTLIN_FILE, kt, 'utf-8');
    return `Synced ${cameras.length} cameras to Kotlin`;
  } catch (e) {
    return 'Kotlin sync error: ' + e.message;
  }
}

const server = http.createServer((req, res) => {
  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204); res.end(); return;
  }

  // =================== API ROUTES ===================

  // POST /api/save-cameras - Lưu toàn bộ danh sách camera
  if (req.method === 'POST' && req.url === '/api/save-cameras') {
    readBody(req).then(cameras => {
      const count = writeCameraFiles(cameras);
      const ktResult = syncToKotlin(cameras);
      console.log(`[SAVE] Saved ${count} cameras. Kotlin: ${ktResult}`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ ok: true, count, kotlin: ktResult }));
    }).catch(e => {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ ok: false, error: e.message }));
    });
    return;
  }

  // POST /api/add-camera - Thêm 1 camera mới
  if (req.method === 'POST' && req.url === '/api/add-camera') {
    readBody(req).then(newCam => {
      let cameras = [];
      try { cameras = JSON.parse(fs.readFileSync(DATA_JSON, 'utf-8')); } catch(e) {}
      
      // Check duplicate
      const dup = cameras.find(c => 
        Math.abs(c.latitude - newCam.latitude) < 0.0002 &&
        Math.abs(c.longitude - newCam.longitude) < 0.0002
      );
      if (dup) {
        res.writeHead(409, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: false, error: 'Camera tại vị trí này đã tồn tại', existing: dup }));
        return;
      }

      cameras.push(newCam);
      const count = writeCameraFiles(cameras);
      const ktResult = syncToKotlin(cameras);
      console.log(`[ADD] Added camera "${newCam.roadName}" at ${newCam.latitude},${newCam.longitude}. Total: ${count}`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ ok: true, count, kotlin: ktResult, camera: newCam }));
    }).catch(e => {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ ok: false, error: e.message }));
    });
    return;
  }

  // DELETE /api/delete-camera?id=xxx
  if (req.method === 'DELETE' && req.url.startsWith('/api/delete-camera')) {
    const id = new URL(req.url, 'http://localhost').searchParams.get('id');
    let cameras = [];
    try { cameras = JSON.parse(fs.readFileSync(DATA_JSON, 'utf-8')); } catch(e) {}
    const before = cameras.length;
    cameras = cameras.filter(c => c.id !== id);
    writeCameraFiles(cameras);
    console.log(`[DELETE] Deleted camera ${id}. ${before} -> ${cameras.length}`);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ ok: true, deleted: before - cameras.length, total: cameras.length }));
    return;
  }

  // GET /api/cameras - Lấy danh sách camera hiện tại
  if (req.method === 'GET' && req.url === '/api/cameras') {
    try {
      const cameras = JSON.parse(fs.readFileSync(DATA_JSON, 'utf-8'));
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(cameras));
    } catch (e) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: e.message }));
    }
    return;
  }

  // =================== STATIC FILES ===================
  const urlPath = req.url.split('?')[0];
  let filePath = path.join(__dirname, urlPath === '/' ? 'index.html' : urlPath);
  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(err.code === 'ENOENT' ? 404 : 500, { 'Content-Type': 'text/plain' });
      res.end(err.code === 'ENOENT' ? '404 Not Found' : `Server Error: ${err.code}`);
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content, 'utf-8');
    }
  });
});

server.listen(PORT, () => {
  console.log(`\n🚦 VN Traffic Camera Inspector Server`);
  console.log(`📡 Running at http://localhost:${PORT}`);
  console.log(`💾 API: POST /api/add-camera | POST /api/save-cameras | DELETE /api/delete-camera`);
  console.log(`📱 Auto-sync to Android Kotlin on every save\n`);
});
