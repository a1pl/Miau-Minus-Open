package miau.mixin.viaversion;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(C0FPacketConfirmTransaction.class)
public class MixinC0FPacketConfirmTransaction {
    @Shadow
    private int field_149536_a;
    @Shadow
    private short field_149534_b;
    @Shadow
    private boolean field_149535_c;

    @Overwrite
    public void func_148840_b(PacketBuffer buf) {
        if (ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_17)) {
            buf.writeInt(this.field_149536_a);
        } else {
            buf.writeByte(this.field_149536_a);
            buf.writeShort(this.field_149534_b);
            buf.writeByte(this.field_149535_c ? 1 : 0);
        }
    }
}
