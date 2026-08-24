const fs = require('fs');
const path = require('path');

const enabled = process.argv[2] === '1' || process.argv[2] === 'true';
const file = path.join(__dirname, '..', 'video-build.json');
fs.writeFileSync(file, JSON.stringify({ enabled }, null, 2) + '\n');
console.log(`video-build.json enabled=${enabled}`);
