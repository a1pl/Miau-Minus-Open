package miau.module.modules.misc;

import java.util.ArrayList;
import java.util.List;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.PlayerUpdateEvent;
import miau.event.impl.Render2DEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.player.Scaffold;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import miau.util.client.KeyBindUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;

public class HypixelDisabler extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final int DEFAULT_SETBACKS = 20;
    private static final long JOIN_DELAY = 200L;
    private static final long DELAY = 0L;
    private static final long CHECK_DISABLED_TIME = 4000L;
    private static final long TIMEOUT = 12000L;
    private static final double MIN_OFFSET = 0.2;
    private long joinTime;
    private long lobbyTime;
    private long finished;
    private boolean awaitJoin;
    private boolean joinTick;
    private boolean awaitSetback;
    private boolean noRotate;
    private boolean awaitJump;
    private boolean awaitGround;
    private int setbackCount;
    private int airTicks;
    private int disablerAirTicks;
    private double minSetbacks;
    private double zOffset;
    private float savedYaw;
    private float savedPitch;
    private boolean waitForJump = true;
    private boolean hideProgress;
    private String text;
    private int dispWidth;
    private int dispHeight;
    private int width;
    public final IntProperty offset = new IntProperty("Offset", 0, -10, 10);
    public final BooleanProperty hideProgressValue = new BooleanProperty("Hide progress", false);
    public final BooleanProperty zeroZeroDisabler = new BooleanProperty("00 disabler", false);

    public HypixelDisabler() {
        super("HypixelDisabler", false);
    }

    @Override
    public void onDisabled() {
        this.resetVars();
        this.waitForJump = true;
    }

    private void resetVars() {
        if (this.noRotate) {
            this.setNoRotate(true);
        }

        this.awaitJoin = this.joinTick = this.awaitSetback = this.noRotate = this.awaitJump = this.awaitGround = false;
        this.minSetbacks = this.zOffset = this.lobbyTime = this.finished = this.setbackCount = 0;
        this.hideProgress = false;
        this.text = null;
    }

    @EventTarget
    public void onPreMotion(PlayerUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            long now = System.currentTimeMillis();
            if (mc.field_71439_g.field_70122_E) {
                this.airTicks = 0;
            } else {
                this.airTicks++;
            }

            if (this.zeroZeroDisabler.getValue()) {
                Scaffold scaffold = (Scaffold)Miau.moduleManager.modules.get(Scaffold.class);
                if (scaffold != null && scaffold.isEnabled()) {
                    this.waitForJump = true;
                } else {
                    double y = mc.field_71439_g.field_70163_u - 0.5;
                    Block block = mc.field_71441_e
                        .func_180495_p(
                            new BlockPos(mc.field_71439_g.field_70165_t, Math.floor(y), mc.field_71439_g.field_70161_v)
                        )
                        .func_177230_c();
                    if (!(block instanceof BlockStairs) && !(block instanceof BlockSlab)) {
                        if (this.waitForJump && this.airTicks > 3) {
                            this.waitForJump = false;
                        }

                        if (!this.waitForJump
                            && mc.field_71439_g.field_70122_E
                            && mc.field_71439_g.field_70163_u % 1.0 == 0.0) {
                            mc.field_71439_g.field_70163_u += 1.0E-14;
                        }
                    } else {
                        this.waitForJump = true;
                    }
                }
            }

            if (!this.awaitGround && !mc.field_71439_g.field_70122_E) {
                this.disablerAirTicks++;
            } else {
                this.awaitGround = false;
                this.disablerAirTicks = 0;
            }

            if (this.awaitJoin && now >= this.joinTime + 200L) {
                ItemStack item = mc.field_71439_g.field_71071_by.func_70301_a(8);
                if (item != null && item.func_77973_b() == Items.field_151104_aV || this.isPit()) {
                    NoRotate noRotateModule = (NoRotate)Miau.moduleManager.modules.get(NoRotate.class);
                    if (noRotateModule != null && noRotateModule.isEnabled() && (this.isSkywars() || this.isPit())) {
                        noRotateModule.setEnabled(false);
                        this.noRotate = true;
                    }

                    this.awaitJoin = false;
                    this.joinTick = true;
                }
            }

            if (this.awaitSetback) {
                this.hideProgress = this.hideProgressValue.getValue()
                    || mc.field_71462_r != null && !(mc.field_71462_r instanceof GuiChat);
                this.text = "§7running disabler §b"
                    + this.round((now - this.lobbyTime) / 1000.0, 1)
                    + "s "
                    + (int)this.round(100.0 * (this.setbackCount / this.minSetbacks), 0)
                    + "%";
                ScaledResolution sr = new ScaledResolution(mc);
                this.dispWidth = sr.func_78326_a();
                this.dispHeight = sr.func_78328_b();
                this.width = mc.field_71466_p.func_78256_a(this.text) / 2 - 2;
            } else {
                this.text = null;
            }

            if (this.finished != 0L && mc.field_71439_g.field_70122_E && now - this.finished > 4000L) {
                ChatUtil.display("&7[&dR&7] &adisabler enabled");
                this.finished = 0L;
            }

            if (this.awaitJump && this.disablerAirTicks == 5) {
                KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74314_A.func_151463_i(), false);
                this.awaitJump = false;
                this.minSetbacks = 20 + this.offset.getValue();
                this.savedYaw = mc.field_71439_g.field_70177_z;
                this.lobbyTime = now;
                this.awaitSetback = true;
            }

            if (!this.joinTick) {
                if (this.awaitSetback) {
                    if (this.setbackCount >= this.minSetbacks) {
                        ChatUtil.display(
                            "&7[&dR&7] &afinished in &b"
                                + this.round((now - this.lobbyTime) / 1000.0, 1)
                                + "&as, wait a few seconds..."
                        );
                        this.resetVars();
                        this.finished = now;
                        return;
                    }

                    if (this.lobbyTime != 0L && now - this.lobbyTime > 12000L) {
                        ChatUtil.display("&7[&dR&7] &cdisabler failed");
                        this.resetVars();
                        return;
                    }

                    if (now - this.lobbyTime > 0L) {
                        mc.field_71439_g.field_70177_z = this.savedYaw;
                        mc.field_71439_g.field_70125_A = this.savedPitch;
                        mc.field_71439_g.field_70159_w = 0.0;
                        mc.field_71439_g.field_70181_x = 0.0;
                        mc.field_71439_g.field_70179_y = 0.0;
                        if (this.isSkywars()) {
                            this.zOffset = 0.13999999999999999;
                            if (mc.field_71439_g.field_70173_aa % 2 == 0) {
                                this.zOffset *= -1.0;
                            }

                            mc.field_71439_g.field_70161_v = mc.field_71439_g.field_70161_v + this.zOffset;
                        } else {
                            mc.field_71439_g.field_70161_v = mc.field_71439_g.field_70161_v + (this.zOffset += 0.2);
                        }
                    }
                }
            } else {
                this.joinTick = false;
                ChatUtil.display("&7[&dR&7] running disabler...");
                if (!mc.field_71439_g.field_70122_E && (!(mc.field_71439_g.field_70143_R < 0.3) || this.isPit())) {
                    this.minSetbacks = 20 + this.offset.getValue();
                    this.savedYaw = mc.field_71439_g.field_70177_z;
                    this.lobbyTime = now;
                    this.awaitSetback = true;
                } else {
                    this.awaitJump = true;
                    KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74314_A.func_151463_i(), true);
                }
            }
        }
    }

    @EventTarget
    public void onPacketReceived(PacketEvent event) {
        if (this.isEnabled()
            && event.getType() == EventType.RECEIVE
            && mc.field_71439_g != null
            && mc.field_71441_e != null) {
            if (this.awaitSetback && event.getPacket() instanceof S08PacketPlayerPosLook) {
                this.setbackCount++;
                this.zOffset = 0.0;
            }
        }
    }

    @EventTarget
    public void onPostPlayerInput(MoveInputEvent event) {
        if (this.isEnabled() && this.awaitSetback && mc.field_71439_g != null) {
            mc.field_71439_g.field_71158_b.field_78900_b = 0.0F;
            mc.field_71439_g.field_71158_b.field_78902_a = 0.0F;
            KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74314_A.func_151463_i(), false);
            mc.field_71439_g.field_71158_b.field_78901_c = false;
        }
    }

    @EventTarget
    public void onRenderTick(Render2DEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (this.awaitSetback) {
                if (this.hideProgress || this.text == null) {
                    return;
                }

                mc.field_71466_p
                    .func_175063_a(this.text, this.dispWidth / 2.0F - this.width, this.dispHeight / 2.0F + 13.0F, -1);
            }
        }
    }

    @EventTarget
    public void onWorldJoin(LoadWorldEvent event) {
        if (this.isEnabled()) {
            this.joinTime = System.currentTimeMillis();
            if (this.awaitSetback) {
                ChatUtil.display("&7[&dR&7] &cdisabing disabler");
                this.resetVars();
            }

            this.awaitJoin = this.awaitGround = true;
        }
    }

    private boolean isSkywars() {
        List<String> sidebar = this.getScoreboardLines();
        return sidebar != null
            && (
                sidebar.size() > 0 && this.strip(sidebar.get(0)).contains("SKYWARS")
                    || sidebar.size() > 8 && this.strip(sidebar.get(8)).contains("SkyWars")
            );
    }

    private boolean isPit() {
        List<String> sidebar = this.getScoreboardLines();
        return sidebar != null && sidebar.size() > 0 && this.strip(sidebar.get(0)).contains("THE HYPIXEL PIT");
    }

    private List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<>();
        if (mc.field_71441_e == null) {
            return lines;
        }

        Scoreboard scoreboard = mc.field_71441_e.func_96441_U();
        ScoreObjective objective = scoreboard.func_96539_a(1);
        if (objective == null) {
            return lines;
        }

        List<Score> scores = new ArrayList<>(scoreboard.func_96534_i(objective));
        if (scores.size() > 15) {
            scores = scores.subList(0, 15);
        }

        for (Score score : scores) {
            lines.add(score.func_96653_e());
        }

        return lines;
    }

    private String strip(String text) {
        return text == null ? "" : text.replaceAll("§[0-9a-fk-or]", "");
    }

    private double round(double value, int places) {
        double factor = Math.pow(10.0, places);
        return Math.round(value * factor) / factor;
    }

    private void setNoRotate(boolean enabled) {
        NoRotate noRotateModule = (NoRotate)Miau.moduleManager.modules.get(NoRotate.class);
        if (noRotateModule != null) {
            noRotateModule.setEnabled(enabled);
        }
    }
}
