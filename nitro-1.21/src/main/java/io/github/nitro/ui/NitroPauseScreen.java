package io.github.nitro.ui;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.clickgui.ClickGuiScreen;
import io.github.nitro.ui.theme.NitroTheme;
import io.github.nitro.video.NitroVideoEdition;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.text.Text;

/**
 * In-game Esc menu — glossy glass stack over a dimmed world.
 */
public final class NitroPauseScreen extends Screen {

	private final boolean showMenu;
	private int logoY;
	private int stackX;
	private int stackY;
	private int stackW;
	private int stackH;
	private int hubX;
	private int hubY;
	private int hubW;
	private int hubH;
	private float openAnim;

	public NitroPauseScreen(boolean showMenu) {
		super(showMenu ? Text.translatable("menu.game") : Text.translatable("menu.paused"));
		this.showMenu = showMenu;
	}

	public static NitroPauseScreen from(GameMenuScreen screen) {
		return new NitroPauseScreen(screen.shouldShowMenu());
	}

	@Override
	protected void init() {
		int logoSize = 58;
		logoY = Math.max(22, height / 2 - 168);

		hubH = 36;
		int modsW = 220;
		hubW = modsW;
		hubX = (width - hubW) / 2;
		hubY = logoY + logoSize + 36;

		addDrawableChild(new NitroActionButton(hubX, hubY, modsW, hubH, Text.literal("MODS"),
				NitroActionButton.Style.GLASS, b -> client.setScreen(new ClickGuiScreen(this))));

		stackW = 268;
		int btnH = 30;
		int btnGap = 7;
		boolean lan = client.isIntegratedServerRunning();
		int rows = lan ? 5 : 4;
		stackH = 18 + rows * (btnH + btnGap);
		stackX = (width - stackW) / 2;
		stackY = hubY + hubH + 16;

		int x = stackX + 16;
		int btnW = stackW - 32;
		int y = stackY + 12;

		addDrawableChild(new NitroActionButton(x, y, btnW, btnH, Text.translatable("menu.returnToGame"),
				NitroActionButton.Style.GLASS, b -> close()));
		y += btnH + btnGap;

		int halfW = (btnW - btnGap) / 2;
		addDrawableChild(new NitroActionButton(x, y, halfW, btnH, Text.translatable("gui.advancements"),
				NitroActionButton.Style.GLASS,
				b -> client.setScreen(new AdvancementsScreen(client.player.networkHandler.getAdvancementHandler(), this))));
		addDrawableChild(new NitroActionButton(x + halfW + btnGap, y, halfW, btnH, Text.translatable("gui.stats"),
				NitroActionButton.Style.GLASS,
				b -> client.setScreen(new StatsScreen(this, client.player.getStatHandler()))));
		y += btnH + btnGap;

		addDrawableChild(new NitroActionButton(x, y, btnW, btnH, Text.translatable("menu.options"),
				NitroActionButton.Style.GLASS, b -> client.setScreen(new OptionsScreen(this, client.options))));
		y += btnH + btnGap;

		if (lan) {
			addDrawableChild(new NitroActionButton(x, y, btnW, btnH, Text.translatable("menu.shareToLan"),
					NitroActionButton.Style.GLASS, b -> client.setScreen(new OpenToLanScreen(this))));
			y += btnH + btnGap;
		}

		Text quitLabel = client.isInSingleplayer()
				? Text.translatable("menu.returnToMenu")
				: Text.translatable("menu.disconnect");
		addDrawableChild(new NitroActionButton(x, y, btnW, btnH, quitLabel,
				NitroActionButton.Style.DANGER, b -> client.setScreen(new ConfirmScreen(confirmed -> {
					if (confirmed) {
						client.disconnect(new TitleScreen(), false);
					} else {
						client.setScreen(this);
					}
				}, quitLabel, Text.literal("Leave the world and return to the Nitro menu?")))));
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		boolean video = NitroVideoEdition.active();
		openAnim = NitroEasing.approach(openAnim, 1F, Math.max(0.016F, delta), video ? 4.5F : 8F);
		float ease = NitroEasing.easeOutCubic(openAnim);
		context.fill(0, 0, width, height, NitroUiDraw.withAlpha(0x04060A, (int) (0x9A + 0x28 * ease)));
		if (NitroConfig.INSTANCE.fancyMainMenu) {
			NitroUiDraw.vignette(context, width, height, 0xAA000000);
			context.fillGradient(0, 0, width, height / 3, 0x55000000, 0x00000000);
			context.fillGradient(0, height - height / 3, width, height, 0x00000000, 0xBB000000);
		}
		NitroUiDraw.softGlow(context, width / 2, height / 2, 420,
				NitroUiDraw.withAlpha(NitroTheme.accent(), (int) (0x14 * ease)));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		boolean video = NitroVideoEdition.active();
		float ease = NitroEasing.easeOutCubic(Math.max(openAnim, 0.001F));
		float stackEase = video ? NitroEasing.easeOutCubic(Math.min(1F, Math.max(0F, openAnim * 1.25F - 0.12F))) : ease;
		int logoSize = video ? Math.round(58 * (0.88F + 0.12F * ease)) : 58;
		int drawLogoY = video ? logoY - Math.round((1F - ease) * 12) : logoY;
		int plate = logoSize + 18;
		int plateX = width / 2 - plate / 2;
		int plateY = drawLogoY - 9;

		NitroUiDraw.softGlow(context, width / 2, drawLogoY + logoSize / 2, 170,
				NitroUiDraw.withAlpha(NitroTheme.accent(), (int) (0x28 + 0x22 * ease)));
		NitroUiDraw.glossyPanel(context, plateX, plateY, plate, plate, 16);
		NitroLogoRenderer.drawLogo(context, width / 2 - logoSize / 2, drawLogoY, logoSize);

		var brand = NitroFonts.brand("NITRO CLIENT");
		int brandY = plateY + plate + 8;
		int brandW = textRenderer.getWidth(brand);
		context.drawCenteredTextWithShadow(textRenderer, brand, width / 2, brandY, 0xFFFFFFFF);
		int lineW = Math.min(72, brandW);
		int lineX = width / 2 - lineW / 2;
		context.fill(lineX, brandY + 12, lineX + lineW, brandY + 13,
				NitroUiDraw.withAlpha(NitroTheme.accent(), (int) (0x70 + 0x50 * ease)));
		context.fill(lineX + 8, brandY + 13, lineX + lineW - 8, brandY + 14, 0x33FFFFFF);

		int hubDrawY = video ? hubY + Math.round((1F - stackEase) * 18) : hubY;
		NitroUiDraw.outerGlow(context, hubX, hubDrawY, hubW, hubH, 10,
				NitroUiDraw.withAlpha(NitroTheme.accent(), (int) (0x18 + 0x20 * stackEase)), 3);

		int stackDrawY = video ? stackY + Math.round((1F - stackEase) * 22) : stackY;
		NitroUiDraw.glossyPanel(context, stackX, stackDrawY, stackW, stackH, 16);
		super.render(context, mouseX, mouseY, delta);

		if (!showMenu) {
			context.drawCenteredTextWithShadow(textRenderer, Text.translatable("menu.paused"),
					width / 2, logoY - 16, NitroTheme.muted());
		}
	}

	@Override
	public boolean shouldPause() {
		return true;
	}
}
