package miau.property.properties;

import com.google.gson.JsonObject;
import miau.property.Property;
import miau.util.animation.Animation;
import miau.util.animation.Easing;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class DragProperty extends Property<Vector2d> {
    public Vector2d position;
    public Vector2d targetPosition;
    public Vector2d scale;
    public Vector2d lastScale;
    public Animation animationPosition;
    public Animation smoothAnimation;
    public ScaledResolution lastScaledResolution;
    public boolean render = true;
    public boolean structure = false;
    private static final Minecraft mc = Minecraft.func_71410_x();

    public DragProperty(String name, Vector2d defaultValue) {
        super(name, defaultValue, null);
        this.position = new Vector2d(defaultValue.x, defaultValue.y);
        this.targetPosition = new Vector2d(defaultValue.x, defaultValue.y);
        this.scale = new Vector2d(100.0, 100.0);
        this.lastScale = new Vector2d(-1.0, -1.0);
        this.animationPosition = new Animation(Easing.LINEAR, 600L);
        this.smoothAnimation = new Animation(Easing.EASE_OUT_EXPO, 300L);
        this.lastScaledResolution = new ScaledResolution(mc);
    }

    public DragProperty(String name, Vector2d defaultValue, boolean render) {
        this(name, defaultValue);
        this.render = render;
    }

    public DragProperty(String name, Vector2d defaultValue, boolean render, boolean structure) {
        this(name, defaultValue);
        this.render = render && !structure;
        this.structure = structure;
    }

    public void setScale(Vector2d scale) {
        this.scale = scale;
        if (this.lastScale.x == -1.0 && this.lastScale.y == -1.0) {
            this.lastScale = this.scale;
        }

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        if (this.position.x > scaledResolution.func_78326_a() / 2.0F) {
            this.targetPosition.x = this.targetPosition.x + (this.lastScale.x - this.scale.x);
            this.position.x = this.targetPosition.x;
        }

        if (this.position.y > scaledResolution.func_78328_b() / 2.0F) {
            this.targetPosition.y = this.targetPosition.y + (this.lastScale.y - this.scale.y);
            this.position.y = this.targetPosition.y;
        }

        this.lastScale = scale;
        this.lastScaledResolution = scaledResolution;
    }

    @Override
    public String getValuePrompt() {
        return "";
    }

    @Override
    public String formatValue() {
        return this.position.x + "," + this.position.y;
    }

    @Override
    public boolean parseString(String string) {
        try {
            String[] split = string.split(",");
            this.position.x = Double.parseDouble(split[0]);
            this.position.y = Double.parseDouble(split[1]);
            this.targetPosition.x = this.position.x;
            this.targetPosition.y = this.position.y;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        if (jsonObject.has(this.getName() + "_x") && jsonObject.has(this.getName() + "_y")) {
            this.position.x = jsonObject.get(this.getName() + "_x").getAsDouble();
            this.position.y = jsonObject.get(this.getName() + "_y").getAsDouble();
            this.targetPosition.x = this.position.x;
            this.targetPosition.y = this.position.y;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName() + "_x", this.position.x);
        jsonObject.addProperty(this.getName() + "_y", this.position.y);
    }
}
