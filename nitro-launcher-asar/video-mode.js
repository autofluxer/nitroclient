const fs = require('fs');
const path = require('path');

function isVideoBuild() {
  try {
    const file = path.join(__dirname, 'video-build.json');
    if (!fs.existsSync(file)) return false;
    const data = JSON.parse(fs.readFileSync(file, 'utf8'));
    return !!data.enabled;
  } catch (_) {
    return false;
  }
}

module.exports = { isVideoBuild };
