package miau.util.client;

import miau.module.modules.misc.MouseRawInput;
import net.minecraft.util.MouseHelper;

public class RawMouseHelper extends MouseHelper {
    public void func_74374_c() {
        int rawDeltaX = MouseRawInput.consumeDeltaX();
        int rawDeltaY = MouseRawInput.consumeDeltaY();
        if (rawDeltaX == 0 && rawDeltaY == 0) {
            super.func_74374_c();
        } else {
            this.field_74377_a = rawDeltaX;
            this.field_74375_b = -rawDeltaY;
        }
    }
}
