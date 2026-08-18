package miau.module.modules.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import miau.event.EventTarget;
import miau.event.impl.Render3DEvent;
import miau.event.impl.RenderLivingEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorMinecraft;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.Team.EnumVisible;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GLContext;

public class EntityCulling extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty cullingDelay = new IntProperty("Culling Delay", 2, 1, 3);
    public final ModeProperty cullingMode = new ModeProperty("Culling Mode", 0, new String[]{"Grouped", "Custom"});
    public final IntProperty entityCullingDis = new IntProperty(
        "Culling Distance", 45, 10, 150, () -> this.cullingMode.getValue() == 0
    );
    public final IntProperty playerCullingDis = new IntProperty(
        "Player Cull Distance", 45, 10, 150, () -> this.cullingMode.getValue() == 1
    );
    public final IntProperty mobCullingDis = new IntProperty(
        "Mob Cull Distance", 40, 10, 150, () -> this.cullingMode.getValue() == 1
    );
    public final IntProperty passiveCullingDis = new IntProperty(
        "Passive Cull Distance", 30, 10, 150, () -> this.cullingMode.getValue() == 1
    );
    private static final ConcurrentHashMap<UUID, EntityCulling.OcclusionQuery> queries = new ConcurrentHashMap<>();
    private static final boolean SUPPORT_NEW_GL = GLContext.getCapabilities().OpenGL33;
    private int destroyTimer;

    public EntityCulling() {
        super("EntityCulling", false);
    }

    @EventTarget
    public void onRenderLiving(RenderLivingEvent e) {
        if (e.getType() != EventType.POST) {
            EntityLivingBase entity = e.getEntity();
            if (entity != mc.field_71439_g
                && entity.field_70170_p == mc.field_71439_g.field_70170_p
                && !entity.func_98034_c(mc.field_71439_g)) {
                if (checkEntity(entity)) {
                    e.setCancelled(true);
                    if (!canRenderName(entity)) {
                        return;
                    }

                    RenderManager rm = mc.func_175598_ae();
                    double x = entity.field_70142_S
                        + (entity.field_70165_t - entity.field_70142_S)
                            * ((IAccessorMinecraft)mc).getTimer().field_74281_c
                        - ((IAccessorRenderManager)rm).getRenderPosX();
                    double y = entity.field_70137_T
                        + (entity.field_70163_u - entity.field_70137_T)
                            * ((IAccessorMinecraft)mc).getTimer().field_74281_c
                        - ((IAccessorRenderManager)rm).getRenderPosY();
                    double z = entity.field_70136_U
                        + (entity.field_70161_v - entity.field_70136_U)
                            * ((IAccessorMinecraft)mc).getTimer().field_74281_c
                        - ((IAccessorRenderManager)rm).getRenderPosZ();

                    try {
                        Render<Entity> renderer = rm.func_78713_a(entity);
                        if (renderer instanceof RendererLivingEntity) {
                            ((RendererLivingEntity)renderer).func_177067_a(entity, x, y, z);
                        }
                    } catch (Exception var11) {
                    }
                }

                if (entity.func_82150_aj() && entity instanceof EntityPlayer) {
                    e.setCancelled(true);
                }

                float entityDistance = entity.func_70032_d(mc.field_71439_g);
                switch (this.cullingMode.getValue()) {
                    case 0:
                        if (entityDistance > this.entityCullingDis.getValue().intValue()) {
                            e.setCancelled(true);
                        }
                        break;
                    case 1:
                        if (entity instanceof IMob && entityDistance > this.mobCullingDis.getValue().intValue()) {
                            e.setCancelled(true);
                        } else if ((
                                entity instanceof EntityAnimal
                                    || entity instanceof EntityAmbientCreature
                                    || entity instanceof EntityWaterMob
                            )
                            && entityDistance > this.passiveCullingDis.getValue().intValue()) {
                            e.setCancelled(true);
                        } else if (entity instanceof EntityPlayer
                            && entityDistance > this.playerCullingDis.getValue().intValue()) {
                            e.setCancelled(true);
                        }
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        this.check();
    }

    public static boolean canRenderName(EntityLivingBase entity) {
        EntityPlayerSP player = mc.field_71439_g;
        if (entity instanceof EntityPlayer && entity != player) {
            Team otherEntityTeam = entity.func_96124_cp();
            Team playerTeam = player.func_96124_cp();
            if (otherEntityTeam != null) {
                EnumVisible teamVisibilityRule = otherEntityTeam.func_178770_i();
                switch (teamVisibilityRule) {
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return playerTeam == null || otherEntityTeam.func_142054_a(playerTeam);
                    case HIDE_FOR_OWN_TEAM:
                        return playerTeam == null || !otherEntityTeam.func_142054_a(playerTeam);
                    case ALWAYS:
                    default:
                        return true;
                }
            }
        }

        return Minecraft.func_71382_s()
            && entity != mc.func_175598_ae().field_78734_h
            && (entity instanceof EntityArmorStand || !entity.func_98034_c(player))
            && entity.field_70153_n == null;
    }

    @EventTarget
    public void onTickEvent(TickEvent e) {
        if (this.destroyTimer++ >= 120) {
            this.destroyTimer = 0;
            WorldClient theWorld = mc.field_71441_e;
            if (theWorld != null) {
                List<UUID> remove = new ArrayList<>();
                Set<UUID> loaded = new HashSet<>();

                for (Entity entity : theWorld.field_72996_f) {
                    loaded.add(entity.func_110124_au());
                }

                for (EntityCulling.OcclusionQuery value : queries.values()) {
                    if (!loaded.contains(value.uuid)) {
                        remove.add(value.uuid);
                        if (value.nextQuery != 0) {
                            GL15.glDeleteQueries(value.nextQuery);
                        }
                    }
                }

                for (UUID uuid : remove) {
                    queries.remove(uuid);
                }
            }
        }
    }

    private void check() {
        long delay = 0L;
        switch (this.cullingDelay.getValue() - 1) {
            case 0:
                delay = 10L;
                break;
            case 1:
                delay = 25L;
                break;
            case 2:
                delay = 50L;
        }

        long nanoTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

        for (EntityCulling.OcclusionQuery query : queries.values()) {
            if (query.nextQuery != 0) {
                long queryObject = GL15.glGetQueryObjecti(query.nextQuery, 34919);
                if (queryObject != 0L) {
                    query.occluded = GL15.glGetQueryObjecti(query.nextQuery, 34918) == 0;
                    GL15.glDeleteQueries(query.nextQuery);
                    query.nextQuery = 0;
                }
            }

            if (query.nextQuery == 0 && nanoTime - query.executionTime > delay) {
                query.executionTime = nanoTime;
                query.refresh = true;
            }
        }
    }

    private static boolean checkEntity(Entity entity) {
        EntityCulling.OcclusionQuery query = queries.computeIfAbsent(
            entity.func_110124_au(), EntityCulling.OcclusionQuery::new
        );
        if (query.refresh) {
            query.nextQuery = getQuery();
            query.refresh = false;
            int mode = SUPPORT_NEW_GL ? 35887 : 35092;
            GL15.glBeginQuery(mode, query.nextQuery);
            drawSelectionBoundingBox(
                entity.func_174813_aQ()
                    .func_72314_b(0.2, 0.2, 0.2)
                    .func_72317_d(
                        -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX(),
                        -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY(),
                        -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
                    )
            );
            GL15.glEndQuery(mode);
        }

        return query.occluded;
    }

    public static void drawSelectionBoundingBox(AxisAlignedBB b) {
        GlStateManager.func_179118_c();
        GlStateManager.func_179129_p();
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179135_a(false, false, false, false);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(8, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(b.field_72336_d, b.field_72337_e, b.field_72334_f).func_181675_d();
        worldrenderer.func_181662_b(b.field_72336_d, b.field_72337_e, b.field_72339_c).func_181675_d();
        worldrenderer.func_181662_b(b.field_72340_a, b.field_72337_e, b.field_72334_f).func_181675_d();
        worldrenderer.func_181662_b(b.field_72340_a, b.field_72337_e, b.field_72339_c).func_181675_d();
        worldrenderer.func_181662_b(b.field_72340_a, b.field_72338_b, b.field_72334_f).func_181675_d();
        worldrenderer.func_181662_b(b.field_72340_a, b.field_72338_b, b.field_72339_c).func_181675_d();
        worldrenderer.func_181662_b(b.field_72340_a, b.field_72337_e, b.field_72339_c).func_181675_d();
        worldrenderer.func_181662_b(b.field_72340_a, b.field_72338_b, b.field_72339_c).func_181675_d();
        worldrenderer.func_181662_b(b.field_72336_d, b.field_72337_e, b.field_72339_c).func_181675_d();
        worldrenderer.func_181662_b(b.field_72336_d, b.field_72338_b, b.field_72339_c).func_181675_d();
        worldrenderer.func_181662_b(b.field_72336_d, b.field_72337_e, b.field_72334_f).func_181675_d();
        worldrenderer.func_181662_b(b.field_72336_d, b.field_72338_b, b.field_72334_f).func_181675_d();
        worldrenderer.func_181662_b(b.field_72340_a, b.field_72337_e, b.field_72334_f).func_181675_d();
        worldrenderer.func_181662_b(b.field_72340_a, b.field_72338_b, b.field_72334_f).func_181675_d();
        worldrenderer.func_181662_b(b.field_72340_a, b.field_72338_b, b.field_72334_f).func_181675_d();
        worldrenderer.func_181662_b(b.field_72336_d, b.field_72338_b, b.field_72334_f).func_181675_d();
        worldrenderer.func_181662_b(b.field_72340_a, b.field_72338_b, b.field_72339_c).func_181675_d();
        worldrenderer.func_181662_b(b.field_72336_d, b.field_72338_b, b.field_72339_c).func_181675_d();
        tessellator.func_78381_a();
        GlStateManager.func_179132_a(true);
        GlStateManager.func_179135_a(true, true, true, true);
        GlStateManager.func_179141_d();
        GlStateManager.func_179089_o();
    }

    private static int getQuery() {
        try {
            return GL15.glGenQueries();
        } catch (Throwable throwable) {
            return 0;
        }
    }

    static class OcclusionQuery {
        private final UUID uuid;
        private int nextQuery;
        private boolean refresh = true;
        private boolean occluded;
        private long executionTime = 0L;

        public OcclusionQuery(UUID uuid) {
            this.uuid = uuid;
        }
    }
}
