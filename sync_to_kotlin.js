const fs = require('fs');
const path = require('path');

const jsonPath = path.join(__dirname, 'web_camera_inspector', 'camera_data.json');
const cameras = JSON.parse(fs.readFileSync(jsonPath, 'utf-8'));

const targetKtPath = path.join(__dirname, 'app', 'src', 'main', 'java', 'com', 'example', 'data', 'VietnamTrafficData.kt');
const originalContent = fs.readFileSync(targetKtPath, 'utf-8');

// Find start and end of ALL_CAMERAS
const startMarker = '  val ALL_CAMERAS: List<TrafficCamera> = listOf(';
const endMarker = '  // Haversine distance in meters';

const startIndex = originalContent.indexOf(startMarker);
const endIndex = originalContent.indexOf(endMarker);

if (startIndex !== -1 && endIndex !== -1) {
  let newAllCameras = '  val ALL_CAMERAS: List<TrafficCamera> = listOf(\n';
  cameras.forEach((c, index) => {
    const isLast = index === cameras.length - 1;
    const bearingStr = c.bearingDegrees !== null && c.bearingDegrees !== undefined ? `${c.bearingDegrees}f` : "null";
    const dirStr = c.directionName ? `"${c.directionName}"` : '"Hai chiều"';

    newAllCameras += `    TrafficCamera(
      id = "${c.id}",
      latitude = ${c.latitude.toFixed(5)},
      longitude = ${c.longitude.toFixed(5)},
      type = CameraType.${c.type},
      roadName = "${c.roadName.replace(/"/g, '\\"')}",
      speedLimit = ${c.speedLimit},
      description = "${c.description.replace(/"/g, '\\"')}",
      districtCity = "${c.districtCity.replace(/"/g, '\\"')}",
      bearingDegrees = ${bearingStr},
      directionName = ${dirStr}
    )${isLast ? '' : ','}\n`;
  });
  newAllCameras += '  )\n\n';

  const newFullContent = originalContent.substring(0, startIndex) + newAllCameras + originalContent.substring(endIndex);
  fs.writeFileSync(targetKtPath, newFullContent, 'utf-8');
  console.log(`Successfully updated ALL_CAMERAS with ${cameras.length} verified cameras!`);
} else {
  console.error('Could not find markers in VietnamTrafficData.kt');
}
