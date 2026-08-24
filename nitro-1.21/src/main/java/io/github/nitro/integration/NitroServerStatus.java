package io.github.nitro.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class NitroServerStatus {

	public static final String HOST = "nitrosmp.lol";
	public static final int PORT = 25565;

	public enum State {
		CHECKING,
		ONLINE,
		OFFLINE
	}

	private static volatile State state = State.CHECKING;
	private static volatile int pingMs = -1;
	private static volatile int playersOnline = -1;
	private static volatile int playersMax = -1;
	private static volatile long lastCheck;
	private static volatile boolean checking;

	private NitroServerStatus() {
	}

	public static State getState() {
		return state;
	}

	public static int getPingMs() {
		return pingMs;
	}

	public static int getPlayersOnline() {
		return playersOnline;
	}

	public static int getPlayersMax() {
		return playersMax;
	}

	public static void refreshIfStale() {
		long now = System.currentTimeMillis();
		if (checking || now - lastCheck < 15000L) {
			return;
		}
		checking = true;
		state = State.CHECKING;
		Thread thread = new Thread(NitroServerStatus::probe, "Nitro-ServerStatus");
		thread.setDaemon(true);
		thread.start();
	}

	private static void probe() {
		long started = System.currentTimeMillis();
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(HOST, PORT), 4500);
			socket.setSoTimeout(4500);
			OutputStream out = socket.getOutputStream();
			InputStream in = socket.getInputStream();
			writeHandshake(out, HOST, PORT);
			writeStatusRequest(out);
			String json = readStatusJson(in);
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			JsonObject players = root.has("players") ? root.getAsJsonObject("players") : null;
			if (players != null) {
				playersOnline = players.has("online") ? players.get("online").getAsInt() : -1;
				playersMax = players.has("max") ? players.get("max").getAsInt() : -1;
			} else {
				playersOnline = -1;
				playersMax = -1;
			}
			writePing(out, started);
			readPong(in);
			pingMs = (int) (System.currentTimeMillis() - started);
			state = State.ONLINE;
		} catch (Throwable ignored) {
			pingMs = -1;
			playersOnline = -1;
			playersMax = -1;
			state = State.OFFLINE;
		} finally {
			lastCheck = System.currentTimeMillis();
			checking = false;
		}
	}

	private static void writeHandshake(OutputStream out, String host, int port) throws IOException {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(payload);
		writeVarInt(data, 0x00);
		writeVarInt(data, 47);
		writeString(data, host);
		data.writeShort(port);
		writeVarInt(data, 1);
		writePacket(out, payload.toByteArray());
	}

	private static void writeStatusRequest(OutputStream out) throws IOException {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(payload);
		writeVarInt(data, 0x00);
		writePacket(out, payload.toByteArray());
	}

	private static void writePing(OutputStream out, long payload) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(buffer);
		writeVarInt(data, 0x01);
		data.writeLong(payload);
		writePacket(out, buffer.toByteArray());
	}

	private static void writePacket(OutputStream out, byte[] payload) throws IOException {
		ByteArrayOutputStream packet = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(packet);
		writeVarInt(data, payload.length);
		data.write(payload);
		out.write(packet.toByteArray());
		out.flush();
	}

	private static String readStatusJson(InputStream in) throws IOException {
		DataInputStream data = new DataInputStream(in);
		readVarInt(data);
		int packetId = readVarInt(data);
		if (packetId != 0x00) {
			throw new IOException("Unexpected status packet");
		}
		int length = readVarInt(data);
		byte[] bytes = new byte[length];
		data.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static void readPong(InputStream in) throws IOException {
		DataInputStream data = new DataInputStream(in);
		readVarInt(data);
		int packetId = readVarInt(data);
		if (packetId != 0x01) {
			return;
		}
		data.readLong();
	}

	private static void writeVarInt(DataOutputStream out, int value) throws IOException {
		while ((value & ~0x7F) != 0) {
			out.writeByte((value & 0x7F) | 0x80);
			value >>>= 7;
		}
		out.writeByte(value);
	}

	private static int readVarInt(DataInputStream in) throws IOException {
		int num = 0;
		int shift = 0;
		while (true) {
			int b = in.readUnsignedByte();
			num |= (b & 0x7F) << shift;
			if ((b & 0x80) == 0) {
				return num;
			}
			shift += 7;
			if (shift > 35) {
				throw new IOException("VarInt too big");
			}
		}
	}

	private static void writeString(DataOutputStream out, String value) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		writeVarInt(out, bytes.length);
		out.write(bytes);
	}
}
