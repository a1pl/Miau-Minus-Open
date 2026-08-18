package miau.module.modules.network;

import java.awt.Color;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import miau.Miau;
import miau.enums.BlinkModules;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.module.modules.misc.AntiBot;
import miau.module.modules.player.BedNuker;
import miau.module.modules.player.Scaffold;
import miau.module.modules.render.HUD;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.ItemUtil;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class LagRange extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final double MINIMUM_DISTANCE = 3.0;
    public final IntProperty delay = new IntProperty("delay", 150, 0, 1000);
    public final FloatProperty range = new FloatProperty("range", 10.0F, 3.0F, 100.0F);
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Delay", "Test", "Lag", "Crew"});
    public final IntProperty blinkTick = new IntProperty("blink-tick", 3, 0, 10, () -> this.mode.getValue() == 2);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);
    public final BooleanProperty teams = new BooleanProperty("teams", true);
    public final ModeProperty showPosition = new ModeProperty(
        "show-position", 0, new String[]{"NONE", "DEFAULT", "HUD"}
    );
    public final BooleanProperty sprintReset = new BooleanProperty(
        "sprint-reset", true, () -> this.mode.getValue() == 1
    );
    public final BooleanProperty blockSword = new BooleanProperty("block-sword", true, () -> this.mode.getValue() == 1);
    public final BooleanProperty splashPotion = new BooleanProperty(
        "splash-potion", true, () -> this.mode.getValue() == 1
    );
    private int tickIndex = -1;
    private long delayCounter = 0L;
    private boolean hasTarget = false;
    private Vec3 lastPosition = null;
    private Vec3 currentPosition = null;
    private boolean isLagging = false;
    private int lastSelfHurtTime = 0;
    private int lastTargetHurtTime = 0;
    private int hitMarkedEntityId = -1;
    private boolean lastSprintState = false;
    private boolean lastBlockingState = false;
    private double lastDistSq = -1.0;
    private EntityPlayer currentTarget = null;
    private long lagStartTime = 0L;
    private final Set<Packet<?>> packetFastTrack = Collections.newSetFromMap(new IdentityHashMap<>());
    private Vec3 indicatorFrom = null;
    private Vec3 indicatorTo = null;
    private long indicatorStartMs = 0L;

    private boolean isValidTarget(EntityPlayer entityPlayer) {
        if (entityPlayer == mc.field_71439_g || entityPlayer == mc.field_71439_g.field_70154_o) {
            return false;
        } else if (entityPlayer == mc.func_175606_aa() || entityPlayer == mc.func_175606_aa().field_70154_o) {
            return false;
        } else if (entityPlayer.field_70725_aQ > 0) {
            return false;
        } else {
            return TeamUtil.isFriend(entityPlayer)
                ? false
                : (!this.teams.getValue() || !TeamUtil.isSameTeam(entityPlayer))
                    && (!this.botCheck.getValue() || !TeamUtil.isBot(entityPlayer));
        }
    }

    private boolean shouldResetOnPacket(Packet<?> packet) {
        if (packet instanceof C02PacketUseEntity) {
            return true;
        }

        if (packet instanceof C07PacketPlayerDigging) {
            return ((C07PacketPlayerDigging)packet).func_180762_c() != Action.RELEASE_USE_ITEM;
        }

        if (!(packet instanceof C08PacketPlayerBlockPlacement)) {
            return false;
        }

        ItemStack item = ((C08PacketPlayerBlockPlacement)packet).func_149574_g();
        return item == null || !(item.func_77973_b() instanceof ItemSword);
    }

    public LagRange() {
        super("LagRange", false);
    }

    private void resetOldMiau() {
        this.tickIndex = -1;
        if (this.mode.getValue() == 2) {
            Miau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
        }
    }

    private void tickOldMiau() {
        if (this.mode.getValue() == 0) {
            Miau.lagManager.setDelay(0);
        }

        this.hasTarget = false;
        Scaffold scaffold = (Scaffold)Miau.moduleManager.modules.get(Scaffold.class);
        if (scaffold != null && scaffold.isEnabled()) {
            this.resetOldMiau();
        } else {
            BedNuker bedNuker = (BedNuker)Miau.moduleManager.modules.get(BedNuker.class);
            if ((!bedNuker.isEnabled() || !bedNuker.isReady())
                && !((IAccessorPlayerControllerMP)mc.field_71442_b).getIsHittingBlock()
                && (!mc.field_71439_g.func_71039_bw() || mc.field_71439_g.func_70632_aY())) {
                boolean weaponOk = !this.weaponsOnly.getValue()
                    || ItemUtil.hasRawUnbreakingEnchant()
                    || this.allowTools.getValue() && ItemUtil.isHoldingTool();
                if (!weaponOk) {
                    this.resetOldMiau();
                } else {
                    List<EntityPlayer> players = mc.field_71441_e
                        .field_72996_f
                        .stream()
                        .filter(entity -> entity instanceof EntityPlayer)
                        .map(entity -> (EntityPlayer)entity)
                        .filter(this::isValidTarget)
                        .collect(Collectors.toList());
                    if (players.isEmpty()) {
                        this.resetOldMiau();
                    } else {
                        double height = mc.field_71439_g.func_70047_e();
                        Vec3 eyePosition = Miau.lagManager.getLastPosition().func_72441_c(0.0, height, 0.0);
                        Vec3 targetEyePosition = new Vec3(
                            mc.field_71439_g.field_70142_S,
                            mc.field_71439_g.field_70137_T + height,
                            mc.field_71439_g.field_70136_U
                        );
                        Vec3 playerEyePosition = new Vec3(
                            mc.field_71439_g.field_70165_t,
                            mc.field_71439_g.field_70163_u + height,
                            mc.field_71439_g.field_70161_v
                        );

                        for (EntityPlayer player : players) {
                            double distance = RotationUtil.distanceToBox(player, playerEyePosition);
                            if (!(distance > this.range.getValue().floatValue())) {
                                double targetDist = RotationUtil.distanceToBox(player, targetEyePosition);
                                double eyeDist = RotationUtil.distanceToBox(player, eyePosition);
                                if (distance < targetDist || distance < eyeDist) {
                                    if (this.tickIndex < 0) {
                                        this.tickIndex = 0;

                                        for (this.delayCounter = this.delayCounter + this.delay.getValue().intValue();
                                            this.delayCounter > 0L;
                                            this.delayCounter -= 50L
                                        ) {
                                            this.tickIndex++;
                                        }
                                    }

                                    if (this.mode.getValue() == 2) {
                                        Miau.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                                        if (Miau.blinkManager.countMovement() > this.blinkTick.getValue().longValue()) {
                                            Miau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                                        }
                                    } else if (this.mode.getValue() == 0) {
                                        Miau.lagManager.setDelay(this.tickIndex);
                                    }

                                    this.hasTarget = true;
                                    return;
                                }
                            }
                        }

                        this.resetOldMiau();
                    }
                }
            } else {
                this.resetOldMiau();
            }
        }
    }

    private boolean crewIsValidTarget(EntityPlayer entityPlayer) {
        if (entityPlayer == mc.field_71439_g || entityPlayer == mc.field_71439_g.field_70154_o) {
            return false;
        }

        if (entityPlayer == mc.func_175606_aa() || entityPlayer == mc.func_175606_aa().field_70154_o) {
            return false;
        }

        if (entityPlayer.field_70725_aQ > 0) {
            return false;
        }

        if (TeamUtil.isFriend(entityPlayer)) {
            return false;
        }

        boolean isBot = AntiBot.isBot(entityPlayer);
        return (!this.teams.getValue() || !TeamUtil.isSameTeam(entityPlayer)) && !isBot;
    }

    private void tickCrew() {
        Miau.lagManager.setDelay(0);
        this.hasTarget = false;
        BedNuker bedNuker = (BedNuker)Miau.moduleManager.modules.get(BedNuker.class);
        if ((!bedNuker.isEnabled() || !bedNuker.isReady())
            && !((IAccessorPlayerControllerMP)mc.field_71442_b).getIsHittingBlock()
            && (!mc.field_71439_g.func_71039_bw() || mc.field_71439_g.func_70632_aY())
            && (
                !this.weaponsOnly.getValue()
                    || ItemUtil.hasRawUnbreakingEnchant()
                    || this.allowTools.getValue() && ItemUtil.isHoldingTool()
            )) {
            List<EntityPlayer> players = mc.field_71441_e
                .field_72996_f
                .stream()
                .filter(entity -> entity instanceof EntityPlayer)
                .map(entity -> (EntityPlayer)entity)
                .filter(this::crewIsValidTarget)
                .collect(Collectors.toList());
            if (players.isEmpty()) {
                this.tickIndex = -1;
            } else {
                double height = mc.field_71439_g.func_70047_e();
                Vec3 eyePosition = Miau.lagManager.getLastPosition().func_72441_c(0.0, height, 0.0);
                Vec3 targetEyePosition = new Vec3(
                    mc.field_71439_g.field_70142_S,
                    mc.field_71439_g.field_70137_T + height,
                    mc.field_71439_g.field_70136_U
                );
                Vec3 playerEyePosition = new Vec3(
                    mc.field_71439_g.field_70165_t,
                    mc.field_71439_g.field_70163_u + height,
                    mc.field_71439_g.field_70161_v
                );

                for (EntityPlayer player : players) {
                    double distance = RotationUtil.distanceToBox(player, playerEyePosition);
                    if (!(distance > this.range.getValue().floatValue())) {
                        double targetDist = RotationUtil.distanceToBox(player, targetEyePosition);
                        double eyeDist = RotationUtil.distanceToBox(player, eyePosition);
                        if (distance < targetDist || distance < eyeDist) {
                            if (this.tickIndex < 0) {
                                this.tickIndex = 0;

                                for (this.delayCounter = this.delayCounter + this.delay.getValue().intValue();
                                    this.delayCounter > 0L;
                                    this.delayCounter -= 50L
                                ) {
                                    this.tickIndex++;
                                }
                            }

                            Miau.lagManager.setDelay(this.tickIndex);
                            this.hasTarget = true;
                            return;
                        }
                    }
                }
            }
        } else {
            this.tickIndex = -1;
        }
    }

    private EntityPlayer getMouseOverTarget(double rangeSq) {
        if (mc.field_71476_x != null
            && mc.field_71476_x.field_72313_a == MovingObjectType.ENTITY
            && mc.field_71476_x.field_72308_g instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)mc.field_71476_x.field_72308_g;
            if (this.isValidTarget(player)) {
                Vec3 eyePos = mc.field_71439_g.func_174824_e(1.0F);
                double distSq = RotationUtil.distanceToBox(player, eyePos);
                if (distSq <= rangeSq) {
                    return player;
                }
            }
        }

        return null;
    }

    private void startLag() {
        if (!this.isLagging) {
            this.isLagging = true;
            this.lagStartTime = System.currentTimeMillis();
            Miau.lagManager.fastTrackSet = null;
        }

        Miau.lagManager.setDelayMs(this.delay.getValue().intValue());
    }

    private void flushLag() {
        if (this.isLagging) {
            Miau.lagManager.fastTrackSet = this.packetFastTrack;
            Miau.lagManager.packetQueue.forEach(lagPacket -> this.packetFastTrack.add(lagPacket.packet));
            Miau.lagManager.resetDelay();
            this.tickIndex = -1;
            this.delayCounter = 0L;
            this.isLagging = false;
            this.lagStartTime = 0L;
            this.clearIndicator();
        }
    }

    private boolean sameTarget(EntityPlayer nextTarget) {
        return this.currentTarget != null && nextTarget != null
            ? this.currentTarget.func_145782_y() == nextTarget.func_145782_y()
            : this.currentTarget == nextTarget;
    }

    private boolean isMoving() {
        return mc.field_71439_g.field_70701_bs != 0.0F || mc.field_71439_g.field_70702_br != 0.0F;
    }

    private void clearIndicator() {
        this.indicatorFrom = null;
        this.indicatorTo = null;
        this.indicatorStartMs = 0L;
    }

    private void clearAggroState() {
        this.currentTarget = null;
        this.lastDistSq = -1.0;
        this.isLagging = false;
        this.lastSelfHurtTime = 0;
        this.lastTargetHurtTime = 0;
        this.hitMarkedEntityId = -1;
        this.lastSprintState = false;
        this.lastBlockingState = false;
        this.lagStartTime = 0L;
        this.packetFastTrack.clear();
        if (Miau.lagManager.fastTrackSet == this.packetFastTrack) {
            Miau.lagManager.fastTrackSet = null;
        }

        this.clearIndicator();
    }

    private void tickAggressive() {
        Miau.lagManager.resetDelay();
        this.hasTarget = false;
        Scaffold scaffold = (Scaffold)Miau.moduleManager.modules.get(Scaffold.class);
        if (scaffold != null && scaffold.isEnabled()) {
            if (this.isLagging) {
                this.flushLag();
            }
        } else {
            BedNuker bedNuker = (BedNuker)Miau.moduleManager.modules.get(BedNuker.class);
            if ((!bedNuker.isEnabled() || !bedNuker.isReady())
                && !((IAccessorPlayerControllerMP)mc.field_71442_b).getIsHittingBlock()
                && (!mc.field_71439_g.func_71039_bw() || mc.field_71439_g.func_70632_aY())) {
                boolean weaponOk = !this.weaponsOnly.getValue()
                    || ItemUtil.hasRawUnbreakingEnchant()
                    || this.allowTools.getValue() && ItemUtil.isHoldingTool();
                if (!weaponOk) {
                    if (this.isLagging) {
                        this.flushLag();
                    }
                } else {
                    List<EntityPlayer> players = mc.field_71441_e
                        .field_72996_f
                        .stream()
                        .filter(entity -> entity instanceof EntityPlayer)
                        .map(entity -> (EntityPlayer)entity)
                        .filter(this::isValidTarget)
                        .collect(Collectors.toList());
                    if (players.isEmpty()) {
                        if (this.isLagging) {
                            this.flushLag();
                        }

                        this.currentTarget = null;
                        this.lastDistSq = -1.0;
                    } else {
                        double rangeSq = this.range.getValue().floatValue();
                        EntityPlayer nextTarget = this.getMouseOverTarget(rangeSq);
                        double closestDist = Double.MAX_VALUE;
                        if (nextTarget == null) {
                            Vec3 eyePos = mc.field_71439_g.func_174824_e(1.0F);

                            for (EntityPlayer player : players) {
                                double dist = RotationUtil.distanceToBox(player, eyePos);
                                if (dist < closestDist) {
                                    closestDist = dist;
                                    nextTarget = player;
                                }
                            }
                        } else {
                            closestDist = RotationUtil.distanceToBox(nextTarget, mc.field_71439_g.func_174824_e(1.0F));
                        }

                        if (nextTarget != null && !(closestDist > rangeSq)) {
                            if (!this.sameTarget(nextTarget)) {
                                if (this.isLagging) {
                                    this.flushLag();
                                }

                                this.lastDistSq = -1.0;
                                this.hitMarkedEntityId = -1;
                                this.lastTargetHurtTime = nextTarget.field_70737_aN;
                            }

                            this.currentTarget = nextTarget;
                            double dist = closestDist;
                            int selfHurtTime = mc.field_71439_g.field_70737_aN;
                            int targetHurtTime = this.currentTarget.field_70737_aN;
                            boolean moving = this.isMoving();
                            if (this.isLagging) {
                                if (dist > this.range.getValue().floatValue()) {
                                    this.flushLag();
                                    this.lastDistSq = dist;
                                    this.lastTargetHurtTime = targetHurtTime;
                                } else {
                                    if (this.lastDistSq >= 0.0 && dist >= this.lastDistSq) {
                                        boolean hitHold = this.hitMarkedEntityId == this.currentTarget.func_145782_y()
                                            && dist <= 3.0
                                            && selfHurtTime == 0;
                                        if (!hitHold) {
                                            this.flushLag();
                                            this.lastDistSq = dist;
                                            this.lastTargetHurtTime = targetHurtTime;
                                            return;
                                        }
                                    }

                                    if (selfHurtTime > this.lastSelfHurtTime) {
                                        this.flushLag();
                                        this.hitMarkedEntityId = -1;
                                        this.lastSelfHurtTime = selfHurtTime;
                                        this.lastDistSq = dist;
                                        this.lastTargetHurtTime = targetHurtTime;
                                    } else {
                                        this.lastSelfHurtTime = selfHurtTime;
                                        if (!weaponOk) {
                                            this.flushLag();
                                            this.lastDistSq = dist;
                                            this.lastTargetHurtTime = targetHurtTime;
                                        } else {
                                            if (this.sprintReset.getValue()) {
                                                boolean sprintingNow = mc.field_71439_g.func_70051_ag();
                                                if (sprintingNow && !this.lastSprintState) {
                                                    this.flushLag();
                                                    this.lastSprintState = sprintingNow;
                                                    this.lastDistSq = dist;
                                                    this.lastTargetHurtTime = targetHurtTime;
                                                    return;
                                                }

                                                this.lastSprintState = sprintingNow;
                                            }

                                            if (this.blockSword.getValue()) {
                                                boolean blockingNow = mc.field_71439_g.func_70632_aY();
                                                if (blockingNow && !this.lastBlockingState) {
                                                    this.flushLag();
                                                    this.lastBlockingState = blockingNow;
                                                    this.lastDistSq = dist;
                                                    this.lastTargetHurtTime = targetHurtTime;
                                                    return;
                                                }

                                                this.lastBlockingState = blockingNow;
                                            }

                                            if (this.splashPotion.getValue() && mc.field_71439_g.func_71039_bw()) {
                                                ItemStack held = mc.field_71439_g.func_70694_bm();
                                                if (held != null
                                                    && held.func_77973_b() instanceof ItemPotion
                                                    && ItemPotion.func_77831_g(held.func_77960_j())) {
                                                    this.flushLag();
                                                    this.lastDistSq = dist;
                                                    this.lastTargetHurtTime = targetHurtTime;
                                                    return;
                                                }
                                            }

                                            long elapsedMs = System.currentTimeMillis() - this.lagStartTime;
                                            if (elapsedMs >= this.delay.getValue().intValue()) {
                                                this.flushLag();
                                                this.lastDistSq = dist;
                                                this.lastTargetHurtTime = targetHurtTime;
                                            } else {
                                                this.lastDistSq = dist;
                                                this.lastTargetHurtTime = targetHurtTime;
                                                this.startLag();
                                                this.hasTarget = true;
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (selfHurtTime > this.lastSelfHurtTime) {
                                    this.hitMarkedEntityId = -1;
                                }

                                this.lastSelfHurtTime = selfHurtTime;
                                this.lastSprintState = mc.field_71439_g.func_70051_ag();
                                this.lastBlockingState = mc.field_71439_g.func_70632_aY();
                                if (selfHurtTime == 0 && this.lastTargetHurtTime == 0 && targetHurtTime > 0) {
                                    this.hitMarkedEntityId = this.currentTarget.func_145782_y();
                                }

                                this.lastTargetHurtTime = targetHurtTime;
                                boolean closing = this.lastDistSq >= 0.0 && dist < this.lastDistSq;
                                boolean outsideMinDist = dist > 3.0;
                                boolean hitMarkedHere = this.hitMarkedEntityId == this.currentTarget.func_145782_y();
                                boolean hitStart = hitMarkedHere
                                    && dist <= 3.0
                                    && selfHurtTime == 0
                                    && moving
                                    && weaponOk;
                                this.lastDistSq = dist;
                                boolean shouldStartLag = selfHurtTime == 0
                                    && weaponOk
                                    && moving
                                    && (closing && outsideMinDist || hitStart);
                                if (shouldStartLag) {
                                    this.startLag();
                                    this.hasTarget = true;
                                }
                            }
                        } else {
                            if (this.isLagging) {
                                this.flushLag();
                            }

                            this.currentTarget = null;
                            this.lastDistSq = -1.0;
                        }
                    }
                }
            } else {
                if (this.isLagging) {
                    this.flushLag();
                }
            }
        }
    }

    @EventTarget(3)
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    if (this.mode.getValue() == 1) {
                        this.tickAggressive();
                    } else {
                        this.tickOldMiau();
                    }
                    break;
                case POST:
                    Vec3 savedPosition = Miau.lagManager.getLastPosition();
                    if (this.currentPosition == null) {
                        this.lastPosition = savedPosition;
                    } else {
                        this.lastPosition = this.currentPosition;
                    }

                    this.currentPosition = savedPosition;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getType() != EventType.SEND) {
                return;
            }

            if (this.mode.getValue() == 1) {
                Packet<?> packet = event.getPacket();
                if (this.shouldResetOnPacket(packet)) {
                    if (this.isLagging) {
                        this.flushLag();
                    }

                    Miau.lagManager.resetDelay();
                }
            } else if (this.shouldResetOnPacket(event.getPacket())) {
                Miau.lagManager.setDelay(0);
                this.tickIndex = -1;
            }
        }
    }

    @EventTarget(1)
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()
            && this.hasTarget
            && this.lastPosition != null
            && this.currentPosition != null
            && this.showPosition.getValue() != 0
            && mc.field_71474_y.field_74320_O != 0) {
            Color color = new Color(-1);
            switch (this.showPosition.getValue()) {
                case 1:
                    color = TeamUtil.getTeamColor(mc.field_71439_g, 1.0F);
                    break;
                case 2:
                    color = ((HUD)Miau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            }

            this.renderBox(event, color);
        }
    }

    private void renderBox(Render3DEvent event, Color color) {
        double x = RenderUtil.lerpDouble(
            this.currentPosition.field_72450_a, this.lastPosition.field_72450_a, event.getPartialTicks()
        );
        double y = RenderUtil.lerpDouble(
            this.currentPosition.field_72448_b, this.lastPosition.field_72448_b, event.getPartialTicks()
        );
        double z = RenderUtil.lerpDouble(
            this.currentPosition.field_72449_c, this.lastPosition.field_72449_c, event.getPartialTicks()
        );
        float size = mc.field_71439_g.func_70111_Y();
        AxisAlignedBB aabb = new AxisAlignedBB(
                x - mc.field_71439_g.field_70130_N / 2.0,
                y,
                z - mc.field_71439_g.field_70130_N / 2.0,
                x + mc.field_71439_g.field_70130_N / 2.0,
                y + mc.field_71439_g.field_70131_O,
                z + mc.field_71439_g.field_70130_N / 2.0
            )
            .func_72314_b(size, size, size)
            .func_72317_d(
                -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX(),
                -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY(),
                -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
            );
        RenderUtil.enableRenderState();
        RenderUtil.drawFilledBox(aabb, color.getRed(), color.getGreen(), color.getBlue());
        RenderUtil.disableRenderState();
    }

    @Override
    public void onDisabled() {
        if (this.mode.getValue() == 1) {
            this.flushLag();
            this.clearAggroState();
        }

        if (this.mode.getValue() == 2) {
            Miau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
        }

        Miau.lagManager.setDelay(0);
        this.tickIndex = -1;
        this.delayCounter = 0L;
        this.hasTarget = false;
        this.lastPosition = null;
        this.currentPosition = null;
    }

    @Override
    public String[] getSuffix() {
        return this.mode.getValue() == 2
            ? new String[]{this.blinkTick.getValue().toString()}
            : new String[]{String.format("%dms", this.delay.getValue())};
    }
}
