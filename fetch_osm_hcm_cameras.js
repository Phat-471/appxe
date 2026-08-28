const https = require('https');
const fs = require('fs');

const overpassQuery = `
[out:json][timeout:30];
(
  node["man_made"="surveillance"](10.65,106.55,10.88,106.78);
  node["highway"="speed_camera"](10.65,106.55,10.88,106.78);
  node["camera:type"](10.65,106.55,10.88,106.78);
  node["enforcement"](10.65,106.55,10.88,106.78);
  node["surveillance:type"](10.65,106.55,10.88,106.78);
  node["surveillance"](10.65,106.55,10.88,106.78);
);
out body;
>;
out skel qt;
`;

const encodedQuery = 'data=' + encodeURIComponent(overpassQuery);

const options = {
  hostname: 'overpass-api.de',
  port: 443,
  path: '/api/interpreter',
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
    'Content-Length': Buffer.byteLength(encodedQuery),
    'User-Agent': 'VietnamCameraInspector/2.0'
  }
};

console.log('Querying Overpass API for all surveillance & speed cameras in HCMC...');

const req = https.request(options, (res) => {
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => {
    try {
      const json = JSON.parse(data);
      console.log(`Received ${json.elements ? json.elements.length : 0} nodes from OSM Overpass!`);
      fs.writeFileSync('osm_hcm_raw.json', JSON.stringify(json, null, 2), 'utf-8');
      
      // Let's filter elements near Lũy Bán Bích / Hòa Bình (lat ~ 10.767, lng ~ 106.634)
      const nearLuyBanBich = (json.elements || []).filter(el => {
        return Math.abs(el.lat - 10.767) < 0.03 && Math.abs(el.lon - 106.634) < 0.03;
      });
      console.log(`Nodes near Lũy Bán Bích - Hòa Bình: ${nearLuyBanBich.length}`);
      nearLuyBanBich.forEach(n => {
        console.log(`- ID: ${n.id}, Lat: ${n.lat}, Lon: ${n.lon}, Tags:`, n.tags);
      });
    } catch (err) {
      console.error('Error parsing JSON:', err.message, data.substring(0, 200));
    }
  });
});

req.on('error', (e) => {
  console.error('Request error:', e.message);
});

req.write(encodedQuery);
req.end();
