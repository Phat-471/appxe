const https = require('https');
const fs = require('fs');
const path = require('path');

// QUERY TẤT CẢ NÚT GIAO ĐÈN ĐỎ & CAMERA GIÁM SÁT TOÀN TP.HCM TỪ OSM
// Mỗi ngã tư đèn đỏ ở TP.HCM hiện nay đều có camera giám sát / phạt nguội
const query = `
[out:json][timeout:60];
(
  // Tất cả nút giao đèn tín hiệu giao thông TP.HCM (Toạ độ chuẩn xác 100% trên mặt đường)
  node["highway"="traffic_signals"](10.65,106.50,10.92,106.88);
  // Tất cả camera giám sát / speed camera
  node["man_made"="surveillance"](10.65,106.50,10.92,106.88);
  node["highway"="speed_camera"](10.65,106.50,10.92,106.88);
);
out body;
`;

function fetchOverpass() {
  return new Promise((resolve, reject) => {
    const body = 'data=' + encodeURIComponent(query);
    const options = {
      hostname: 'overpass-api.de',
      path: '/api/interpreter',
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(body),
        'User-Agent': 'VNTrafficRadar/3.0'
      }
    };

    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          reject(e);
        }
      });
    });

    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function run() {
  console.log('Đang quét toàn bộ nút giao đèn tín hiệu và camera giao thông TP.HCM...');
  try {
    const res = await fetchOverpass();
    const elements = res.elements || [];
    console.log(`✅ Tìm thấy tổng cộng: ${elements.length} nút giao đèn tín hiệu & camera thực tế tại TP.HCM!`);
    
    // Lưu mẫu ra file để kiểm tra
    fs.writeFileSync('osm_hcm_traffic_nodes.json', JSON.stringify(elements.slice(0, 100), null, 2));
  } catch (e) {
    console.error('Lỗi khi fetch:', e.message);
  }
}

run();
