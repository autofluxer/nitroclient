package io.github.nitro.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class NitroHelloNetworking {

	private NitroHelloNetworking() {
	}

	public static void registerPayloadTypes() {
		PayloadTypeRegistry.playC2S().register(NitroHelloPayload.ID, NitroHelloPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(NitroHelloPayload.ID, NitroHelloPayload.CODEC);
	}

	public static void registerClient() {
		ClientPlayNetworking.registerGlobalReceiver(NitroHelloPayload.ID, (payload, context) -> {
			context.client().execute(() -> NitroUsers.mark(payload.playerId()));
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			NitroUsers.clear();
			NitroPresence.stop();
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			client.execute(() -> {
				if (client.player != null) {
					NitroUsers.mark(client.player.getUuid());
				}
				announce(client);
			});
		});
	}

	public static void registerServer() {
		ServerPlayNetworking.registerGlobalReceiver(NitroHelloPayload.ID, (payload, context) -> {
			MinecraftServer server = context.server();
			server.execute(() -> {
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					ServerPlayNetworking.send(player, payload);
				}
			});
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			// New joiners pick up Nitro users when those clients re-announce / presence polls.
		});
	}

	public static void announce(MinecraftClient client) {
		if (client.player == null || !ClientPlayNetworking.canSend(NitroHelloPayload.ID)) {
			return;
		}
		ClientPlayNetworking.send(new NitroHelloPayload(client.player.getUuid()));
	}

	/** Called each tick: keep local mark + try mod channel when the server supports it. */
	public static void tick(MinecraftClient client) {
		if (client == null || client.player == null) {
			return;
		}
		NitroUsers.mark(client.player.getUuid());
		if (client.player.age % 100 == 0) {
			announce(client);
		}
	}
}
