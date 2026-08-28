/**
 * Fetch ALL Vietnam cameras from OpenStreetMap - Toàn quốc
 */
const https = require('https');
const fs = require('fs');
const path = require('path');

const QUERY = `[out:json][timeout:90];
(
  node["highway"="speed_camera"](8.5,102.0,23.5,110.0);
  node["enforcement"="maxspeed"](8.5,102.0,23.5,110.0);
  node["enforcement"="traffic_signals"](8.5,102.0,23.5,110.0);
  node["camera:type"="fixed"](8.5,102.0,23.5,110.0);
  node["man_made"="surveillance"]["surveillance"="traffic"](8.5,102.0,23.5,110.0);
);
out body;`;

function post(query) {
  return new Promise((resolve, reject) => {
    const body = 'data=' + encodeURIComponent(query);
    const opts = {
      hostname: 'overpass.kumi.systems',
      path: '/api/interpreter',
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(body),
        'User-Agent': 'VNTrafficRadar/2.0'
      }
    };
    const req = https.request(opts, res => {
      let d = '';
      res.on('data', c => d += c);
      res.on('end', () => {
        try { resolve(JSON.parse(d)); }
        catch (e) { reject(new Error('Parse error: ' + d.slice(0, 200))); }
      });
    });
    req.setTimeout(95000, () => { req.destroy(); reject(new Error('Timeout')); });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function main() {
  console.log('Fetching ALL Vietnam cameras from OSM...');
  const r = await post(QUERY);
  const nodes = r.elements || [];
  console.log('Total nodes:', nodes.length);

  const typeCount = {};
  nodes.forEach(n => {
    const t = n.tags;
    const key = t.highway || t.enforcement || t['camera:type'] || t.man_made || 'unknown';
    typeCount[key] = (typeCount[key] || 0) + 1;
  });
  console.log('By type:', JSON.stringify(typeCount, null, 2));

  // Save raw
  fs.writeFileSync(path.join(__dirname, 'osm_vietnam_cameras_raw.json'),
    JSON.stringify(nodes, null, 2));
  console.log('Saved to osm_vietnam_cameras_raw.json');

  // Show sample
  nodes.slice(0, 30).forEach(n => {
    console.log(`  ${n.lat} ${n.lon} | ${JSON.stringify(n.tags)}`);
  });
}

main().catch(e => { console.error('ERROR:', e.message); process.exit(1); });
