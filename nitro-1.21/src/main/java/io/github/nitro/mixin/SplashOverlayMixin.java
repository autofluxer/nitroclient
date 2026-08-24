package io.github.nitro.mixin;

import io.github.nitro.ui.NitroSplashRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

	@Shadow
	@Final
	private boolean reloading;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void nitro$onSplashCreated(MinecraftClient client, ResourceReload monitor,
			Consumer<Optional<Throwable>> exceptionHandler, boolean reloading, CallbackInfo ci) {
		// Mid-session reloads (leave world / F3+T) must not hold the overlay.
		NitroSplashRenderer.beginOverlay(reloading);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void nitro$drawSplash(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		// Skip branded splash during mid-session reloads so Leave World cannot get stuck here.
		if (this.reloading) {
			return;
		}
		NitroSplashRenderer.render(context, context.getScaledWindowWidth(), context.getScaledWindowHeight(), delta);
	}

	@Inject(method = "isInGracePeriod", at = @At("RETURN"), cancellable = true)
	private void nitro$extendGrace(CallbackInfoReturnable<Boolean> cir) {
		if (this.reloading) {
			return;
		}
		long reloadCompleteTime = ((SplashOverlayAccessor) this).nitro$getReloadCompleteTime();
		if (reloadCompleteTime > 0L && NitroSplashRenderer.shouldHold()) {
			cir.setReturnValue(true);
		}
	}
}
