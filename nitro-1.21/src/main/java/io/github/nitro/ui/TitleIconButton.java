package io.github.nitro.ui;

import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class TitleIconButton extends ClickableWidget {

	public static final int SIZE = 22;
	public static final int GAP = 4;
	private static final int ICON = 10;
	private static final int RADIUS = 4;

	private final Kind kind;
	private final String tip;
	private final PressAction onPress;
	private final boolean accent;
	private float hover;
	private float alpha = 1F;

	public TitleIconButton(int x, int y, Kind kind, String tip, boolean accent, PressAction onPress) {
		this(x, y, SIZE, kind, tip, accent, onPress);
	}

	public TitleIconButton(int x, int y, int w, Kind kind, String tip, boolean accent, PressAction onPress) {
		super(x, y, w, SIZE, Text.literal(tip));
		this.kind = kind;
		this.tip = tip;
		this.accent = accent;
		this.onPress = onPress;
	}

	public void setMenuAlpha(float alpha) {
		this.alpha = NitroEasing.clamp01(alpha);
	}

	public String tip() {
		return tip;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		hover = NitroEasing.approach(hover, hovered ? 1F : 0F, Math.max(0.016F, delta), 14F);
		if (accent) {
			NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, RADIUS, mulAlpha(FeatherPalette.RED, alpha));
		} else if (hover > 0.02F) {
			NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, RADIUS, mulAlpha(0xE01A1C24, alpha));
			NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, RADIUS,
					mulAlpha(NitroTheme.accent(), (int) (85F * hover * alpha)));
		} else {
			NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, RADIUS, mulAlpha(0xB814161C, alpha));
			NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, RADIUS, mulAlpha(0x22FFFFFF, alpha));
		}
		int chip = Math.min(width, SIZE);
		int iy = getY() + (height - ICON) / 2;
		if (width > SIZE && kind == Kind.USER) {
			NitroIcons.draw(context, NitroIcons.Id.USER, getX() + 7, iy, ICON);
		} else {
			int ix = getX() + (chip - ICON) / 2;
			if (kind == Kind.BRAND) {
				NitroLogoRenderer.drawLogo(context, ix, iy, ICON);
			} else if (kind.icon() != null) {
				NitroIcons.draw(context, kind.icon(), ix, iy, ICON);
			}
		}
	}

	public void renderTooltip(DrawContext context) {
		if (!hovered || tip == null || tip.isEmpty() || width > SIZE) {
			return;
		}
		var tr = MinecraftClient.getInstance().textRenderer;
		int tw = tr.getWidth(tip) + 10;
		int th = 14;
		int tx = getX() + (width - tw) / 2;
		int ty = getY() + height + 4;
		NitroUiDraw.fillRoundRect(context, tx, ty, tw, th, 3, 0xE014161C);
		NitroUiDraw.strokeRoundRect(context, tx, ty, tw, th, 3, 0x22FFFFFF);
		context.drawText(tr, tip, tx + 5, ty + 3, 0xFFFFFFFF, false);
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

	public enum Kind {
		USER(NitroIcons.Id.USER),
		LAYOUT(NitroIcons.Id.LAYOUT),
		CHAT(NitroIcons.Id.CHAT),
		CAMERA(NitroIcons.Id.CAMERA),
		SETTINGS(NitroIcons.Id.GEAR),
		STORE(NitroIcons.Id.DIAMOND),
		FOLDER(NitroIcons.Id.FOLDER),
		BRAND(null),
		MODS(NitroIcons.Id.MODS),
		HUD(NitroIcons.Id.MONITOR),
		SPEED(NitroIcons.Id.SPEED);

		private final NitroIcons.Id icon;

		Kind(NitroIcons.Id icon) {
			this.icon = icon;
		}

		public NitroIcons.Id icon() {
			return icon;
		}
	}

	public interface PressAction {
		void onPress(TitleIconButton button);
	}
}
