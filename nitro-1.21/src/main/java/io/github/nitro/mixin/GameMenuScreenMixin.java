package io.github.nitro.mixin;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {

	@Inject(method = "render", at = @At("HEAD"))
	private void nitro$chrome(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!NitroConfig.INSTANCE.fancyMainMenu) {
			return;
		}
		GameMenuScreen screen = (GameMenuScreen) (Object) this;
		int px = Math.max(40, (screen.width - 220) / 2);
		int py = Math.max(48, (screen.height - 204) / 2);
		int panelW = 244;
		int panelH = 220;
		NitroUiDraw.glassPanel(context, px - 12, py - 32, panelW, panelH, 14);
		context.drawCenteredTextWithShadow(screen.getTextRenderer(), Text.literal("NITRO CLIENT"),
				screen.width / 2, py - 22, NitroTheme.foreground());
		context.drawCenteredTextWithShadow(screen.getTextRenderer(), Text.literal("Game Menu"),
				screen.width / 2, py - 10, NitroTheme.muted());
	}
}
