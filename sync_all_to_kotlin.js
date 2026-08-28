const fs = require('fs');
const path = require('path');

const jsonPath = path.join(__dirname, 'web_camera_inspector', 'camera_data.json');
const ktPath = path.join(__dirname, 'app', 'src', 'main', 'java', 'com', 'example', 'data', 'VietnamTrafficData.kt');

const cameras = JSON.parse(fs.readFileSync(jsonPath, 'utf-8'));
console.log(`Đang đồng bộ ${cameras.length} camera từ camera_data.json sang Kotlin...`);

const typeMap = {
  'SPEED_CAMERA': 'CameraType.SPEED_CAMERA',
  'RED_LIGHT_CAMERA': 'CameraType.RED_LIGHT_CAMERA',
  'COLD_FINE_SURVEILLANCE': 'CameraType.COLD_FINE_SURVEILLANCE',
  'MOTORBIKE_PROHIBITED_ZONE': 'CameraType.MOTORBIKE_PROHIBITED_ZONE'
};

const esc = (s) => String(s || '')
  .replace(/\\/g, '\\\\')
  .replace(/"/g, '\\"')
  .replace(/\r?\n/g, ' ')
  .replace(/\t/g, ' ')
  .trim();

const ktListString = cameras.map(c => {
  const t = typeMap[c.type] || 'CameraType.COLD_FINE_SURVEILLANCE';
  return `    TrafficCamera(
      id = "${esc(c.id)}",
      latitude = ${c.latitude.toFixed(6)},
      longitude = ${c.longitude.toFixed(6)},
      type = ${t},
      roadName = "${esc(c.roadName)}",
      speedLimit = ${c.speedLimit},
      description = "${esc(c.description)}",
      districtCity = "${esc(c.districtCity)}",
      bearingDegrees = ${c.bearingDegrees || 0}f,
      directionName = "${esc(c.directionName || '')}"
    )`;
}).join(',\n');

let ktFile = fs.readFileSync(ktPath, 'utf-8');
const startToken = 'val ALL_CAMERAS: List<TrafficCamera> = listOf(';
const sIdx = ktFile.indexOf(startToken);

if (sIdx !== -1) {
  const afterStart = ktFile.substring(sIdx + startToken.length);
  const distanceFuncIdx = afterStart.indexOf('fun calculateDistanceMeters(');
  if (distanceFuncIdx !== -1) {
    const camerasSegment = afterStart.substring(0, distanceFuncIdx);
    const lastParenIdx = camerasSegment.lastIndexOf(')');
    if (lastParenIdx !== -1) {
      const newKt = ktFile.substring(0, sIdx + startToken.length) + '\n' + ktListString + '\n  ' + afterStart.substring(lastParenIdx);
      fs.writeFileSync(ktPath, newKt, 'utf-8');
      console.log(`✅ Đã đồng bộ thành công ${cameras.length} camera vào VietnamTrafficData.kt!`);
    }
  }
}
