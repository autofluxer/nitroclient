const net = require('net');

const DEFAULT_HOST = 'nitrosmp.lol';
const DEFAULT_PORT = 25565;
const TIMEOUT_MS = 4500;

function writeVarInt(value) {
  const bytes = [];
  do {
    let temp = value & 0x7f;
    value >>>= 7;
    if (value !== 0) temp |= 0x80;
    bytes.push(temp);
  } while (value !== 0);
  return Buffer.from(bytes);
}

function writeString(value) {
  const str = Buffer.from(value, 'utf8');
  return Buffer.concat([writeVarInt(str.length), str]);
}

function readVarInt(buffer, offset = 0) {
  let num = 0;
  let shift = 0;
  let i = offset;
  while (true) {
    if (i >= buffer.length) return { value: null, length: 0 };
    const byte = buffer[i++];
    num |= (byte & 0x7f) << shift;
    if ((byte & 0x80) === 0) break;
    shift += 7;
    if (shift > 35) return { value: null, length: 0 };
  }
  return { value: num, length: i - offset };
}

function readString(buffer, offset = 0) {
  const len = readVarInt(buffer, offset);
  if (!len.value && len.value !== 0) return null;
  const start = offset + len.length;
  const end = start + len.value;
  if (end > buffer.length) return null;
  return {
    value: buffer.toString('utf8', start, end),
    length: len.length + len.value
  };
}

function stripMinecraftFormatting(text) {
  return (text || '').replace(/§./g, '').trim();
}

function extractChatText(component) {
  if (!component) return '';
  if (typeof component === 'string') return component;
  let text = component.text || '';
  if (Array.isArray(component.extra)) {
    text += component.extra.map(extractChatText).join('');
  }
  return text;
}

function parseMotd(description) {
  if (!description) return '';
  if (typeof description === 'string') return stripMinecraftFormatting(description);
  return stripMinecraftFormatting(extractChatText(description));
}

function pingServer(host = DEFAULT_HOST, port = DEFAULT_PORT) {
  const started = Date.now();

  return new Promise((resolve) => {
    const socket = net.connect({ host, port, timeout: TIMEOUT_MS });
    let buffer = Buffer.alloc(0);

    const finish = (result) => {
      if (!socket.destroyed) socket.destroy();
      resolve(result);
    };

    socket.on('connect', () => {
      const handshake = Buffer.concat([
        writeVarInt(0x00),
        writeVarInt(47),
        writeString(host),
        Buffer.from([(port >> 8) & 0xff, port & 0xff]),
        writeVarInt(1)
      ]);
      const handshakePacket = Buffer.concat([writeVarInt(handshake.length), handshake]);
      socket.write(handshakePacket);
      socket.write(Buffer.from([0x01, 0x00]));
    });

    socket.on('data', (chunk) => {
      buffer = Buffer.concat([buffer, chunk]);
      if (buffer.length < 2) return;

      const packetLen = readVarInt(buffer, 0);
      if (!packetLen.value && packetLen.value !== 0) return;
      const total = packetLen.length + packetLen.value;
      if (buffer.length < total) return;

      const packetId = readVarInt(buffer, packetLen.length);
      if (packetId.value !== 0x00) {
        finish({ online: false, host, port, error: 'Unexpected response' });
        return;
      }

      const jsonPart = readString(buffer, packetLen.length + packetId.length);
      if (!jsonPart) {
        finish({ online: false, host, port, error: 'Invalid JSON packet' });
        return;
      }

      try {
        const data = JSON.parse(jsonPart.value);
        const players = data.players || {};
        finish({
          online: true,
          host,
          port,
          motd: parseMotd(data.description),
          version: data.version?.name || '',
          playersOnline: players.online ?? 0,
          playersMax: players.max ?? 0,
          ping: Date.now() - started,
          favicon: typeof data.favicon === 'string' ? data.favicon : null
        });
      } catch (_) {
        finish({ online: false, host, port, error: 'Could not parse server status' });
      }
    });

    socket.on('timeout', () => finish({ online: false, host, port, error: 'Timed out' }));
    socket.on('error', (err) => finish({ online: false, host, port, error: err.message || 'Offline' }));
  });
}

module.exports = {
  DEFAULT_HOST,
  DEFAULT_PORT,
  pingServer,
  stripMinecraftFormatting
};
