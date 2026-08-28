const fs = require('fs');
const path = require('path');

const srcJson = path.join(__dirname, 'web_camera_inspector', 'camera_data.json');
const assetsDir = path.join(__dirname, 'app', 'src', 'main', 'assets');
const destJson = path.join(assetsDir, 'vietnam_cameras.json');

if (!fs.existsSync(assetsDir)) {
  fs.mkdirSync(assetsDir, { recursive: true });
}

fs.copyFileSync(srcJson, destJson);
console.log(`✅ Đã sao chép camera_data.json -> app/src/main/assets/vietnam_cameras.json`);
