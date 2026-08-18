package miau.module.modules.combat;

import com.google.common.base.Predicates;
import miau.mixin.IAccessorEntity;
import miau.module.Module;
import miau.module.modules.misc.AntiBot;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class Piercing extends Module {
    public final ModeProperty sortMode = new ModeProperty("Sort-mode", 0, new String[]{"Hurt time", "Health"});
    public final BooleanProperty ignoreBlocks = new BooleanProperty("Ignore-blocks", false);
    public final BooleanProperty ignoreTeammates = new BooleanProperty("Ignore-teammates", true);
    public final BooleanProperty ignoreNonPlayer = new BooleanProperty("Ignore-non-players", true);
    public final BooleanProperty weaponOnly = new BooleanProperty("Weapon-only", false);
    public final BooleanProperty insideHitboxOnly = new BooleanProperty("Inside-hitbox-only", false);
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final String[] sortModes = new String[]{"Hurt time", "Health"};

    public Piercing() {
        super("Piercing", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{sortModes[this.sortMode.getValue()]};
    }

    public boolean shouldOverrideMouseOver() {
        if (!this.isEnabled()) {
            return false;
        } else if (mc != null && mc.field_71439_g != null && mc.field_71441_e != null) {
            return !this.weaponOnly.getValue()
                    || mc.field_71439_g.func_70694_bm() != null
                        && (
                            mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemSword
                                || mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemAxe
                        )
                ? this.ignoreBlocks.getValue()
                    || mc.field_71476_x == null
                    || mc.field_71476_x.field_72313_a != MovingObjectType.BLOCK
                : false;
        } else {
            return false;
        }
    }

    public void modifyMouseOver(float partialTicks) {
        if (this.shouldOverrideMouseOver()) {
            this.keystrokesmod$modifyMouseOverVanillaLook(partialTicks);
        }
    }

    private void keystrokesmod$modifyMouseOverVanillaLook(float partialTicks) {
        Entity viewEntity = mc.func_175606_aa();
        if (viewEntity != null && mc.field_71441_e != null) {
            double reach = mc.field_71442_b.func_78757_d();
            Vec3 eyes = viewEntity.func_174824_e(partialTicks);
            if (mc.field_71442_b.func_78749_i()) {
                reach = 6.0;
            }

            Vec3 look;
            if (RotationUtil.customRots) {
                look = ((IAccessorEntity)viewEntity)
                    .callGetVectorForRotation(RotationUtil.serverPitch, RotationUtil.serverYaw);
            } else {
                look = viewEntity.func_70676_i(partialTicks);
            }

            Vec3 rayEnd = eyes.func_72441_c(
                look.field_72450_a * reach, look.field_72448_b * reach, look.field_72449_c * reach
            );
            Entity best = null;
            Vec3 bestHit = null;
            double bestDist = Double.MAX_VALUE;
            boolean bestLiving = false;
            int bestHurt = Integer.MAX_VALUE;
            float bestHp = Float.POSITIVE_INFINITY;
            int modeSel = this.sortMode.getValue();

            for (Entity e : mc.field_71441_e
                .func_175674_a(
                    viewEntity,
                    viewEntity.func_174813_aQ()
                        .func_72321_a(
                            look.field_72450_a * reach, look.field_72448_b * reach, look.field_72449_c * reach
                        )
                        .func_72314_b(1.0, 1.0, 1.0),
                    Predicates.and(EntitySelectors.field_180132_d, Entity::func_70067_L)
                )) {
                if ((!this.ignoreNonPlayer.getValue() || e instanceof EntityPlayer)
                    && (
                        !this.ignoreTeammates.getValue()
                            || !(e instanceof EntityPlayer)
                            || !TeamUtil.isSameTeam((EntityPlayer)e)
                    )
                    && (!(e instanceof EntityLivingBase) || !AntiBot.isBot((EntityLivingBase)e))
                    && (!(e instanceof EntityPlayer) || !TeamUtil.isFriend((EntityPlayer)e))) {
                    float cb = e.func_70111_Y();
                    AxisAlignedBB bb = e.func_174813_aQ().func_72314_b(cb, cb, cb);
                    MovingObjectPosition hit = bb.func_72327_a(eyes, rayEnd);
                    boolean inside = bb.func_72318_a(eyes);
                    if (inside || hit != null) {
                        double dist = inside ? 0.0 : eyes.func_72438_d(hit.field_72307_f);
                        if ((mc.field_71442_b.func_78749_i() || !(dist > 3.0))
                            && !(dist > reach)
                            && !(dist >= bestDist)
                            && (!this.insideHitboxOnly.getValue() || !(dist > 0.1F))
                            && (e != viewEntity.field_70154_o || viewEntity.canRiderInteract() || best == null)) {
                            boolean living = e instanceof EntityLivingBase;
                            int hurt = living ? ((EntityLivingBase)e).field_70737_aN : Integer.MAX_VALUE;
                            float hp = living ? ((EntityLivingBase)e).func_110143_aJ() : Float.POSITIVE_INFINITY;
                            boolean take = false;
                            if (best == null) {
                                take = true;
                            } else if (living && !bestLiving) {
                                take = true;
                            } else if (living == bestLiving) {
                                if (!living) {
                                    take = dist < bestDist;
                                } else if (modeSel == 0) {
                                    if (hurt < bestHurt) {
                                        take = true;
                                    } else if (hurt == bestHurt && dist < bestDist) {
                                        take = true;
                                    }
                                } else if (hp < bestHp) {
                                    take = true;
                                } else if (hp == bestHp && dist < bestDist) {
                                    take = true;
                                }
                            }

                            if (take) {
                                best = e;
                                bestHit = inside ? (hit == null ? eyes : hit.field_72307_f) : hit.field_72307_f;
                                bestDist = dist;
                                bestLiving = living;
                                bestHurt = hurt;
                                bestHp = hp;
                            }
                        }
                    }
                }
            }

            if (best != null && reach > 3.0 && bestDist > 3.0 && !mc.field_71442_b.func_78749_i()) {
                mc.field_71476_x = new MovingObjectPosition(MovingObjectType.MISS, bestHit, null, new BlockPos(bestHit));
            } else {
                if (best != null) {
                    mc.field_71476_x = new MovingObjectPosition(best, bestHit);
                    if (best instanceof EntityLivingBase || best instanceof EntityItemFrame) {
                        mc.field_147125_j = best;
                    }
                }
            }
        }
    }
}
