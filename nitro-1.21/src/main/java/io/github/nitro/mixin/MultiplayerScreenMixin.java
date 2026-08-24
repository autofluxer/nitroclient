package io.github.nitro.mixin;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.ui.NitroTitleScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {

	@Shadow
	@Final
	@Mutable
	private Screen parent;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void nitro$forceNitroParent(Screen parent, CallbackInfo ci) {
		if (NitroConfig.INSTANCE.fancyMainMenu) {
			this.parent = new NitroTitleScreen();
		}
	}
}
