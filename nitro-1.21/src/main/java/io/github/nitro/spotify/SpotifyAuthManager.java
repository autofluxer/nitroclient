package io.github.nitro.spotify;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class SpotifyAuthManager {

	private static final Logger LOGGER = LoggerFactory.getLogger("nitroclient");
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(8))
			.build();

	private final Object lock = new Object();
	private volatile SpotifyTokenStore tokens = SpotifyTokenStore.load();
	private HttpServer callbackServer;
	private String pendingState;
	private String pendingVerifier;
	private volatile CountDownLatch activeLatch;
	private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
	private volatile boolean authorizing;

	public SpotifyTokenStore tokens() {
		return tokens;
	}

	public boolean isConnected() {
		return tokens.hasRefreshToken();
	}

	public boolean isAuthorizing() {
		return authorizing;
	}

	public synchronized CompletableFuture<Boolean> beginLogin() {
		String clientId = SpotifyClientConfig.resolveClientId();
		if (clientId.isBlank()) {
			return CompletableFuture.failedFuture(new IllegalStateException(SpotifyMessages.CLIENT_ID_MISSING));
		}
		cancelLoginInternal(false);
		cancelRequested.set(false);
		authorizing = true;
		pendingVerifier = pkceVerifier();
		pendingState = randomUrlSafe(32);
		String challenge = pkceChallenge(pendingVerifier);

		CountDownLatch latch = new CountDownLatch(1);
		activeLatch = latch;
		AtomicReference<String> codeRef = new AtomicReference<>();
		AtomicReference<String> errorRef = new AtomicReference<>();

		try {
			callbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", SpotifyClientConfig.CALLBACK_PORT), 0);
			callbackServer.createContext("/callback", exchange -> handleCallback(exchange, latch, codeRef, errorRef));
			callbackServer.setExecutor(null);
			callbackServer.start();
		} catch (IOException e) {
			authorizing = false;
			LOGGER.warn("Spotify callback server failed: {}", e.toString());
			return CompletableFuture.failedFuture(new IOException(SpotifyMessages.UNABLE_CONNECT, e));
		}

		String authUrl = "https://accounts.spotify.com/authorize"
				+ "?client_id=" + enc(clientId)
				+ "&response_type=code"
				+ "&redirect_uri=" + enc(SpotifyClientConfig.REDIRECT_URI)
				+ "&scope=" + enc(SpotifyClientConfig.SCOPES)
				+ "&state=" + enc(pendingState)
				+ "&code_challenge_method=S256"
				+ "&code_challenge=" + enc(challenge);

		Util.getOperatingSystem().open(URI.create(authUrl));

		return CompletableFuture.supplyAsync(() -> {
			try {
				boolean ok = latch.await(3, TimeUnit.MINUTES);
				stopCallbackServer();
				if (cancelRequested.get()) {
					throw new IllegalStateException(SpotifyMessages.AUTH_CANCELLED);
				}
				if (!ok) {
					throw new IllegalStateException(SpotifyMessages.AUTH_FAILED);
				}
				if (errorRef.get() != null) {
					String err = errorRef.get();
					if ("access_denied".equalsIgnoreCase(err) || "cancelled".equalsIgnoreCase(err)) {
						throw new IllegalStateException(SpotifyMessages.AUTH_CANCELLED);
					}
					throw new IllegalStateException(SpotifyMessages.AUTH_FAILED);
				}
				String code = codeRef.get();
				if (code == null || code.isBlank()) {
					throw new IllegalStateException(SpotifyMessages.AUTH_FAILED);
				}
				exchangeCode(clientId, code, pendingVerifier);
				return true;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				stopCallbackServer();
				throw new IllegalStateException(SpotifyMessages.AUTH_CANCELLED, e);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalStateException(SpotifyMessages.AUTH_FAILED, e);
			} finally {
				authorizing = false;
				pendingState = null;
				pendingVerifier = null;
				activeLatch = null;
			}
		});
	}

	/** Cancel an in-progress browser OAuth wait. */
	public synchronized void cancelLogin() {
		cancelLoginInternal(true);
	}

	private void cancelLoginInternal(boolean notifyLatch) {
		cancelRequested.set(true);
		authorizing = false;
		stopCallbackServer();
		if (notifyLatch) {
			CountDownLatch latch = activeLatch;
			if (latch != null) {
				latch.countDown();
			}
		}
	}

	public synchronized void disconnect() {
		cancelLoginInternal(true);
		tokens.clear();
		tokens = new SpotifyTokenStore();
	}

	public synchronized String validAccessToken() throws IOException, InterruptedException {
		if (!tokens.hasRefreshToken()) {
			throw new IOException(SpotifyMessages.NOT_CONNECTED);
		}
		if (!tokens.accessExpired(30_000L)) {
			return tokens.accessToken;
		}
		refreshAccessToken();
		if (tokens.accessToken == null || tokens.accessToken.isBlank()) {
			throw new IOException(SpotifyMessages.UNABLE_CONNECT);
		}
		return tokens.accessToken;
	}

	public synchronized void refreshAccessToken() throws IOException, InterruptedException {
		String clientId = SpotifyClientConfig.resolveClientId();
		if (clientId.isBlank() || !tokens.hasRefreshToken()) {
			throw new IOException(SpotifyMessages.NOT_CONNECTED);
		}
		String body = "grant_type=refresh_token"
				+ "&refresh_token=" + enc(tokens.refreshToken)
				+ "&client_id=" + enc(clientId);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://accounts.spotify.com/api/token"))
				.timeout(Duration.ofSeconds(10))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() >= 400) {
			LOGGER.warn("Spotify token refresh failed: HTTP {}", response.statusCode());
			throw new IOException(SpotifyMessages.RECONNECTING);
		}
		applyTokenResponse(response.body(), false);
	}

	public void shutdown() {
		cancelLoginInternal(true);
	}

	private void exchangeCode(String clientId, String code, String verifier) throws IOException, InterruptedException {
		String body = "grant_type=authorization_code"
				+ "&code=" + enc(code)
				+ "&redirect_uri=" + enc(SpotifyClientConfig.REDIRECT_URI)
				+ "&client_id=" + enc(clientId)
				+ "&code_verifier=" + enc(verifier);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://accounts.spotify.com/api/token"))
				.timeout(Duration.ofSeconds(12))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() >= 400) {
			throw new IOException(SpotifyMessages.AUTH_FAILED);
		}
		applyTokenResponse(response.body(), true);
	}

	private void applyTokenResponse(String json, boolean requireRefresh) {
		JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
		synchronized (lock) {
			tokens.accessToken = text(obj, "access_token");
			String refresh = text(obj, "refresh_token");
			if (!refresh.isBlank()) {
				tokens.refreshToken = refresh;
			} else if (requireRefresh) {
				throw new IllegalStateException(SpotifyMessages.AUTH_FAILED);
			}
			int expiresIn = obj.has("expires_in") ? obj.get("expires_in").getAsInt() : 3600;
			tokens.expiresAtEpochMs = System.currentTimeMillis() + expiresIn * 1000L;
			tokens.tokenType = text(obj, "token_type");
			if (tokens.tokenType.isBlank()) {
				tokens.tokenType = "Bearer";
			}
			tokens.scope = text(obj, "scope");
			tokens.save();
		}
	}

	private void handleCallback(HttpExchange exchange, CountDownLatch latch,
			AtomicReference<String> codeRef, AtomicReference<String> errorRef) throws IOException {
		try {
			URI uri = exchange.getRequestURI();
			Map<String, String> q = query(uri.getRawQuery());
			String state = q.getOrDefault("state", "");
			if (pendingState == null || !pendingState.equals(state)) {
				errorRef.set("invalid_state");
				writeHtml(exchange, 400, "Authorization failed: invalid state.");
			} else if (q.containsKey("error")) {
				errorRef.set(q.get("error"));
				writeHtml(exchange, 200, "Authorization cancelled. You can close this tab.");
			} else {
				codeRef.set(q.getOrDefault("code", ""));
				writeHtml(exchange, 200, "Spotify authorization successful. You can close this tab and return to Minecraft.");
			}
		} finally {
			latch.countDown();
		}
	}

	private void writeHtml(HttpExchange exchange, int status, String message) throws IOException {
		byte[] bytes = ("<!DOCTYPE html><html><body style=\"font-family:Segoe UI,sans-serif;background:#0b1018;color:#fff;"
				+ "display:grid;place-items:center;height:100vh;margin:0\">"
				+ "<div style=\"text-align:center;max-width:420px\">"
				+ "<h2 style=\"margin:0 0 12px\">Nitro Client</h2>"
				+ "<p style=\"color:#a8c4da;line-height:1.5\">" + message + "</p>"
				+ "</div></body></html>")
				.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	private synchronized void stopCallbackServer() {
		if (callbackServer != null) {
			try {
				callbackServer.stop(0);
			} catch (Exception ignored) {
			}
			callbackServer = null;
		}
	}

	private static Map<String, String> query(String raw) {
		java.util.HashMap<String, String> map = new java.util.HashMap<>();
		if (raw == null || raw.isBlank()) {
			return map;
		}
		for (String part : raw.split("&")) {
			int eq = part.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String k = java.net.URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
			String v = java.net.URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
			map.put(k, v);
		}
		return map;
	}

	private static String text(JsonObject obj, String key) {
		return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
	}

	private static String enc(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String pkceVerifier() {
		return randomUrlSafe(64);
	}

	private static String pkceChallenge(String verifier) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static String randomUrlSafe(int bytes) {
		byte[] buf = new byte[bytes];
		new SecureRandom().nextBytes(buf);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	}
}
