package miau.management;

import java.awt.Color;
import java.util.ArrayList;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.management.drag.Orientation;
import miau.management.drag.Snap;
import miau.module.Module;
import miau.property.Property;
import miau.property.properties.DragProperty;
import miau.ui.clickgui.ClickGui;
import miau.util.render.ShapeUtil;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public class DragManager {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private boolean wasMouseDown = false;
    private static DragProperty selectedValue = null;
    private static Vector2d offset;
    private static final ArrayList<DragProperty> draggables = new ArrayList<>();
    private static final ArrayList<String> draggableNames = new ArrayList<>();
    public static ArrayList<Snap> snaps = new ArrayList<>();
    public static Snap selected;

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            boolean shouldRender = mc.field_71462_r instanceof GuiChat || mc.field_71462_r instanceof ClickGui;
            if (!shouldRender) {
                selectedValue = null;
                this.wasMouseDown = false;
            } else {
                ScaledResolution scaledResolution = new ScaledResolution(mc);
                int width = scaledResolution.func_78326_a();
                int height = scaledResolution.func_78328_b();
                draggables.clear();
                draggableNames.clear();

                for (Module module : Miau.moduleManager.modules.values()) {
                    if (module.isEnabled()) {
                        for (Property<?> value : module.getValues()) {
                            if (value instanceof DragProperty) {
                                DragProperty dp = (DragProperty)value;
                                if (!dp.structure) {
                                    draggables.add(dp);
                                    draggableNames.add(module.getName());
                                }
                            }
                        }
                    }
                }

                if (Miau.notificationManager != null) {
                }

                int mouseX = Mouse.getX() * width / mc.field_71443_c;
                int mouseY = height - Mouse.getY() * height / mc.field_71440_d - 1;
                boolean isMouseDown = Mouse.isButtonDown(0);
                boolean justClicked = isMouseDown && !this.wasMouseDown;
                this.wasMouseDown = isMouseDown;
                if (!isMouseDown) {
                    selectedValue = null;
                }

                if (justClicked) {
                    for (int i = 0; i < draggables.size(); i++) {
                        DragProperty positionValue = draggables.get(i);
                        Vector2d position = positionValue.position;
                        Vector2d scale = positionValue.scale;
                        if (!positionValue.structure
                            && positionValue.render
                            && mouseX >= position.x
                            && mouseX <= position.x + scale.x
                            && mouseY >= position.y
                            && mouseY <= position.y + scale.y) {
                            selectedValue = positionValue;
                            offset = new Vector2d(position.x - mouseX, position.y - mouseY);
                        }
                    }
                }

                if (selectedValue != null) {
                    double positionX = mouseX + offset.x;
                    double positionY = mouseY + offset.y;
                    selectedValue.targetPosition = new Vector2d(positionX, positionY);
                    snaps.clear();
                    double edgeSnap = 2.0;
                    snaps.add(new Snap(width / 2.0F, 5.0, Orientation.HORIZONTAL, true, true, true));
                    snaps.add(new Snap(height / 2.0F, 5.0, Orientation.VERTICAL, true, true, true));
                    snaps.add(new Snap(height - edgeSnap, 5.0, Orientation.VERTICAL, false, false, true));
                    snaps.add(new Snap(edgeSnap, 5.0, Orientation.VERTICAL, false, true, false));
                    snaps.add(new Snap(width - edgeSnap, 5.0, Orientation.HORIZONTAL, false, false, true));
                    snaps.add(new Snap(edgeSnap, 5.0, Orientation.HORIZONTAL, false, true, false));

                    for (DragProperty positionValue : draggables) {
                        if (positionValue != selectedValue) {
                            snaps.add(
                                new Snap(
                                    positionValue.position.x + positionValue.scale.x + edgeSnap,
                                    5.0,
                                    Orientation.HORIZONTAL,
                                    false,
                                    true,
                                    false
                                )
                            );
                            snaps.add(
                                new Snap(
                                    positionValue.position.x - edgeSnap,
                                    5.0,
                                    Orientation.HORIZONTAL,
                                    false,
                                    false,
                                    true
                                )
                            );
                            snaps.add(new Snap(positionValue.position.y, 5.0, Orientation.VERTICAL, false, false, true));
                            snaps.add(
                                new Snap(
                                    positionValue.position.y + positionValue.scale.y,
                                    5.0,
                                    Orientation.VERTICAL,
                                    false,
                                    true,
                                    false
                                )
                            );
                        }
                    }

                    selected = null;
                    int snapColor = new Color(255, 255, 255, 60).getRGB();

                    for (Snap snap : snaps) {
                        switch (snap.orientation) {
                            case VERTICAL:
                                double closest = Double.MAX_VALUE;
                                double y = -selectedValue.scale.y;

                                for (; y <= 0.0; y += selectedValue.scale.y / 2.0) {
                                    if ((y != -selectedValue.scale.y / 2.0 || snap.center)
                                        && (y != -selectedValue.scale.y || snap.left)
                                        && (y != 0.0 || snap.right)) {
                                        double distance = Math.abs(selectedValue.targetPosition.y - (snap.position + y));
                                        if (distance < snap.distance && distance < closest) {
                                            closest = distance;
                                            selectedValue.targetPosition.y = snap.position + y;
                                            selected = snap;
                                            ShapeUtil.drawRect(
                                                0.0F,
                                                (float)selected.position,
                                                width,
                                                (float)selected.position + 0.5F,
                                                snapColor
                                            );
                                        }
                                    }
                                }
                                break;
                            case HORIZONTAL:
                                double closest = Double.MAX_VALUE;

                                for (double x = -selectedValue.scale.x; x <= 0.0; x += selectedValue.scale.x / 2.0) {
                                    if ((x != -selectedValue.scale.x / 2.0 || snap.center)
                                        && (x != -selectedValue.scale.x || snap.left)
                                        && (x != 0.0 || snap.right)) {
                                        double distance = Math.abs(selectedValue.targetPosition.x - (snap.position + x));
                                        if (distance < snap.distance && distance < closest) {
                                            closest = distance;
                                            selectedValue.targetPosition.x = snap.position + x;
                                            selected = snap;
                                            ShapeUtil.drawRect(
                                                (float)selected.position,
                                                0.0F,
                                                (float)selected.position + 0.5F,
                                                height,
                                                snapColor
                                            );
                                        }
                                    }
                                }
                        }
                    }
                }

                for (int i = 0; i < draggables.size(); i++) {
                    DragProperty positionValue = draggables.get(i);
                    String name = draggableNames.get(i);
                    float padding = 2.0F;
                    if (positionValue.render) {
                        positionValue.position.x = Math.max(padding, positionValue.position.x);
                        positionValue.position.x = Math.min(
                            width - positionValue.scale.x - padding, positionValue.position.x
                        );
                        positionValue.position.y = Math.max(padding, positionValue.position.y);
                        positionValue.position.y = Math.min(
                            height - positionValue.scale.y - padding, positionValue.position.y
                        );
                        positionValue.targetPosition.x = Math.max(padding, positionValue.targetPosition.x);
                        positionValue.targetPosition.x = Math.min(
                            width - positionValue.scale.x - padding, positionValue.targetPosition.x
                        );
                        positionValue.targetPosition.y = Math.max(padding, positionValue.targetPosition.y);
                        positionValue.targetPosition.y = Math.min(
                            height - positionValue.scale.y - padding, positionValue.targetPosition.y
                        );
                        positionValue.position = new Vector2d(
                            Math.min(width - positionValue.scale.x - padding, positionValue.targetPosition.x),
                            Math.min(height - positionValue.scale.y - padding, positionValue.targetPosition.y)
                        );
                    }
                }
            }
        }
    }
}
