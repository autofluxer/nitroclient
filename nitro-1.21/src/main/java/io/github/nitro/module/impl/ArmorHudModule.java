package io.github.nitro.module.impl;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.hud.HudEditorState;
import io.github.nitro.hud.HudLayoutStore;
import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public final class ArmorHudModule extends NitroModule implements PositionedHudModule {

	private static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};
	private static final String[] EMPTY_LABELS = { "Helmet", "Chestplate", "Leggings", "Boots" };
	private static final int[] INV_SLOTS = { 39, 38, 37, 36 };
	private static final int ROW_H = 20;
	private static final int BOX_W = 92;

	public ArmorHudModule() {
		super("armor", "nitro.module.armor.name", "nitro.module.armor.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "armor";
	}

	@Override
	protected void onEnable() {
		HudElementLayout layout = layout();
		layout.visible = true;
		HudLayoutStore.save(hudId(), layout);
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		HudElementLayout layout = layout();
		if (!layout.visible) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}

		int boxH = SLOTS.length * ROW_H + 6;
		keepOnScreen(layout, BOX_W, boxH);

		var matrices = context.getMatrices();
		float scale = Math.max(0.75F, layout.scale);
		matrices.pushMatrix();
		matrices.translate(layout.x, layout.y);
		matrices.scale(scale, scale);

		NitroUiDraw.hudGlassChip(context, 0, 0, BOX_W, boxH);
		TextRenderer tr = client.textRenderer;
		int y = 3;
		for (int i = 0; i < SLOTS.length; i++) {
			ItemStack stack = equipped(client.player, i);
			int rowY = y + i * ROW_H;
			NitroUiDraw.fillRoundRect(context, 3, rowY, 16, 16, 2, 0x66000000);
			if (!stack.isEmpty()) {
				context.drawItemWithoutEntity(stack, 3, rowY);
				context.drawStackOverlay(tr, stack, 3, rowY);
				String name = tr.trimToWidth(stack.getName().getString(), 68);
				context.drawTextWithShadow(tr, name, 22, rowY + 1, NitroTheme.foreground());
				if (stack.isDamageable()) {
					int max = Math.max(1, stack.getMaxDamage());
					int left = Math.max(0, max - stack.getDamage());
					float pct = left / (float) max;
					int color = pct > 0.5F ? NitroTheme.success() : pct > 0.2F ? NitroTheme.warning() : NitroTheme.danger();
					context.drawTextWithShadow(tr, left + "/" + max, 22, rowY + 9, color);
					int barX = 22;
					int barY = rowY + 17;
					int barW = 64;
					context.fill(barX, barY, barX + barW, barY + 2, 0x66000000);
					context.fill(barX, barY, barX + Math.max(1, Math.round(barW * pct)), barY + 2, color);
				} else {
					context.drawTextWithShadow(tr, "—", 22, rowY + 9, NitroTheme.muted());
				}
			} else {
				context.drawTextWithShadow(tr, EMPTY_LABELS[i], 22, rowY + 1, NitroTheme.muted());
				context.drawTextWithShadow(tr, "empty", 22, rowY + 9, 0xFF5A6270);
			}
		}

		matrices.popMatrix();

		if (HudEditorState.active) {
			NitroUiDraw.strokeRoundRect(context, layout.x - 2, layout.y - 2,
					(int) (BOX_W * scale) + 4, (int) (boxH * scale) + 4, 5,
					NitroUiDraw.withAlpha(NitroTheme.accent(), 0xAA));
		}
	}

	private static ItemStack equipped(PlayerEntity player, int index) {
		ItemStack stack = player.getEquippedStack(SLOTS[index]);
		if (!stack.isEmpty()) {
			return stack;
		}
		PlayerInventory inventory = player.getInventory();
		if (inventory != null) {
			ItemStack fromInv = inventory.getStack(INV_SLOTS[index]);
			if (fromInv != null && !fromInv.isEmpty()) {
				return fromInv;
			}
		}
		return stack;
	}

	private static void keepOnScreen(HudElementLayout layout, int boxW, int boxH) {
		MinecraftClient client = MinecraftClient.getInstance();
		int sw = client.getWindow().getScaledWidth();
		int sh = client.getWindow().getScaledHeight();
		float scale = Math.max(0.75F, layout.scale);
		int w = (int) (boxW * scale);
		int h = (int) (boxH * scale);
		if (layout.x + w < 4 || layout.x > sw - 4 || layout.y + h < 4 || layout.y > sh - 4) {
			layout.x = Math.max(4, sw - w - 8);
			layout.y = Math.max(4, sh / 2 - h / 2);
		}
	}
}
