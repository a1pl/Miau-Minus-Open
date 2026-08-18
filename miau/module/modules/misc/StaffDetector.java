package miau.module.modules.misc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.notification.NotificationType;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S38PacketPlayerListItem.Action;
import net.minecraft.network.play.server.S38PacketPlayerListItem.AddPlayerData;

public class StaffDetector extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"3FMC"});
    public final BooleanProperty autoLeave = new BooleanProperty("auto-leave", false);
    private static final Set<String> STAFF_LIST = new HashSet<>(
        Arrays.asList(
            "VinhGaming", "cheesethesylveon", "thanhhau", "sennekoi", "lasgana", "novapev4", "_DuckVN_", "khoaho01623"
        )
    );

    public StaffDetector() {
        super("StaffDetector", false, false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof S38PacketPlayerListItem) {
                S38PacketPlayerListItem pkt = (S38PacketPlayerListItem)packet;
                if (pkt.func_179768_b() == Action.ADD_PLAYER) {
                    for (AddPlayerData data : pkt.func_179767_a()) {
                        if (data != null && data.func_179962_a() != null) {
                            String name = data.func_179962_a().getName();
                            if (name != null && STAFF_LIST.contains(name.toLowerCase())) {
                                this.alert(
                                    NotificationType.ERROR,
                                    "Staff Online!",
                                    name + " joined the server",
                                    "&c&l[STAFF] &r&f" + name + " &ajoined the server!"
                                );
                                this.triggerAutoLeave(name);
                            }
                        }
                    }
                } else if (pkt.func_179768_b() == Action.REMOVE_PLAYER && mc.field_71441_e != null) {
                    for (AddPlayerData data : pkt.func_179767_a()) {
                        if (data != null && data.func_179962_a() != null) {
                            EntityPlayer entity = mc.field_71441_e.func_152378_a(data.func_179962_a().getId());
                            if (entity != null && entity != mc.field_71439_g) {
                                String name = entity.func_146103_bH().getName();
                                if (STAFF_LIST.contains(name.toLowerCase())) {
                                    this.alert(
                                        NotificationType.SUCCESS,
                                        "Staff Left",
                                        name + " left the server",
                                        "&a[STAFF] &f" + name + " &cleft the server."
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void alert(NotificationType type, String title, String desc, String chatMsg) {
        Miau.notificationManager.builder(type).title(title).description(desc).duration(3000).buildAndPublish();
        ChatUtil.display(chatMsg);
    }

    private void triggerAutoLeave(String name) {
        if (this.autoLeave.getValue() && mc.field_71439_g != null) {
            ChatUtil.sendMessage("/hub");
            this.setEnabled(false);
        }
    }
}
