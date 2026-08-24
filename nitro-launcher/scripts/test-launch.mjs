import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';

const game = path.join(process.env.LOCALAPPDATA, 'Nitro Client', 'game');
const run = path.join(process.env.APPDATA, 'nitroclient', 'nitro-189');
const cfg = JSON.parse(fs.readFileSync(path.join(game, '.nitro-launch.json'), 'utf8').replace(/^\uFEFF/, ''));
const java = 'C:\\Program Files (x86)\\Common Files\\Oracle\\Java\\java8path\\java.exe';

const args = [...cfg.jvmArgs, '-cp', cfg.classpath, cfg.mainClass];
for (let i = 0; i < cfg.args.length; i++) {
  if (cfg.args[i] === '--username') {
    args.push('--username', 'TestUser');
    i++;
  } else {
    args.push(cfg.args[i]);
  }
}

const child = spawn(java, args, { cwd: run, detached: true, stdio: 'ignore', windowsHide: false });
child.unref();

const started = Date.now();
const timer = setInterval(() => {
  let alive = false;
  try {
    process.kill(child.pid, 0);
    alive = true;
  } catch (_) {}

  const logPath = path.join(run, 'logs', 'latest.log');
  const log = fs.existsSync(logPath) ? fs.readFileSync(logPath, 'utf8') : '';
  const tail = log.trim().split('\n').slice(-5).join(' | ');

  if (!alive || Date.now() - started > 30000) {
    clearInterval(timer);
    console.log('alive:', alive, 'after', Date.now() - started, 'ms');
    console.log('tail:', tail);
    if (!alive) console.log('full tail:\n' + log.trim().split('\n').slice(-15).join('\n'));
  }
}, 1000);
