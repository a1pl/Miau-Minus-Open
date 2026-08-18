package miau.mixin.viaversion;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NetHandlerPlayClient.class, priority = 1001)
public class MixinNetHandlerPlayClient {
    @Shadow
    private Minecraft field_147299_f;

    @Shadow
    public void func_147297_a(Packet<?> packet) {
    }

    @Overwrite
    public void func_147239_a(S32PacketConfirmTransaction packetIn) {
        PacketThreadUtil.func_180031_a(packetIn, (NetHandlerPlayClient)this, this.field_147299_f);
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_17)) {
            this.func_147297_a(new C0FPacketConfirmTransaction(packetIn.func_148889_c(), (short)0, false));
        } else {
            Container container = null;
            EntityPlayer entityplayer = this.field_147299_f.field_71439_g;
            if (entityplayer != null) {
                if (packetIn.func_148889_c() == 0) {
                    container = entityplayer.field_71069_bz;
                } else if (packetIn.func_148889_c() == entityplayer.field_71070_bA.field_75152_c) {
                    container = entityplayer.field_71070_bA;
                }

                if (container != null && !packetIn.func_148888_e()) {
                    this.func_147297_a(
                        new C0FPacketConfirmTransaction(packetIn.func_148889_c(), packetIn.func_148890_d(), true)
                    );
                }
            }
        }
    }
}
