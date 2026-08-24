const path = require('path');
const fs = require('fs');
const fabricSetup = require('./fabric-setup');

const gameDir = path.join(process.env.APPDATA, 'nitroclient', 'nitroclient', 'nitro-1.21.11-test');
const mc = '1.21.11';

(async () => {
  console.log('gameDir', gameDir);
  const versionId = await fabricSetup.prepareModernFabricInstall(gameDir, mc);
  console.log('fabric version', versionId);
  const jsonPath = path.join(gameDir, 'versions', versionId, `${versionId}.json`);
  console.log('json exists', fs.existsSync(jsonPath));
  const mods = fs.readdirSync(path.join(gameDir, 'mods'));
  console.log('mods', mods);

  const { Client, Authenticator } = require('minecraft-launcher-core');
  const launcher = new Client();
  launcher.on('debug', (line) => console.log('[debug]', line));
  launcher.on('data', (line) => process.stdout.write(line));

  const proc = await launcher.launch({
    authorization: Authenticator.getAuth('TestPlayer'),
    root: gameDir,
    javaPath: 'C:\\Program Files\\Java\\jdk-21\\bin\\javaw.exe',
    version: { number: mc, type: 'release', custom: versionId },
    memory: { max: '4096M', min: '1024M' },
    overrides: { detached: true, gameDirectory: gameDir, cwd: gameDir }
  });

  console.log('proc', proc ? proc.pid : null);
  if (!proc) process.exit(1);
  setTimeout(() => {
    console.log('still running', proc.pid);
    process.exit(0);
  }, 15000);
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
