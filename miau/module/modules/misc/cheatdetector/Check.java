package miau.module.modules.misc.cheatdetector;

import miau.Miau;
import miau.event.impl.PacketEvent;
import miau.module.modules.misc.CheatDetector;
import miau.notification.NotificationType;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public abstract class Check {
    protected static final Minecraft mc = Minecraft.func_71410_x();
    public TimerUtil flagTimer = new TimerUtil();

    public abstract String getName();

    public void onUpdate(EntityPlayer player) {
    }

    public void onPacket(PacketEvent event, EntityPlayer player) {
    }

    public void flag(EntityPlayer player, String verbose) {
        if (this.flagTimer
            .hasTimeElapsed(
                (long)((CheatDetector)Miau.moduleManager.getModule(CheatDetector.class))
                    .alertCoolDown
                    .getValue()
                    .floatValue()
            )) {
            CheatDetector cd = (CheatDetector)Miau.moduleManager.getModule(CheatDetector.class);
            String verboseStr = "";
            if (cd.verbose.getValue() && verbose != null && !verbose.isEmpty()) {
                verboseStr = " [" + EnumChatFormatting.WHITE + verbose + EnumChatFormatting.GRAY + "]";
            }

            if (cd.alertMode.getValue() == 0) {
                Miau.notificationManager
                    .pop(
                        "CheatDetector",
                        player.func_70005_c_()
                            + EnumChatFormatting.WHITE
                            + " has failed "
                            + EnumChatFormatting.GRAY
                            + this.getName()
                            + EnumChatFormatting.GRAY
                            + verboseStr,
                        NotificationType.INFO
                    );
            } else {
                mc.field_71439_g
                    .func_145747_a(
                        new ChatComponentText(
                            EnumChatFormatting.DARK_GRAY
                                + "["
                                + EnumChatFormatting.RED
                                + "CheatDetector"
                                + EnumChatFormatting.DARK_GRAY
                                + "]"
                                + EnumChatFormatting.GRAY
                                + " » "
                                + EnumChatFormatting.WHITE
                                + player.func_70005_c_()
                                + " failed "
                                + EnumChatFormatting.RED
                                + this.getName()
                                + EnumChatFormatting.GRAY
                                + verboseStr
                        )
                    );
            }

            cd.mark(player);
            this.flagTimer.reset();
        }
    }
}
