package miau.module.modules.render;

import java.awt.Color;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.ui.clickgui.ClickGui;
import miau.ui.clickgui.augustus.AugustusClickGui;
import miau.ui.clickgui.demise.PanelGui;
import miau.ui.clickgui.faiths.FaithsCharacterRenderer;
import miau.ui.clickgui.faiths.FaithsClickGui;
import miau.ui.clickgui.miauminus.MiauMinusClickGui;
import miau.ui.clickgui.normal.ClickGuiScreen;
import miau.ui.clickgui.rise.RiseClickGui;
import miau.util.font.FontRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class ClickGUI extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private boolean switchingGuiStyle;
    private static final int[] COLORS = new int[]{-11549705, -8271996, -30107, -4560696, -10929, -37013, -11684180, -1};
    private static final String[] COLOR_NAMES = new String[]{
        "Sky Blue", "Green", "Orange", "Purple", "Yellow", "Red", "Teal", "White"
    };
    public final ModeProperty accentColor = new ModeProperty("Color", 0, COLOR_NAMES);
    public final ModeProperty guiFont = new ModeProperty("Font", 0, FontRepository.FONT_NAMES);
    public final ModeProperty style = new ModeProperty(
        "Style", 0, new String[]{"Miau", "Rise", "Faiths", "Demise", "Normal", "Augustus", "styles miauminus"}
    );
    public final ModeProperty theme = new ModeProperty(
        "Theme", 0, new String[]{"Default", "Dark", "Blue", "Red", "Green", "Purple", "Orange", "Cyan"}
    );
    public final ModeProperty character = new ModeProperty("Character", 0, FaithsCharacterRenderer.getCharacterArray());
    public final BooleanProperty saveGuiState = new BooleanProperty("Save GUI State", true);
    public final BooleanProperty blur = new BooleanProperty("Blur", true);
    public final BooleanProperty shaders = new BooleanProperty("Shaders", false);
    public final BooleanProperty showCharacter = new BooleanProperty("show-character", true);
    public final IntProperty windowWidth = new IntProperty("Window Width", 600, 300, 1200);
    public final IntProperty windowHeight = new IntProperty("Window Height", 400, 200, 800);
    public final FloatProperty cornerRadius = new FloatProperty("Corner Radius", 8.0F, 0.0F, 20.0F);
    private ClickGui clickGui;
    private FaithsClickGui faithsClickGui;
    private AugustusClickGui augustusClickGui;
    private MiauMinusClickGui miauMinusClickGui;
    private ClickGuiScreen normalClickGuiScreen;
    private RiseClickGui riseClickGui;

    public ClickGUI() {
        super("ClickGUI", false);
        this.setKey(54);
        FontRepository.setGuiFace(this.guiFont.getValue());
    }

    public Color getAccentColor() {
        int idx = this.accentColor.getValue();
        if (idx < 0 || idx >= COLORS.length) {
            idx = 0;
        }

        return new Color(COLORS[idx], true);
    }

    public GuiScreen getSelectedGui() {
        int modeVal = this.style.getValue();
        switch (modeVal) {
            case 0:
                if (this.clickGui == null) {
                    this.clickGui = new ClickGui();
                }

                return this.clickGui;
            case 1:
                if (this.riseClickGui == null) {
                    this.riseClickGui = new RiseClickGui();
                }

                return this.riseClickGui;
            case 2:
                if (this.faithsClickGui == null) {
                    this.faithsClickGui = new FaithsClickGui();
                }

                return this.faithsClickGui;
            case 3:
                return new PanelGui();
            case 4:
                if (this.normalClickGuiScreen == null) {
                    this.normalClickGuiScreen = ClickGuiScreen.getInstance();
                }

                return this.normalClickGuiScreen != null ? this.normalClickGuiScreen : new ClickGuiScreen();
            case 5:
                if (this.augustusClickGui == null) {
                    this.augustusClickGui = new AugustusClickGui();
                }

                return this.augustusClickGui;
            case 6:
                if (this.miauMinusClickGui == null) {
                    this.miauMinusClickGui = new MiauMinusClickGui();
                }

                return this.miauMinusClickGui;
            default:
                if (this.clickGui == null) {
                    this.clickGui = new ClickGui();
                }

                return this.clickGui;
        }
    }

    public void openSelectedGui() {
        GuiScreen screen = this.getSelectedGui();
        this.switchingGuiStyle = mc.field_71462_r instanceof ClickGui
            || mc.field_71462_r instanceof ClickGuiScreen
            || mc.field_71462_r instanceof RiseClickGui
            || mc.field_71462_r instanceof FaithsClickGui
            || mc.field_71462_r instanceof PanelGui
            || mc.field_71462_r instanceof AugustusClickGui;

        try {
            mc.func_147108_a(screen);
        } finally {
            this.switchingGuiStyle = false;
        }
    }

    public boolean isSwitchingGuiStyle() {
        return this.switchingGuiStyle;
    }

    @Override
    public void verifyValue(String name) {
        if ("Font".equalsIgnoreCase(name)) {
            FontRepository.setGuiFace(this.guiFont.getValue());
            FontRepository.clearCache();
        } else if ("Style".equalsIgnoreCase(name)
            && (
                mc.field_71462_r instanceof ClickGui
                    || mc.field_71462_r instanceof ClickGuiScreen
                    || mc.field_71462_r instanceof RiseClickGui
                    || mc.field_71462_r instanceof FaithsClickGui
                    || mc.field_71462_r instanceof PanelGui
                    || mc.field_71462_r instanceof AugustusClickGui
            )) {
            this.openSelectedGui();
        }
    }

    @Override
    public void onEnabled() {
        this.setEnabled(false);
        if (mc.field_71441_e != null) {
            this.character.setModes(FaithsCharacterRenderer.getCharacterArray());
            this.openSelectedGui();
        }
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        mc.func_147108_a(null);
        if (mc.field_71462_r == null) {
            mc.func_71381_h();
        }
    }

    public void checkModeSwitch() {
        if (mc.field_71462_r != null) {
            int currentMode = this.style.getValue();
            if (currentMode == 0 && !(mc.field_71462_r instanceof ClickGui)) {
                this.openSelectedGui();
            } else if (currentMode == 1 && !(mc.field_71462_r instanceof RiseClickGui)) {
                this.openSelectedGui();
            } else if (currentMode == 2 && !(mc.field_71462_r instanceof FaithsClickGui)) {
                this.openSelectedGui();
            } else if (currentMode == 3 && !(mc.field_71462_r instanceof PanelGui)) {
                this.openSelectedGui();
            } else if (currentMode == 4 && !(mc.field_71462_r instanceof ClickGuiScreen)) {
                this.openSelectedGui();
            } else if (currentMode == 5 && !(mc.field_71462_r instanceof AugustusClickGui)) {
                this.openSelectedGui();
            } else if (currentMode == 6 && !(mc.field_71462_r instanceof MiauMinusClickGui)) {
                this.openSelectedGui();
            }
        }
    }
}
