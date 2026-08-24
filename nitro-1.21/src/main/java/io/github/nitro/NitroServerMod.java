package io.github.nitro;

import net.fabricmc.api.DedicatedServerModInitializer;

public final class NitroServerMod implements DedicatedServerModInitializer {

	@Override
	public void onInitializeServer() {
		io.github.nitro.client.NitroHelloNetworking.registerPayloadTypes();
		io.github.nitro.client.NitroHelloNetworking.registerServer();
	}
}
