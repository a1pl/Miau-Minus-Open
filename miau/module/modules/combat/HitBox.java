package miau.module.modules.combat;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.LeftClickMouseEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.TeamUtil;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
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
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class HitBox extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private MovingObjectPosition targetEntity = null;
    public final FloatProperty multiplier = new FloatProperty("multiplier", 1.2F, 1.0F, 5.0F);
    public final ModeProperty showHitbox = new ModeProperty(
        "show-hitbox", 0, new String[]{"NONE", "PLAYERS", "MOBS", "ANIMALS", "ALL"}
    );
    public final ColorProperty color = new ColorProperty(
        "color", new Color(255, 255, 255).getRGB(), () -> this.showHitbox.getValue() != 0
    );
    public final BooleanProperty teams = new BooleanProperty(
        "teams", true, () -> this.showHitbox.getValue() == 1 || this.showHitbox.getValue() == 4
    );
    public final BooleanProperty botCheck = new BooleanProperty(
        "bot-check", true, () -> this.showHitbox.getValue() == 1 || this.showHitbox.getValue() == 4
    );

    public HitBox() {
        super("HitBox", false);
    }

    public static float getExpansion(Entity entity) {
        HitBox hitBox = (HitBox)Miau.moduleManager.modules.get(HitBox.class);
        return hitBox != null && hitBox.isEnabled() && entity instanceof EntityLivingBase
            ? hitBox.multiplier.getValue()
            : 1.0F;
    }

    private void calculateMouseOver(float partialTicks) {
        if (mc.func_175606_aa() != null && mc.field_71441_e != null) {
            mc.field_147125_j = null;
            Entity pointedEntity = null;
            double reach = 3.0;
            this.targetEntity = mc.func_175606_aa().func_174822_a(reach, partialTicks);
            double distance = reach;
            Vec3 eyePos = mc.func_175606_aa().func_174824_e(partialTicks);
            if (this.targetEntity != null) {
                distance = this.targetEntity.field_72307_f.func_72438_d(eyePos);
            }

            Vec3 lookVec = mc.func_175606_aa().func_70676_i(partialTicks);
            Vec3 reachVec = eyePos.func_72441_c(
                lookVec.field_72450_a * reach, lookVec.field_72448_b * reach, lookVec.field_72449_c * reach
            );
            Vec3 hitVec = null;
            float expansion = 1.0F;
            List<Entity> entities = mc.field_71441_e
                .func_72839_b(
                    mc.func_175606_aa(),
                    mc.func_175606_aa()
                        .func_174813_aQ()
                        .func_72321_a(
                            lookVec.field_72450_a * reach, lookVec.field_72448_b * reach, lookVec.field_72449_c * reach
                        )
                        .func_72314_b(expansion, expansion, expansion)
                );
            double closestDistance = distance;

            for (Entity entity : entities) {
                if (entity.func_70067_L()) {
                    float collisionSize = (float)((double)entity.func_70111_Y() * getExpansion(entity));
                    AxisAlignedBB expandedBox = entity.func_174813_aQ()
                        .func_72314_b(collisionSize, collisionSize, collisionSize);
                    MovingObjectPosition intercept = expandedBox.func_72327_a(eyePos, reachVec);
                    if (expandedBox.func_72318_a(eyePos)) {
                        if (0.0 < closestDistance || closestDistance == 0.0) {
                            pointedEntity = entity;
                            hitVec = intercept == null ? eyePos : intercept.field_72307_f;
                            closestDistance = 0.0;
                        }
                    } else if (intercept != null) {
                        double interceptDistance = eyePos.func_72438_d(intercept.field_72307_f);
                        if (interceptDistance < closestDistance || closestDistance == 0.0) {
                            if (entity != mc.func_175606_aa().field_70154_o || entity.canRiderInteract()) {
                                pointedEntity = entity;
                                hitVec = intercept.field_72307_f;
                                closestDistance = interceptDistance;
                            } else if (closestDistance == 0.0) {
                                pointedEntity = entity;
                                hitVec = intercept.field_72307_f;
                            }
                        }
                    }
                }
            }

            if (pointedEntity != null && (closestDistance < distance || this.targetEntity == null)) {
                this.targetEntity = new MovingObjectPosition(pointedEntity, hitVec);
                if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame) {
                    mc.field_147125_j = pointedEntity;
                }
            }
        }
    }

    private boolean shouldShowEntity(EntityLivingBase entity) {
        if (entity == mc.field_71439_g) {
            return false;
        }

        if (entity.field_70725_aQ <= 0 && !(entity instanceof EntityArmorStand) && !entity.func_82150_aj()) {
            if (mc.func_175606_aa().func_70032_d(entity) > 128.0F) {
                return false;
            }

            if (!entity.field_70158_ak && !RenderUtil.isInViewFrustum(entity.func_174813_aQ(), 0.1F)) {
                return false;
            }

            switch (this.showHitbox.getValue()) {
                case 0:
                    return false;
                case 1:
                    if (entity instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer)entity;
                        if (TeamUtil.isFriend(player)) {
                            return false;
                        }

                        if (this.teams.getValue() && TeamUtil.isSameTeam(player)) {
                            return false;
                        }

                        if (this.botCheck.getValue() && TeamUtil.isBot(player)) {
                            return false;
                        }

                        return true;
                    }

                    return false;
                case 2:
                    if (!(entity instanceof EntityDragon) && !(entity instanceof EntityWither)) {
                        if (!(entity instanceof EntityMob) && !(entity instanceof EntitySlime)) {
                            return false;
                        }

                        return !(entity instanceof EntitySilverfish);
                    }

                    return true;
                case 3:
                    return entity instanceof EntityAnimal
                        || entity instanceof EntityBat
                        || entity instanceof EntitySquid
                        || entity instanceof EntityVillager
                        || entity instanceof EntityIronGolem;
                case 4:
                    if (entity instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer)entity;
                        if (TeamUtil.isFriend(player)) {
                            return false;
                        }

                        if (this.teams.getValue() && TeamUtil.isSameTeam(player)) {
                            return false;
                        }

                        if (this.botCheck.getValue() && TeamUtil.isBot(player)) {
                            return false;
                        }
                    }

                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.calculateMouseOver(1.0F);
        }
    }

    @EventTarget(1)
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && !event.isCancelled() && this.targetEntity != null) {
            mc.field_71476_x = this.targetEntity;
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled() && this.showHitbox.getValue() != 0) {
            List<EntityLivingBase> entities = mc.field_71441_e
                .field_72996_f
                .stream()
                .filter(entityx -> entityx instanceof EntityLivingBase)
                .map(entityx -> (EntityLivingBase)entityx)
                .filter(this::shouldShowEntity)
                .collect(Collectors.toList());
            if (!entities.isEmpty()) {
                RenderUtil.enableRenderState();
                Color renderColor = new Color(this.color.getValue());

                for (EntityLivingBase entity : entities) {
                    float collisionSize = (float)(
                        (double)entity.func_70111_Y() * this.multiplier.getValue().floatValue()
                    );
                    AxisAlignedBB expandedBox = entity.func_174813_aQ()
                        .func_72314_b(collisionSize, collisionSize, collisionSize);
                    AxisAlignedBB offsetBox = new AxisAlignedBB(
                        expandedBox.field_72340_a
                            - entity.field_70165_t
                            + (
                                RenderUtil.lerpDouble(
                                        entity.field_70165_t, entity.field_70142_S, event.getPartialTicks()
                                    )
                                    - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX()
                            ),
                        expandedBox.field_72338_b
                            - entity.field_70163_u
                            + (
                                RenderUtil.lerpDouble(
                                        entity.field_70163_u, entity.field_70137_T, event.getPartialTicks()
                                    )
                                    - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY()
                            ),
                        expandedBox.field_72339_c
                            - entity.field_70161_v
                            + (
                                RenderUtil.lerpDouble(
                                        entity.field_70161_v, entity.field_70136_U, event.getPartialTicks()
                                    )
                                    - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
                            ),
                        expandedBox.field_72336_d
                            - entity.field_70165_t
                            + (
                                RenderUtil.lerpDouble(
                                        entity.field_70165_t, entity.field_70142_S, event.getPartialTicks()
                                    )
                                    - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX()
                            ),
                        expandedBox.field_72337_e
                            - entity.field_70163_u
                            + (
                                RenderUtil.lerpDouble(
                                        entity.field_70163_u, entity.field_70137_T, event.getPartialTicks()
                                    )
                                    - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY()
                            ),
                        expandedBox.field_72334_f
                            - entity.field_70161_v
                            + (
                                RenderUtil.lerpDouble(
                                        entity.field_70161_v, entity.field_70136_U, event.getPartialTicks()
                                    )
                                    - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
                            )
                    );
                    RenderUtil.drawBoundingBox(
                        offsetBox, renderColor.getRed(), renderColor.getGreen(), renderColor.getBlue(), 150, 1.5F
                    );
                }

                RenderUtil.disableRenderState();
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%.1fx", this.multiplier.getValue())};
    }
}
