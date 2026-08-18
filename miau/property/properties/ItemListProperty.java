package miau.property.properties;

import java.util.function.BooleanSupplier;
import net.minecraft.item.ItemStack;

public class ItemListProperty extends TextProperty {
    public ItemListProperty(String name, String value) {
        super(name, value);
    }

    public ItemListProperty(String name, String value, BooleanSupplier booleanSupplier) {
        super(name, value, booleanSupplier);
    }

    public boolean matches(ItemStack stack) {
        if (stack == null) {
            return false;
        }

        String val = this.getValue();
        if (val != null && !val.isEmpty()) {
            String[] items = val.split(",");
            String itemName = stack.func_77977_a().toLowerCase();
            String displayName = stack.func_82833_r().toLowerCase();

            for (String item : items) {
                item = item.trim().toLowerCase();
                if (!item.isEmpty() && (itemName.contains(item) || displayName.contains(item))) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }
}
