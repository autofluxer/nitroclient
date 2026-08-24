package io.github.nitro.ui;

import io.github.nitro.ui.animation.NitroEasing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * FastClient title-row button — proportions sampled from FastClient 1.21.11 screenshots:
 * ~200×24, radius 3, ~3px gaps, 11px left icon, thin border, flat dark glass.
 * Featured = soft-blue L→R gradient (Nitro, not orange).
 */
public final class LunarMenuButton extends ClickableWidget {

	public enum Icon {
		NONE(null),
		PLAY(NitroIcons.Id.USER),
		MULTIPLAYER(NitroIcons.Id.USERS),
		COSMETICS(NitroIcons.Id.HANGER),
		SCREENSHOTS(NitroIcons.Id.CAMERA),
		MODS(NitroIcons.Id.GRID),
		SETTINGS(NitroIcons.Id.GEAR),
		STORE(NitroIcons.Id.SHOP),
		QUIT(null);

		private final NitroIcons.Id texture;

		Icon(NitroIcons.Id texture) {
			this.texture = texture;
		}
	}

	public enum Style {
		NORMAL, FEATURED, DANGER, TEXT
	}

	public interface PressAction {
		void onPress(LunarMenuButton button);
	}

	/** FastClient main stack — defaults; title screen overrides via screen-pixel targets. */
	public static final int WIDTH = 150;
	public static final int HEIGHT = 19;
	public static final int GAP = 3;
	private static final int RADIUS = 3;
	private static final int ICON_SIZE = 11;
	private static final int ICON_TEXT_GAP = 8;
	private static final float HOVER_SPEED = 14F;

	private final Icon icon;
	private final Style style;
	private final PressAction onPress;
	private float hover;
	private float alpha = 1F;

	public LunarMenuButton(int x, int y, int w, int h, Text label, Icon icon, Style style, PressAction onPress) {
		super(x, y, w, h, label);
		this.icon = icon == null ? Icon.NONE : icon;
		this.style = style == null ? Style.NORMAL : style;
		this.onPress = onPress;
	}

	public LunarMenuButton(int x, int y, int w, int h, Text label, Icon icon, PressAction onPress) {
		this(x, y, w, h, label, icon, Style.NORMAL, onPress);
	}

	public LunarMenuButton(int x, int y, int w, int h, Text label, boolean accent, PressAction onPress) {
		this(x, y, w, h, label, accent ? Icon.PLAY : Icon.NONE, accent ? Style.FEATURED : Style.NORMAL, onPress);
	}

	public LunarMenuButton(int x, int y, Text label, boolean accent, PressAction onPress) {
		this(x, y, WIDTH, HEIGHT, label, accent, onPress);
	}

	public void setMenuAlpha(float alpha) {
		this.alpha = NitroEasing.clamp01(alpha);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		hover = NitroEasing.approach(hover, hovered ? 1F : 0F, Math.max(0.016F, delta), HOVER_SPEED);

		switch (style) {
			case FEATURED -> {
				int left = mulAlpha(NitroUiDraw.lerpColor(FeatherPalette.RED, FeatherPalette.RED_HOVER, hover * 0.35F), alpha);
				int right = mulAlpha(NitroUiDraw.lerpColor(FeatherPalette.RED_DARK, 0xFF5A1010, hover * 0.2F), alpha);
				NitroUiDraw.fillHGradientRoundRect(context, getX(), getY(), width, height, RADIUS, left, right);
			}
			case DANGER -> {
				int fill = mulAlpha(NitroUiDraw.lerpColor(0xC06B2830, 0xD07A3040, hover), alpha);
				NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, RADIUS + 1, fill);
				NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, RADIUS + 1,
						mulAlpha(0x44AA5555, alpha));
			}
			case TEXT -> {
				// no box — Feather quit label
			}
			default -> {
				int fill = mulAlpha(NitroUiDraw.lerpColor(FeatherPalette.GLASS, FeatherPalette.GLASS_HOVER, hover), alpha);
				NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, RADIUS, fill);
				NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, RADIUS,
						mulAlpha(0x22FFFFFF, alpha));
			}
		}

		var tr = MinecraftClient.getInstance().textRenderer;
		String label = getMessage().getString();
		int textW = tr.getWidth(label);
		int textY = getY() + (height - 8) / 2;
		int textColor = style == Style.TEXT
				? mulAlpha(NitroUiDraw.lerpColor(FeatherPalette.RED, FeatherPalette.RED_HOVER, hover), alpha)
				: mulAlpha(0xFFFFFFFF, alpha);

		if (icon.texture != null) {
			// Icon + label centered as one group (FastClient layout)
			int contentW = ICON_SIZE + ICON_TEXT_GAP + textW;
			int startX = getX() + Math.max(6, (width - contentW) / 2);
			int iconY = getY() + (height - ICON_SIZE) / 2;
			NitroIcons.draw(context, icon.texture, startX, iconY, ICON_SIZE, alpha);
			context.drawText(tr, label, startX + ICON_SIZE + ICON_TEXT_GAP, textY, textColor, false);
		} else {
			context.drawText(tr, label, getX() + (width - textW) / 2, textY, textColor, false);
		}
	}

	private static int mulAlpha(int argb, float alpha) {
		int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0F, Math.min(1F, alpha)));
		return (a << 24) | (argb & 0x00FFFFFF);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		if (alpha < 0.85F) {
			return;
		}
		onPress.onPress(this);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}
