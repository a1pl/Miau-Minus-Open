package miau.module.modules.misc;

import miau.enums.ChatColors;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.event.HoverEvent;
import net.minecraft.event.HoverEvent.Action;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.network.play.client.C15PacketClientSettings;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class ViewPackets extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty showKillAuraRange = new BooleanProperty("Show Range Info", true);
    public final BooleanProperty showTargetHealth = new BooleanProperty("Show Target Health", true);
    public final BooleanProperty includeCancelled = new BooleanProperty("Include cancelled", false);
    public final BooleanProperty singlePlayer = new BooleanProperty("Singleplayer", false);
    public final BooleanProperty sent = new BooleanProperty("Sent", false);
    public final BooleanProperty ignoreC00 = new BooleanProperty("Ignore C00", true);
    public final BooleanProperty ignoreC03 = new BooleanProperty("Ignore C03", true);
    public final BooleanProperty compactC03 = new BooleanProperty("Compact C03", true);
    public final BooleanProperty ignoreC0F = new BooleanProperty("Ignore C0F", true);
    public final BooleanProperty received = new BooleanProperty("Received", false);
    private Packet packet;
    public static long tick;

    public ViewPackets() {
        super("ViewPackets", false);
    }

    @Override
    public void onDisabled() {
        this.packet = null;
        tick = 0L;
    }

    private static String formatBoolean(boolean b) {
        return b ? "&atrue" : "&cfalse";
    }

    private void sendMessage(Packet packet, boolean received) {
        if (mc.field_71439_g != null) {
            String s = received ? "&a" + packet.getClass().getSimpleName() : this.applyInfo(packet);
            String string = (this.compactC03.getValue() && packet instanceof C03PacketPlayer ? "&6" : "&d")
                + packet.getClass().getSimpleName();
            ChatComponentText chatComponentText = new ChatComponentText(
                ChatColors.formatColor(
                    "&7[&dR&7]&r &7" + (received ? "Received" : "Sent") + " packet (t:&b" + tick + "&7): "
                )
            );
            ChatStyle chatStyle = new ChatStyle();
            IChatComponent hoverText = new ChatComponentText(ChatColors.formatColor(s));
            chatStyle.func_150209_a(new HoverEvent(Action.SHOW_TEXT, hoverText));
            chatComponentText.func_150257_a(
                new ChatComponentText(ChatColors.formatColor(string)).func_150255_a(chatStyle)
            );
            mc.field_71439_g.func_145747_a(chatComponentText);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            if (e.getType() == EventType.SEND && e.getPacket() instanceof C02PacketUseEntity) {
                C02PacketUseEntity useEntity = (C02PacketUseEntity)e.getPacket();
                if (useEntity.func_149565_c() == net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK) {
                    Entity target = useEntity.func_149564_a(mc.field_71441_e);
                    if (target != null) {
                        if (this.showKillAuraRange.getValue()) {
                            Vec3 eyePos = new Vec3(
                                mc.field_71439_g.field_70165_t,
                                mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
                                mc.field_71439_g.field_70161_v
                            );
                            Vec3 targetEyePos = new Vec3(
                                target.field_70165_t,
                                target.field_70163_u + target.func_70047_e(),
                                target.field_70161_v
                            );
                            double distanceRange = eyePos.func_72438_d(targetEyePos);
                            AxisAlignedBB bb = target.func_174813_aQ();
                            Vec3 closestPoint = new Vec3(
                                MathHelper.func_151237_a(eyePos.field_72450_a, bb.field_72340_a, bb.field_72336_d),
                                MathHelper.func_151237_a(eyePos.field_72448_b, bb.field_72338_b, bb.field_72337_e),
                                MathHelper.func_151237_a(eyePos.field_72449_c, bb.field_72339_c, bb.field_72334_f)
                            );
                            double realRange = eyePos.func_72438_d(closestPoint);
                            String rangeMsg = ChatColors.formatColor(
                                "&7[&dMiau&7] &e==> &7Distance Range &b"
                                    + MathUtil.round(distanceRange, 7)
                                    + " &7| Real Range &a"
                                    + MathUtil.round(realRange, 7)
                            );
                            mc.field_71439_g.func_145747_a(new ChatComponentText(rangeMsg));
                        }

                        if (this.showTargetHealth.getValue() && target instanceof EntityLivingBase) {
                            EntityLivingBase livingTarget = (EntityLivingBase)target;
                            float hp = livingTarget.func_110143_aJ();
                            float absHp = livingTarget.func_110139_bj();
                            String healthFormatted = MathUtil.round(hp, 1)
                                + (absHp > 0.0F ? " &e(+" + MathUtil.round(absHp, 1) + " Abs)" : "");
                            String healthMsg = ChatColors.formatColor(
                                "&7[&dMiau&7] &e==> &fPlayer: &c"
                                    + target.func_70005_c_()
                                    + " &7- Máu: &a"
                                    + healthFormatted
                                    + " HP"
                            );
                            mc.field_71439_g.func_145747_a(new ChatComponentText(healthMsg));
                        }
                    }
                }
            }

            if (e.getType() == EventType.SEND) {
                if (!this.sent.getValue()) {
                    return;
                }

                if (this.singlePlayer.getValue()
                    && mc.func_71356_B()
                    && e.getPacket().getClass().getSimpleName().charAt(0) == 'S') {
                    return;
                }

                if (e.isCancelled() && !this.includeCancelled.getValue()) {
                    return;
                }

                if (this.ignoreC00.getValue() && e.getPacket() instanceof C00PacketKeepAlive) {
                    return;
                }

                if (this.ignoreC0F.getValue() && e.getPacket() instanceof C0FPacketConfirmTransaction) {
                    return;
                }

                if (e.getPacket() instanceof C03PacketPlayer
                    && (
                        this.ignoreC03.getValue()
                            || this.compactC03.getValue()
                                && (this.packet == null || this.packet instanceof C03PacketPlayer)
                    )) {
                    return;
                }

                this.sendMessage(this.packet = e.getPacket(), false);
            } else if (e.getType() == EventType.RECEIVE) {
                if (!this.received.getValue()) {
                    return;
                }

                if (this.singlePlayer.getValue()
                    && mc.func_71356_B()
                    && e.getPacket().getClass().getSimpleName().charAt(0) == 'C') {
                    return;
                }

                this.sendMessage(e.getPacket(), true);
            }
        }
    }

    private String applyInfo(Packet packet) {
        String s = "&a" + packet.getClass().getSimpleName();
        if (packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging c07PacketPlayerDigging = (C07PacketPlayerDigging)packet;
            String string = s
                + "\n&7Status: &b"
                + c07PacketPlayerDigging.func_180762_c().name()
                + "\n&7Facing: &b"
                + c07PacketPlayerDigging.func_179714_b().name();
            BlockPos getPosition = c07PacketPlayerDigging.func_179715_a();
            s = string
                + "\n&7Position: &b"
                + getPosition.func_177958_n()
                + "&7, &b"
                + getPosition.func_177956_o()
                + "&7, &b"
                + getPosition.func_177952_p();
        } else if (packet instanceof C09PacketHeldItemChange) {
            s = s + "\n&7Swap to slot: &b" + ((C09PacketHeldItemChange)packet).func_149614_c();
        } else if (packet instanceof C0BPacketEntityAction) {
            s = s
                + "\n&7Action: &b"
                + ((C0BPacketEntityAction)packet).func_180764_b().name()
                + "\n&7Aux data: &b"
                + ((C0BPacketEntityAction)packet).func_149512_e();
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement c08PacketPlayerBlockPlacement = (C08PacketPlayerBlockPlacement)packet;
            String string2 = s
                + "\n&7Item: &b"
                + (
                    c08PacketPlayerBlockPlacement.func_149574_g() == null
                        ? "null"
                        : c08PacketPlayerBlockPlacement.func_149574_g()
                            .func_77973_b()
                            .getRegistryName()
                            .replace("minecraft:", "")
                )
                + "\n&7Direction: &b"
                + c08PacketPlayerBlockPlacement.func_149568_f();
            BlockPos getPosition = c08PacketPlayerBlockPlacement.func_179724_a();
            s = string2
                + "\n&7Position: &b"
                + getPosition.func_177958_n()
                + "&7, &b"
                + getPosition.func_177956_o()
                + "&7, &b"
                + getPosition.func_177952_p()
                + "\n&7Offset: &b"
                + MathUtil.round(c08PacketPlayerBlockPlacement.func_149573_h(), 3)
                + "&7, &b"
                + MathUtil.round(c08PacketPlayerBlockPlacement.func_149569_i(), 3)
                + "&7, &b"
                + MathUtil.round(c08PacketPlayerBlockPlacement.func_149575_j(), 3);
        } else if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity c02PacketUseEntity = (C02PacketUseEntity)packet;
            String string3 = s + "\n&7Action: &b" + c02PacketUseEntity.func_149565_c().name();
            Entity getEntityFromWorld = c02PacketUseEntity.func_149564_a(mc.field_71441_e);
            String string4 = string3
                + "\n&7Target: &b"
                + (getEntityFromWorld == null ? "null" : getEntityFromWorld.func_70005_c_());
            Vec3 getHitVec = c02PacketUseEntity.func_179712_b();
            if (getHitVec == null) {
                s = string4 + "\n&7Hit vec: &bnull";
            } else {
                s = string4
                    + "\n&7Hit vec: &b"
                    + MathUtil.round(getHitVec.field_72450_a, 3)
                    + "&7, &b"
                    + MathUtil.round(getHitVec.field_72448_b, 3)
                    + "&7, &b"
                    + MathUtil.round(getHitVec.field_72449_c, 3);
            }
        } else if (packet instanceof C01PacketChatMessage) {
            s = s + "\n&7Length: &b" + ((C01PacketChatMessage)packet).func_149439_c().length();
        } else if (packet instanceof C17PacketCustomPayload) {
            s = s + "\n&7Channel: &b" + ((C17PacketCustomPayload)packet).func_149559_c();
        } else if (packet instanceof C15PacketClientSettings) {
            s = s
                + "\n&7Language: &b"
                + ((C15PacketClientSettings)packet).func_149524_c()
                + "\n&7Chat visibility: &b"
                + ((C15PacketClientSettings)packet).func_149523_e().name();
        } else if (packet instanceof C00PacketKeepAlive) {
            s = s + "\n&7Key: &b" + ((C00PacketKeepAlive)packet).func_149460_c();
        } else if (packet instanceof C16PacketClientStatus) {
            s = s + "\n&7Status: &b" + ((C16PacketClientStatus)packet).func_149435_c().name();
        } else if (packet instanceof C10PacketCreativeInventoryAction) {
            s = s
                + "\n&7Slot: &b"
                + ((C10PacketCreativeInventoryAction)packet).func_149627_c()
                + "\n&7Item: &b"
                + (
                    ((C10PacketCreativeInventoryAction)packet).func_149625_d() == null
                        ? "null"
                        : ((C10PacketCreativeInventoryAction)packet)
                            .func_149625_d()
                            .func_77973_b()
                            .getRegistryName()
                            .replace("minecraft:", "")
                );
        } else if (packet instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow c0EPacketClickWindow = (C0EPacketClickWindow)packet;
            s = s
                + "\n&7Window: &b"
                + c0EPacketClickWindow.func_149548_c()
                + "\n&7Slot: &b"
                + c0EPacketClickWindow.func_149544_d()
                + "\n&7Button: &b"
                + c0EPacketClickWindow.func_149543_e()
                + "\n&7Action: &b"
                + c0EPacketClickWindow.func_149547_f()
                + "\n&7Mode: &b"
                + c0EPacketClickWindow.func_149542_h()
                + "\n&7Item: &b"
                + (
                    c0EPacketClickWindow.func_149546_g() == null
                        ? "null"
                        : c0EPacketClickWindow.func_149546_g()
                            .func_77973_b()
                            .getRegistryName()
                            .replace("minecraft:", "")
                );
        } else if (packet instanceof C0FPacketConfirmTransaction) {
            s = s
                + "\n&7Window: &b"
                + ((C0FPacketConfirmTransaction)packet).func_149532_c()
                + "\n&7Uid: &b"
                + ((C0FPacketConfirmTransaction)packet).func_149533_d();
        } else if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer c03PacketPlayer = (C03PacketPlayer)packet;
            s = s
                + "\n&7Position: &b"
                + MathUtil.round(c03PacketPlayer.func_149464_c(), 3)
                + "&7, &b"
                + MathUtil.round(c03PacketPlayer.func_149467_d(), 3)
                + "&7, &b"
                + MathUtil.round(c03PacketPlayer.func_149472_e(), 3)
                + "\n&7Rotations: &b"
                + MathUtil.round(c03PacketPlayer.func_149462_g(), 3)
                + "&7, &b"
                + MathUtil.round(c03PacketPlayer.func_149470_h(), 3)
                + "\n&7Ground: "
                + formatBoolean(c03PacketPlayer.func_149465_i())
                + "\n&7Moving: "
                + formatBoolean(c03PacketPlayer.func_149466_j())
                + "\n&7Rotating: "
                + formatBoolean(c03PacketPlayer.func_149463_k());
        }

        return s + "\n&7Client tick: &e" + tick;
    }

    @EventTarget
    public void onTick(TickEvent e) {
        if (e.getType() == EventType.PRE) {
            tick++;
        }
    }
}
