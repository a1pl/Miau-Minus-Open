package miau.module.modules.player.scaffold;

import java.util.Collections;
import java.util.List;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.SafeWalkEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.UpdateEvent;
import miau.property.Property;

public interface ScaffoldComponent {
    default List<Property<?>> getProperties() {
        return Collections.emptyList();
    }

    default void onUpdate(UpdateEvent event) {
    }

    default void onStrafe(StrafeEvent event) {
    }

    default void onMoveInput(MoveInputEvent event) {
    }

    default void onSafeWalk(SafeWalkEvent event) {
    }

    default void onLivingUpdate(LivingUpdateEvent event) {
    }

    default void onEnable() {
    }

    default void onDisable() {
    }

    default void onBlockPlaced() {
    }

    default void onRender3D(Render3DEvent event) {
    }
}
