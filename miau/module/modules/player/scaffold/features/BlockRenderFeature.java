package miau.module.modules.player.scaffold.features;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import miau.Miau;
import miau.event.impl.Render3DEvent;
import miau.module.modules.player.Scaffold;
import miau.module.modules.player.scaffold.ScaffoldComponent;
import miau.module.modules.render.HUD;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BlockRenderFeature implements ScaffoldComponent {
    private final Scaffold scaffold;
    public final ModeProperty espMode = new ModeProperty("ESP", 0, new String[]{"Default", "HUD", "None"});
    public final BooleanProperty raytrace = new BooleanProperty("Raytrace", false, () -> this.espMode.getValue() != 2);
    public final IntProperty alpha = new IntProperty(
        "Alpha", 200, 0, 255, () -> this.espMode.getValue() != 2 && this.raytrace.getValue()
    );
    public final BooleanProperty outline = new BooleanProperty("Outline", true, () -> this.espMode.getValue() != 2);
    public final BooleanProperty shade = new BooleanProperty("Shade", false, () -> this.espMode.getValue() != 2);
    private final Map<BlockPos, Long> highlight = new HashMap<>();
    private MovingObjectPosition lastESPRaytrace = null;

    public BlockRenderFeature(Scaffold scaffold) {
        this.scaffold = scaffold;
    }

    @Override
    public List<Property<?>> getProperties() {
        return Arrays.asList(this.espMode, this.raytrace, this.alpha, this.outline, this.shade);
    }

    @Override
    public void onEnable() {
        this.highlight.clear();
        this.lastESPRaytrace = null;
    }

    @Override
    public void onDisable() {
        this.highlight.clear();
    }

    public void markPlaced(BlockPos pos) {
        if (this.espMode.getValue() != 2 && !this.raytrace.getValue()) {
            this.highlight.put(pos, System.currentTimeMillis());
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (this.espMode.getValue() != 2) {
            HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
            int themeColor = this.espMode.getValue() == 1 ? hud.getColor(0L).getRGB() : Color.CYAN.getRGB();
            if (!this.highlight.isEmpty()) {
                Iterator<Entry<BlockPos, Long>> iterator = this.highlight.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry<BlockPos, Long> entry = iterator.next();
                    long time = System.currentTimeMillis() - entry.getValue();
                    if (time > 750L) {
                        iterator.remove();
                    } else {
                        int currentAlpha = (int)(210.0 - time / 750.0 * 210.0);
                        if (currentAlpha <= 0) {
                            iterator.remove();
                        } else if (!this.raytrace.getValue()) {
                            RenderUtil.renderBlock(
                                entry.getKey(),
                                themeColor & 16777215 | currentAlpha << 24,
                                this.outline.getValue(),
                                this.shade.getValue()
                            );
                        }
                    }
                }
            }

            if (this.raytrace.getValue()) {
                Minecraft mc = Minecraft.func_71410_x();
                MovingObjectPosition hitResult = mc.field_71476_x;
                if (hitResult != null && hitResult.field_72313_a == MovingObjectType.MISS) {
                    hitResult = this.lastESPRaytrace;
                } else {
                    this.lastESPRaytrace = hitResult;
                }

                if (hitResult != null && hitResult.field_72313_a == MovingObjectType.BLOCK) {
                    RenderUtil.renderBlock(
                        hitResult.func_178782_a(),
                        themeColor & 16777215 | this.alpha.getValue() << 24,
                        this.outline.getValue(),
                        this.shade.getValue()
                    );
                }
            }
        }
    }
}
