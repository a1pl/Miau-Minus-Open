package miau.mixin;

import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin(S27PacketExplosion.class)
public interface IAccessorS27PacketExplosion {
    @Accessor("field_149152_f")
    void setMotionX(float var1);

    @Accessor("field_149153_g")
    void setMotionY(float var1);

    @Accessor("field_149159_h")
    void setMotionZ(float var1);

    @Accessor("field_149152_f")
    float getMotionX();

    @Accessor("field_149153_g")
    float getMotionY();

    @Accessor("field_149159_h")
    float getMotionZ();
}
