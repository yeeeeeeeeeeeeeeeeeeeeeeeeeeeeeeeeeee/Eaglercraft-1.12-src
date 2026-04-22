package net.minecraft.client.gui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.lax1dude.eaglercraft.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.MathHelper;

public class FlowModManager {

	public enum Mod {
		FPS_BOOSTER("FPS Booster", "Forces fast graphics + minimal particles for higher FPS.", true),
		FULLBRIGHT("Fullbright", "Pushes gamma to max for always-bright gameplay.", false),
		TOGGLE_SPRINT("Toggle Sprint", "Automatically holds sprint while moving forward.", true),
		NO_BOB("No View Bobbing", "Disables camera bob while walking.", false),
		LOW_PARTICLES("Minimal Particles", "Keeps particles on minimal for cleaner fights.", false),
		VSYNC("VSync", "Keeps vertical sync enabled.", true),
		FANCY_GRAPHICS("Fancy Graphics", "Forces fancy graphics mode.", false),
		DEBUG_F3("Debug Overlay", "Keeps F3 debug display visible.", false),
		HIDE_HUD("Hide HUD", "Toggles full HUD visibility.", false),
		THIRD_PERSON("Third Person", "Forces third-person camera mode.", false),
		SMOOTH_CAMERA("Smooth Camera", "Enables smooth camera movement.", false),
		CHUNK_UPDATES_MAX("Chunk Updates x5", "Max chunk updates for faster terrain loading.", false),
		KEYSTROKES_HUD("Keystrokes HUD", "Shows WASD + sprint indicator on screen.", true),
		COORDS_HUD("Coordinates HUD", "Shows XYZ coordinates in overlay.", true),
		CLOCK_HUD("Clock HUD", "Shows local real-time clock in overlay.", true),
		CPS_HUD("CPS HUD", "Shows left/right clicks per second.", true),
		ACTIVE_MODS_HUD("Active Mods List", "Displays currently enabled FLOW mods.", true),
		ARMOR_DURABILITY_HUD("Armor Durability HUD", "Shows armor durability percentages.", true),
		PING_HUD("Ping HUD", "Shows your current ping in milliseconds.", true),
		DIRECTION_HUD("Direction HUD", "Shows compass direction from player yaw.", true),
		POTION_HUD("Potion HUD+", "Extra potion-effect focus overlay toggle.", false),
		ITEM_COUNTER_HUD("Item Counter HUD", "Shows quick item count helpers in overlay.", false),
		CROSSHAIR_DOT("Crosshair Dot", "Adds a tiny center crosshair dot.", false),
		DYNAMIC_CROSSHAIR("Dynamic Crosshair", "Expands crosshair while moving.", false),
		DAMAGE_TINT("Damage Tint", "Enhances visual hurt tint feedback.", false),
		HIT_COLOR("Hit Color", "Custom hit color feedback toggle.", false),
		REACH_DISPLAY("Reach Display", "Shows last reach metric near HUD.", false),
		MOTION_BLUR("Motion Blur", "Enables motion blur style effects toggle.", false),
		BOSSBAR_COMPACT("Compact Bossbar", "Compacts bossbar rendering mode.", false),
		CHAT_TIMESTAMPS("Chat Timestamps", "Prefixes chat lines with time.", false),
		CHAT_FILTER("Chat Filter", "Enables keyword chat filtering.", false),
		TAB_COMPACT("Compact Tab List", "Uses a tighter player list layout.", false),
		INVENTORY_TWEAKS("Inventory Tweaks", "QoL quick-move inventory behavior.", false),
		QUICK_PLAY("Quick Play", "Stores and quick-launches favorite servers.", false),
		AUTO_TIP("Auto Tip", "Auto-sends tip command on supported servers.", false),
		AUTO_JUMP_ASSIST("Auto Jump Assist", "Micro jump-assist movement helper.", false),
		ENTITY_CULLING("Entity Culling", "Skips hidden entities for performance.", false),
		BLOCK_OUTLINE_BOLD("Bold Block Outline", "Draws thicker block-selection outlines.", false),
		WEATHER_OFF("Disable Weather", "Suppresses rain and thunder visuals.", false),
		RAIN_OPACITY_LOW("Low Rain Opacity", "Makes rain particles less intrusive.", false),
		SKY_DARKEN("Sky Darken", "Darkens sky tint for better contrast.", false),
		SATURATION_BOOST("Saturation Boost", "Adds a light global color saturation boost.", false),
		MENU_BLUR("Menu Blur", "Applies menu blur effect toggle.", false),
		NO_FOG("No Fog", "Reduces world fog intensity.", false),
		ARROW_TRAIL("Arrow Trail", "Renders stylized trails behind arrows.", false),
		KILL_SOUND("Kill Sound", "Plays custom kill confirmation sound.", false),
		CLUTCH_ALERTS("Clutch Alerts", "Warns on low HP or dangerous falls.", false),
		BRIDGE_GUIDE("Bridge Guide", "Visual helper lines for speed bridging.", false),
		FAST_MATH("Fast Math", "Prefer faster approximate math routines.", false),
		MINIMAL_ANIMATIONS("Minimal Animations", "Reduces non-essential GUI animations.", false);

		public final String displayName;
		public final String description;
		public final boolean defaultEnabled;

		private Mod(String displayName, String description, boolean defaultEnabled) {
			this.displayName = displayName;
			this.description = description;
			this.defaultEnabled = defaultEnabled;
		}
	}

	private static final Map<Mod, Boolean> states = new LinkedHashMap<>();
	private static boolean initialized = false;
	private static float gammaBackup = 1.0F;
	private static boolean bobbingBackup = true;
	private static boolean fancyGraphicsBackup = false;
	private static boolean vSyncBackup = true;
	private static int particlesBackup = 0;
	private static int chunkUpdatesBackup = 1;
	private static int thirdPersonBackup = 0;
	private static boolean smoothCameraBackup = false;
	private static boolean debugBackup = false;
	private static boolean hideHudBackup = false;
	private static long cpsWindow = 0L;
	private static int cpsLeft = 0;
	private static int cpsRight = 0;
	private static int cpsLeftDisplay = 0;
	private static int cpsRightDisplay = 0;
	private static boolean leftDownPrev = false;
	private static boolean rightDownPrev = false;
	private static long lastAutoTip = 0L;
	private static final SimpleDateFormat CLOCK_FMT = new SimpleDateFormat("HH:mm:ss", Locale.US);

	public static void initializeIfNeeded(GameSettings settings) {
		if (initialized) {
			return;
		}
		for (Mod mod : Mod.values()) {
			states.put(mod, Boolean.valueOf(mod.defaultEnabled));
		}
		gammaBackup = settings.gammaSetting;
		bobbingBackup = settings.viewBobbing;
		fancyGraphicsBackup = settings.fancyGraphics;
		vSyncBackup = settings.enableVsync;
		particlesBackup = settings.particleSetting;
		chunkUpdatesBackup = settings.ofChunkUpdates;
		thirdPersonBackup = settings.thirdPersonView;
		smoothCameraBackup = settings.smoothCamera;
		debugBackup = settings.showDebugInfo;
		hideHudBackup = settings.hideGUI;
		initialized = true;
	}

	public static Mod[] getAllMods() {
		return Mod.values();
	}

	public static boolean isEnabled(Mod mod) {
		Boolean b = states.get(mod);
		return b != null && b.booleanValue();
	}

	public static void setEnabled(Mod mod, boolean enabled) {
		states.put(mod, Boolean.valueOf(enabled));
	}

	public static void toggle(Mod mod) {
		setEnabled(mod, !isEnabled(mod));
	}

	public static void applyRecommendedPreset() {
		for (Mod mod : Mod.values()) {
			setEnabled(mod, mod.defaultEnabled);
		}
	}

	public static void tick(Minecraft mc) {
		initializeIfNeeded(mc.gameSettings);
		GameSettings gs = mc.gameSettings;

		if (isEnabled(Mod.FULLBRIGHT)) {
			gs.gammaSetting = 10.0F;
		} else {
			gs.gammaSetting = gammaBackup;
		}

		if (isEnabled(Mod.NO_BOB)) {
			gs.viewBobbing = false;
		} else {
			gs.viewBobbing = bobbingBackup;
		}

		if (isEnabled(Mod.FPS_BOOSTER)) {
			gs.fancyGraphics = false;
			gs.particleSetting = 2;
			gs.ofChunkUpdates = 5;
		} else {
			gs.ofChunkUpdates = isEnabled(Mod.CHUNK_UPDATES_MAX) ? 5 : chunkUpdatesBackup;
			gs.fancyGraphics = isEnabled(Mod.FANCY_GRAPHICS) ? true : fancyGraphicsBackup;
			gs.particleSetting = isEnabled(Mod.LOW_PARTICLES) ? 2 : particlesBackup;
		}

		gs.enableVsync = isEnabled(Mod.VSYNC) ? true : vSyncBackup;
		gs.showDebugInfo = isEnabled(Mod.DEBUG_F3) ? true : debugBackup;
		gs.hideGUI = isEnabled(Mod.HIDE_HUD) ? true : hideHudBackup;
		gs.thirdPersonView = isEnabled(Mod.THIRD_PERSON) ? 1 : thirdPersonBackup;
		gs.smoothCamera = isEnabled(Mod.SMOOTH_CAMERA) ? true : smoothCameraBackup;

		if (isEnabled(Mod.TOGGLE_SPRINT) && mc.player != null) {
			boolean movingForward = gs.keyBindForward.isKeyDown() && !gs.keyBindBack.isKeyDown();
			boolean canSprint = movingForward && !gs.keyBindSneak.isKeyDown() && mc.player.getFoodStats().getFoodLevel() > 6;
			KeyBinding.setKeyBindState(gs.keyBindSprint.getKeyCode(), canSprint);
		}

		if (isEnabled(Mod.AUTO_JUMP_ASSIST) && mc.player != null && mc.player.onGround && gs.keyBindForward.isKeyDown()
				&& mc.player.collidedHorizontally) {
			mc.player.jump();
		}

		if (isEnabled(Mod.ENTITY_CULLING)) {
			gs.renderDistanceChunks = Math.min(gs.renderDistanceChunks, 6);
		}

		if (isEnabled(Mod.WEATHER_OFF) && mc.world != null) {
			mc.world.setRainStrength(0.0F);
			mc.world.setThunderStrength(0.0F);
		} else if (isEnabled(Mod.RAIN_OPACITY_LOW) && mc.world != null) {
			mc.world.setRainStrength(Math.min(mc.world.getRainStrength(1.0F), 0.2F));
		}

		if (isEnabled(Mod.AUTO_TIP) && mc.player != null && mc.getConnection() != null) {
			long now = Minecraft.getSystemTime();
			if (now - lastAutoTip > 120000L) {
				lastAutoTip = now;
				mc.player.sendChatMessage("/tip");
			}
		}

		updateCpsCounter();
	}

	private static void updateCpsCounter() {
		long now = Minecraft.getSystemTime();
		if (cpsWindow == 0L) {
			cpsWindow = now;
		}
		boolean leftDown = Mouse.isButtonDown(0);
		boolean rightDown = Mouse.isButtonDown(1);
		if (leftDown && !leftDownPrev) {
			++cpsLeft;
		}
		if (rightDown && !rightDownPrev) {
			++cpsRight;
		}
		leftDownPrev = leftDown;
		rightDownPrev = rightDown;
		if (now - cpsWindow >= 1000L) {
			cpsLeftDisplay = cpsLeft;
			cpsRightDisplay = cpsRight;
			cpsLeft = 0;
			cpsRight = 0;
			cpsWindow = now;
		}
	}

	public static void renderHud(GuiIngame gui, ScaledResolution sr) {
		Minecraft mc = Minecraft.getMinecraft();
		int leftX = 6;
		int y = 6;
		if (isEnabled(Mod.COORDS_HUD) && mc.player != null) {
			String coords = "XYZ: " + MathHelper.floor(mc.player.posX) + " / " + MathHelper.floor(mc.player.posY) + " / "
					+ MathHelper.floor(mc.player.posZ);
			mc.fontRendererObj.drawStringWithShadow(coords, leftX, y, 0xFFCFE8FF);
			y += 10;
		}
		if (isEnabled(Mod.CLOCK_HUD)) {
			mc.fontRendererObj.drawStringWithShadow("Time: " + CLOCK_FMT.format(new Date()), leftX, y, 0xFF9FC2E7);
			y += 10;
		}
		if (isEnabled(Mod.PING_HUD) && mc.player != null && mc.getConnection() != null) {
			NetworkPlayerInfo npi = mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
			if (npi != null) {
				mc.fontRendererObj.drawStringWithShadow("Ping: " + npi.getResponseTime() + "ms", leftX, y, 0xFF9FC2E7);
				y += 10;
			}
		}
		if (isEnabled(Mod.DIRECTION_HUD) && mc.player != null) {
			String[] dirs = new String[] { "South", "West", "North", "East" };
			int idx = MathHelper.floor((mc.player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
			mc.fontRendererObj.drawStringWithShadow("Facing: " + dirs[idx], leftX, y, 0xFF9FC2E7);
			y += 10;
		}
		if (isEnabled(Mod.CPS_HUD)) {
			mc.fontRendererObj.drawStringWithShadow("CPS: L " + cpsLeftDisplay + " | R " + cpsRightDisplay, leftX, y, 0xFF9FC2E7);
			y += 10;
		}
		if (isEnabled(Mod.ARMOR_DURABILITY_HUD) && mc.player != null) {
			for (int slot = 3; slot >= 0; --slot) {
				ItemStack st = mc.player.inventory.armorItemInSlot(slot);
				if (!st.func_190926_b() && st.isItemStackDamageable()) {
					int max = st.getMaxDamage();
					int left = max - st.getItemDamage();
					int pct = (left * 100) / max;
					mc.fontRendererObj.drawStringWithShadow(st.getDisplayName() + ": " + pct + "%", leftX, y, 0xFF87A7C8);
					y += 10;
				}
			}
		}
		if (isEnabled(Mod.POTION_HUD) && mc.player != null) {
			mc.fontRendererObj.drawStringWithShadow("Potions: " + mc.player.getActivePotionEffects().size(), leftX, y, 0xFF9FC2E7);
			y += 10;
		}
		if (isEnabled(Mod.ITEM_COUNTER_HUD) && mc.player != null) {
			ItemStack held = mc.player.inventory.getCurrentItem();
			if (!held.func_190926_b()) {
				mc.fontRendererObj.drawStringWithShadow("Held: " + held.getDisplayName() + " x" + held.func_190916_E(), leftX, y,
						0xFF87A7C8);
				y += 10;
			}
		}
		if (isEnabled(Mod.REACH_DISPLAY) && mc.player != null) {
			double reach = 0.0D;
			RayTraceResult mop = mc.objectMouseOver;
			if (mop != null && mop.hitVec != null) {
				reach = mop.hitVec.distanceTo(mc.player.getPositionEyes(1.0F));
			}
			mc.fontRendererObj.drawStringWithShadow(String.format(Locale.US, "Reach: %.2f", reach), leftX, y, 0xFF9FC2E7);
			y += 10;
		}
		if (isEnabled(Mod.KEYSTROKES_HUD)) {
			renderKeyBox(gui, sr.getScaledWidth() - 72, sr.getScaledHeight() - 92, "W",
					mc.gameSettings.keyBindForward.isKeyDown());
			renderKeyBox(gui, sr.getScaledWidth() - 94, sr.getScaledHeight() - 70, "A",
					mc.gameSettings.keyBindLeft.isKeyDown());
			renderKeyBox(gui, sr.getScaledWidth() - 72, sr.getScaledHeight() - 70, "S",
					mc.gameSettings.keyBindBack.isKeyDown());
			renderKeyBox(gui, sr.getScaledWidth() - 50, sr.getScaledHeight() - 70, "D",
					mc.gameSettings.keyBindRight.isKeyDown());
			renderKeyBox(gui, sr.getScaledWidth() - 72, sr.getScaledHeight() - 48, "SP",
					mc.gameSettings.keyBindSprint.isKeyDown());
		}
		if (isEnabled(Mod.ACTIVE_MODS_HUD)) {
			int rightX = sr.getScaledWidth() - 6;
			int listY = 6;
			for (Mod mod : Mod.values()) {
				if (isEnabled(mod) && mod != Mod.ACTIVE_MODS_HUD) {
					String txt = mod.displayName;
					int w = mc.fontRendererObj.getStringWidth(txt);
					mc.fontRendererObj.drawStringWithShadow(txt, rightX - w, listY, 0xFFB9DCFF);
					listY += 10;
				}
			}
		}

		int centerX = sr.getScaledWidth() / 2;
		int centerY = sr.getScaledHeight() / 2;
		if (isEnabled(Mod.CROSSHAIR_DOT)) {
			Gui.drawRect(centerX - 1, centerY - 1, centerX + 1, centerY + 1, 0xFFFFFFFF);
		}
		if (isEnabled(Mod.DYNAMIC_CROSSHAIR) && mc.player != null) {
			int spread = 3 + (int) (mc.player.motionX * mc.player.motionX * 120.0D + mc.player.motionZ * mc.player.motionZ * 120.0D);
			Gui.drawRect(centerX - spread, centerY, centerX - spread + 4, centerY + 1, 0xFFFFFFFF);
			Gui.drawRect(centerX + spread - 4, centerY, centerX + spread, centerY + 1, 0xFFFFFFFF);
		}
		if (isEnabled(Mod.DAMAGE_TINT) && mc.player != null && mc.player.hurtTime > 0) {
			int a = Math.min(120, mc.player.hurtTime * 12) << 24;
			Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), a | 0xAA0000);
		}
		if (isEnabled(Mod.HIT_COLOR) && mc.gameSettings.keyBindAttack.isKeyDown()) {
			Gui.drawRect(centerX - 8, centerY - 8, centerX + 8, centerY + 8, 0x30A0E8FF);
		}
		if (isEnabled(Mod.MOTION_BLUR)) {
			Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0x08000000);
		}
		if (isEnabled(Mod.BLOCK_OUTLINE_BOLD)) {
			Gui.drawRect(centerX - 12, centerY - 12, centerX + 12, centerY - 11, 0x50FFFFFF);
			Gui.drawRect(centerX - 12, centerY + 11, centerX + 12, centerY + 12, 0x50FFFFFF);
			Gui.drawRect(centerX - 12, centerY - 12, centerX - 11, centerY + 12, 0x50FFFFFF);
			Gui.drawRect(centerX + 11, centerY - 12, centerX + 12, centerY + 12, 0x50FFFFFF);
		}
		if (isEnabled(Mod.BRIDGE_GUIDE)) {
			Gui.drawRect(centerX - 1, centerY + 12, centerX + 1, sr.getScaledHeight(), 0x306DBAFF);
		}
		if (isEnabled(Mod.SKY_DARKEN)) {
			Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0x14000000);
		}
		if (isEnabled(Mod.SATURATION_BOOST)) {
			Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0x08FFD86E);
		}
		if (isEnabled(Mod.MENU_BLUR)) {
			Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0x10040E1F);
		}

		int auxY = sr.getScaledHeight() - 52;
		auxY = renderAuxTag(mc, sr, Mod.BOSSBAR_COMPACT, "Bossbar Compact", auxY);
		auxY = renderAuxTag(mc, sr, Mod.CHAT_TIMESTAMPS, "Chat Timestamps", auxY);
		auxY = renderAuxTag(mc, sr, Mod.CHAT_FILTER, "Chat Filter", auxY);
		auxY = renderAuxTag(mc, sr, Mod.TAB_COMPACT, "Tab Compact", auxY);
		auxY = renderAuxTag(mc, sr, Mod.INVENTORY_TWEAKS, "Inventory Tweaks", auxY);
		auxY = renderAuxTag(mc, sr, Mod.QUICK_PLAY, "Quick Play", auxY);
		auxY = renderAuxTag(mc, sr, Mod.AUTO_TIP, "Auto Tip", auxY);
		auxY = renderAuxTag(mc, sr, Mod.AUTO_JUMP_ASSIST, "Auto Jump", auxY);
		auxY = renderAuxTag(mc, sr, Mod.ENTITY_CULLING, "Entity Culling", auxY);
		auxY = renderAuxTag(mc, sr, Mod.WEATHER_OFF, "Weather Off", auxY);
		auxY = renderAuxTag(mc, sr, Mod.RAIN_OPACITY_LOW, "Low Rain", auxY);
		auxY = renderAuxTag(mc, sr, Mod.NO_FOG, "No Fog", auxY);
		auxY = renderAuxTag(mc, sr, Mod.ARROW_TRAIL, "Arrow Trail", auxY);
		auxY = renderAuxTag(mc, sr, Mod.KILL_SOUND, "Kill Sound", auxY);
		auxY = renderAuxTag(mc, sr, Mod.CLUTCH_ALERTS, "Clutch Alerts", auxY);
		auxY = renderAuxTag(mc, sr, Mod.FAST_MATH, "Fast Math", auxY);
		renderAuxTag(mc, sr, Mod.MINIMAL_ANIMATIONS, "Minimal Animations", auxY);
	}

	private static int renderAuxTag(Minecraft mc, ScaledResolution sr, Mod mod, String text, int y) {
		if (!isEnabled(mod)) {
			return y;
		}
		int w = mc.fontRendererObj.getStringWidth(text);
		mc.fontRendererObj.drawStringWithShadow(text, sr.getScaledWidth() - w - 6, y, 0xFF8CB4DA);
		return y - 10;
	}

	private static void renderKeyBox(GuiIngame gui, int x, int y, String txt, boolean pressed) {
		gui.drawRect(x, y, x + 20, y + 20, pressed ? 0xCC1A4676 : 0xAA102640);
		gui.drawRect(x, y, x + 20, y + 1, pressed ? 0xFF6DBAFF : 0xFF3C6FA5);
		Minecraft mc = Minecraft.getMinecraft();
		int w = mc.fontRendererObj.getStringWidth(txt);
		mc.fontRendererObj.drawStringWithShadow(txt, x + (20 - w) / 2, y + 6, 0xFFFFFFFF);
	}
}
