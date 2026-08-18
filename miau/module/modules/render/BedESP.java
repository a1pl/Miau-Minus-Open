package miau.module.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.render.RenderUtil;
import miau.util.render.SharedBlockHighlightCache;
import miau.util.render.Themes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class BedESP extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final float DEFENSE_AUTO_SCALE_THRESHOLD = 8.0F;
    public final FloatProperty range = new FloatProperty("Range", 10.0F, 2.0F, 200.0F);
    public final FloatProperty scanSpeed = new FloatProperty("Scan speed", 8.0F, 1.0F, 32.0F);
    public final BooleanProperty firstBed = new BooleanProperty("Only render first bed", false);
    public final BooleanProperty renderFullBlock = new BooleanProperty("Render full block", false);
    public final BooleanProperty showExposedOutline = new BooleanProperty("Exposed outline", false);
    public final BooleanProperty showDefenseLayers = new BooleanProperty("Show defense layers", false);
    public final BooleanProperty showDefenseTools = new BooleanProperty("Show break tools", false);
    public final BooleanProperty showDefenseCounts = new BooleanProperty("Show defense counts", true);
    public final FloatProperty defenseHeight = new FloatProperty("Defense height", 0.6F, 0.1F, 3.0F);
    public final FloatProperty defenseScale = new FloatProperty("Defense scale", 1.0F, 0.1F, 2.0F);
    public final BooleanProperty defenseAutoScale = new BooleanProperty("Auto Scale", false);
    private boolean lastDefenseToolMode;
    private final List<BlockPos[]> lastRenderedBedPairs = new ArrayList<>();
    private static final int DEFENSE_ICON_SIZE = 16;
    private static final int DEFENSE_ICON_SPACING = 18;
    private static final int DEFENSE_PADDING = 3;
    private static final float DEFENSE_BACKGROUND_RADIUS = 8.0F;
    private static final int DEFENSE_BACKGROUND_COLOR = 1929379840;
    private static final int DEFENSE_BACKGROUND_OUTLINE_COLOR = -1778384896;
    private static final int DEFENSE_MAX_LAYERS = 5;
    private static final float DEFENSE_AIR_RATIO_THRESHOLD = 0.2F;
    private static final float DEFENSE_BLOCK_RATIO_THRESHOLD = 0.2F;
    private static final EnumMap<EnumFacing, BedESP.LayerOffsets[]> DEFENSE_OFFSETS = buildLayerOffsetsByFacing();
    private final List<BlockPos[]> activeBedPairs = new ArrayList<>();
    private final Map<Long, BedESP.DefenseOverlaySnapshot> defenseSnapshots = new HashMap<>();
    private final Map<Long, BedESP.DefenseWatchRegion> defenseWatchRegions = new HashMap<>();
    private final Map<Long, Set<Long>> watchedBedsByDefensePos = new HashMap<>();
    private final Map<Long, Set<Long>> watchedBedsByChunk = new HashMap<>();
    private final Set<Long> dirtyDefenseBeds = new HashSet<>();
    private final SharedBlockHighlightCache.UpdateListener defenseUpdateListener = new SharedBlockHighlightCache.UpdateListener(
        
    ) {
        @Override
        public void onBlockChanged(BlockPos pos, IBlockState newState) {
            BedESP.this.markBedsDirtyForDefensePos(pos);
        }

        @Override
        public void onChunkQueued(int chunkX, int chunkZ) {
            BedESP.this.markBedsDirtyForChunk(chunkX, chunkZ);
        }

        @Override
        public void onChunkRemoved(int chunkX, int chunkZ) {
            BedESP.this.markBedsDirtyForChunk(chunkX, chunkZ);
        }

        @Override
        public void onCacheCleared() {
            BedESP.this.clearDefenseState();
        }
    };

    public BedESP() {
        super("BedESP", false);
    }

    @Override
    public void onEnabled() {
        SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
        cache.addUpdateListener(this.defenseUpdateListener);
        cache.attachBed();
        cache.enqueueLoadedChunks();
        if (mc.field_71438_f != null) {
            mc.field_71438_f.func_72712_a();
        }
    }

    @Override
    public void onDisabled() {
        SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
        cache.removeUpdateListener(this.defenseUpdateListener);
        cache.detachBed();
        this.activeBedPairs.clear();
        this.lastRenderedBedPairs.clear();
        this.lastDefenseToolMode = false;
        this.clearDefenseState();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent e) {
        this.activeBedPairs.clear();
        this.lastRenderedBedPairs.clear();
        this.lastDefenseToolMode = false;
        this.clearDefenseState();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE) {
            SharedBlockHighlightCache.get().handlePacket(event);
        }
    }

    public int getScanSpeedBudget() {
        return this.isEnabled() ? this.scanSpeed.getValue().intValue() : 0;
    }

    @EventTarget(1)
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.isEnabled()) {
                if (mc.field_71441_e != null && mc.field_71439_g != null) {
                    SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
                    cache.tickScan(this.getScanSpeedBudget());
                    double rangeSq = this.range.getValue() * this.range.getValue();
                    double px = mc.field_71439_g.field_70165_t;
                    double py = mc.field_71439_g.field_70163_u;
                    double pz = mc.field_71439_g.field_70161_v;
                    List<BlockPos[]> candidatePairs = this.collectActiveBedPairs(cache, px, py, pz, rangeSq);
                    this.activeBedPairs.clear();

                    for (BlockPos[] pair : candidatePairs) {
                        this.activeBedPairs.add(copyBedPair(pair));
                    }

                    boolean defenseToolMode = this.showDefenseTools.getValue();
                    if (this.lastDefenseToolMode != defenseToolMode) {
                        this.clearDefenseState();
                        this.lastDefenseToolMode = defenseToolMode;
                    }

                    if (!this.showDefenseLayers.getValue()) {
                        this.clearDefenseState();
                    } else {
                        Set<Long> activeFeet = new HashSet<>();

                        for (BlockPos[] pair : candidatePairs) {
                            BlockPos foot = pair[0];
                            BlockPos head = pair[1];
                            long footKey = foot.func_177986_g();
                            activeFeet.add(footKey);
                            BedESP.DefenseWatchRegion region = this.defenseWatchRegions.get(footKey);
                            if (region == null || !region.matches(head)) {
                                this.unregisterDefenseWatch(footKey);
                                this.registerDefenseWatch(foot, head);
                                this.dirtyDefenseBeds.add(footKey);
                            }

                            BedESP.DefenseOverlaySnapshot snapshot = this.defenseSnapshots.get(footKey);
                            if (snapshot == null || !snapshot.matches(head) || this.dirtyDefenseBeds.remove(footKey)) {
                                this.defenseSnapshots.put(footKey, this.computeDefenseSnapshot(foot, head));
                            }
                        }

                        for (Long footKey : new ArrayList<>(this.defenseWatchRegions.keySet())) {
                            if (!activeFeet.contains(footKey)) {
                                this.unregisterDefenseWatch(footKey);
                            }
                        }

                        this.dirtyDefenseBeds.retainAll(activeFeet);
                    }
                } else {
                    this.activeBedPairs.clear();
                    this.clearDefenseState();
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        if (!this.isEnabled()) {
            return new String[0];
        }

        int n = SharedBlockHighlightCache.get().totalBedFeet();
        return n > 0 ? new String[]{String.valueOf(n)} : new String[0];
    }

    @EventTarget(4)
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            if (mc.field_71441_e != null && mc.field_71439_g != null) {
                float blockHeight = this.getBlockHeight();
                double rangeSq = this.range.getValue() * this.range.getValue();
                double px = mc.field_71439_g.field_70165_t;
                double py = mc.field_71439_g.field_70163_u;
                double pz = mc.field_71439_g.field_70161_v;
                List<BlockPos[]> pairsToRender = new ArrayList<>();
                Set<BlockPos> addedFeet = new HashSet<>();

                for (BlockPos[] pair : this.activeBedPairs) {
                    BlockPos foot = pair[0];
                    AxisAlignedBB bb = bedWorldBounds(pair[0], pair[1], blockHeight);
                    if (RenderUtil.isInViewFrustum(bb) && addedFeet.add(foot)) {
                        pairsToRender.add(copyBedPair(pair));
                    }
                }

                for (BlockPos[] prev : new ArrayList<>(this.lastRenderedBedPairs)) {
                    if (prev != null && prev.length >= 2) {
                        BlockPos foot = prev[0];
                        BlockPos head = prev[1];
                        if (!addedFeet.contains(foot) && this.stillHasRenderableBed(prev)) {
                            double dx = foot.func_177958_n() + 0.5 - px;
                            double dy = foot.func_177956_o() + 0.5 - py;
                            double dz = foot.func_177952_p() + 0.5 - pz;
                            if (!(dx * dx + dy * dy + dz * dz > rangeSq)) {
                                AxisAlignedBB bb = bedWorldBounds(foot, head, blockHeight);
                                if (RenderUtil.isInViewFrustum(bb)) {
                                    pairsToRender.add(copyBedPair(prev));
                                    addedFeet.add(foot);
                                }
                            }
                        }
                    }
                }

                for (BlockPos[] pair : pairsToRender) {
                    this.renderBed(pair, blockHeight);
                    if (this.showDefenseLayers.getValue() && this.isLiveBedPair(pair)) {
                        this.renderDefenseOverlay(pair, blockHeight);
                    }
                }

                this.lastRenderedBedPairs.clear();

                for (BlockPos[] pair : pairsToRender) {
                    this.lastRenderedBedPairs.add(copyBedPair(pair));
                }
            }
        }
    }

    private List<BlockPos[]> collectActiveBedPairs(
        SharedBlockHighlightCache cache, double px, double py, double pz, double rangeSq
    ) {
        List<BlockPos[]> candidatePairs = new ArrayList<>();

        for (Entry<Long, Set<BlockPos>> chunk : cache.entriesBedFeet()) {
            for (BlockPos foot : chunk.getValue()) {
                double dx = foot.func_177958_n() + 0.5 - px;
                double dy = foot.func_177956_o() + 0.5 - py;
                double dz = foot.func_177952_p() + 0.5 - pz;
                if (!(dx * dx + dy * dy + dz * dz > rangeSq)) {
                    BlockPos[] pair = this.footAndHead(foot);
                    if (pair != null && this.stillHasRenderableBed(pair)) {
                        candidatePairs.add(copyBedPair(pair));
                    }
                }
            }
        }

        if (this.firstBed.getValue() && candidatePairs.size() > 1) {
            BlockPos[] nearest = null;
            double nearestDistanceSq = Double.MAX_VALUE;

            for (BlockPos[] pair : candidatePairs) {
                double dx = pair[0].func_177958_n() + 0.5 - px;
                double dy = pair[0].func_177956_o() + 0.5 - py;
                double dz = pair[0].func_177952_p() + 0.5 - pz;
                double distanceSq = dx * dx + dy * dy + dz * dz;
                if (distanceSq < nearestDistanceSq) {
                    nearestDistanceSq = distanceSq;
                    nearest = pair;
                }
            }

            candidatePairs.clear();
            if (nearest != null) {
                candidatePairs.add(nearest);
            }

            return candidatePairs;
        } else {
            return candidatePairs;
        }
    }

    private static BlockPos[] copyBedPair(BlockPos[] pair) {
        return new BlockPos[]{new BlockPos(pair[0]), new BlockPos(pair[1])};
    }

    private static boolean isBedFoot(IBlockState st) {
        return st != null
            && st.func_177230_c() instanceof BlockBed
            && st.func_177229_b(BlockBed.field_176472_a) == EnumPartType.FOOT;
    }

    private boolean stillHasRenderableBed(BlockPos[] pair) {
        if (pair != null && pair.length >= 2 && mc.field_71441_e != null) {
            IBlockState a = mc.field_71441_e.func_180495_p(pair[0]);
            IBlockState b = mc.field_71441_e.func_180495_p(pair[1]);
            return a != null && a.func_177230_c() instanceof BlockBed
                || b != null && b.func_177230_c() instanceof BlockBed;
        } else {
            return false;
        }
    }

    private boolean isLiveBedPair(BlockPos[] pair) {
        if (pair != null && pair.length >= 2 && mc.field_71441_e != null) {
            IBlockState footState = mc.field_71441_e.func_180495_p(pair[0]);
            IBlockState headState = mc.field_71441_e.func_180495_p(pair[1]);
            return isBedFoot(footState) && headState != null && headState.func_177230_c() instanceof BlockBed;
        } else {
            return false;
        }
    }

    private static AxisAlignedBB bedWorldBounds(BlockPos foot, BlockPos head, float height) {
        int fx = foot.func_177958_n();
        int fy = foot.func_177956_o();
        int fz = foot.func_177952_p();
        double h = fy + height;
        if (foot.func_177958_n() != head.func_177958_n()) {
            return foot.func_177958_n() > head.func_177958_n()
                ? new AxisAlignedBB(fx - 1.0, fy, fz, fx + 1.0, h, fz + 1.0)
                : new AxisAlignedBB(fx, fy, fz, fx + 2.0, h, fz + 1.0);
        } else {
            return foot.func_177952_p() > head.func_177952_p()
                ? new AxisAlignedBB(fx, fy, fz - 1.0, fx + 1.0, h, fz + 1.0)
                : new AxisAlignedBB(fx, fy, fz, fx + 1.0, h, fz + 2.0);
        }
    }

    private BlockPos[] footAndHead(BlockPos foot) {
        IBlockState st = mc.field_71441_e.func_180495_p(foot);
        if (!(st.func_177230_c() instanceof BlockBed)) {
            return null;
        }

        EnumFacing facing = (EnumFacing)st.func_177229_b(BlockBed.field_176387_N);
        return new BlockPos[]{foot, foot.func_177972_a(facing)};
    }

    private void renderBed(BlockPos[] blocks, float height) {
        boolean exposed = this.showExposedOutline.getValue() && this.isBedExposed(blocks);
        double x = blocks[0].func_177958_n() - mc.func_175598_ae().field_78730_l;
        double y = blocks[0].func_177956_o() - mc.func_175598_ae().field_78731_m;
        double z = blocks[0].func_177952_p() - mc.func_175598_ae().field_78728_n;
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glLineWidth(2.0F);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        Color themeColor = Themes.getCurrentTheme().getAccentColor();
        float drawA = 0.2509804F;
        float r = themeColor.getRed() / 255.0F;
        float g = themeColor.getGreen() / 255.0F;
        float b = themeColor.getBlue() / 255.0F;
        GL11.glColor4d(r, g, b, drawA);
        AxisAlignedBB axisAlignedBB;
        if (blocks[0].func_177958_n() != blocks[1].func_177958_n()) {
            if (blocks[0].func_177958_n() > blocks[1].func_177958_n()) {
                axisAlignedBB = new AxisAlignedBB(x - 1.0, y, z, x + 1.0, y + height, z + 1.0);
            } else {
                axisAlignedBB = new AxisAlignedBB(x, y, z, x + 2.0, y + height, z + 1.0);
            }
        } else if (blocks[0].func_177952_p() > blocks[1].func_177952_p()) {
            axisAlignedBB = new AxisAlignedBB(x, y, z - 1.0, x + 1.0, y + height, z + 1.0);
        } else {
            axisAlignedBB = new AxisAlignedBB(x, y, z, x + 1.0, y + height, z + 2.0);
        }

        RenderUtil.drawBoundingBox(axisAlignedBB, r, g, b, drawA);
        if (exposed) {
            Color outlineColor = Themes.getCurrentTheme().getAccentColor();
            GL11.glLineWidth(3.0F);
            GL11.glColor4f(
                outlineColor.getRed() / 255.0F, outlineColor.getGreen() / 255.0F, outlineColor.getBlue() / 255.0F, 1.0F
            );
            RenderGlobal.func_181561_a(axisAlignedBB);
            GL11.glLineWidth(2.0F);
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
    }

    private boolean isBedExposed(BlockPos[] pair) {
        if (pair != null && pair.length >= 2 && mc.field_71441_e != null) {
            for (BlockPos bedPart : pair) {
                for (EnumFacing side : EnumFacing.values()) {
                    BlockPos neighbor = bedPart.func_177972_a(side);
                    Block neighborBlock = mc.field_71441_e.func_180495_p(neighbor).func_177230_c();
                    if (neighborBlock == Blocks.field_150350_a) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private void renderDefenseOverlay(BlockPos[] blocks, float blockHeight) {
        BedESP.DefenseOverlaySnapshot snapshot = this.defenseSnapshots.get(blocks[0].func_177986_g());
        if (snapshot != null && snapshot.matches(blocks[1]) && !snapshot.entries.isEmpty()) {
            RenderManager renderManager = mc.func_175598_ae();
            FontRenderer fontRenderer = mc.field_71466_p;
            if (renderManager != null && fontRenderer != null) {
                AxisAlignedBB bedBounds = bedWorldBounds(blocks[0], blocks[1], blockHeight);
                double x = (bedBounds.field_72340_a + bedBounds.field_72336_d) * 0.5 - renderManager.field_78730_l;
                double y = bedBounds.field_72337_e
                    + this.defenseHeight.getValue().floatValue()
                    - renderManager.field_78731_m;
                double z = (bedBounds.field_72339_c + bedBounds.field_72334_f) * 0.5 - renderManager.field_78728_n;
                float renderScale = this.computeDefenseBaseScaleValue();
                if (this.defenseAutoScale.getValue()) {
                    float distance = (float)Math.sqrt(x * x + y * y + z * z);
                    renderScale = this.computeDefenseScaleValue(distance);
                }

                List<BedESP.DefenseOverlayEntry> stacks = snapshot.entries;
                int contentWidth = stacks.size() * 18 - 2;
                int left = -contentWidth / 2;
                int iconY = -8;
                int backgroundLeft = left - 3;
                int backgroundTop = iconY - 3;
                int backgroundRight = left + contentWidth + 3;
                int backgroundBottom = iconY + 16 + 3;
                GlStateManager.func_179094_E();

                try {
                    GlStateManager.func_179109_b((float)x, (float)y, (float)z);
                    GlStateManager.func_179114_b(-renderManager.field_78735_i, 0.0F, 1.0F, 0.0F);
                    GlStateManager.func_179114_b(renderManager.field_78732_j, 1.0F, 0.0F, 0.0F);
                    GlStateManager.func_179152_a(-renderScale, -renderScale, renderScale);
                    GlStateManager.func_179140_f();
                    GlStateManager.func_179132_a(false);
                    GlStateManager.func_179097_i();
                    GlStateManager.func_179147_l();
                    GlStateManager.func_179120_a(770, 771, 1, 0);
                    this.renderDefenseBackground(backgroundLeft, backgroundTop, backgroundRight, backgroundBottom);
                    this.applyDefenseOverlayTextState();

                    for (int i = 0; i < stacks.size(); i++) {
                        BedESP.DefenseOverlayEntry stackData = stacks.get(i);
                        int iconX = left + i * 18;
                        this.renderDefenseEntry(stackData, iconX, iconY);
                        this.applyDefenseOverlayTextState();
                        if (!this.showDefenseTools.getValue()
                            && this.showDefenseCounts.getValue()
                            && stackData.count > 1) {
                            String countText = String.valueOf(stackData.getCount());
                            fontRenderer.func_175063_a(
                                countText, iconX + 17 - fontRenderer.func_78256_a(countText), iconY + 9, 16777215
                            );
                            this.applyDefenseOverlayTextState();
                        }
                    }
                } finally {
                    GlStateManager.func_179126_j();
                    GlStateManager.func_179132_a(true);
                    GlStateManager.func_179140_f();
                    GlStateManager.func_179101_C();
                    GlStateManager.func_179084_k();
                    GlStateManager.func_179141_d();
                    GlStateManager.func_179098_w();
                    GlStateManager.func_179120_a(770, 771, 1, 0);
                    GL11.glTexEnvi(8960, 8704, 8448);
                    GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                    GlStateManager.func_179121_F();
                }
            }
        }
    }

    private void renderDefenseEntry(BedESP.DefenseOverlayEntry entry, int iconX, int iconY) {
        if (entry != null) {
            if (entry.hasItemStack()) {
                RenderUtil.renderItemAndEffectIntoGui3D(entry.renderStack, iconX, iconY);
            } else {
                if (entry.hasBlockSprite()) {
                    this.renderDefenseBlockSprite(entry.blockSprite, iconX, iconY);
                }
            }
        }
    }

    private void renderDefenseBlockSprite(TextureAtlasSprite sprite, int iconX, int iconY) {
        if (sprite != null) {
            mc.func_110434_K().func_110577_a(TextureMap.field_110575_b);
            GlStateManager.func_179098_w();
            GlStateManager.func_179147_l();
            GlStateManager.func_179141_d();
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            Tessellator tessellator = Tessellator.func_178181_a();
            WorldRenderer worldRenderer = tessellator.func_178180_c();
            worldRenderer.func_181668_a(7, DefaultVertexFormats.field_181707_g);
            worldRenderer.func_181662_b(iconX, iconY + 16, 0.0)
                .func_181673_a(sprite.func_94209_e(), sprite.func_94210_h())
                .func_181675_d();
            worldRenderer.func_181662_b(iconX + 16, iconY + 16, 0.0)
                .func_181673_a(sprite.func_94212_f(), sprite.func_94210_h())
                .func_181675_d();
            worldRenderer.func_181662_b(iconX + 16, iconY, 0.0)
                .func_181673_a(sprite.func_94212_f(), sprite.func_94206_g())
                .func_181675_d();
            worldRenderer.func_181662_b(iconX, iconY, 0.0)
                .func_181673_a(sprite.func_94209_e(), sprite.func_94206_g())
                .func_181675_d();
            tessellator.func_78381_a();
        }
    }

    private void renderDefenseBackground(int left, int top, int right, int bottom) {
        RenderUtil.drawRoundedGradientOutlinedRectangle(
            left, top, right, bottom, 8.0F, 1929379840, -1778384896, -1778384896
        );
    }

    private void applyDefenseOverlayTextState() {
        GlStateManager.func_179140_f();
        GlStateManager.func_179097_i();
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179098_w();
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a(516, 0.1F);
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GL11.glTexEnvi(8960, 8704, 8448);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private BedESP.DefenseOverlaySnapshot computeDefenseSnapshot(BlockPos foot, BlockPos head) {
        BedESP.LayerOffsets[] layers = this.getLayerOffsets(foot, head);
        if (layers == null) {
            return new BedESP.DefenseOverlaySnapshot(new BlockPos(head), Collections.emptyList());
        }

        Map<BedESP.DefenseBlockKey, Integer> finalCounts = new HashMap<>();
        Map<Integer, BedESP.DefenseBlockKey> resolvedBlockKeys = new HashMap<>();
        int sparseLayerCount = 0;
        MutableBlockPos mutablePos = new MutableBlockPos();

        for (BedESP.LayerOffsets layerOffsets : layers) {
            Map<BedESP.DefenseBlockKey, Integer> layerCounts = new HashMap<>();
            int layerTotalBlocks = 0;
            int layerAirBlocks = 0;

            for (BedESP.RelativeOffset offset : layerOffsets.positions) {
                mutablePos.func_181079_c(
                    foot.func_177958_n() + offset.x, foot.func_177956_o() + offset.y, foot.func_177952_p() + offset.z
                );
                if (this.accumulateDefenseBlock(mutablePos, layerCounts, resolvedBlockKeys)) {
                    layerAirBlocks++;
                }

                layerTotalBlocks++;
            }

            if (layerTotalBlocks != 0 && !((float)layerAirBlocks / layerTotalBlocks > 0.2F)) {
                sparseLayerCount = 0;

                for (Entry<BedESP.DefenseBlockKey, Integer> countedBlock : layerCounts.entrySet()) {
                    int count = countedBlock.getValue();
                    if ((float)count / layerTotalBlocks >= 0.2F) {
                        finalCounts.put(
                            countedBlock.getKey(), finalCounts.getOrDefault(countedBlock.getKey(), 0) + count
                        );
                    }
                }
            } else if (++sparseLayerCount >= 2) {
                break;
            }
        }

        List<BedESP.DefenseOverlayEntry> entries;
        if (this.showDefenseTools.getValue()) {
            Set<BedESP.ToolOverlayType> toolTypes = new HashSet<>();

            for (BedESP.DefenseBlockKey countedBlock : finalCounts.keySet()) {
                BedESP.ToolOverlayType toolType = BedESP.ToolOverlayType.fromState(countedBlock.state);
                if (toolType != null) {
                    toolTypes.add(toolType);
                }
            }

            if (toolTypes.contains(BedESP.ToolOverlayType.DIAMOND_PICKAXE)) {
                toolTypes.remove(BedESP.ToolOverlayType.IRON_PICKAXE);
            }

            entries = new ArrayList<>(toolTypes.size());

            for (BedESP.ToolOverlayType toolType : toolTypes) {
                entries.add(new BedESP.DefenseOverlayEntry(toolType.createRenderStack(), null, 1, toolType.sortName));
            }

            entries.sort((left, right) -> left.sortName.compareToIgnoreCase(right.sortName));
        } else {
            entries = new ArrayList<>();

            for (Entry<BedESP.DefenseBlockKey, Integer> countedBlock : finalCounts.entrySet()) {
                entries.add(
                    new BedESP.DefenseOverlayEntry(
                        countedBlock.getKey().createRenderStack(),
                        countedBlock.getKey().blockSprite,
                        countedBlock.getValue(),
                        countedBlock.getKey().sortName
                    )
                );
            }

            entries.sort((left, right) -> {
                int countCompare = Integer.compare(right.count, left.count);
                return countCompare != 0 ? countCompare : left.sortName.compareToIgnoreCase(right.sortName);
            });
        }

        return new BedESP.DefenseOverlaySnapshot(new BlockPos(head), Collections.unmodifiableList(entries));
    }

    private boolean accumulateDefenseBlock(
        BlockPos pos,
        Map<BedESP.DefenseBlockKey, Integer> layerCounts,
        Map<Integer, BedESP.DefenseBlockKey> resolvedBlockKeys
    ) {
        IBlockState state = mc.field_71441_e.func_180495_p(pos);
        if (state != null && state.func_177230_c() != Blocks.field_150350_a) {
            IBlockState normalizedState = normalizeDefenseState(state);
            int stateId = Block.func_176210_f(normalizedState);
            BedESP.DefenseBlockKey key = resolvedBlockKeys.get(stateId);
            if (key == null) {
                key = BedESP.DefenseBlockKey.from(normalizedState, pos, mc.field_71441_e);
                resolvedBlockKeys.put(stateId, key);
            }

            layerCounts.put(key, layerCounts.getOrDefault(key, 0) + 1);
            return false;
        } else {
            return true;
        }
    }

    private BedESP.LayerOffsets[] getLayerOffsets(BlockPos foot, BlockPos head) {
        EnumFacing facing = getBedFacing(foot, head);
        return facing == null ? null : DEFENSE_OFFSETS.get(facing);
    }

    private static EnumMap<EnumFacing, BedESP.LayerOffsets[]> buildLayerOffsetsByFacing() {
        EnumMap<EnumFacing, BedESP.LayerOffsets[]> offsets = new EnumMap<>(EnumFacing.class);

        for (EnumFacing f : EnumFacing.field_176754_o) {
            offsets.put(f, buildLayerOffsets(f));
        }

        return offsets;
    }

    private static BedESP.LayerOffsets[] buildLayerOffsets(EnumFacing canonicalFacing) {
        BlockPos foot = BlockPos.field_177992_a;
        BlockPos head = foot.func_177972_a(canonicalFacing);
        boolean facingZ = canonicalFacing.func_176740_k() == Axis.Z;
        BlockPos firstBedPart = facingZ
            ? (head.func_177952_p() > foot.func_177952_p() ? head : foot)
            : (head.func_177958_n() > foot.func_177958_n() ? head : foot);
        BlockPos secondBedPart = firstBedPart.equals(foot) ? head : foot;
        BlockPos[] bedParts = new BlockPos[]{firstBedPart, secondBedPart};
        BedESP.LayerOffsets[] layers = new BedESP.LayerOffsets[5];
        Set<Long> seenAcrossLayers = new HashSet<>();

        for (int layer = 1; layer <= 5; layer++) {
            List<BedESP.RelativeOffset> offsets = new ArrayList<>();

            for (int bedIndex = 0; bedIndex < bedParts.length; bedIndex++) {
                BlockPos bed = bedParts[bedIndex];
                int outwardOffset = bedIndex == 0 ? layer : -layer;
                int startX = facingZ ? bed.func_177958_n() : bed.func_177958_n() + outwardOffset;
                int startZ = facingZ ? bed.func_177952_p() + outwardOffset : bed.func_177952_p();

                for (int advance = 0; advance <= layer; advance++) {
                    int yOffset = 0;

                    for (int breadth = advance; breadth >= 0; breadth--) {
                        int firstY = bed.func_177956_o() + yOffset;
                        int secondY = firstY;
                        int firstX;
                        int firstZ;
                        int secondX;
                        int secondZ;
                        if (facingZ) {
                            int z = startZ - (bedIndex == 0 ? advance : -advance);
                            firstX = startX - breadth;
                            firstZ = z;
                            secondX = startX + breadth;
                            secondZ = z;
                        } else {
                            int x = startX - (bedIndex == 0 ? advance : -advance);
                            firstX = x;
                            firstZ = startZ - breadth;
                            secondX = x;
                            secondZ = startZ + breadth;
                        }

                        addOffset(offsets, seenAcrossLayers, firstX, firstY, firstZ);
                        addOffset(offsets, seenAcrossLayers, secondX, secondY, secondZ);
                        if (breadth > 0) {
                            yOffset++;
                        }
                    }
                }
            }

            layers[layer - 1] = new BedESP.LayerOffsets(Collections.unmodifiableList(offsets));
        }

        return layers;
    }

    private static void addOffset(List<BedESP.RelativeOffset> offsets, Set<Long> seen, int x, int y, int z) {
        long key = new BlockPos(x, y, z).func_177986_g();
        if (seen.add(key)) {
            offsets.add(new BedESP.RelativeOffset(x, y, z));
        }
    }

    private static EnumFacing getBedFacing(BlockPos foot, BlockPos head) {
        int dx = head.func_177958_n() - foot.func_177958_n();
        int dz = head.func_177952_p() - foot.func_177952_p();

        for (EnumFacing facing : EnumFacing.field_176754_o) {
            if (facing.func_82601_c() == dx && facing.func_82599_e() == dz) {
                return facing;
            }
        }

        return null;
    }

    private void clearDefenseState() {
        this.defenseSnapshots.clear();
        this.defenseWatchRegions.clear();
        this.watchedBedsByDefensePos.clear();
        this.watchedBedsByChunk.clear();
        this.dirtyDefenseBeds.clear();
    }

    private void markBedsDirtyForDefensePos(BlockPos pos) {
        if (pos != null) {
            Set<Long> feet = this.watchedBedsByDefensePos.get(pos.func_177986_g());
            if (feet != null) {
                this.dirtyDefenseBeds.addAll(feet);
            }
        }
    }

    private void markBedsDirtyForChunk(int chunkX, int chunkZ) {
        Set<Long> feet = this.watchedBedsByChunk.get(chunkKey(chunkX, chunkZ));
        if (feet != null) {
            this.dirtyDefenseBeds.addAll(feet);
        }
    }

    private void registerDefenseWatch(BlockPos foot, BlockPos head) {
        BedESP.LayerOffsets[] layers = this.getLayerOffsets(foot, head);
        long footKey = foot.func_177986_g();
        if (layers == null) {
            this.defenseWatchRegions
                .put(
                    footKey,
                    new BedESP.DefenseWatchRegion(head.func_177986_g(), Collections.emptySet(), Collections.emptySet())
                );
        } else {
            Set<Long> watchedPositions = new HashSet<>();
            Set<Long> watchedChunks = new HashSet<>();

            for (BedESP.LayerOffsets layer : layers) {
                for (BedESP.RelativeOffset offset : layer.positions) {
                    BlockPos watchedPos = new BlockPos(
                        foot.func_177958_n() + offset.x,
                        foot.func_177956_o() + offset.y,
                        foot.func_177952_p() + offset.z
                    );
                    long posKey = watchedPos.func_177986_g();
                    if (watchedPositions.add(posKey)) {
                        this.watchedBedsByDefensePos.computeIfAbsent(posKey, ignored -> new HashSet<>()).add(footKey);
                        watchedChunks.add(chunkKey(watchedPos.func_177958_n() >> 4, watchedPos.func_177952_p() >> 4));
                    }
                }
            }

            for (Long chunkKey : watchedChunks) {
                this.watchedBedsByChunk.computeIfAbsent(chunkKey, ignored -> new HashSet<>()).add(footKey);
            }

            this.defenseWatchRegions
                .put(
                    footKey,
                    new BedESP.DefenseWatchRegion(
                        head.func_177986_g(),
                        Collections.unmodifiableSet(watchedPositions),
                        Collections.unmodifiableSet(watchedChunks)
                    )
                );
        }
    }

    private void unregisterDefenseWatch(long footKey) {
        BedESP.DefenseWatchRegion region = this.defenseWatchRegions.remove(footKey);
        this.defenseSnapshots.remove(footKey);
        this.dirtyDefenseBeds.remove(footKey);
        if (region != null) {
            for (Long posKey : region.watchedPositions) {
                Set<Long> feet = this.watchedBedsByDefensePos.get(posKey);
                if (feet != null) {
                    feet.remove(footKey);
                    if (feet.isEmpty()) {
                        this.watchedBedsByDefensePos.remove(posKey);
                    }
                }
            }

            for (Long chunkKey : region.watchedChunks) {
                Set<Long> feet = this.watchedBedsByChunk.get(chunkKey);
                if (feet != null) {
                    feet.remove(footKey);
                    if (feet.isEmpty()) {
                        this.watchedBedsByChunk.remove(chunkKey);
                    }
                }
            }
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long)chunkX << 32 | chunkZ & 4294967295L;
    }

    public float getBlockHeight() {
        return this.renderFullBlock.getValue() ? 1.0F : 0.5625F;
    }

    private float computeDefenseBaseScaleValue() {
        return this.defenseScale.getValue() * 0.02F;
    }

    private float computeDefenseScaleValue(float distance) {
        float baseScale = this.computeDefenseBaseScaleValue();
        float effectiveDistance = Math.max(1.0F, distance);
        float scaledValue = baseScale * (effectiveDistance / 8.0F);
        return Math.max(baseScale, scaledValue);
    }

    private static ItemStack resolveBlockItemStack(IBlockState state, BlockPos pos, World world) {
        Block block = state.func_177230_c();

        try {
            Item item = block.func_180665_b(world, pos);
            if (item == null) {
                return null;
            }

            int meta = item.func_77614_k() ? block.func_176222_j(world, pos) : 0;
            return new ItemStack(item, 1, meta);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static TextureAtlasSprite resolveBlockSprite(IBlockState state) {
        return mc != null && mc.func_175602_ab() != null && mc.func_175602_ab().func_175023_a() != null
            ? mc.func_175602_ab().func_175023_a().func_178122_a(state)
            : null;
    }

    private static IBlockState normalizeDefenseState(IBlockState state) {
        if (state == null) {
            return null;
        } else {
            Block block = state.func_177230_c();
            if (block == Blocks.field_150355_j || block == Blocks.field_150358_i) {
                return Blocks.field_150355_j.func_176223_P();
            } else if (block == Blocks.field_150353_l || block == Blocks.field_150356_k) {
                return Blocks.field_150353_l.func_176223_P();
            } else {
                return block == Blocks.field_150480_ab ? Blocks.field_150480_ab.func_176223_P() : state;
            }
        }
    }

    private static String getFallbackStateName(IBlockState state) {
        String localizedName = state.func_177230_c().func_149732_F();
        if (localizedName != null && !localizedName.isEmpty()) {
            return localizedName;
        } else {
            Object registryName = Block.field_149771_c.func_177774_c(state.func_177230_c());
            if (registryName != null) {
                int meta = state.func_177230_c().func_176201_c(state);
                return meta != 0 ? registryName + ":" + meta : registryName.toString();
            } else {
                return "unknown";
            }
        }
    }

    private static ItemStack fallbackRenderStack(Block block) {
        if (block == Blocks.field_150324_C) {
            return new ItemStack(Items.field_151104_aV);
        }

        try {
            Item item = Item.func_150898_a(block);
            if (item != null) {
                int meta = block.func_176201_c(block.func_176223_P());
                return new ItemStack(item, 1, meta);
            }
        } catch (Exception var3) {
        }

        return new ItemStack(Blocks.field_180401_cv);
    }

    private static final class DefenseBlockKey {
        private final IBlockState state;
        private final String identityKey;
        private final String sortName;
        private final int hashCode;
        private final ItemStack renderStack;
        private final TextureAtlasSprite blockSprite;

        private DefenseBlockKey(
            IBlockState state,
            String identityKey,
            String sortName,
            ItemStack renderStack,
            TextureAtlasSprite blockSprite
        ) {
            this.state = state;
            this.identityKey = identityKey;
            this.sortName = sortName;
            this.renderStack = renderStack;
            this.blockSprite = blockSprite;
            this.hashCode = identityKey.hashCode();
        }

        private static BedESP.DefenseBlockKey from(IBlockState state, BlockPos pos, World world) {
            String fallbackName = BedESP.getFallbackStateName(state);
            ItemStack stack = BedESP.resolveBlockItemStack(state, pos, world);
            String identityKey = resolveIdentityKey(state, stack);
            TextureAtlasSprite sprite = null;
            if (stack == null || stack.func_77973_b() == null) {
                sprite = BedESP.resolveBlockSprite(state);
                if (sprite == null) {
                    stack = BedESP.fallbackRenderStack(state.func_177230_c());
                } else {
                    stack = null;
                }
            }

            String sortName = stack != null && stack.func_77973_b() != null
                ? getSafeDisplayName(stack, fallbackName)
                : fallbackName;
            return new BedESP.DefenseBlockKey(state, identityKey, sortName, stack, sprite);
        }

        private ItemStack createRenderStack() {
            return this.renderStack == null ? null : this.renderStack.func_77946_l();
        }

        private static String resolveIdentityKey(IBlockState state, ItemStack stack) {
            if (stack != null && stack.func_77973_b() != null) {
                return Item.func_150891_b(stack.func_77973_b()) + ":" + stack.func_77960_j();
            }

            Object registryName = Block.field_149771_c.func_177774_c(state.func_177230_c());
            return registryName != null
                ? registryName.toString()
                : Integer.toString(Block.func_149682_b(state.func_177230_c()));
        }

        private static String getSafeDisplayName(ItemStack stack, String fallback) {
            if (stack != null && stack.func_77973_b() != null) {
                try {
                    String displayName = stack.func_82833_r();
                    return displayName != null && !displayName.isEmpty() ? displayName : fallback;
                } catch (Exception ignored) {
                    return fallback;
                }
            } else {
                return fallback;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof BedESP.DefenseBlockKey)) {
                return false;
            }

            BedESP.DefenseBlockKey other = (BedESP.DefenseBlockKey)obj;
            return this.identityKey.equals(other.identityKey);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private static final class DefenseOverlayEntry {
        private final ItemStack renderStack;
        private final TextureAtlasSprite blockSprite;
        private final int count;
        private final String sortName;

        private DefenseOverlayEntry(ItemStack renderStack, TextureAtlasSprite blockSprite, int count, String sortName) {
            this.renderStack = renderStack;
            this.blockSprite = blockSprite;
            this.count = count;
            this.sortName = sortName;
        }

        private boolean hasItemStack() {
            return this.renderStack != null && this.renderStack.func_77973_b() != null;
        }

        private boolean hasBlockSprite() {
            return this.blockSprite != null;
        }

        private int getCount() {
            return this.count;
        }
    }

    private static final class DefenseOverlaySnapshot {
        private final long headKey;
        private final List<BedESP.DefenseOverlayEntry> entries;

        private DefenseOverlaySnapshot(BlockPos head, List<BedESP.DefenseOverlayEntry> entries) {
            this.headKey = head.func_177986_g();
            this.entries = entries;
        }

        private boolean matches(BlockPos otherHead) {
            return otherHead != null && this.headKey == otherHead.func_177986_g();
        }
    }

    private static final class DefenseWatchRegion {
        private final long headKey;
        private final Set<Long> watchedPositions;
        private final Set<Long> watchedChunks;

        private DefenseWatchRegion(long headKey, Set<Long> watchedPositions, Set<Long> watchedChunks) {
            this.headKey = headKey;
            this.watchedPositions = watchedPositions;
            this.watchedChunks = watchedChunks;
        }

        private boolean matches(BlockPos otherHead) {
            return otherHead != null && this.headKey == otherHead.func_177986_g();
        }
    }

    private static final class LayerOffsets {
        private final List<BedESP.RelativeOffset> positions;

        private LayerOffsets(List<BedESP.RelativeOffset> positions) {
            this.positions = positions;
        }
    }

    private static final class RelativeOffset {
        private final int x;
        private final int y;
        private final int z;

        private RelativeOffset(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private enum ToolOverlayType {
        DIAMOND_PICKAXE(Items.field_151046_w, "Diamond Pickaxe"),
        IRON_AXE(Items.field_151036_c, "Iron Axe"),
        IRON_HOE(Items.field_151019_K, "Iron Hoe"),
        IRON_PICKAXE(Items.field_151035_b, "Iron Pickaxe"),
        SHEARS(Items.field_151097_aZ, "Shears"),
        IRON_SHOVEL(Items.field_151037_a, "Iron Shovel"),
        IRON_SWORD(Items.field_151040_l, "Iron Sword");

        private final Item item;
        private final String sortName;
        private final ItemStack renderStack;

        ToolOverlayType(Item item, String sortName) {
            this.item = item;
            this.sortName = sortName;
            this.renderStack = new ItemStack(item);
        }

        private ItemStack createRenderStack() {
            return this.renderStack.func_77946_l();
        }

        private static BedESP.ToolOverlayType fromState(IBlockState state) {
            if (state == null) {
                return null;
            }

            Block block = state.func_177230_c();
            if (block == Blocks.field_150343_Z) {
                return DIAMOND_PICKAXE;
            }

            BedESP.ToolOverlayType bestTool = IRON_PICKAXE;
            float bestEfficiency = 1.0F;

            for (BedESP.ToolOverlayType toolType : values()) {
                if (toolType != DIAMOND_PICKAXE) {
                    float efficiency = getEfficiency(toolType.renderStack, block);
                    if (efficiency > bestEfficiency) {
                        bestEfficiency = efficiency;
                        bestTool = toolType;
                    }
                }
            }

            return bestTool;
        }

        private static float getEfficiency(ItemStack itemStack, Block block) {
            float getStrVsBlock = itemStack.func_150997_a(block);
            if (getStrVsBlock > 1.0F) {
                int getEnchantmentLevel = EnchantmentHelper.func_77506_a(
                    Enchantment.field_77349_p.field_77352_x, itemStack
                );
                if (getEnchantmentLevel > 0) {
                    getStrVsBlock += getEnchantmentLevel * getEnchantmentLevel + 1;
                }
            }

            return getStrVsBlock;
        }
    }
}
