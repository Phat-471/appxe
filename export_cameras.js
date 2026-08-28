const fs = require('fs');
const path = require('path');

const ktPath = path.join(__dirname, 'app', 'src', 'main', 'java', 'com', 'example', 'data', 'VietnamTrafficData.kt');
const content = fs.readFileSync(ktPath, 'utf-8');

const cameras = [];

// Robust multi-line block parser
const blockRegex = /TrafficCamera\s*\(([\s\S]*?)\)(?=\s*,\s*TrafficCamera|\s*,\s*\/\*|\s*,\s*\/\/|\s*\)\s*|\s*$)/g;

let match;
while ((match = blockRegex.exec(content)) !== null) {
  const block = match[1];
  
  // Extract fields
  let id = null, latitude = null, longitude = null, type = null, roadName = null, speedLimit = 60, description = "", districtCity = "", bearingDegrees = null, directionName = "Hai chiều";
  
  // Try named parameters
  const idM = block.match(/id\s*=\s*"([^"]+)"/);
  const latM = block.match(/latitude\s*=\s*([0-9.-]+)/);
  const lngM = block.match(/longitude\s*=\s*([0-9.-]+)/);
  const typeM = block.match(/type\s*=\s*CameraType\.([A-Z_]+)/);
  const roadM = block.match(/roadName\s*=\s*"([^"]+)"/);
  const speedM = block.match(/speedLimit\s*=\s*([0-9]+)/);
  const descM = block.match(/description\s*=\s*"([^"]+)"/);
  const distM = block.match(/districtCity\s*=\s*"([^"]+)"/);
  const bearM = block.match(/bearingDegrees\s*=\s*([0-9.f]+)/);
  const dirM = block.match(/directionName\s*=\s*"([^"]+)"/);

  if (idM && latM && lngM && typeM) {
    id = idM[1];
    latitude = parseFloat(latM[1]);
    longitude = parseFloat(lngM[1]);
    type = typeM[1];
    roadName = roadM ? roadM[1] : "";
    speedLimit = speedM ? parseInt(speedM[1], 10) : 60;
    description = descM ? descM[1] : "";
    districtCity = distM ? distM[1] : "";
    bearingDegrees = bearM ? parseFloat(bearM[1].replace('f', '')) : null;
    directionName = dirM ? dirM[1] : "Hai chiều";
  } else {
    // Try positional parameters: "id", lat, lng, CameraType.TYPE, "road", speed, "desc", "district", bearing, "dir"
    const posPattern = /"([^"]+)",\s*([0-9.-]+),\s*([0-9.-]+),\s*CameraType\.([A-Z_]+),\s*"([^"]+)",\s*([0-9]+),\s*"([^"]+)",\s*"([^"]+)"(?:,\s*([0-9.fnull]+))?(?:,\s*"([^"]+)")?/;
    const posM = block.match(posPattern);
    if (posM) {
      id = posM[1];
      latitude = parseFloat(posM[2]);
      longitude = parseFloat(posM[3]);
      type = posM[4];
      roadName = posM[5];
      speedLimit = parseInt(posM[6], 10);
      description = posM[7];
      districtCity = posM[8];
      bearingDegrees = posM[9] && posM[9] !== 'null' ? parseFloat(posM[9].replace('f', '')) : null;
      directionName = posM[10] || "Hai chiều";
    }
  }

  if (id && latitude && longitude && !cameras.some(c => c.id === id)) {
    cameras.push({
      id,
      latitude,
      longitude,
      type,
      roadName,
      speedLimit,
      description,
      districtCity,
      bearingDegrees,
      directionName
    });
  }
}

console.log(`Extracted ${cameras.length} cameras from VietnamTrafficData.kt`);

const outputDir = path.join(__dirname, 'web_camera_inspector');
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

fs.writeFileSync(
  path.join(outputDir, 'camera_data.js'),
  `// Auto-generated camera dataset from Android app database (${cameras.length} verified cameras)\nconst INITIAL_CAMERAS = ${JSON.stringify(cameras, null, 2)};\n`,
  'utf-8'
);
fs.writeFileSync(
  path.join(outputDir, 'camera_data.json'),
  JSON.stringify(cameras, null, 2),
  'utf-8'
);

console.log(`Saved to ${path.join(outputDir, 'camera_data.js')} and camera_data.json`);
