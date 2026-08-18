package miau.module.modules.misc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.TickEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.IChatComponent;

public class MurderDetector extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final Set<Integer> MURDER_ITEMS = new HashSet<>();
    private static final Set<UUID> MURDERER_IDS = new HashSet<>();
    private static final float DEFAULT_TEXT_WIDTH = 130.0F;
    public static int textX = -1;
    public static int textY = 66;
    public final BooleanProperty showText = new BooleanProperty("show-text", true);
    public final BooleanProperty chat = new BooleanProperty("chat", true);
    private final Set<UUID> notifiedIds = new HashSet<>();
    private EntityPlayer murder1;
    private EntityPlayer murder2;

    public MurderDetector() {
        super("MurdererDetector", false, false);
    }

    public static boolean isMurderer(EntityPlayer player) {
        return player != null && MURDERER_IDS.contains(player.func_110124_au());
    }

    public static String getMurdererTabName(NetworkPlayerInfo info) {
        if (info != null && info.func_178845_a() != null && MURDERER_IDS.contains(info.func_178845_a().getId())) {
            IChatComponent displayName = info.func_178854_k();
            String name;
            if (displayName != null) {
                name = displayName.func_150254_d();
            } else {
                name = ScorePlayerTeam.func_96667_a(info.func_178850_i(), info.func_178845_a().getName());
            }

            return "§c" + name + " §c[Murderer]";
        } else {
            return null;
        }
    }

    public static void setTextPosition(int x, int y) {
        textX = x;
        textY = y;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && mc.field_71441_e != null && mc.field_71439_g != null) {
            if (mc.field_71439_g.field_70173_aa % 2 == 0) {
                for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                    this.checkPlayer(player);
                }

                if (mc.func_147114_u() != null) {
                    for (NetworkPlayerInfo info : mc.func_147114_u().func_175106_d()) {
                        EntityPlayer player = mc.field_71441_e.func_152378_a(info.func_178845_a().getId());
                        this.checkPlayer(player);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && this.showText.getValue()) {
            float x = this.getTextX();
            if (this.murder1 == null && this.murder2 == null) {
                this.drawText("Murderers: §cNone", x, textY);
            } else {
                this.drawText("Murderers:", x, textY);
                int row = 1;
                if (this.murder1 != null) {
                    this.drawText("- §c" + this.murder1.func_70005_c_(), x, textY + 11.0F * row++);
                }

                if (this.murder2 != null) {
                    this.drawText("- §c" + this.murder2.func_70005_c_(), x, textY + 11.0F * row);
                }
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.clearDetections();
    }

    private void checkPlayer(EntityPlayer player) {
        if (player != null && player != mc.field_71439_g && !isMurderer(player)) {
            ItemStack heldItem = player.func_70694_bm();
            if (this.isMurderItem(heldItem)) {
                this.addMurderer(player);
            }
        }
    }

    private boolean isMurderItem(ItemStack stack) {
        if (stack != null && stack.func_77973_b() != null) {
            String displayName = stack.func_82833_r();
            return displayName != null && displayName.toLowerCase().contains("knife")
                ? true
                : MURDER_ITEMS.contains(Item.func_150891_b(stack.func_77973_b()));
        } else {
            return false;
        }
    }

    private void addMurderer(EntityPlayer player) {
        MURDERER_IDS.add(player.func_110124_au());
        if (this.murder1 == null) {
            this.murder1 = player;
        } else if (this.murder2 == null && !player.func_110124_au().equals(this.murder1.func_110124_au())) {
            this.murder2 = player;
        }

        if (this.notifiedIds.add(player.func_110124_au())) {
            if (this.chat.getValue()) {
                ChatUtil.display("&7[&cMurdererDetector&7] &e" + player.func_70005_c_() + " &fis Murderer!");
            }
        }
    }

    private void drawText(String text, float x, float y) {
        mc.field_71466_p.func_175063_a(text, x, y, -1);
    }

    private float getTextX() {
        return textX < 0 ? new ScaledResolution(mc).func_78326_a() / 2.0F - 65.0F : textX;
    }

    private void clearDetections() {
        this.murder1 = null;
        this.murder2 = null;
        this.notifiedIds.clear();
        MURDERER_IDS.clear();
    }

    @Override
    public void onDisabled() {
        this.clearDetections();
    }

    static {
        int[] itemIds = new int[]{
            267,
            272,
            256,
            280,
            271,
            268,
            273,
            369,
            277,
            359,
            400,
            285,
            398,
            357,
            279,
            283,
            276,
            293,
            421,
            333,
            409,
            349,
            364,
            382,
            351,
            340,
            406,
            396,
            260,
            2258,
            76,
            32,
            19,
            122,
            175,
            405,
            130
        };

        for (int id : itemIds) {
            MURDER_ITEMS.add(id);
        }
    }
}
