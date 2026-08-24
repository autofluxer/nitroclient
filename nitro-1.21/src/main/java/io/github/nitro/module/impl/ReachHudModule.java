package io.github.nitro.module.impl;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.hud.HudLayoutStore;
import io.github.nitro.mixin.CameraAccessor;
import io.github.nitro.module.NitroModule;
import io.github.nitro.render.ClientFov;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import io.github.nitro.module.TickableModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ReachHudModule extends NitroModule implements PositionedHudModule, TickableModule {

	private static final int SEGMENTS = 72;
	private static final int CIRCLE = 0xE0E03C3C;
	private static final int CIRCLE_SOFT = 0x88E03C3C;

	private double lastHit;
	private int lastHitTicks;

	public ReachHudModule() {
		super("reach", "nitro.module.reach.name", "nitro.module.reach.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "reach";
	}

	@Override
	protected void onEnable() {
		HudElementLayout layout = layout();
		layout.visible = true;
		HudLayoutStore.save(hudId(), layout);
	}

	@Override
	public void onClientTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}
		if (lastHitTicks > 0) {
			lastHitTicks--;
		}
		if (client.options.attackKey.isPressed() && client.crosshairTarget instanceof EntityHitResult hit) {
			lastHit = client.player.distanceTo(hit.getEntity());
			lastHitTicks = 40;
		}
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}
		float tickProgress = tickCounter.getTickProgress(true);
		double reach = attackReach(client.player);
		drawReachCircle(context, client, tickProgress, reach);

		HudElementLayout layout = layout();
		if (!layout.visible) {
			return;
		}
		String label;
		if (lastHitTicks > 0) {
			label = String.format("Reach %.2f", lastHit);
		} else if (client.crosshairTarget instanceof EntityHitResult hit) {
			label = String.format("Reach %.2f", client.player.distanceTo(hit.getEntity()));
		} else {
			label = String.format("Reach %.1f", reach);
		}
		renderPositioned(context, label);
	}

	private static double attackReach(PlayerEntity player) {
		var attribute = player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
		if (attribute != null) {
			return attribute.getValue();
		}
		return 3.0;
	}

	private static void drawReachCircle(DrawContext context, MinecraftClient client, float tickProgress, double radius) {
		Vec3d feet = client.player.getLerpedPos(tickProgress);
		double y = feet.y + 0.08;
		float[] prev = new float[2];
		boolean prevValid = false;
		for (int i = 0; i <= SEGMENTS; i++) {
			double angle = (Math.PI * 2.0 * i) / SEGMENTS;
			Vec3d world = new Vec3d(
					feet.x + Math.cos(angle) * radius,
					y,
					feet.z + Math.sin(angle) * radius);
			float[] screen = new float[2];
			boolean valid = worldToScreen(client, world, screen);
			if (valid && prevValid) {
				drawLine(context, prev[0], prev[1], screen[0], screen[1], CIRCLE, 2);
				drawLine(context, prev[0], prev[1], screen[0], screen[1], CIRCLE_SOFT, 4);
			}
			prevValid = valid;
			prev[0] = screen[0];
			prev[1] = screen[1];
		}
	}

	private static boolean worldToScreen(MinecraftClient client, Vec3d world, float[] out) {
		Camera camera = client.gameRenderer.getCamera();
		if (camera == null || !camera.isReady()) {
			return false;
		}
		Vec3d camPos = ((CameraAccessor) camera).nitro$getPos();
		Vector3f rel = new Vector3f(
				(float) (world.x - camPos.x),
				(float) (world.y - camPos.y),
				(float) (world.z - camPos.z));
		new Quaternionf(camera.getRotation()).conjugate().transform(rel);
		if (rel.z >= -0.08F) {
			return false;
		}
		int sw = client.getWindow().getScaledWidth();
		int sh = client.getWindow().getScaledHeight();
		if (sw <= 0 || sh <= 0) {
			return false;
		}
		float fov = Math.max(10F, ClientFov.degrees);
		float tanHalf = (float) Math.tan(Math.toRadians(fov) * 0.5);
		float aspect = sw / (float) sh;
		float ndcX = rel.x / (-rel.z * tanHalf * aspect);
		float ndcY = rel.y / (-rel.z * tanHalf);
		out[0] = (ndcX + 1F) * 0.5F * sw;
		out[1] = (1F - ndcY) * 0.5F * sh;
		return out[0] >= -80 && out[0] <= sw + 80 && out[1] >= -80 && out[1] <= sh + 80;
	}

	private static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, int color, int thickness) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float dist = (float) Math.hypot(dx, dy);
		if (dist < 0.5F) {
			return;
		}
		int steps = Math.max(1, Math.round(dist));
		int half = Math.max(1, thickness / 2);
		for (int i = 0; i <= steps; i++) {
			float t = i / (float) steps;
			int x = Math.round(x1 + dx * t);
			int y = Math.round(y1 + dy * t);
			context.fill(x - half, y - half, x + half + 1, y + half + 1, color);
		}
	}
}
