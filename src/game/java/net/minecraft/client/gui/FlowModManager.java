package net.minecraft.client.gui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.lax1dude.eaglercraft.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.MathHelper;

public class FlowModManager {

	public enum Mod {
		FPS_BOOSTER("FPS Booster", "Forces aggressive low-end settings for the best FPS.", true),
		FULLBRIGHT("Fullbright", "Pushes gamma to max for always-bright gameplay.", false),
		TOGGLE_SPRINT("Toggle Sprint", "Automatically holds sprint while moving forward.", false),
		NO_BOB("No View Bobbing", "Disables camera bob while walking.", false),
		LOW_PARTICLES("Minimal Particles", "Keeps particles on minimal for cleaner fights.", false),
		VSYNC("VSync", "Keeps vertical sync enabled.", false),
		FANCY_GRAPHICS("Fancy Graphics", "Forces fancy graphics mode.", false),
		DEBUG_F3("Debug Overlay", "Keeps F3 debug display visible.", false),
		HIDE_HUD("Hide HUD", "Toggles full HUD visibility.", false),
		THIRD_PERSON("Third Person", "Forces third-person camera mode.", false),
		SMOOTH_CAMERA("Smooth Camera", "Enables smooth camera movement.", false),
		CHUNK_UPDATES_MAX("Chunk Updates x5", "Max chunk updates for faster terrain loading.", false),
		KEYSTROKES_HUD("Keystrokes HUD", "Shows WASD + sprint indicator on screen.", false),
		COORDS_HUD("Coordinates HUD", "Shows XYZ coordinates in overlay.", false),
		CLOCK_HUD("Clock HUD", "Shows local real-time clock in overlay.", false),
		CPS_HUD("CPS HUD", "Shows left/right clicks per second.", false),
		ACTIVE_MODS_HUD("Active Mods List", "Displays currently enabled FLOW mods.", false),
		ARMOR_DURABILITY_HUD("Armor Durability HUD", "Shows armor durability percentages.", false),
		PING_HUD("Ping HUD", "Shows your current ping in milliseconds.", false),
		DIRECTION_HUD("Direction HUD", "Shows compass direction from player yaw.", false),
		POTION_HUD("Potion HUD", "Shows active potion effects and remaining duration.", false),
		ITEM_COUNTER_HUD("Item Counter HUD", "Shows held-item stack totals + arrows/pearls.", false),
		CROSSHAIR_DOT("Crosshair Dot", "Adds a tiny center crosshair dot.", false),
		DYNAMIC_CROSSHAIR("Dynamic Crosshair", "Expands crosshair while moving.", false),
		REACH_DISPLAY("Reach Display", "Shows last reach metric near HUD.", false),
		WEATHER_OFF("Disable Weather", "Suppresses rain and thunder visuals.", false),
		MINIMAL_ANIMATIONS("Minimal Animations", "Disables tooltip fade by forcing instant GUI updates.", false);

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
	private static int renderDistanceBackup = 4;
	private static int cloudsBackup = 1;
	private static int maxFpsBackup = 260;
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
	private static float lastReachDistance = 0.0F;
	private static final SimpleDateFormat CLOCK_FMT = new SimpleDateFormat("HH:mm:ss", Locale.US);
	private static long cachedHudBasicAt = 0L;
	private static long cachedHudInventoryAt = 0L;
	private static String cachedCoordsLine = null;
	private static String cachedClockLine = null;
	private static String cachedPingLine = null;
	private static String cachedFacingLine = null;
	private static String cachedCpsLine = null;
	private static String cachedReachLine = null;
	private static final List<String> cachedPotionLines = new ArrayList<>();
	private static final List<String> cachedItemCounterLines = new ArrayList<>();

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
		renderDistanceBackup = settings.renderDistanceChunks;
		cloudsBackup = settings.clouds;
		maxFpsBackup = settings.limitFramerate;
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
			gs.renderDistanceChunks = 4;
			gs.clouds = 0;
			gs.limitFramerate = 260;
		} else {
			gs.ofChunkUpdates = isEnabled(Mod.CHUNK_UPDATES_MAX) ? 5 : chunkUpdatesBackup;
			gs.fancyGraphics = isEnabled(Mod.FANCY_GRAPHICS) ? true : fancyGraphicsBackup;
			gs.particleSetting = isEnabled(Mod.LOW_PARTICLES) ? 2 : particlesBackup;
			gs.renderDistanceChunks = renderDistanceBackup;
			gs.clouds = cloudsBackup;
			gs.limitFramerate = maxFpsBackup;
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
		if (isEnabled(Mod.WEATHER_OFF) && mc.world != null) {
			mc.world.setRainStrength(0.0F);
			mc.world.setThunderStrength(0.0F);
		}
		if (mc.player != null && mc.objectMouseOver != null) {
			RayTraceResult ray = mc.objectMouseOver;
			if (ray.hitVec != null) {
				lastReachDistance = (float) ray.hitVec.distanceTo(mc.player.getPositionEyes(1.0F));
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
		boolean showAnyLeftHud = isEnabled(Mod.COORDS_HUD) || isEnabled(Mod.CLOCK_HUD) || isEnabled(Mod.PING_HUD)
				|| isEnabled(Mod.DIRECTION_HUD) || isEnabled(Mod.CPS_HUD) || isEnabled(Mod.ARMOR_DURABILITY_HUD)
				|| isEnabled(Mod.POTION_HUD) || isEnabled(Mod.ITEM_COUNTER_HUD) || isEnabled(Mod.REACH_DISPLAY);
		boolean showAnyCrosshair = isEnabled(Mod.CROSSHAIR_DOT) || isEnabled(Mod.DYNAMIC_CROSSHAIR);
		boolean showAnyRightHud = isEnabled(Mod.KEYSTROKES_HUD) || isEnabled(Mod.ACTIVE_MODS_HUD);
		if (!showAnyLeftHud && !showAnyCrosshair && !showAnyRightHud) {
			return;
		}
		updateHudCaches(mc);
		int leftX = 6;
		int y = 6;
		if (isEnabled(Mod.COORDS_HUD) && cachedCoordsLine != null) {
			mc.fontRendererObj.drawStringWithShadow(cachedCoordsLine, leftX, y, 0xFFCFE8FF);
			y += 10;
		}
		if (isEnabled(Mod.CLOCK_HUD) && cachedClockLine != null) {
			mc.fontRendererObj.drawStringWithShadow(cachedClockLine, leftX, y, 0xFF9FC2E7);
			y += 10;
		}
		if (isEnabled(Mod.PING_HUD) && cachedPingLine != null) {
			mc.fontRendererObj.drawStringWithShadow(cachedPingLine, leftX, y, 0xFF9FC2E7);
			y += 10;
		}
		if (isEnabled(Mod.DIRECTION_HUD) && cachedFacingLine != null) {
			mc.fontRendererObj.drawStringWithShadow(cachedFacingLine, leftX, y, 0xFF9FC2E7);
			y += 10;
		}
		if (isEnabled(Mod.CPS_HUD) && cachedCpsLine != null) {
			mc.fontRendererObj.drawStringWithShadow(cachedCpsLine, leftX, y, 0xFF9FC2E7);
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
			for (String txt : cachedPotionLines) {
				mc.fontRendererObj.drawStringWithShadow(txt, leftX, y, 0xFFD2E8FF);
				y += 10;
			}
		}
		if (isEnabled(Mod.ITEM_COUNTER_HUD) && mc.player != null) {
			for (String txt : cachedItemCounterLines) {
				mc.fontRendererObj.drawStringWithShadow(txt, leftX, y, 0xFF87A7C8);
				y += 10;
			}
		}
		if (isEnabled(Mod.REACH_DISPLAY) && cachedReachLine != null) {
			mc.fontRendererObj.drawStringWithShadow(cachedReachLine, leftX, y, 0xFF9FC2E7);
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
		if (isEnabled(Mod.CROSSHAIR_DOT) || isEnabled(Mod.DYNAMIC_CROSSHAIR)) {
			int centerX = sr.getScaledWidth() / 2;
			int centerY = sr.getScaledHeight() / 2;
			if (isEnabled(Mod.CROSSHAIR_DOT)) {
				gui.drawRect(centerX - 1, centerY - 1, centerX + 1, centerY + 1, 0xCCFFFFFF);
			}
			if (isEnabled(Mod.DYNAMIC_CROSSHAIR) && mc.player != null) {
				float speed = (float) Math.sqrt(mc.player.motionX * mc.player.motionX + mc.player.motionZ * mc.player.motionZ);
				int gap = 4 + MathHelper.clamp((int) (speed * 18.0F), 0, 8);
				int color = 0xCCB7D9FF;
				gui.drawRect(centerX - 1, centerY - gap - 3, centerX + 1, centerY - gap, color);
				gui.drawRect(centerX - 1, centerY + gap, centerX + 1, centerY + gap + 3, color);
				gui.drawRect(centerX - gap - 3, centerY - 1, centerX - gap, centerY + 1, color);
				gui.drawRect(centerX + gap, centerY - 1, centerX + gap + 3, centerY + 1, color);
			}
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
	}

	private static void updateHudCaches(Minecraft mc) {
		long now = Minecraft.getSystemTime();
		if (now - cachedHudBasicAt >= 200L) {
			cachedCoordsLine = null;
			cachedPingLine = null;
			cachedFacingLine = null;
			cachedClockLine = "Time: " + CLOCK_FMT.format(new Date());
			cachedCpsLine = "CPS: L " + cpsLeftDisplay + " | R " + cpsRightDisplay;
			cachedReachLine = String.format(Locale.US, "Reach: %.2fm", lastReachDistance);
			if (mc.player != null) {
				cachedCoordsLine = "XYZ: " + MathHelper.floor(mc.player.posX) + " / " + MathHelper.floor(mc.player.posY) + " / "
						+ MathHelper.floor(mc.player.posZ);
				String[] dirs = new String[] { "South", "West", "North", "East" };
				int idx = MathHelper.floor((mc.player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
				cachedFacingLine = "Facing: " + dirs[idx];
				if (mc.getConnection() != null) {
					NetworkPlayerInfo npi = mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
					if (npi != null) {
						cachedPingLine = "Ping: " + npi.getResponseTime() + "ms";
					}
				}
			}
			cachedHudBasicAt = now;
		}
		if (now - cachedHudInventoryAt >= 400L) {
			cachedPotionLines.clear();
			cachedItemCounterLines.clear();
			if (mc.player != null) {
				Collection<PotionEffect> effects = mc.player.getActivePotionEffects();
				for (PotionEffect effect : effects) {
					cachedPotionLines.add(effect.getEffectName() + " " + PotionEffect.getPotionDurationString(effect, 1.0F));
				}
				ItemStack held = mc.player.getHeldItemMainhand();
				if (!held.func_190926_b()) {
					cachedItemCounterLines.add("Held Total: " + countMatchingStacks(mc, held));
				}
				cachedItemCounterLines.add("Arrows: " + countItem(mc, Items.ARROW));
				cachedItemCounterLines.add("Pearls: " + countItem(mc, Items.ENDER_PEARL));
			}
			cachedHudInventoryAt = now;
		}
	}

	private static int countItem(Minecraft mc, net.minecraft.item.Item item) {
		int total = 0;
		for (ItemStack st : mc.player.inventory.mainInventory) {
			if (!st.func_190926_b() && st.getItem() == item) {
				total += st.func_190916_E();
			}
		}
		return total;
	}

	private static int countMatchingStacks(Minecraft mc, ItemStack held) {
		int total = 0;
		for (ItemStack st : mc.player.inventory.mainInventory) {
			if (!st.func_190926_b() && st.getItem() == held.getItem() && st.getMetadata() == held.getMetadata()) {
				total += st.func_190916_E();
			}
		}
		return total;
	}

	private static void renderKeyBox(GuiIngame gui, int x, int y, String txt, boolean pressed) {
		gui.drawRect(x, y, x + 20, y + 20, pressed ? 0xCC1A4676 : 0xAA102640);
		gui.drawRect(x, y, x + 20, y + 1, pressed ? 0xFF6DBAFF : 0xFF3C6FA5);
		Minecraft mc = Minecraft.getMinecraft();
		int w = mc.fontRendererObj.getStringWidth(txt);
		mc.fontRendererObj.drawStringWithShadow(txt, x + (20 - w) / 2, y + 6, 0xFFFFFFFF);
	}
}
