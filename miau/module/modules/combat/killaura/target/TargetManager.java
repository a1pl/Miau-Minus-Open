package miau.module.modules.combat.killaura.target;

import java.util.ArrayList;
import java.util.List;
import miau.module.modules.combat.KillAura;
import miau.module.modules.misc.AntiBot;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

public class TargetManager {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final KillAura killAura;

    public TargetManager(KillAura killAura) {
        this.killAura = killAura;
    }

    public List<EntityLivingBase> getValidTargets() {
        ArrayList<EntityLivingBase> targets = new ArrayList<>();
        if (mc.field_71441_e == null) {
            return targets;
        }

        for (Entity entity : mc.field_71441_e.field_72996_f) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase livingBase = (EntityLivingBase)entity;
                if (this.isValidTarget(livingBase) && this.isInRange(livingBase)) {
                    targets.add(livingBase);
                }
            }
        }

        return targets;
    }

    public AttackData findBestTarget(List<EntityLivingBase> targets) {
        if (targets.isEmpty()) {
            return null;
        }

        if (targets.stream().anyMatch(this::isInSwingRange)) {
            targets.removeIf(e -> !this.isInSwingRange(e));
        }

        if (targets.stream().anyMatch(this::isInAttackRange)) {
            targets.removeIf(e -> !this.isInAttackRange(e));
        }

        if (targets.stream().anyMatch(this::isPlayerTarget)) {
            targets.removeIf(e -> !this.isPlayerTarget(e));
        }

        targets.sort(
            (e1, e2) -> {
                int sortBase = 0;
                switch (this.killAura.sort.getValue()) {
                    case 0:
                        sortBase = Float.compare(TeamUtil.getHealthScore(e1), TeamUtil.getHealthScore(e2));
                        break;
                    case 1:
                        sortBase = Integer.compare(e1.field_70737_aN, e2.field_70737_aN);
                }

                return sortBase != 0
                    ? sortBase
                    : Double.compare(RotationUtil.distanceToEntity(e1), RotationUtil.distanceToEntity(e2));
            }
        );
        if (this.killAura.mode.getValue() == 1 && targets.size() > 1) {
            targets.sort((e1, e2) -> {
                LastAttackData data1 = this.killAura.targetMap.get(e1.func_145782_y());
                LastAttackData data2 = this.killAura.targetMap.get(e2.func_145782_y());
                double score1 = -(e1.func_110143_aJ() * 25.0 + (data1 == null ? 0L : data1.getTime()));
                double score2 = -(e2.func_110143_aJ() * 25.0 + (data2 == null ? 0L : data2.getTime()));
                return Double.compare(score1, score2);
            });
        }

        if (this.killAura.mode.getValue() == 1 && this.killAura.hitRegistered) {
            this.killAura.hitRegistered = false;
            this.killAura.switchTick = 0;
        }

        if (this.killAura.mode.getValue() == 0 || this.killAura.switchTick >= targets.size()) {
            this.killAura.switchTick = 0;
        }

        return new AttackData(targets.get(this.killAura.switchTick));
    }

    public boolean isValidTarget(EntityLivingBase entityLivingBase) {
        return this.isValid(entityLivingBase)
            && (this.killAura.throughWalls.getValue() || RotationUtil.rayTrace(entityLivingBase) == null);
    }

    public boolean isInRange(EntityLivingBase entityLivingBase) {
        double maxRange = this.killAura.attackRange.getValue().floatValue();
        maxRange += this.killAura.expandRange;
        return RotationUtil.distanceToEntity(entityLivingBase) <= maxRange;
    }

    public boolean isInSwingRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= this.killAura.attackRange.getValue().floatValue();
    }

    public boolean isBoxInSwingRange(AxisAlignedBB axisAlignedBB) {
        return RotationUtil.distanceToBox(axisAlignedBB) <= this.killAura.attackRange.getValue().floatValue();
    }

    public boolean isInAttackRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= this.killAura.attackRange.getValue().floatValue();
    }

    public boolean isBoxInAttackRange(AxisAlignedBB axisAlignedBB) {
        return RotationUtil.distanceToBox(axisAlignedBB) <= this.killAura.attackRange.getValue().floatValue();
    }

    public boolean isPlayerTarget(EntityLivingBase entityLivingBase) {
        return entityLivingBase instanceof EntityPlayer && TeamUtil.isTarget((EntityPlayer)entityLivingBase);
    }

    public boolean isValid(EntityLivingBase entityLivingBase) {
        if (entityLivingBase == null || mc.field_71441_e == null || mc.field_71439_g == null) {
            return false;
        }

        if (!mc.field_71441_e.field_72996_f.contains(entityLivingBase)) {
            return false;
        }

        if (entityLivingBase == mc.field_71439_g || entityLivingBase == mc.field_71439_g.field_70154_o) {
            return false;
        }

        if (entityLivingBase == mc.func_175606_aa() || entityLivingBase == mc.func_175606_aa().field_70154_o) {
            return false;
        }

        if (entityLivingBase.field_70725_aQ > 0) {
            return false;
        }

        if (entityLivingBase instanceof EntityOtherPlayerMP) {
            return this.isValidPlayer((EntityPlayer)entityLivingBase);
        }

        if (entityLivingBase instanceof EntityDragon || entityLivingBase instanceof EntityWither) {
            return this.killAura.targetBosses.getValue();
        }

        if (!(entityLivingBase instanceof EntityMob) && !(entityLivingBase instanceof EntitySlime)) {
            if (entityLivingBase instanceof EntityAnimal
                || entityLivingBase instanceof EntityBat
                || entityLivingBase instanceof EntitySquid
                || entityLivingBase instanceof EntityVillager) {
                return this.killAura.targetAnimals.getValue();
            } else {
                return !(entityLivingBase instanceof EntityIronGolem)
                    ? false
                    : this.killAura.targetGolems.getValue() && this.allowTeamColor(entityLivingBase);
            }
        } else {
            return !(entityLivingBase instanceof EntitySilverfish)
                ? this.killAura.targetMobs.getValue()
                : this.killAura.targetSilverfish.getValue() && this.allowTeamColor(entityLivingBase);
        }
    }

    private boolean isValidPlayer(EntityPlayer player) {
        if (!this.killAura.targetPlayers.getValue()) {
            return false;
        } else {
            boolean isInvisible = player.func_82150_aj();
            if (isInvisible && !this.killAura.targetInvisibles.getValue()) {
                return false;
            } else {
                return TeamUtil.isFriend(player)
                    ? false
                    : this.allowSameTeam(player) && (isInvisible || !AntiBot.isBot(player));
            }
        }
    }

    private boolean allowTeamColor(EntityLivingBase entityLivingBase) {
        return this.killAura.targetTeams.getValue() || !TeamUtil.hasTeamColor(entityLivingBase);
    }

    private boolean allowSameTeam(EntityPlayer player) {
        return this.killAura.targetTeams.getValue() || !TeamUtil.isSameTeam(player);
    }
}
