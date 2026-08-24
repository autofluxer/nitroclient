package io.github.nitro.mixin;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.ui.NitroTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {

	@Redirect(
			method = "onCloseScreen",
			at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V", remap = false)
	)
	private void nitro$navigateAfterCancel(Runnable runnable) {
		runnable.run();
		if (!NitroConfig.INSTANCE.fancyMainMenu) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		client.setScreen(new SelectWorldScreen(new NitroTitleScreen()));
	}
}
