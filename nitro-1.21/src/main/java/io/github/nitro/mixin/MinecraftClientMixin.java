package io.github.nitro.mixin;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.integration.NitroAutoJoin;
import io.github.nitro.ui.NitroMenus;
import io.github.nitro.ui.NitroTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen nitro$sanitizeScreen(Screen screen) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		screen = NitroMenus.sanitizeScreen(client, screen);
		screen = NitroAutoJoin.maybeReplaceDisconnected(screen);
		NitroAutoJoin.onScreenChange(screen);
		return screen;
	}

	@Inject(method = "setScreen", at = @At("TAIL"))
	private void nitro$swapVanillaTitleAtTail(CallbackInfo ci) {
		if (!NitroConfig.INSTANCE.fancyMainMenu) {
			return;
		}
		MinecraftClient client = (MinecraftClient) (Object) this;
		Screen current = client.currentScreen;
		if (current == null || current.getClass() != TitleScreen.class) {
			return;
		}
		// Wait until the world / integrated server has fully closed before swapping menus.
		// Never call setScreen while saving — that freezes "Saving world…".
		if (!NitroMenus.shouldUseNitroMainMenu(client) || NitroMenus.isWorldTearingDown(client)) {
			return;
		}
		client.setScreen(new NitroTitleScreen());
	}
}
