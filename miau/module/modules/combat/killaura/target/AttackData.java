package miau.module.modules.combat.killaura.target;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;

public class AttackData {
    private final EntityLivingBase entity;
    private final AxisAlignedBB box;
    private final double x;
    private final double y;
    private final double z;

    public AttackData(EntityLivingBase entityLivingBase) {
        this.entity = entityLivingBase;
        double collisionBorderSize = entityLivingBase.func_70111_Y();
        this.box = entityLivingBase.func_174813_aQ()
            .func_72314_b(collisionBorderSize, collisionBorderSize, collisionBorderSize);
        this.x = entityLivingBase.field_70165_t;
        this.y = entityLivingBase.field_70163_u;
        this.z = entityLivingBase.field_70161_v;
    }

    public EntityLivingBase getEntity() {
        return this.entity;
    }

    public AxisAlignedBB getBox() {
        return this.box;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }
}
