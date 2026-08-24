package io.github.nitro.discord;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal Discord IPC Rich Presence (Windows named pipe / Unix socket).
 * Shows Nitro Client + server + Minecraft version while playing.
 */
public final class DiscordIpcClient {

	private static final Logger LOGGER = LoggerFactory.getLogger("nitroclient");
	private static final Gson GSON = new Gson();
	private static final long CLIENT_ID = 1520708712241823876L;
	private static final int OP_HANDSHAKE = 0;
	private static final int OP_FRAME = 1;
	private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "nitro-discord-rpc");
		t.setDaemon(true);
		return t;
	});
	private final AtomicBoolean connected = new AtomicBoolean(false);
	private RandomAccessFile pipe;
	private String lastDetails = "";
	private String lastState = "";
	private long startedAtSec = 0L;

	public void connect() {
		worker.execute(() -> {
			if (connected.get()) {
				return;
			}
			try {
				pipe = openPipe();
				if (pipe == null) {
					LOGGER.info("Discord RPC: Discord desktop not found");
					return;
				}
				JsonObject handshake = new JsonObject();
				handshake.addProperty("v", 1);
				handshake.addProperty("client_id", Long.toString(CLIENT_ID));
				write(OP_HANDSHAKE, handshake.toString());
				// Read READY (best-effort)
				readFrameQuiet();
				connected.set(true);
				LOGGER.info("Discord RPC connected");
				if (!lastDetails.isEmpty()) {
					apply(lastDetails, lastState);
				}
			} catch (Throwable t) {
				LOGGER.warn("Discord RPC connect failed: {}", t.toString());
				closeQuiet();
			}
		});
	}

	public void setActivity(String details, String state) {
		lastDetails = details == null ? "" : details;
		lastState = state == null ? "" : state;
		worker.execute(() -> {
			if (!connected.get()) {
				connect();
				return;
			}
			try {
				apply(lastDetails, lastState);
			} catch (Throwable t) {
				connected.set(false);
				closeQuiet();
			}
		});
	}

	public void clear() {
		worker.execute(() -> {
			if (!connected.get() || pipe == null) {
				return;
			}
			try {
				JsonObject args = new JsonObject();
				args.addProperty("pid", ProcessHandle.current().pid());
				JsonObject root = new JsonObject();
				root.addProperty("cmd", "SET_ACTIVITY");
				root.add("args", args);
				root.addProperty("nonce", UUID.randomUUID().toString());
				write(OP_FRAME, root.toString());
			} catch (Throwable ignored) {
			}
		});
	}

	public void shutdown() {
		clear();
		worker.execute(this::closeQuiet);
		worker.shutdown();
	}

	private void apply(String details, String state) throws IOException {
		if (startedAtSec <= 0L) {
			startedAtSec = System.currentTimeMillis() / 1000L;
		}
		JsonObject timestamps = new JsonObject();
		timestamps.addProperty("start", startedAtSec);

		JsonObject assets = new JsonObject();
		// HTTPS URL bypasses Discord Developer Portal asset/CDN cache (asset keys stay stuck).
		assets.addProperty("large_image", "https://litter.catbox.moe/9m35ug.png");
		assets.addProperty("large_text", "Nitro Client");

		JsonObject activity = new JsonObject();
		activity.addProperty("details", details);
		activity.addProperty("state", state);
		activity.add("timestamps", timestamps);
		activity.add("assets", assets);

		JsonObject args = new JsonObject();
		args.addProperty("pid", ProcessHandle.current().pid());
		args.add("activity", activity);

		JsonObject root = new JsonObject();
		root.addProperty("cmd", "SET_ACTIVITY");
		root.add("args", args);
		root.addProperty("nonce", UUID.randomUUID().toString());
		write(OP_FRAME, root.toString());
	}

	private void write(int opcode, String json) throws IOException {
		byte[] data = json.getBytes(StandardCharsets.UTF_8);
		ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		header.putInt(opcode);
		header.putInt(data.length);
		pipe.write(header.array());
		pipe.write(data);
	}

	private void readFrameQuiet() {
		try {
			byte[] header = new byte[8];
			if (pipe.read(header) != 8) {
				return;
			}
			ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
			buf.getInt(); // opcode
			int len = buf.getInt();
			if (len <= 0 || len > 65536) {
				return;
			}
			byte[] body = new byte[len];
			int read = 0;
			while (read < len) {
				int n = pipe.read(body, read, len - read);
				if (n < 0) {
					break;
				}
				read += n;
			}
		} catch (Throwable ignored) {
		}
	}

	private static RandomAccessFile openPipe() {
		String os = System.getProperty("os.name", "").toLowerCase();
		if (os.contains("win")) {
			for (int i = 0; i < 10; i++) {
				try {
					return new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
				} catch (Throwable ignored) {
				}
			}
			return null;
		}
		// Unix: $XDG_RUNTIME_DIR/discord-ipc-N
		String runtime = System.getenv("XDG_RUNTIME_DIR");
		if (runtime == null || runtime.isBlank()) {
			runtime = "/tmp";
		}
		for (int i = 0; i < 10; i++) {
			try {
				return new RandomAccessFile(runtime + "/discord-ipc-" + i, "rw");
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	private void closeQuiet() {
		connected.set(false);
		if (pipe != null) {
			try {
				pipe.close();
			} catch (Throwable ignored) {
			}
			pipe = null;
		}
	}
}
