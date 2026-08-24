package io.github.nitro.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks which players are running Nitro Client (for tab-list icons). */
public final class NitroUsers {

	private static final Set<UUID> USERS = ConcurrentHashMap.newKeySet();

	private NitroUsers() {
	}

	public static void mark(UUID id) {
		if (id != null) {
			USERS.add(id);
		}
	}

	public static void clear() {
		USERS.clear();
	}

	public static boolean isNitro(UUID id) {
		return id != null && USERS.contains(id);
	}
}
