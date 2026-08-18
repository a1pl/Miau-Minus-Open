package miau.module.modules.combat.velocity;

import miau.event.impl.AttackEvent;
import miau.module.modules.combat.Velocity;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import net.minecraft.entity.player.EntityPlayer;

public class DexlandVelocity extends VelocityMode {
    public final IntProperty times = new IntProperty("times", 2, 1, 10);
    public final FloatProperty hReduce = new FloatProperty("h-reduce", 0.42F, 0.0F, 1.0F);
    private int count = 0;
    private long lastAttackTime = 0L;

    public DexlandVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.count = 0;
        this.lastAttackTime = 0L;
    }

    @Override
    public void onAttack(AttackEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            if (player.field_70737_aN > 0
                && ++this.count % this.times.getValue() == 0
                && System.currentTimeMillis() - this.lastAttackTime <= 8000L) {
                VelocityUtil.reduceXZ(this.hReduce.getValue().floatValue());
            }

            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onDisable() {
        this.count = 0;
        this.lastAttackTime = 0L;
    }
}
