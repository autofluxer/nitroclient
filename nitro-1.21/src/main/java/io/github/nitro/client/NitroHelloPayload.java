package io.github.nitro.client;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/** Lightweight handshake: this player is on Nitro Client. */
public record NitroHelloPayload(UUID playerId) implements CustomPayload {

	public static final CustomPayload.Id<NitroHelloPayload> ID =
			new CustomPayload.Id<>(Identifier.of("nitro", "hello"));
	public static final PacketCodec<PacketByteBuf, NitroHelloPayload> CODEC = PacketCodec.tuple(
			Uuids.PACKET_CODEC, NitroHelloPayload::playerId,
			NitroHelloPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
