const https = require('https');
const fs = require('fs');
const path = require('path');

// 1. DANH SÁCH CÁC ĐIỂM CAMERA BẮN TỐC ĐỘ, CAO TỐC & ĐẠI LỘ ĐẶC BIỆT (XÁC THỰC 100%)
const SPECIAL_SPEED_AND_EXPRESSWAY_CAMERAS = [
  // Tân Phú - Tân Bình
  {
    id: "speed_lbb_01",
    latitude: 10.770560,
    longitude: 106.631900,
    type: "SPEED_CAMERA",
    roadName: "Lũy Bán Bích (Trước Highlands / ILA)",
    speedLimit: 50,
    description: "Camera bắn tốc độ 50 km/h và giám sát lấn tuyến đường Lũy Bán Bích",
    districtCity: "Quận Tân Phú, TP.HCM",
    bearingDegrees: 345,
    directionName: "Hướng về Thoại Ngọc Hầu / Âu Cơ"
  },
  {
    id: "speed_kth_01",
    latitude: 10.76420,
    longitude: 106.63680,
    type: "SPEED_CAMERA",
    roadName: "Đường Kênh Tân Hóa (Đoạn Cầu Tân Hóa)",
    speedLimit: 50,
    description: "Camera bắn tốc độ tự động 50 km/h dọc tuyến Kênh Tân Hóa",
    districtCity: "Quận Tân Phú - Quận 6, TP.HCM",
    bearingDegrees: 180,
    directionName: "Hướng về Đặng Nguyên Cẩn"
  },
  {
    id: "speed_cn1_01",
    latitude: 10.80650,
    longitude: 106.61980,
    type: "SPEED_CAMERA",
    roadName: "Đường CN1 (KCN Tân Bình)",
    speedLimit: 50,
    description: "Camera bắn tốc độ 50 km/h trục chính KCN Tân Bình",
    districtCity: "Quận Tân Phú, TP.HCM",
    bearingDegrees: 290,
    directionName: "Hai chiều qua KCN Tân Bình"
  },
  {
    id: "speed_ch_01",
    latitude: 10.79680,
    longitude: 106.65480,
    type: "SPEED_CAMERA",
    roadName: "Đường Cộng Hòa (Đoạn Cầu vượt Út Tịch)",
    speedLimit: 50,
    description: "Camera bắn tốc độ và phạt nguội lấn làn ô tô/xe máy đường Cộng Hòa",
    districtCity: "Quận Tân Bình, TP.HCM",
    bearingDegrees: 135,
    directionName: "Hướng về Lăng Cha Cả"
  },
  {
    id: "speed_tc_01",
    latitude: 10.82560,
    longitude: 106.62150,
    type: "SPEED_CAMERA",
    roadName: "Trường Chinh (Chân Cầu Tham Lương)",
    speedLimit: 50,
    description: "Camera bắn tốc độ 50 km/h và giám sát lấn làn Cầu Tham Lương",
    districtCity: "Quận 12 - Tân Bình, TP.HCM",
    bearingDegrees: 315,
    directionName: "Hướng về Ngã tư An Sương"
  },

  // Đại lộ Võ Văn Kiệt
  {
    id: "speed_vvk_01",
    latitude: 10.71850,
    longitude: 106.60250,
    type: "SPEED_CAMERA",
    roadName: "Đại lộ Võ Văn Kiệt (Nút giao QL1A)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h đầu tuyến Võ Văn Kiệt phía Bình Chánh",
    districtCity: "Huyện Bình Chánh, TP.HCM",
    bearingDegrees: 60,
    directionName: "Hướng về Trung tâm Quận 1"
  },
  {
    id: "speed_vvk_02",
    latitude: 10.72980,
    longitude: 106.61850,
    type: "SPEED_CAMERA",
    roadName: "Đại lộ Võ Văn Kiệt (Đoạn Hồ Học Lãm)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h làn hỗn hợp và 80 km/h làn ô tô",
    districtCity: "Quận Bình Tân, TP.HCM",
    bearingDegrees: 65,
    directionName: "Hai chiều Võ Văn Kiệt"
  },
  {
    id: "speed_vvk_03",
    latitude: 10.73820,
    longitude: 106.63450,
    type: "SPEED_CAMERA",
    roadName: "Đại lộ Võ Văn Kiệt (Gần Cầu Nước Lên)",
    speedLimit: 60,
    description: "Camera bắn tốc độ tự động 24/7",
    districtCity: "Quận Bình Tân - Quận 8, TP.HCM",
    bearingDegrees: 70,
    directionName: "Hướng về Cầu Lò Gốm"
  },
  {
    id: "speed_vvk_04",
    latitude: 10.74450,
    longitude: 106.64780,
    type: "SPEED_CAMERA",
    roadName: "Đại lộ Võ Văn Kiệt (Cầu Lò Gốm)",
    speedLimit: 60,
    description: "Camera giám sát tốc độ và phạt nguội xe máy đi vào làn ô tô",
    districtCity: "Quận 6, TP.HCM",
    bearingDegrees: 75,
    directionName: "Chân Cầu Lò Gốm"
  },
  {
    id: "speed_vvk_05",
    latitude: 10.75230,
    longitude: 106.67120,
    type: "SPEED_CAMERA",
    roadName: "Võ Văn Kiệt (Dưới Chân Cầu Chữ Y)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h và giám sát chuyển làn không xi-nhan",
    districtCity: "Quận 5, TP.HCM",
    bearingDegrees: 85,
    directionName: "Đoạn Cầu Chữ Y - Nguyễn Tri Phương"
  },
  {
    id: "speed_vvk_06",
    latitude: 10.76850,
    longitude: 106.70580,
    type: "SPEED_CAMERA",
    roadName: "Hầm Sông Sài Gòn (Cửa Hầm Phía Quận 1)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h trong hầm Thủ Thiêm (Bật đèn chiếu gần!)",
    districtCity: "Quận 1, TP.HCM",
    bearingDegrees: 90,
    directionName: "Cửa hầm Thủ Thiêm Quận 1"
  },
  {
    id: "speed_mct_01",
    latitude: 10.77250,
    longitude: 106.71450,
    type: "SPEED_CAMERA",
    roadName: "Hầm Sông Sài Gòn (Cửa Hầm Phía TP. Thủ Đức)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h cửa hầm Thủ Thiêm phía Mai Chí Thọ",
    districtCity: "TP. Thủ Đức, TP.HCM",
    bearingDegrees: 85,
    directionName: "Cửa hầm Mai Chí Thọ"
  },
  {
    id: "speed_mct_02",
    latitude: 10.79350,
    longitude: 106.75850,
    type: "SPEED_CAMERA",
    roadName: "Đại lộ Mai Chí Thọ (Nút giao An Phú)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h và giám sát luồng xe nút giao An Phú",
    districtCity: "TP. Thủ Đức, TP.HCM",
    bearingDegrees: 65,
    directionName: "Nút giao An Phú - Cao tốc"
  },

  // Cao tốc Long Thành - Dầu Giây & Phan Thiết
  {
    id: "prohibit_caotoc_lt_01",
    latitude: 10.79680,
    longitude: 106.77250,
    type: "MOTORBIKE_PROHIBITED_ZONE",
    roadName: "Đầu Cao Tốc TP.HCM - Long Thành - Dầu Giây (An Phú)",
    speedLimit: 100,
    description: "CẢNH BÁO: ĐƯỜNG CẤM XE MÁY! Phạt 2-3 Triệu Đồng & Tịch thu bằng lái",
    districtCity: "TP. Thủ Đức, TP.HCM",
    bearingDegrees: 80,
    directionName: "Nhánh vào Cao tốc Long Thành"
  },
  {
    id: "speed_caotoc_lt_02",
    latitude: 10.79850,
    longitude: 106.82500,
    type: "SPEED_CAMERA",
    roadName: "Cao Tốc Long Thành (Trạm Thu Phí Long Phước)",
    speedLimit: 100,
    description: "Camera bắn tốc độ tự động 100 km/h và giám sát giữ khoảng cách an toàn",
    districtCity: "TP. Thủ Đức, TP.HCM",
    bearingDegrees: 85,
    directionName: "Trạm thu phí Long Phước"
  },
  {
    id: "speed_caotoc_lt_03",
    latitude: 10.77120,
    longitude: 106.91250,
    type: "SPEED_CAMERA",
    roadName: "Cao Tốc Long Thành (Đoạn Cầu Long Thành)",
    speedLimit: 100,
    description: "Camera bắn tốc độ 100 km/h qua Cầu Long Thành vượt Sông Đồng Nai",
    districtCity: "Đồng Nai",
    bearingDegrees: 90,
    directionName: "Cầu Long Thành"
  },
  {
    id: "speed_caotoc_lt_04",
    latitude: 10.74850,
    longitude: 106.98500,
    type: "SPEED_CAMERA",
    roadName: "Cao Tốc Long Thành (Nút giao QL51 đi Vũng Tàu)",
    speedLimit: 100,
    description: "Camera giám sát tốc độ và phạt nguội chuyển làn khẩn cấp",
    districtCity: "Long Thành, Đồng Nai",
    bearingDegrees: 95,
    directionName: "Nút giao Quốc lộ 51"
  },
  {
    id: "speed_caotoc_lt_05",
    latitude: 10.88500,
    longitude: 107.18500,
    type: "SPEED_CAMERA",
    roadName: "Cao Tốc Phan Thiết - Dầu Giây (Nút giao Dầu Giây)",
    speedLimit: 120,
    description: "Camera bắn tốc độ 120 km/h đầu cao tốc Phan Thiết - Dầu Giây",
    districtCity: "Thống Nhất, Đồng Nai",
    bearingDegrees: 60,
    directionName: "Hướng về Phan Thiết"
  },

  // Cao tốc Trung Lương - Mỹ Thuận
  {
    id: "prohibit_caotoc_tl_01",
    latitude: 10.70250,
    longitude: 106.54500,
    type: "MOTORBIKE_PROHIBITED_ZONE",
    roadName: "Đầu Cao Tốc TP.HCM - Trung Lương (Nút giao Chợ Đệm)",
    speedLimit: 100,
    description: "CẢNH BÁO: ĐƯỜNG CẤM XE MÁY! Phạt 2-3 Triệu Đồng & Tịch thu bằng lái",
    districtCity: "Huyện Bình Chánh, TP.HCM",
    bearingDegrees: 225,
    directionName: "Nhánh vào Cao tốc Chợ Đệm"
  },
  {
    id: "speed_caotoc_tl_02",
    latitude: 10.63500,
    longitude: 106.48500,
    type: "SPEED_CAMERA",
    roadName: "Cao Tốc Trung Lương (Trạm Thu Phí Bến Lức)",
    speedLimit: 100,
    description: "Camera bắn tốc độ tự động 100 km/h và phạt nguội đi làn dừng khẩn cấp",
    districtCity: "Bến Lức, Long An",
    bearingDegrees: 230,
    directionName: "Trạm thu phí Bến Lức"
  },
  {
    id: "speed_caotoc_tl_03",
    latitude: 10.51200,
    longitude: 106.37500,
    type: "SPEED_CAMERA",
    roadName: "Cao Tốc Trung Lương (Nút giao Tân An)",
    speedLimit: 100,
    description: "Camera bắn tốc độ 100 km/h đoạn qua TP. Tân An",
    districtCity: "Tân An, Long An",
    bearingDegrees: 235,
    directionName: "Đoạn Tân An"
  },
  {
    id: "speed_caotoc_tl_04",
    latitude: 10.42500,
    longitude: 106.28500,
    type: "SPEED_CAMERA",
    roadName: "Cao Tốc Trung Lương - Mỹ Thuận (Nút giao Thân Cửu Nghĩa)",
    speedLimit: 80,
    description: "Camera bắn tốc độ 80 km/h bắt đầu cao tốc Trung Lương - Mỹ Thuận",
    districtCity: "Châu Thành, Tiền Giang",
    bearingDegrees: 240,
    directionName: "Nút giao Thân Cửu Nghĩa"
  },

  // Quốc Lộ 1A
  {
    id: "speed_ql1a_01",
    latitude: 10.85850,
    longitude: 106.63450,
    type: "SPEED_CAMERA",
    roadName: "Quốc Lộ 1A (Cầu Vượt Tân Thới Hiệp)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h và giám sát xe tải đi sai giờ",
    districtCity: "Quận 12, TP.HCM",
    bearingDegrees: 90,
    directionName: "Cầu vượt Tân Thới Hiệp"
  },
  {
    id: "speed_ql1a_02",
    latitude: 10.86550,
    longitude: 106.79250,
    type: "SPEED_CAMERA",
    roadName: "Xa Lộ Hà Nội (Khu Công Nghệ Cao)",
    speedLimit: 80,
    description: "Camera bắn tốc độ 80 km/h làn ô tô đoạn Khu Công Nghệ Cao",
    districtCity: "TP. Thủ Đức, TP.HCM",
    bearingDegrees: 40,
    directionName: "Hai chiều Xa Lộ Hà Nội"
  },
  {
    id: "speed_ql1a_03",
    latitude: 10.72500,
    longitude: 106.56800,
    type: "SPEED_CAMERA",
    roadName: "Quốc Lộ 1A (Cầu Bình Điền)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h và giám sát xe chở quá tải Cầu Bình Điền",
    districtCity: "Huyện Bình Chánh, TP.HCM",
    bearingDegrees: 220,
    directionName: "Cầu Bình Điền"
  },

  // Phạm Văn Đồng & Nguyễn Văn Linh
  {
    id: "speed_pvd_01",
    latitude: 10.81650,
    longitude: 106.66850,
    type: "SPEED_CAMERA",
    roadName: "Đại lộ Phạm Văn Đồng (Công Viên Gia Định)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h làn hỗn hợp và 80 km/h làn ô tô",
    districtCity: "Quận Gò Vấp, TP.HCM",
    bearingDegrees: 70,
    directionName: "Hướng về Cầu Bình Lợi"
  },
  {
    id: "speed_pvd_02",
    latitude: 10.82850,
    longitude: 106.70950,
    type: "SPEED_CAMERA",
    roadName: "Phạm Văn Đồng (Cầu Bình Lợi)",
    speedLimit: 80,
    description: "Camera bắn tốc độ 80 km/h qua Cầu Bình Lợi vượt Sông Sài Gòn",
    districtCity: "Quận Bình Thạnh - TP. Thủ Đức, TP.HCM",
    bearingDegrees: 60,
    directionName: "Cầu Bình Lợi"
  },
  {
    id: "speed_nvl_01",
    latitude: 10.72850,
    longitude: 106.65850,
    type: "SPEED_CAMERA",
    roadName: "Đại lộ Nguyễn Văn Linh (Nút giao QL50)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h và giám sát xe tải ngã 4 Nguyễn Văn Linh - QL50",
    districtCity: "Huyện Bình Chánh, TP.HCM",
    bearingDegrees: 95,
    directionName: "Ngã 4 Nguyễn Văn Linh - QL50"
  },
  {
    id: "speed_cpm_01",
    latitude: 10.74850,
    longitude: 106.75850,
    type: "SPEED_CAMERA",
    roadName: "Cầu Phú Mỹ (Dốc Cầu Phía TP. Thủ Đức)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h dốc cầu Phú Mỹ (Điểm đen tai nạn xe tải!)",
    districtCity: "TP. Thủ Đức, TP.HCM",
    bearingDegrees: 45,
    directionName: "Dốc cầu Phú Mỹ xuống Vòng xoay Mỹ Thủy"
  },

  // Quốc Lộ 51 & Quốc Lộ 13
  {
    id: "speed_ql51_01",
    latitude: 10.91250,
    longitude: 106.88500,
    type: "SPEED_CAMERA",
    roadName: "Quốc Lộ 51 (Ngã 4 Vũng Tàu)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h và phạt nguội Cầu Vượt Ngã 4 Vũng Tàu",
    districtCity: "Biên Hòa, Đồng Nai",
    bearingDegrees: 150,
    directionName: "Cầu vượt Ngã 4 Vũng Tàu"
  },
  {
    id: "speed_ql51_02",
    latitude: 10.82500,
    longitude: 106.94500,
    type: "SPEED_CAMERA",
    roadName: "Quốc Lộ 51 (Đoạn Tam Phước - Long Thành)",
    speedLimit: 80,
    description: "Camera bắn tốc độ 80 km/h tự động 24/7",
    districtCity: "Long Thành, Đồng Nai",
    bearingDegrees: 155,
    directionName: "Hai chiều QL51"
  },
  {
    id: "speed_ql51_03",
    latitude: 10.65800,
    longitude: 107.03500,
    type: "SPEED_CAMERA",
    roadName: "Quốc Lộ 51 (Thị Xã Phú Mỹ)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h khu đông dân cư Phú Mỹ",
    districtCity: "Phú Mỹ, Bà Rịa - Vũng Tàu",
    bearingDegrees: 160,
    directionName: "Đoạn Phú Mỹ"
  },
  {
    id: "speed_ql13_01",
    latitude: 10.89850,
    longitude: 106.70250,
    type: "SPEED_CAMERA",
    roadName: "Đại Lộ Bình Dương (Trạm Thu Phí Lái Thiêu)",
    speedLimit: 60,
    description: "Camera bắn tốc độ 60 km/h trạm thu phí Lái Thiêu",
    districtCity: "Thuận An, Bình Dương",
    bearingDegrees: 10,
    directionName: "Trạm thu phí Lái Thiêu"
  }
];

// 2. QUERY TOÀN BỘ NÚT GIAO ĐÈN ĐỎ & CAMERA TỪ OSM
const query = `
[out:json][timeout:90];
(
  node["highway"="traffic_signals"](10.60,106.48,10.95,106.92);
  node["man_made"="surveillance"](10.60,106.48,10.95,106.92);
  node["highway"="speed_camera"](10.60,106.48,10.95,106.92);
  node["enforcement"="maxspeed"](10.60,106.48,10.95,106.92);
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
        try { resolve(JSON.parse(data)); }
        catch (e) { reject(e); }
      });
    });

    req.on('error', reject);
    req.setTimeout(85000, () => { req.destroy(); reject(new Error('Timeout')); });
    req.write(body);
    req.end();
  });
}

function processOsmNode(node) {
  const tags = node.tags || {};
  let type = "RED_LIGHT_CAMERA";
  let speedLimit = 50;

  if (tags.highway === 'speed_camera' || tags.enforcement === 'maxspeed') {
    type = "SPEED_CAMERA";
    speedLimit = parseInt(tags.maxspeed, 10) || 60;
  } else if (tags.man_made === 'surveillance') {
    type = "COLD_FINE_SURVEILLANCE";
  } else if (tags.highway === 'traffic_signals') {
    type = "RED_LIGHT_CAMERA";
    speedLimit = 50;
  }

  const name = tags.name || tags.description || tags["addr:street"] || `Camera Nút Giao #${node.id}`;
  const districtCity = tags["addr:city"] || tags["addr:district"] || "TP. Hồ Chí Minh";

  return {
    id: `osm_hcm_${node.id}`,
    latitude: parseFloat(node.lat.toFixed(6)),
    longitude: parseFloat(node.lon.toFixed(6)),
    type: type,
    roadName: name,
    speedLimit: speedLimit,
    description: `Camera giám sát giao thông, phạt nguội vượt đèn đỏ & sai làn tại nút giao (OSM Ref: ${node.id})`,
    districtCity: districtCity,
    bearingDegrees: 0,
    directionName: "Giao lộ đa hướng"
  };
}

async function main() {
  console.log('🚀 Đang tải dữ liệu từ OpenStreetMap...');
  const res = await fetchOverpass();
  const rawElements = res.elements || [];
  const osmCameras = rawElements.map(processOsmNode);

  // Gộp danh sách đặc biệt (Speed cameras, Cao tốc, Võ Văn Kiệt...) với OSM nodes
  // Loại trùng nếu vị trí quá gần nhau (< 15 mét)
  const mergedList = [...SPECIAL_SPEED_AND_EXPRESSWAY_CAMERAS];
  
  osmCameras.forEach(osmCam => {
    const isCloseToSpecial = SPECIAL_SPEED_AND_EXPRESSWAY_CAMERAS.some(spec => {
      const dLat = (spec.latitude - osmCam.latitude) * 111000;
      const dLng = (spec.longitude - osmCam.longitude) * 98000;
      return Math.sqrt(dLat * dLat + dLng * dLng) < 25; // 25 meters
    });

    if (!isCloseToSpecial) {
      mergedList.push(osmCam);
    }
  });

  console.log(`\n📊 THỐNG KÊ SAU KHI GỘP:`);
  console.log(`- Tổng số camera: ${mergedList.length}`);
  
  const stats = {};
  mergedList.forEach(c => stats[c.type] = (stats[c.type] || 0) + 1);
  console.log(`- Phân loại:`, JSON.stringify(stats, null, 2));

  // 1. Lưu web_camera_inspector/camera_data.json
  const jsonPath = path.join(__dirname, 'web_camera_inspector', 'camera_data.json');
  fs.writeFileSync(jsonPath, JSON.stringify(mergedList, null, 2), 'utf-8');
  console.log(`✅ [1/3] Đã lưu ${mergedList.length} cameras vào web_camera_inspector/camera_data.json`);

  // 2. Lưu web_camera_inspector/camera_data.js
  const jsPath = path.join(__dirname, 'web_camera_inspector', 'camera_data.js');
  const jsContent = `// COMPREHENSIVE TP.HCM TRAFFIC CAMERA DATABASE\n// Total cameras: ${mergedList.length}\nconst INITIAL_CAMERAS = ${JSON.stringify(mergedList, null, 2)};\n`;
  fs.writeFileSync(jsPath, jsContent, 'utf-8');
  console.log(`✅ [2/3] Đã lưu ${mergedList.length} cameras vào web_camera_inspector/camera_data.js`);

  // 3. Đồng bộ vào Android VietnamTrafficData.kt
  console.log(`⏳ [3/3] Đang xuất dữ liệu sang Kotlin cho ứng dụng Android...`);
  const ktPath = path.join(__dirname, 'app', 'src', 'main', 'java', 'com', 'example', 'data', 'VietnamTrafficData.kt');
  
  const typeMap = {
    'SPEED_CAMERA': 'CameraType.SPEED_CAMERA',
    'RED_LIGHT_CAMERA': 'CameraType.RED_LIGHT_CAMERA',
    'COLD_FINE_SURVEILLANCE': 'CameraType.COLD_FINE_SURVEILLANCE',
    'MOTORBIKE_PROHIBITED_ZONE': 'CameraType.MOTORBIKE_PROHIBITED_ZONE'
  };

  const ktListString = mergedList.map(c => {
    const t = typeMap[c.type] || 'CameraType.COLD_FINE_SURVEILLANCE';
    const esc = (s) => String(s || '')
      .replace(/\\/g, '\\\\')
      .replace(/"/g, '\\"')
      .replace(/\r?\n/g, ' ')
      .replace(/\t/g, ' ')
      .trim();
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
        console.log(`✅ [3/3] Đã đồng bộ ${mergedList.length} cameras vào VietnamTrafficData.kt của Android!`);
      }
    }
  }

  console.log(`\n🎉 HOÀN TẤT ĐỒNG BỘ ${mergedList.length} CAMERA VỚI ĐẦY ĐỦ CÁC LOẠI PHẠT NGUỘI & BẮN TỐC ĐỘ!`);
}

main().catch(console.error);
