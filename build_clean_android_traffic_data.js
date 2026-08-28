const fs = require('fs');
const path = require('path');

const jsonPath = path.join(__dirname, 'web_camera_inspector', 'camera_data.json');
const baseKtPath = path.join(__dirname, 'clean_base_traffic.kt');
const targetKtPath = path.join(__dirname, 'app', 'src', 'main', 'java', 'com', 'example', 'data', 'VietnamTrafficData.kt');

const cameras = JSON.parse(fs.readFileSync(jsonPath, 'utf-8'));
console.log(`Đang nén ${cameras.length} camera cho Android Kotlin...`);

// Nén thành mảng gọn: [id, lat, lng, typeInt, roadName, speedLimit, description, districtCity, bearing, direction]
// typeInt: 0=SPEED_CAMERA, 1=RED_LIGHT_CAMERA, 2=COLD_FINE_SURVEILLANCE, 3=MOTORBIKE_PROHIBITED_ZONE
const typeToInt = {
  'SPEED_CAMERA': 0,
  'RED_LIGHT_CAMERA': 1,
  'COLD_FINE_SURVEILLANCE': 2,
  'MOTORBIKE_PROHIBITED_ZONE': 3
};

const compactArray = cameras.map(c => [
  c.id,
  c.latitude,
  c.longitude,
  typeToInt[c.type] || 1,
  c.roadName,
  c.speedLimit || 50,
  c.description || '',
  c.districtCity || '',
  c.bearingDegrees || 0,
  c.directionName || ''
]);

const jsonString = JSON.stringify(compactArray);
console.log(`Kích thước JSON nén: ${(jsonString.length / 1024).toFixed(1)} KB`);

// Chia thành các đoạn chuỗi < 40KB để an toàn tuyệt đối với Java Constant Pool 65KB limit
const CHUNK_SIZE = 35000;
const chunks = [];
for (let i = 0; i < jsonString.length; i += CHUNK_SIZE) {
  chunks.push(jsonString.substring(i, i + CHUNK_SIZE));
}

console.log(`Đã chia thành ${chunks.length} chuỗi chunks.`);

const kotlinChunks = chunks.map((chunk, idx) => {
  const esc = JSON.stringify(chunk);
  return `    private val CHUNK_${idx} = ${esc}`;
}).join('\n');

const kotlinChunkJoin = chunks.map((_, idx) => `CHUNK_${idx}`).join(' + ');

const newCameraLoader = `  // =========================================================================
  // CƠ SỞ DỮ LIỆU CAMERA TOÀN DIỆN (2.700+ ĐIỂM TOẠ ĐỘ THỰC TẾ)
  // =========================================================================
${kotlinChunks}

  private var _allCamerasCache: List<TrafficCamera>? = null

  val ALL_CAMERAS: List<TrafficCamera>
    get() {
      if (_allCamerasCache == null) {
        _allCamerasCache = parseCompactCameras()
      }
      return _allCamerasCache!!
    }

  val ALL_CAMERAS_FULL: List<TrafficCamera> get() = ALL_CAMERAS

  private fun parseCompactCameras(): List<TrafficCamera> {
    val list = ArrayList<TrafficCamera>(${cameras.length + 100})
    try {
      val fullJson = ${kotlinChunkJoin}
      val jsonArray = org.json.JSONArray(fullJson)
      val total = jsonArray.length()
      for (i in 0 until total) {
        val item = jsonArray.getJSONArray(i)
        val id = item.getString(0)
        val lat = item.getDouble(1)
        val lng = item.getDouble(2)
        val typeInt = item.getInt(3)
        val road = item.getString(4)
        val speed = item.getInt(5)
        val desc = item.getString(6)
        val city = item.getString(7)
        val bearing = item.getDouble(8).toFloat()
        val dir = item.getString(9)

        val camType = when (typeInt) {
          0 -> CameraType.SPEED_CAMERA
          1 -> CameraType.RED_LIGHT_CAMERA
          2 -> CameraType.COLD_FINE_SURVEILLANCE
          3 -> CameraType.MOTORBIKE_PROHIBITED_ZONE
          else -> CameraType.RED_LIGHT_CAMERA
        }

        list.add(
          TrafficCamera(
            id = id,
            latitude = lat,
            longitude = lng,
            type = camType,
            roadName = road,
            speedLimit = speed,
            description = desc,
            districtCity = city,
            bearingDegrees = bearing,
            directionName = dir
          )
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return list
  }
`;

let baseKt = fs.readFileSync(baseKtPath, 'utf-8');

// Thay thế phần ALL_CAMERAS trong baseKt
const startIdx = baseKt.indexOf('  // Realistic verified camera database for Vietnam routes');
const endIdx = baseKt.indexOf('  // Haversine distance in meters');

if (startIdx !== -1 && endIdx !== -1) {
  const newKt = baseKt.substring(0, startIdx) + newCameraLoader + '\n' + baseKt.substring(endIdx);
  fs.writeFileSync(targetKtPath, newKt, 'utf-8');
  console.log(`✅ Đã tạo file ${targetKtPath} hoàn chỉnh!`);
} else {
  console.error('Không tìm thấy vị trí thay thế trong clean_base_traffic.kt');
}
