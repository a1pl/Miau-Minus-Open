package miau.module;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.KeyEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.modules.render.HUD;
import miau.util.client.ChatUtil;
import miau.util.client.Mp3Util;

public class ModuleManager {
    private static final Logger LOGGER = Logger.getLogger(ModuleManager.class.getName());
    private static final String MODULE_PACKAGE = "miau.module.modules.";
    private static final Map<String, String> CATEGORY_NAMES = new LinkedHashMap<>();
    private boolean sound = false;
    private boolean soundEnabled = false;
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();

    public Module getModule(String string) {
        return this.modules
            .values()
            .stream()
            .filter(mD -> mD.getName().replace(" ", "").equalsIgnoreCase(string.replace(" ", "")))
            .findFirst()
            .orElse(null);
    }

    public Module getModule(Class<?> clazz) {
        return this.modules.get(clazz);
    }

    public LinkedHashMap<String, List<Module>> getModulesByCategory() {
        LinkedHashMap<String, List<Module>> categories = new LinkedHashMap<>();

        for (String categoryName : CATEGORY_NAMES.values()) {
            categories.put(categoryName, new ArrayList<>());
        }

        for (Module module : this.modules.values()) {
            String categoryName = getCategoryName(module);
            if (categoryName != null) {
                categories.get(categoryName).add(module);
            }
        }

        Comparator<Module> byName = Comparator.comparing(modulex -> modulex.getName().toLowerCase());
        categories.values().forEach(modules -> modules.sort(byName));
        categories.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return categories;
    }

    private static String getCategoryName(Module module) {
        String packageName = module.getClass().getPackage().getName();
        if (!packageName.startsWith("miau.module.modules.")) {
            return null;
        }

        String categoryKey = packageName.substring("miau.module.modules.".length());
        int nestedPackage = categoryKey.indexOf(46);
        if (nestedPackage >= 0) {
            categoryKey = categoryKey.substring(0, nestedPackage);
        }

        return CATEGORY_NAMES.get(categoryKey);
    }

    public void playSound() {
        this.sound = true;
    }

    public void playSound(boolean enabled) {
        this.sound = true;
        this.soundEnabled = enabled;
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        for (Module module : this.modules.values()) {
            if (module.getKey() == event.getKey()) {
                boolean shouldNotify = module.toggle();
                HUD hud = (HUD)this.modules.get(HUD.class);
                if (hud != null && shouldNotify) {
                    shouldNotify = hud.toggleAlerts.getValue();
                }

                if (shouldNotify) {
                    String status = module.isEnabled() ? "&a&lON" : "&c&lOFF";
                    String message = String.format("%s%s: %s&r", Miau.clientName, module.getName(), status);
                    ChatUtil.display(message);
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE && this.sound) {
            this.sound = false;
            if (this.soundEnabled) {
                Mp3Util.play("miau/sound/enable/Sigma5.mp3");
            } else {
                Mp3Util.play("miau/sound/disble/Sigma5.mp3");
            }
        }
    }

    static {
        CATEGORY_NAMES.put("combat", "Combat");
        CATEGORY_NAMES.put("ghost", "Ghost");
        CATEGORY_NAMES.put("movement", "Movement");
        CATEGORY_NAMES.put("render", "Render");
        CATEGORY_NAMES.put("player", "Player");
        CATEGORY_NAMES.put("misc", "Misc");
        CATEGORY_NAMES.put("network", "Network");
        CATEGORY_NAMES.put("minigames", "Minigames");
        CATEGORY_NAMES.put("grind", "Grind");
    }
}
