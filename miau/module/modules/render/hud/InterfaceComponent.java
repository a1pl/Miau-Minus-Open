package miau.module.modules.render.hud;

import java.awt.Color;
import miau.module.Module;
import miau.module.modules.render.HUD;
import miau.util.vector.Vector2d;

public final class InterfaceComponent {
    public Module module;
    public Vector2d position = new Vector2d(5000.0, 0.0);
    public Vector2d targetPosition = new Vector2d(5000.0, 0.0);
    public float animationTime;
    public String tag = "";
    public float nameWidth = 0.0F;
    public float tagWidth;
    public Color color = Color.WHITE;
    public String translatedName = "";
    public boolean hidden = false;
    public String displayName = "";
    public String displayTag = "";
    public boolean hasTag;

    public float getTotalWidth() {
        return this.nameWidth + this.tagWidth;
    }

    public InterfaceComponent(Module module) {
        this.module = module;
    }

    public boolean shouldDisplay(HUD hudInstance) {
        String name = this.module.getName().toLowerCase();
        switch (hudInstance.modulesToShow.getValue()) {
            case 0:
                return true;
            case 1:
                if (!name.equals("clickgui") && !name.equals("gui") && !name.equals("hud")) {
                    String category = this.module.getCategory();
                    return category == null || !category.equalsIgnoreCase("render");
                } else {
                    return false;
                }
            case 2:
                if (!name.equals("clickgui") && !name.equals("gui") && !name.equals("hud")) {
                    return this.module.getKey() != 0 && this.module.getKey() != 0;
                }

                return false;
            default:
                return true;
        }
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }
}
