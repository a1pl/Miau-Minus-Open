package miau.module.modules.minigames;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import miau.Miau;
import miau.enums.ChatColors;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.DragProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.TextProperty;
import miau.util.client.ChatUtil;
import miau.util.client.SoundUtil;
import miau.util.player.TeamUtil;
import miau.util.render.ColorUtil;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class BedTracker extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final long BED_SCAN_DELAY_MS = 3000L;
    private final LinkedHashMap<String, Long> alertCooldowns = new LinkedHashMap<>();
    private final LinkedHashSet<EntityEnderPearl> trackedPearls = new LinkedHashSet<>();
    private final LinkedHashSet<String> whitelistedPlayers = new LinkedHashSet<>();
    private final Color wBed = new Color(ChatColors.WHITE.toAwtColor());
    private final Color rBed = new Color(ChatColors.RED.toAwtColor());
    private final Color yBed = new Color(ChatColors.YELLOW.toAwtColor());
    private final Color gBed = new Color(ChatColors.GREEN.toAwtColor());
    private BlockPos bedPos = null;
    private long lastMarcoTime = -1L;
    private boolean waiting = false;
    private long bedScanAt = -1L;
    public final BooleanProperty alerts = new BooleanProperty("alerts", true);
    public final IntProperty alertRange = new IntProperty("alerts-range", 48, 8, 128, this.alerts::getValue);
    public final BooleanProperty alertOnPearl = new BooleanProperty("alerts-on-pearl", true);
    public final ModeProperty alertSound = new ModeProperty(
        "alerts-sound",
        1,
        new String[]{"NONE", "MEOW", "ANVIL"},
        () -> this.alerts.getValue() || this.alertOnPearl.getValue()
    );
    public final IntProperty alertFrequency = new IntProperty(
        "alerts-frequency", 5, 1, 30, () -> this.alerts.getValue() || this.alertOnPearl.getValue()
    );
    public final BooleanProperty marco = new BooleanProperty("macro", false);
    public final IntProperty marcoRange = new IntProperty("macro-range", 24, 8, 128, this.marco::getValue);
    public final BooleanProperty marcoOnPreal = new BooleanProperty("macro-on-pearl", false);
    public final TextProperty marcoText = new TextProperty(
        "macro-text", "/lobby", () -> this.marco.getValue() || this.marcoOnPreal.getValue()
    );
    public final IntProperty marcoDelay = new IntProperty(
        "macro-delay", 1, 1, 10, () -> this.marco.getValue() || this.marcoOnPreal.getValue()
    );
    public final BooleanProperty hud = new BooleanProperty("hud", true);
    public final DragProperty hudDrag = new DragProperty("BedTracker HUD", new Vector2d(10.0, 50.0), true);
    public final FloatProperty hudScale = new FloatProperty("hud-scale", 1.0F, 0.5F, 1.5F, this.hud::getValue);
    public final BooleanProperty hudShadow = new BooleanProperty("hud-shadow", true, this.hud::getValue);

    private void playAlertSound() {
        switch (this.alertSound.getValue()) {
            case 1:
                SoundUtil.playSound("mob.cat.meow");
                break;
            case 2:
                SoundUtil.playSound("random.anvil_land");
        }
    }

    private Color getHudColor(int distance) {
        if (distance < 0) {
            return this.wBed;
        } else if (distance <= 100) {
            return this.gBed;
        } else if (distance <= 114) {
            return ColorUtil.interpolate((114 - distance) / 14.0F, this.yBed, this.gBed);
        } else {
            return distance <= 128 ? ColorUtil.interpolate((128 - distance) / 14.0F, this.rBed, this.yBed) : this.rBed;
        }
    }

    private boolean isBed(BlockPos blockPos) {
        return blockPos != null
            && mc.field_71441_e != null
            && mc.field_71441_e.func_180495_p(blockPos).func_177230_c() == Blocks.field_150324_C;
    }

    public BedTracker() {
        super("BedTracker", false, true);
    }

    private void resetTracking() {
        this.alertCooldowns.clear();
        this.trackedPearls.clear();
        this.whitelistedPlayers.clear();
        this.bedPos = null;
        this.lastMarcoTime = -1L;
    }

    private void scheduleBedScan() {
        this.bedScanAt = System.currentTimeMillis() + 3000L;
    }

    private void runPendingBedScan() {
        if (this.bedScanAt != -1L && System.currentTimeMillis() >= this.bedScanAt) {
            this.bedScanAt = -1L;
            if (mc.field_71441_e != null && mc.field_71439_g != null) {
                int x = MathHelper.func_76128_c(mc.field_71439_g.field_70165_t);
                int y = MathHelper.func_76128_c(mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
                int z = MathHelper.func_76128_c(mc.field_71439_g.field_70161_v);

                for (int i = x - 25; i <= x + 25; i++) {
                    for (int j = y - 25; j <= y + 25; j++) {
                        for (int k = z - 25; k <= z + 25; k++) {
                            BlockPos blockPos = new BlockPos(i, j, k);
                            if (this.isBed(blockPos)) {
                                this.bedPos = blockPos;
                                ChatUtil.sendFormatted(
                                    String.format(
                                        "%s%s: &fWhitelisted your bed at (%d, %d, %d) &a&l✔&r",
                                        Miau.clientName,
                                        this.getName(),
                                        this.bedPos.func_177958_n(),
                                        this.bedPos.func_177956_o(),
                                        this.bedPos.func_177952_p()
                                    )
                                );
                                SoundUtil.playSound("note.pling");
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void pruneTrackedPearls() {
        if (mc.field_71441_e == null) {
            this.trackedPearls.clear();
        } else {
            Iterator<EntityEnderPearl> iterator = this.trackedPearls.iterator();

            while (iterator.hasNext()) {
                EntityEnderPearl pearl = iterator.next();
                if (pearl.field_70128_L || !mc.field_71441_e.field_72996_f.contains(pearl)) {
                    iterator.remove();
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            this.runPendingBedScan();
            this.pruneTrackedPearls();
            if (!this.isBed(this.bedPos)) {
                return;
            }

            long millis = System.currentTimeMillis();
            boolean pearl = false;
            boolean marco = false;

            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityEnderPearl) {
                    EntityEnderPearl enderPearl = (EntityEnderPearl)entity;
                    if (!this.trackedPearls.contains(enderPearl)) {
                        this.trackedPearls.add(enderPearl);
                        if (this.alertOnPearl.getValue()) {
                            ChatUtil.sendFormatted(
                                String.format(
                                    "%s%s: &fDetected &5Ender Pearl&r &e&l⚠&r", Miau.clientName, this.getName()
                                )
                            );
                            pearl = true;
                        }

                        if (this.marcoOnPreal.getValue()
                            && this.lastMarcoTime + this.marcoDelay.getValue().intValue() * 1000L <= millis) {
                            this.lastMarcoTime = millis;
                            marco = true;
                        }
                    }
                }
            }

            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (!TeamUtil.isBot(player) && !this.whitelistedPlayers.contains(player.func_70005_c_())) {
                    if (TeamUtil.isSameTeam(player)) {
                        this.whitelistedPlayers.add(player.func_70005_c_());
                    } else {
                        double distance = player.func_70011_f(
                            this.bedPos.func_177958_n() + 0.5,
                            this.bedPos.func_177956_o() + 0.5,
                            this.bedPos.func_177952_p() + 0.5
                        );
                        String name = player.func_70005_c_();
                        String text = player.func_145748_c_().func_150254_d();
                        ItemStack item = player.func_70694_bm();
                        boolean isPearl = item != null && item.func_77973_b() instanceof ItemEnderPearl;
                        if (this.alerts.getValue() && distance < this.alertRange.getValue().intValue()) {
                            Long cooldown = this.alertCooldowns.get(name);
                            if (cooldown == null
                                || cooldown + this.alertFrequency.getValue().intValue() * 1000L <= millis) {
                                this.alertCooldowns.put(name, millis);
                                ChatUtil.sendFormatted(
                                    String.format(
                                        "%s%s: %s&r &fis %d blocks away from your bed &e&l⚠&r",
                                        Miau.clientName,
                                        this.getName(),
                                        text,
                                        (int)distance + 1
                                    )
                                );
                                pearl = true;
                            }
                        }

                        if (this.alertOnPearl.getValue() && isPearl) {
                            Long cooldown = this.alertCooldowns.get(name);
                            if (cooldown == null
                                || cooldown + this.alertFrequency.getValue().intValue() * 1000L <= millis) {
                                this.alertCooldowns.put(name, millis);
                                ChatUtil.sendFormatted(
                                    String.format(
                                        "%s%s: %s&r &fhas &5Ender Pearl&r &e&l⚠&r",
                                        Miau.clientName,
                                        this.getName(),
                                        text
                                    )
                                );
                                pearl = true;
                            }
                        }

                        if ((
                                this.marco.getValue() && distance < this.marcoRange.getValue().intValue()
                                    || this.marcoOnPreal.getValue() && isPearl
                            )
                            && this.lastMarcoTime + this.marcoDelay.getValue().intValue() * 1000L <= millis) {
                            this.lastMarcoTime = millis;
                            marco = true;
                        }
                    }
                }
            }

            if (pearl) {
                this.playAlertSound();
            }

            if (marco) {
                ChatUtil.sendRaw(
                    String.format(
                        ChatColors.formatColor("%s%s: &fRunning &6%s&r"),
                        ChatColors.formatColor(Miau.clientName),
                        this.getName(),
                        this.marcoText.getValue()
                    )
                );
                ChatUtil.sendMessage(this.marcoText.getValue());
            }
        }
    }

    @EventTarget(3)
    public void onRender(Render2DEvent event) {
        if (this.isEnabled()
            && this.hud.getValue()
            && mc.field_71441_e != null
            && mc.field_71439_g != null
            && !mc.field_71474_y.field_74330_P) {
            GuiScreen currentScreen = mc.field_71462_r;
            if (currentScreen == null || currentScreen instanceof GuiChat) {
                int distanceSq = 0;
                boolean hasBed = this.isBed(this.bedPos);
                if (hasBed) {
                    double xDiff = mc.field_71439_g.field_70165_t - this.bedPos.func_177958_n();
                    double zDiff = mc.field_71439_g.field_70161_v - this.bedPos.func_177952_p();
                    distanceSq = (int)Math.sqrt(xDiff * xDiff + zDiff * zDiff) + 1;
                }

                String text = ChatColors.formatColor(
                    String.format(
                        "&fBed: %s%s",
                        !hasBed ? "&cfalse&r" : "&atrue&r",
                        !hasBed
                            ? ""
                            : String.format(" &7| &fDistance: &r%d%s", distanceSq, distanceSq >= 128 ? " &c&l⚠&r" : "")
                    )
                );
                new ScaledResolution(mc);
                float width = mc.field_71466_p.func_78256_a(text);
                float height = mc.field_71466_p.field_78288_b - 1.0F;
                float x = (float)this.hudDrag.position.x;
                float y = (float)this.hudDrag.position.y;
                float sc = this.hudScale.getValue();
                GlStateManager.func_179094_E();
                GlStateManager.func_179152_a(sc, sc, 1.0F);
                GlStateManager.func_179109_b(x / sc, y / sc, 0.0F);
                GlStateManager.func_179097_i();
                GlStateManager.func_179147_l();
                GlStateManager.func_179112_b(770, 771);
                mc.field_71466_p
                    .func_175065_a(text, 0.0F, 0.0F, this.getHudColor(distanceSq).getRGB(), this.hudShadow.getValue());
                GlStateManager.func_179084_k();
                GlStateManager.func_179126_j();
                GlStateManager.func_179121_F();
                this.hudDrag.setScale(new Vector2d(width * sc, height * sc));
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.waiting = false;
        this.bedScanAt = -1L;
        this.resetTracking();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getPacket() instanceof S02PacketChat) {
                String msg = ((S02PacketChat)event.getPacket()).func_148915_c().func_150254_d();
                if (msg.contains("§e§lProtect your bed and destroy the enemy bed")
                    || msg.contains("§e§lDestroy the enemy bed and then eliminate them")) {
                    this.bedScanAt = -1L;
                    this.resetTracking();
                    this.waiting = true;
                }
            }

            if (event.getPacket() instanceof S08PacketPlayerPosLook && this.waiting) {
                this.waiting = false;
                this.scheduleBedScan();
            }
        }
    }

    @Override
    public void onDisabled() {
        this.waiting = false;
        this.bedScanAt = -1L;
        this.resetTracking();
    }
}
