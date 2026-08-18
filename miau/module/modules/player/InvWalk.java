package miau.module.modules.player;

import com.google.common.base.CaseFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorC0DPacketCloseWindow;
import miau.module.Module;
import miau.module.modules.movement.Sprint;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.KeyBindUtil;
import miau.util.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState;

public class InvWalk extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final Queue<C0EPacketClickWindow> clickQueue = new ConcurrentLinkedQueue<>();
    private boolean keysPressed = false;
    private C16PacketClientStatus pendingStatus = null;
    private final Map<KeyBinding, Boolean> movementKeys = new HashMap<KeyBinding, Boolean>(8) {
        {
            this.put(InvWalk.mc.field_71474_y.field_74351_w, false);
            this.put(InvWalk.mc.field_71474_y.field_74368_y, false);
            this.put(InvWalk.mc.field_71474_y.field_74370_x, false);
            this.put(InvWalk.mc.field_71474_y.field_74366_z, false);
            this.put(InvWalk.mc.field_71474_y.field_74314_A, false);
            this.put(InvWalk.mc.field_71474_y.field_74311_E, false);
            this.put(InvWalk.mc.field_71474_y.field_151444_V, false);
        }
    };
    public final ModeProperty mode = new ModeProperty("Mode", 1, new String[]{"VANILLA", "LEGIT"});
    public final BooleanProperty guiEnabled = new BooleanProperty("click-gui", true);
    public final BooleanProperty lockMoveKey = new BooleanProperty("lock-move-dey", false);

    public InvWalk() {
        super("InvWalk", false);
    }

    public void pressMovementKeys(boolean skipSneak) {
        this.movementKeys
            .keySet()
            .stream()
            .filter(key -> !skipSneak || key != mc.field_71474_y.field_74311_E)
            .forEach(key -> KeyBindUtil.updateKeyState(key.func_151463_i()));
        if (Miau.moduleManager.modules.get(Sprint.class).isEnabled()) {
            KeyBindUtil.setKeyBindState(mc.field_71474_y.field_151444_V.func_151463_i(), true);
        }

        this.keysPressed = true;
    }

    public void resetMovementKeys() {
        this.movementKeys.replaceAll((k, v) -> false);
    }

    public boolean isSetMovementKeys() {
        return this.movementKeys.values().stream().anyMatch(Boolean::booleanValue);
    }

    public void storeMovementKeys() {
        this.movementKeys.replaceAll((k, v) -> KeyBindUtil.isKeyDown(k.func_151463_i()));
    }

    public void restoreMovementKeys() {
        for (Entry<KeyBinding, Boolean> keyBinding : this.movementKeys.entrySet()) {
            KeyBindUtil.setKeyBindState(keyBinding.getKey().func_151463_i(), keyBinding.getValue());
        }

        if (Miau.moduleManager.modules.get(Sprint.class).isEnabled()) {
            KeyBindUtil.setKeyBindState(mc.field_71474_y.field_151444_V.func_151463_i(), true);
        }

        this.keysPressed = true;
    }

    public boolean canInvWalk() {
        if (!(mc.field_71462_r instanceof GuiContainer)) {
            return false;
        }

        if (mc.field_71462_r instanceof GuiContainerCreative) {
            return false;
        }

        switch (this.mode.getValue()) {
            case 0:
                return true;
            case 1:
                if (!(mc.field_71462_r instanceof GuiInventory)) {
                    return false;
                }

                return this.pendingStatus != null && this.clickQueue.isEmpty();
            default:
                return false;
        }
    }

    private boolean canGuiWalk() {
        return mc.field_71462_r != null
            && !(mc.field_71462_r instanceof GuiChat)
            && !(mc.field_71462_r instanceof GuiContainer)
            && this.guiEnabled.getValue();
    }

    private boolean shouldRefreshKeysPostTick() {
        return this.mode.getValue() == 0 && (this.canInvWalk() || this.canGuiWalk());
    }

    public boolean temporaryStackIsEmpty() {
        if (mc.field_71439_g.field_71071_by.func_70445_o() != null) {
            return false;
        }

        if (mc.field_71439_g.field_71069_bz instanceof ContainerPlayer) {
            ContainerPlayer containerPlayer = (ContainerPlayer)mc.field_71439_g.field_71069_bz;

            for (int i = 0; i < containerPlayer.field_75181_e.func_70302_i_(); i++) {
                ItemStack stack = containerPlayer.field_75181_e.func_70301_a(i);
                if (stack != null) {
                    return false;
                }
            }
        }

        return true;
    }

    @EventTarget(4)
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            while (!this.clickQueue.isEmpty()) {
                PacketUtil.sendPacketNoEvent((Packet<?>)this.clickQueue.poll());
            }
        } else if (event.getType() == EventType.POST && this.isEnabled() && this.shouldRefreshKeysPostTick()) {
            if (this.isSetMovementKeys() && this.lockMoveKey.getValue()) {
                this.restoreMovementKeys();
            } else {
                this.pressMovementKeys(true);
            }
        }
    }

    @EventTarget(4)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.canGuiWalk()) {
                this.pressMovementKeys(true);
            } else {
                if (this.canInvWalk()) {
                    if (this.isSetMovementKeys() && this.lockMoveKey.getValue()) {
                        this.restoreMovementKeys();
                    } else {
                        this.pressMovementKeys(true);
                    }
                } else {
                    if (this.keysPressed) {
                        if (mc.field_71462_r != null) {
                            KeyBinding.func_74506_a();
                        } else if (this.isSetMovementKeys()) {
                            this.resetMovementKeys();
                            this.pressMovementKeys(false);
                        }

                        this.keysPressed = false;
                    }

                    if (this.pendingStatus != null) {
                        PacketUtil.sendPacketNoEvent(this.pendingStatus);
                        this.pendingStatus = null;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.SEND) {
            if (event.getPacket() instanceof C16PacketClientStatus) {
                this.storeMovementKeys();
                if (this.mode.getValue() == 1) {
                    C16PacketClientStatus packet = (C16PacketClientStatus)event.getPacket();
                    if (packet.func_149435_c() == EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                        event.setCancelled(true);
                        this.pendingStatus = packet;
                    }
                }
            } else if (!(event.getPacket() instanceof C0EPacketClickWindow)) {
                if (event.getPacket() instanceof C0DPacketCloseWindow) {
                    C0DPacketCloseWindow packet = (C0DPacketCloseWindow)event.getPacket();
                    if (((IAccessorC0DPacketCloseWindow)packet).getWindowId() == 0) {
                        if (this.pendingStatus != null) {
                            this.pendingStatus = null;
                            event.setCancelled(true);
                        }
                    } else if (!this.clickQueue.isEmpty()) {
                        this.clickQueue.clear();
                    }
                }
            } else {
                C0EPacketClickWindow packet = (C0EPacketClickWindow)event.getPacket();
                if (this.mode.getValue() == 1 && packet.func_149548_c() == 0) {
                    if ((packet.func_149542_h() == 3 || packet.func_149542_h() == 4) && packet.func_149544_d() == -999) {
                        event.setCancelled(true);
                        return;
                    }

                    if (this.pendingStatus != null) {
                        KeyBinding.func_74506_a();
                        event.setCancelled(true);
                        this.clickQueue.offer(packet);
                    }
                }

                if (this.pendingStatus != null) {
                    PacketUtil.sendPacketNoEvent(this.pendingStatus);
                    this.pendingStatus = null;
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        if (this.keysPressed) {
            if (mc.field_71462_r != null) {
                KeyBinding.func_74506_a();
            }

            this.keysPressed = false;
        }

        if (this.pendingStatus != null) {
            PacketUtil.sendPacketNoEvent(this.pendingStatus);
            this.pendingStatus = null;
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
