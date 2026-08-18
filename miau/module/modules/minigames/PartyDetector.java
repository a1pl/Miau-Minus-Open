package miau.module.modules.minigames;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.util.client.ChatUtil;
import miau.util.client.SoundUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;

public class PartyDetector extends Module {
    public final BooleanProperty sound = new BooleanProperty("Ping sound", true);
    public final BooleanProperty showMissed = new BooleanProperty("Show missed players", true);
    public final BooleanProperty twos = new BooleanProperty("Bedwars 2s", true);
    public final BooleanProperty threes = new BooleanProperty("Bedwars 3s", true);
    public final BooleanProperty foursNormal = new BooleanProperty("Bedwars 4s", true);
    public final BooleanProperty foursTwo = new BooleanProperty("Bedwars 4v4", true);
    private int playerCounter = 0;
    private long lastJoinTime = 0L;
    private boolean countingPlayers = false;
    private int missedCounter = 0;
    private boolean alertedMissed = false;
    private int tickCounter = 0;
    private boolean gameStarted = false;
    private final Set<EntityPlayer> knownPlayers = new HashSet<>();
    private final Minecraft mc = Minecraft.func_71410_x();

    public PartyDetector() {
        super("PartyDetector", false, true);
    }

    @Override
    public void onEnabled() {
        super.onEnabled();
        this.onReset();
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
    }

    private void onReset() {
        this.playerCounter = 0;
        this.lastJoinTime = 0L;
        this.countingPlayers = false;
        this.alertedMissed = false;
        this.missedCounter = 0;
        this.tickCounter = 0;
        this.gameStarted = false;
        this.knownPlayers.clear();
    }

    private int getBedwarsMode() {
        if (this.mc.field_71441_e != null && this.mc.field_71441_e.func_96441_U() != null) {
            ScoreObjective objective = this.mc.field_71441_e.func_96441_U().func_96539_a(1);
            if (objective == null) {
                return 0;
            }

            String title = EnumChatFormatting.func_110646_a(objective.func_96678_d());
            if (title != null && title.contains("BED WARS")) {
                for (ScorePlayerTeam team : this.mc.field_71441_e.func_96441_U().func_96525_g()) {
                    String prefix = team.func_96668_e() != null ? team.func_96668_e() : "";
                    String suffix = team.func_96663_f() != null ? team.func_96663_f() : "";
                    String line = EnumChatFormatting.func_110646_a(prefix + suffix).toLowerCase();
                    if (line.contains("4v4v4v4") && this.foursNormal.getValue()) {
                        return 4;
                    }

                    if (line.contains("3v3v3v3") && this.threes.getValue()) {
                        return 3;
                    }

                    if (line.contains("doubles") && this.twos.getValue()) {
                        return 2;
                    }

                    if (line.contains("4v4") && this.foursTwo.getValue()) {
                        return 4;
                    }
                }

                return 0;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.POST) {
                if (this.mc.field_71439_g != null && this.mc.field_71441_e != null) {
                    int mode = this.getBedwarsMode();
                    if (mode != 0) {
                        List<EntityPlayer> currentPlayers = this.mc.field_71441_e.field_73010_i;

                        for (EntityPlayer player : currentPlayers) {
                            if (player != this.mc.field_71439_g && !this.knownPlayers.contains(player)) {
                                this.knownPlayers.add(player);
                                long now = System.currentTimeMillis();
                                if (!this.countingPlayers) {
                                    this.lastJoinTime = now;
                                }

                                if (now - this.lastJoinTime <= 1000L) {
                                    this.playerCounter++;
                                    this.countingPlayers = true;
                                } else {
                                    this.countingPlayers = false;
                                    this.lastJoinTime = 0L;
                                    this.playerCounter = 0;
                                }
                            }
                        }

                        this.knownPlayers.retainAll(currentPlayers);
                        if (this.playerCounter != 0 && this.playerCounter >= mode) {
                            if (!this.gameStarted) {
                                ChatUtil.sendFormatted(
                                    String.format(
                                        "%s%s: &cWarning: &e%d&f players joined! &8(&9Party&8)",
                                        Miau.clientName,
                                        this.getName(),
                                        mode
                                    )
                                );
                                if (this.sound.getValue()) {
                                    SoundUtil.playSound("note.pling");
                                }
                            }

                            this.playerCounter = 0;
                            this.lastJoinTime = 0L;
                            this.countingPlayers = false;
                        }

                        if (this.showMissed.getValue() && !this.alertedMissed && !this.gameStarted) {
                            this.tickCounter++;
                            if (this.tickCounter >= 10) {
                                for (EntityPlayer player : this.mc.field_71441_e.field_73010_i) {
                                    if (player != null
                                        && player != this.mc.field_71439_g
                                        && player.func_110124_au().version() == 2) {
                                        this.missedCounter++;
                                    }
                                }

                                if (this.missedCounter != 0) {
                                    ChatUtil.sendFormatted(
                                        String.format(
                                            "%s%s: Missed players: &e%d",
                                            Miau.clientName,
                                            this.getName(),
                                            this.missedCounter
                                        )
                                    );
                                }

                                this.alertedMissed = true;
                            }
                        }
                    }
                } else {
                    this.onReset();
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof S02PacketChat) {
                S02PacketChat packet = (S02PacketChat)event.getPacket();
                String msg = packet.func_148915_c().func_150260_c();
                if (msg.contains("The game starts in 1 second!")) {
                    this.gameStarted = true;
                }
            }
        }
    }
}
