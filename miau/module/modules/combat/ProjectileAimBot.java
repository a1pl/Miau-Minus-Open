package miau.module.modules.combat;

import java.awt.Color;
import java.util.Comparator;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.management.RotationState;
import miau.module.Module;
import miau.module.modules.misc.AntiBot;
import miau.module.modules.render.HUD;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.GlStateManager;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class ProjectileAimBot extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty bow = new BooleanProperty("bow", true);
    public final BooleanProperty egg = new BooleanProperty("egg", true);
    public final BooleanProperty snowball = new BooleanProperty("snowball", true);
    public final BooleanProperty pearl = new BooleanProperty("ender-pearl", false);
    public final BooleanProperty otherItems = new BooleanProperty("other-items", false);
    public final FloatProperty range = new FloatProperty("range", 10.0F, 0.0F, 30.0F);
    public final BooleanProperty throughWalls = new BooleanProperty("through-walls", false);
    public final FloatProperty throughWallsRange = new FloatProperty(
        "through-walls-range", 10.0F, 0.0F, 30.0F, () -> this.throughWalls.getValue()
    );
    public final ModeProperty priority = new ModeProperty(
        "priority", 2, new String[]{"Health", "Distance", "Direction"}
    );
    public final ModeProperty gravityType = new ModeProperty("gravity-type", 1, new String[]{"None", "Projectile"});
    public final BooleanProperty predict = new BooleanProperty("predict", true, () -> this.gravityType.getValue() == 1);
    public final FloatProperty predictSize = new FloatProperty(
        "predict-size", 2.0F, 0.1F, 5.0F, () -> this.predict.getValue() && this.gravityType.getValue() == 1
    );
    public final BooleanProperty mark = new BooleanProperty("mark", true);
    public final BooleanProperty silentAim = new BooleanProperty("silent-aim", true);
    public final ModeProperty moveFix = new ModeProperty(
        "move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"}, () -> this.silentAim.getValue()
    );
    public final ModeProperty showTarget = new ModeProperty("show-target", 0, new String[]{"NONE", "DEFAULT", "HUD"});
    public final BooleanProperty targetPlayers = new BooleanProperty("target-players", true);
    public final BooleanProperty targetInvisibles = new BooleanProperty(
        "target-invisibles", false, this.targetPlayers::getValue
    );
    public final BooleanProperty targetBosses = new BooleanProperty("target-bosses", false);
    public final BooleanProperty targetMobs = new BooleanProperty("target-mobs", false);
    public final BooleanProperty targetAnimals = new BooleanProperty("target-animals", false);
    public final BooleanProperty targetGolems = new BooleanProperty("target-golems", false);
    public final BooleanProperty targetSilverfish = new BooleanProperty("target-silverfish", false);
    public final BooleanProperty targetTeams = new BooleanProperty("target-teams", true);
    public final BooleanProperty auto = new BooleanProperty("auto", false);
    public final IntProperty autoAmount = new IntProperty("auto-amount", 1, 1, 10, this.auto::getValue);
    public final BooleanProperty weaponOnly = new BooleanProperty("weapon-only", true, this.auto::getValue);
    public final FloatProperty autoDelay = new FloatProperty("auto-delay", 500.0F, 0.0F, 2000.0F, this.auto::getValue);
    private EntityLivingBase target;
    private int throwState = 0;
    private int lastSlot = -1;
    private long lastThrowTime = 0L;
    private int throwsRemaining = 0;
    private boolean hasRotated = false;

    public ProjectileAimBot() {
        super("ProjectileAimBot", false, false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()
            && event.getType() == EventType.PRE
            && mc.field_71439_g != null
            && mc.field_71441_e != null) {
            if (!this.auto.getValue()) {
                this.target = null;
                ItemStack stack = mc.field_71439_g.func_70694_bm();
                if (stack != null && this.isValidHeldItem(stack)) {
                    if (!(stack.func_77973_b() instanceof ItemBow) || mc.field_71439_g.func_71039_bw()) {
                        this.target = this.getTarget();
                        if (this.target != null) {
                            float[] rotations = this.gravityType.getValue() == 1
                                ? this.getProjectileRotations(this.target, stack)
                                : this.getDirectRotations(this.target);
                            if (rotations != null) {
                                float yaw = rotations[0];
                                float pitch = rotations[1];
                                event.setRotation(yaw, pitch, 3);
                                if (this.silentAim.getValue()) {
                                    if (this.moveFix.getValue() != 0) {
                                        event.setPervRotation(yaw, 3);
                                    }
                                } else {
                                    event.setPervRotation(yaw, 3);
                                    Miau.rotationManager.setRotation(yaw, pitch, 3, true);
                                }
                            }
                        }
                    }
                }
            } else if (this.weaponOnly.getValue()
                && (
                    mc.field_71439_g.func_70694_bm() == null
                        || !(mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemSword)
                )) {
                if (this.throwState != 0 || this.lastSlot != -1) {
                    this.switchBack();
                }

                this.resetAutoState();
            } else if (!this.hasProjectile()) {
                this.resetAutoState();
                this.switchBack();
            } else {
                if (this.throwState == 0) {
                    this.target = this.getTarget();
                    if (this.target == null) {
                        return;
                    }

                    KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                    if (killAura != null && killAura.isEnabled() && killAura.isInRange(this.target)) {
                        return;
                    }

                    if ((float)(System.currentTimeMillis() - this.lastThrowTime) < this.autoDelay.getValue()) {
                        return;
                    }

                    this.throwsRemaining = this.autoAmount.getValue();
                    this.throwState = 1;
                    this.hasRotated = false;
                }

                if (this.throwState == 1) {
                    this.switchToProjectile();
                    this.throwState = 2;
                } else if (this.throwState == 2) {
                    if (this.throwsRemaining > 0) {
                        ItemStack stack = mc.field_71439_g.func_70694_bm();
                        if (stack == null) {
                            this.throwState = 4;
                            return;
                        }

                        float[] rotations = this.gravityType.getValue() == 1
                            ? this.getProjectileRotations(this.target, stack)
                            : this.getDirectRotations(this.target);
                        if (rotations != null) {
                            event.setRotation(rotations[0], rotations[1], 3);
                            event.setPervRotation(rotations[0], 3);
                            this.hasRotated = true;
                            this.throwState = 3;
                        } else {
                            this.throwState = 4;
                        }
                    } else {
                        this.throwState = 4;
                    }
                } else if (this.throwState == 3) {
                    this.throwProjectile();
                    this.throwsRemaining--;
                    this.throwState = this.throwsRemaining > 0 ? 2 : 4;
                } else if (this.throwState == 4) {
                    this.switchBack();
                    this.resetAutoState();
                    this.lastThrowTime = System.currentTimeMillis();
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && this.silentAim.getValue()) {
            if (this.moveFix.getValue() == 1
                && RotationState.isActived()
                && RotationState.getPriority() == 3.0F
                && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && this.target != null && TeamUtil.isEntityLoaded(this.target)) {
            if (this.mark.getValue()) {
                this.drawPlatform(this.target, new Color(37, 126, 255, 70), event.getPartialTicks());
            }

            if (this.showTarget.getValue() != 0) {
                Color color = this.showTarget.getValue() == 2
                    ? ((HUD)Miau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis())
                    : new Color(255, 0, 0);
                RenderUtil.drawEntityBox(this.target, color.getRed(), color.getGreen(), color.getBlue());
            }
        }
    }

    private boolean isValidHeldItem(ItemStack stack) {
        Item item = stack.func_77973_b();
        if (item instanceof ItemBow) {
            return this.bow.getValue();
        } else if (item instanceof ItemEgg) {
            return this.egg.getValue();
        } else if (item instanceof ItemSnowball) {
            return this.snowball.getValue();
        } else {
            return item instanceof ItemEnderPearl ? this.pearl.getValue() : this.otherItems.getValue();
        }
    }

    private void resetAutoState() {
        this.target = null;
        this.throwState = 0;
        this.throwsRemaining = 0;
        this.hasRotated = false;
    }

    private boolean hasProjectile() {
        return this.getProjectileSlot() != -1;
    }

    private int getProjectileSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null && this.isValidHeldItem(stack) && !(stack.func_77973_b() instanceof ItemBow)) {
                return i;
            }
        }

        return -1;
    }

    private void switchToProjectile() {
        int slot = this.getProjectileSlot();
        if (slot != -1) {
            this.lastSlot = mc.field_71439_g.field_71071_by.field_70461_c;
            mc.field_71439_g.field_71071_by.field_70461_c = slot;
        }
    }

    private void switchBack() {
        if (this.lastSlot != -1) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.lastSlot;
            this.lastSlot = -1;
        }
    }

    private void throwProjectile() {
        int slot = mc.field_71439_g.field_71071_by.field_70461_c;
        ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(slot);
        if (stack != null) {
            mc.func_147114_u().func_147297_a(new C08PacketPlayerBlockPlacement(stack));
        }
    }

    private EntityLivingBase getTarget() {
        return mc.field_71441_e
            .field_72996_f
            .stream()
            .filter(entity -> entity instanceof EntityLivingBase)
            .map(entity -> (EntityLivingBase)entity)
            .filter(this::isValidTarget)
            .min(Comparator.comparingDouble(this::priorityValue))
            .orElse(null);
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (!this.isValid(entity)) {
            return false;
        } else if (RotationUtil.distanceToEntity(entity) > this.range.getValue().floatValue()) {
            return false;
        } else {
            return !this.throughWalls.getValue()
                ? RotationUtil.rayTrace(entity) == null
                : RotationUtil.distanceToEntity(entity) <= this.throughWallsRange.getValue().floatValue()
                    || RotationUtil.rayTrace(entity) == null;
        }
    }

    private boolean isValid(EntityLivingBase entityLivingBase) {
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
            return this.targetBosses.getValue();
        }

        if (!(entityLivingBase instanceof EntityMob) && !(entityLivingBase instanceof EntitySlime)) {
            if (entityLivingBase instanceof EntityAnimal
                || entityLivingBase instanceof EntityBat
                || entityLivingBase instanceof EntitySquid
                || entityLivingBase instanceof EntityVillager) {
                return this.targetAnimals.getValue();
            } else {
                return !(entityLivingBase instanceof EntityIronGolem)
                    ? false
                    : this.targetGolems.getValue() && this.allowTeamColor(entityLivingBase);
            }
        } else {
            return !(entityLivingBase instanceof EntitySilverfish)
                ? this.targetMobs.getValue()
                : this.targetSilverfish.getValue() && this.allowTeamColor(entityLivingBase);
        }
    }

    private boolean isValidPlayer(EntityPlayer player) {
        if (!this.targetPlayers.getValue()) {
            return false;
        } else if (player.func_70608_bn()) {
            return false;
        } else {
            boolean isInvisible = player.func_82150_aj();
            if (isInvisible && !this.targetInvisibles.getValue()) {
                return false;
            } else if (TeamUtil.isFriend(player)) {
                return false;
            } else {
                return !TeamUtil.isTarget(player)
                    ? false
                    : this.allowSameTeam(player) && (isInvisible || !AntiBot.isBot(player));
            }
        }
    }

    private boolean allowTeamColor(EntityLivingBase entityLivingBase) {
        return this.targetTeams.getValue() || !TeamUtil.hasTeamColor(entityLivingBase);
    }

    private boolean allowSameTeam(EntityPlayer player) {
        return this.targetTeams.getValue() || !TeamUtil.isSameTeam(player);
    }

    private double priorityValue(EntityLivingBase entity) {
        switch (this.priority.getValue()) {
            case 0:
                return TeamUtil.getHealthScore(entity);
            case 1:
                return RotationUtil.distanceToEntity(entity);
            case 2:
            default:
                return Math.abs(MathHelper.func_76142_g(this.getYawTo(entity) - mc.field_71439_g.field_70177_z));
        }
    }

    private float[] getDirectRotations(EntityLivingBase entity) {
        Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
        double x = entity.field_70165_t - eyes.field_72450_a;
        double y = entity.field_70163_u + entity.func_70047_e() - eyes.field_72448_b;
        double z = entity.field_70161_v - eyes.field_72449_c;
        return RotationUtil.getRotations(
            x, y, z, mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A, 180.0F, 0.0F
        );
    }

    private float[] getProjectileRotations(EntityLivingBase entity, ItemStack stack) {
        Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
        double targetX = entity.field_70165_t;
        double targetY = entity.field_70163_u + entity.func_70047_e();
        double targetZ = entity.field_70161_v;
        if (this.predict.getValue()) {
            double predictTicks = this.predictSize.getValue().floatValue();
            targetX += (entity.field_70165_t - entity.field_70169_q) * predictTicks;
            targetY += (entity.field_70163_u - entity.field_70167_r) * predictTicks;
            targetZ += (entity.field_70161_v - entity.field_70166_s) * predictTicks;
        }

        double diffX = targetX - eyes.field_72450_a;
        double diffY = targetY - eyes.field_72448_b;
        double diffZ = targetZ - eyes.field_72449_c;
        double horizontal = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float velocity = this.getProjectileVelocity(stack);
        float gravity = this.getProjectileGravity(stack);
        float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch;
        if (gravity <= 0.0F) {
            pitch = (float)(-Math.toDegrees(Math.atan2(diffY, horizontal)));
        } else {
            double velocitySq = velocity * velocity;
            double root = velocitySq * velocitySq
                - gravity * (gravity * horizontal * horizontal + 2.0 * diffY * velocitySq);
            if (root < 0.0) {
                return this.getDirectRotations(entity);
            }

            pitch = (float)(-Math.toDegrees(Math.atan((velocitySq - Math.sqrt(root)) / (gravity * horizontal))));
        }

        return new float[]{
            RotationUtil.quantizeAngle(yaw), RotationUtil.quantizeAngle(MathHelper.func_76131_a(pitch, -90.0F, 90.0F))
        };
    }

    private float getProjectileVelocity(ItemStack stack) {
        if (stack.func_77973_b() instanceof ItemBow) {
            int useDuration = mc.field_71439_g.func_71057_bx();
            float charge = useDuration / 20.0F;
            charge = (charge * charge + charge * 2.0F) / 3.0F;
            return Math.min(charge, 1.0F) * 3.0F;
        } else {
            return 0.5F;
        }
    }

    private float getProjectileGravity(ItemStack stack) {
        return stack.func_77973_b() instanceof ItemBow ? 0.05F : 0.03F;
    }

    private float getYawTo(Entity entity) {
        double x = entity.field_70165_t - mc.field_71439_g.field_70165_t;
        double z = entity.field_70161_v - mc.field_71439_g.field_70161_v;
        return (float)Math.toDegrees(Math.atan2(z, x)) - 90.0F;
    }

    private void drawPlatform(Entity entity, Color color, float partialTicks) {
        double renderX = entity.field_70142_S
            + (entity.field_70165_t - entity.field_70142_S) * partialTicks
            - mc.func_175598_ae().field_78730_l;
        double renderY = entity.field_70137_T
            + (entity.field_70163_u - entity.field_70137_T) * partialTicks
            - mc.func_175598_ae().field_78731_m;
        double renderZ = entity.field_70136_U
            + (entity.field_70161_v - entity.field_70136_U) * partialTicks
            - mc.func_175598_ae().field_78728_n;
        AxisAlignedBB box = entity.func_174813_aQ();
        double radius = Math.max(box.field_72336_d - box.field_72340_a, box.field_72334_f - box.field_72339_c) * 0.75;
        GlStateManager.func_179094_E();
        GlStateManager.func_179137_b(renderX, renderY + 0.02, renderZ);
        RenderUtil.drawLine((float)(-radius), 0.0F, (float)radius, 0.0F, 2.0F, color.getRGB());
        RenderUtil.drawLine(0.0F, (float)(-radius), 0.0F, (float)radius, 2.0F, color.getRGB());
        GlStateManager.func_179121_F();
    }

    public boolean hasTarget() {
        return this.target != null && mc.field_71439_g != null && RotationUtil.rayTrace(this.target) == null;
    }

    @Override
    public void onDisabled() {
        this.switchBack();
        this.resetAutoState();
        this.target = null;
        RotationState.applyState(false, 0.0F, 0.0F, 0.0F, Integer.MIN_VALUE);
    }
}
