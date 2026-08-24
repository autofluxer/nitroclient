const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');

const FABRIC_LOADER = '0.19.3';
const FABRIC_API_VERSION = '0.141.4+1.21.11';

function fetchJson(url) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http;
    lib.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        fetchJson(res.headers.location).then(resolve).catch(reject);
        return;
      }
      let body = '';
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => {
        try {
          resolve(JSON.parse(body));
        } catch (err) {
          reject(err);
        }
      });
    }).on('error', reject);
  });
}

function downloadFile(url, dest) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http;
    const file = fs.createWriteStream(dest);
    lib.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        file.close();
        fs.unlink(dest, () => {});
        downloadFile(res.headers.location, dest).then(resolve).catch(reject);
        return;
      }
      if (res.statusCode !== 200) {
        file.close();
        fs.unlink(dest, () => {});
        reject(new Error(`Download failed (${res.statusCode}): ${url}`));
        return;
      }
      res.pipe(file);
      file.on('finish', () => file.close(resolve));
    }).on('error', (err) => {
      file.close();
      fs.unlink(dest, () => {});
      reject(err);
    });
  });
}

function fabricVersionId(mcVersion, loaderVersion = FABRIC_LOADER) {
  return `fabric-loader-${loaderVersion}-${mcVersion}`;
}

async function ensureFabricProfile(gameDir, mcVersion, loaderVersion = FABRIC_LOADER) {
  const versionId = fabricVersionId(mcVersion, loaderVersion);
  const versionDir = path.join(gameDir, 'versions', versionId);
  const versionJsonPath = path.join(versionDir, `${versionId}.json`);

  if (!fs.existsSync(versionJsonPath)) {
    fs.mkdirSync(versionDir, { recursive: true });
    const profile = await fetchJson(
      `https://meta.fabricmc.net/v2/versions/loader/${mcVersion}/${loaderVersion}/profile/json`
    );
    fs.writeFileSync(versionJsonPath, JSON.stringify(profile, null, 2));
  }

  const loaderJar = path.join(versionDir, `fabric-loader-${loaderVersion}.jar`);
  if (!fs.existsSync(loaderJar)) {
    const profile = JSON.parse(fs.readFileSync(versionJsonPath, 'utf8'));
    const loaderLib = (profile.libraries || []).find((lib) => String(lib.name || '').includes('fabric-loader'));
    if (!loaderLib) {
      throw new Error('Fabric loader library missing from profile metadata');
    }
    const [group, artifact, version] = loaderLib.name.split(':');
    const mavenPath = `${group.replace(/\./g, '/')}/${artifact}/${version}/${artifact}-${version}.jar`;
    const baseUrl = (loaderLib.url || 'https://maven.fabricmc.net/').replace(/\/$/, '');
    await downloadFile(`${baseUrl}/${mavenPath}`, loaderJar);
  }

  return versionId;
}

async function ensureFabricApiMod(modsDir) {
  fs.mkdirSync(modsDir, { recursive: true });
  const existing = fs.readdirSync(modsDir).find((name) => /^fabric-api-.*\.jar$/i.test(name));
  if (existing) return path.join(modsDir, existing);

  const versions = await fetchJson(
    'https://api.modrinth.com/v2/project/P7dR8mSH/version?game_versions=%5B%221.21.11%22%5D&loaders=%5B%22fabric%22%5D'
  );
  if (!versions?.length) {
    throw new Error('Fabric API not found for Minecraft 1.21.11 on Modrinth');
  }
  const primary = versions[0].files.find((file) => file.primary) || versions[0].files[0];
  const dest = path.join(modsDir, primary.filename);
  await downloadFile(primary.url, dest);
  return dest;
}

async function prepareModernFabricInstall(gameDir, mcVersion) {
  const modsDir = path.join(gameDir, 'mods');
  const versionId = await ensureFabricProfile(gameDir, mcVersion);
  await ensureFabricApiMod(modsDir);
  return versionId;
}

module.exports = {
  FABRIC_LOADER,
  fabricVersionId,
  prepareModernFabricInstall
};
