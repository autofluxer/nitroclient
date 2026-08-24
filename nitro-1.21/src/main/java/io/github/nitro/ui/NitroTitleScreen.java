package io.github.nitro.ui;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.clickgui.ClickGuiScreen;
import io.github.nitro.ui.clickgui.ClickGuiTab;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Feather-style title: blurred plate, logo+name, icon buttons, red Store, text Quit.
 */
public final class NitroTitleScreen extends Screen {

	private static final String DISCORD_URL = "https://discord.gg/nitrosmp";
	private static final String STORE_URL = "https://nitrosmp.lol";

	private float fadeIn;
	private String toastText = "";
	private int toastTicks;
	private final List<LunarMenuButton> mainButtons = new ArrayList<>();
	private final List<TitleIconButton> iconButtons = new ArrayList<>();
	private DiscordCardButton discordCard;

	public NitroTitleScreen() {
		super(Text.literal("Nitro Client"));
	}

	@Override
	protected void init() {
		NitroConfig.INSTANCE.menuTheme = "nitro";
		clearChildren();
		mainButtons.clear();
		iconButtons.clear();
		discordCard = null;

		int btnW = Math.min(200, Math.max(156, width / 5));
		int btnH = 22;
		int gap = 4;
		int logo = 28;
		int brandW = textRenderer.getWidth(NitroFonts.brand("NITRO CLIENT"));
		int headerW = logo + 10 + brandW;
		int stackH = btnH * 5 + gap * 4 + 22;
		int totalH = 36 + stackH;
		int top = Math.max(28, (height - totalH) / 2 - 8);
		int left = width / 2 - btnW / 2;
		int y = top + 40;

		addMain(left, y, btnW, btnH, Text.translatable("menu.singleplayer"), LunarMenuButton.Icon.PLAY,
				LunarMenuButton.Style.NORMAL, b -> client.setScreen(new SelectWorldScreen(this)));
		y += btnH + gap;
		addMain(left, y, btnW, btnH, Text.translatable("menu.multiplayer"), LunarMenuButton.Icon.MULTIPLAYER,
				LunarMenuButton.Style.NORMAL, b -> client.setScreen(new MultiplayerScreen(this)));
		y += btnH + gap;
		addMain(left, y, btnW, btnH, Text.literal("Cosmetics"), LunarMenuButton.Icon.COSMETICS,
				LunarMenuButton.Style.NORMAL, b -> showToast("Cosmetics coming soon"));
		y += btnH + gap;
		addMain(left, y, btnW, btnH, Text.literal("Screenshots"), LunarMenuButton.Icon.SCREENSHOTS,
				LunarMenuButton.Style.NORMAL, b -> openScreenshots());
		y += btnH + gap;
		addMain(left, y, btnW, btnH, Text.literal("STORE"), LunarMenuButton.Icon.STORE,
				LunarMenuButton.Style.FEATURED, b -> openUrl(STORE_URL));
		y += btnH + 10;
		addMain(left, y, btnW, btnH, Text.literal("QUIT GAME"), LunarMenuButton.Icon.NONE,
				LunarMenuButton.Style.TEXT, b -> client.stop());

		String name = client != null && client.getSession() != null
				? client.getSession().getUsername()
				: "Player";
		int userW = Math.max(78, textRenderer.getWidth(name) + 28);
		int topY = 10;
		int ix = width - 12;

		ix -= TitleIconButton.SIZE;
		addIcon(ix, topY, TitleIconButton.Kind.LAYOUT, "Modules", false,
				b -> client.setScreen(new ClickGuiScreen(this, ClickGuiTab.MODULES)));
		ix -= TitleIconButton.SIZE + TitleIconButton.GAP;
		addIcon(ix, topY, TitleIconButton.Kind.CHAT, "Discord", false, b -> openUrl(DISCORD_URL));
		ix -= TitleIconButton.SIZE + TitleIconButton.GAP;
		addIcon(ix, topY, TitleIconButton.Kind.SETTINGS, "Settings", false,
				b -> client.setScreen(new OptionsScreen(this, client.options)));
		ix -= TitleIconButton.SIZE + TitleIconButton.GAP;
		addIcon(ix, topY, TitleIconButton.Kind.STORE, "Store", false, b -> openUrl(STORE_URL));
		ix -= TitleIconButton.SIZE + TitleIconButton.GAP;
		addIcon(ix, topY, TitleIconButton.Kind.FOLDER, "Resource Packs", false, b -> openScreenshots());
		ix -= TitleIconButton.SIZE + TitleIconButton.GAP;
		addIcon(ix, topY, TitleIconButton.Kind.HUD, "HUD Editor", false,
				b -> client.setScreen(new ClickGuiScreen(this, ClickGuiTab.HUD)));
		ix -= userW + TitleIconButton.GAP;
		TitleIconButton user = new TitleIconButton(ix, topY, userW, TitleIconButton.Kind.USER, name, false,
				b -> client.setScreen(new NitroAccountsScreen(this)));
		user.setMenuAlpha(fadeIn);
		iconButtons.add(user);
		addDrawableChild(user);

		int railX = 10;
		int railY = height / 2 - 40;
		addIcon(railX, railY, TitleIconButton.Kind.MODS, "Mods", false,
				b -> client.setScreen(new ClickGuiScreen(this, ClickGuiTab.MODULES)));
		addIcon(railX, railY + 26, TitleIconButton.Kind.HUD, "HUD", false,
				b -> client.setScreen(new ClickGuiScreen(this, ClickGuiTab.HUD)));
		addIcon(railX, railY + 52, TitleIconButton.Kind.CHAT, "Discord", false, b -> openUrl(DISCORD_URL));

		discordCard = new DiscordCardButton(width - DiscordCardButton.WIDTH - 12,
				height - DiscordCardButton.HEIGHT - 12, b -> openUrl(DISCORD_URL));
		discordCard.setMenuAlpha(fadeIn);
		addDrawableChild(discordCard);
	}

	private void addMain(int x, int y, int w, int h, Text label, LunarMenuButton.Icon icon,
			LunarMenuButton.Style style, LunarMenuButton.PressAction action) {
		LunarMenuButton button = new LunarMenuButton(x, y, w, h, label, icon, style, action);
		button.setMenuAlpha(fadeIn);
		mainButtons.add(button);
		addDrawableChild(button);
	}

	private void addIcon(int x, int y, TitleIconButton.Kind kind, String tip, boolean accent,
			TitleIconButton.PressAction action) {
		TitleIconButton b = new TitleIconButton(x, y, kind, tip, accent, action);
		b.setMenuAlpha(fadeIn);
		iconButtons.add(b);
		addDrawableChild(b);
	}

	private void showToast(String message) {
		toastText = message == null ? "" : message;
		toastTicks = 60;
	}

	private void openScreenshots() {
		try {
			var dir = FabricLoader.getInstance().getGameDir().resolve("screenshots");
			java.nio.file.Files.createDirectories(dir);
			Util.getOperatingSystem().open(dir.toUri());
		} catch (Throwable ignored) {
			showToast("Could not open screenshots folder");
		}
	}

	private static void openUrl(String url) {
		try {
			Util.getOperatingSystem().open(java.net.URI.create(url));
		} catch (Throwable ignored) {
		}
	}

	@Override
	public void tick() {
		if (toastTicks > 0) {
			toastTicks--;
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		fadeIn = NitroEasing.approach(fadeIn, 1F, Math.max(0.016F, delta), 8F);
		for (int i = 0; i < mainButtons.size(); i++) {
			mainButtons.get(i).setMenuAlpha(NitroEasing.easeOutCubic(NitroEasing.clamp01(fadeIn * 1.2F - i * 0.04F)));
		}
		for (TitleIconButton button : iconButtons) {
			button.setMenuAlpha(fadeIn);
		}
		if (discordCard != null) {
			discordCard.setMenuAlpha(fadeIn);
		}

		VideoMenuBackground.setPlaying(false);
		NitroDraw.drawCoverBackground(context, NitroDraw.BG_MENU, width, height, 1.06F);
		context.fill(0, 0, width, height, 0x66000000);

		int a = Math.round(0xFF * fadeIn);
		int logo = 28;
		int brandW = textRenderer.getWidth(NitroFonts.brand("NITRO CLIENT"));
		int headerW = logo + 10 + brandW;
		int stackH = 22 * 5 + 4 * 4 + 22;
		int totalH = 36 + stackH;
		int top = Math.max(28, (height - totalH) / 2 - 8);
		int hx = width / 2 - headerW / 2;
		NitroLogoRenderer.drawLogo(context, hx, top, logo, fadeIn);
		context.drawText(textRenderer, NitroFonts.brand("NITRO CLIENT"), hx + logo + 10, top + 10,
				NitroUiDraw.withAlpha(0xFFFFFF, a), false);

		for (TitleIconButton b : iconButtons) {
			if (b.tip() != null && b.getWidth() > TitleIconButton.SIZE) {
				context.drawText(textRenderer, textRenderer.trimToWidth(b.tip(), b.getWidth() - 22),
						b.getX() + 20, b.getY() + (TitleIconButton.SIZE - 8) / 2,
						NitroUiDraw.withAlpha(0xFFFFFF, a), false);
			}
		}

		String ver = versionLabel();
		context.drawText(textRenderer, ver, 6, height - 12, NitroUiDraw.withAlpha(0x66FFFFFF, a), false);

		if (toastTicks > 0 && !toastText.isEmpty()) {
			int tw = textRenderer.getWidth(toastText) + 16;
			int tx = (width - tw) / 2;
			int ty = height / 2 - 80;
			NitroUiDraw.fillRoundRect(context, tx, ty, tw, 18, 4, 0xE0121212);
			context.drawText(textRenderer, toastText, tx + 8, ty + 5, 0xFFFFFFFF, false);
		}
	}

	private static String versionLabel() {
		String modVer = FabricLoader.getInstance().getModContainer("nitroclient")
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("1.21.11");
		return "Nitro Client 1.21.11 · " + modVer;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		for (TitleIconButton b : iconButtons) {
			b.renderTooltip(context);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
