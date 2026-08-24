const fs = require('fs');
const https = require('https');
const http = require('http');
const { URL } = require('url');
const { dialog } = require('electron');
const { sanitizeLiveConfig } = require('./owner-config');

function requestJson(method, targetUrl, body, token, timeoutMs = 12000) {
  return new Promise((resolve, reject) => {
    let parsed;
    try {
      parsed = new URL(targetUrl);
    } catch (err) {
      reject(new Error('Invalid publish URL'));
      return;
    }

    const lib = parsed.protocol === 'https:' ? https : http;
    const payload = Buffer.from(JSON.stringify(body, null, 2), 'utf8');
    const headers = {
      'Content-Type': 'application/json; charset=utf-8',
      'Content-Length': payload.length,
      'User-Agent': 'NitroOwner/2.5'
    };
    if (token) {
      headers.Authorization = `Bearer ${token}`;
      headers['X-Nitro-Owner-Token'] = token;
    }

    const req = lib.request({
      protocol: parsed.protocol,
      hostname: parsed.hostname,
      port: parsed.port || undefined,
      path: parsed.pathname + parsed.search,
      method,
      headers,
      timeout: timeoutMs
    }, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve({ status: res.statusCode, body: data });
          return;
        }
        reject(new Error(`HTTP ${res.statusCode}${data ? ': ' + data.slice(0, 180) : ''}`));
      });
    });

    req.on('timeout', () => {
      req.destroy();
      reject(new Error('Publish timed out'));
    });
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

async function publishLiveConfig(config, publishUrl, publishToken) {
  const clean = sanitizeLiveConfig({ ...config, updatedAt: Date.now() });
  const url = String(publishUrl || '').trim();
  if (!url) {
    throw new Error('Publish URL is empty');
  }

  // Try PUT first (common for static JSON hosts / APIs), then POST.
  try {
    await requestJson('PUT', url, clean, publishToken);
    return { ok: true, method: 'PUT', config: clean };
  } catch (putErr) {
    try {
      await requestJson('POST', url, clean, publishToken);
      return { ok: true, method: 'POST', config: clean };
    } catch (postErr) {
      throw new Error(postErr.message || putErr.message || 'Publish failed');
    }
  }
}

async function exportLiveConfigFile(browserWindow, config) {
  const clean = sanitizeLiveConfig({ ...config, updatedAt: Date.now() });
  const result = await dialog.showSaveDialog(browserWindow, {
    title: 'Export Nitro live config',
    defaultPath: 'nitro-launcher.json',
    filters: [{ name: 'JSON', extensions: ['json'] }]
  });
  if (result.canceled || !result.filePath) {
    return { canceled: true };
  }
  fs.writeFileSync(result.filePath, JSON.stringify(clean, null, 2), 'utf8');
  return { canceled: false, path: result.filePath, config: clean };
}

module.exports = {
  publishLiveConfig,
  exportLiveConfigFile
};
