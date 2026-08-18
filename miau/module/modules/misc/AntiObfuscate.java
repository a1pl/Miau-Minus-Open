package miau.module.modules.misc;

import miau.module.Module;

public class AntiObfuscate extends Module {
    public AntiObfuscate() {
        super("AntiObfuscate", false, true);
    }

    public String stripObfuscated(String input) {
        return input == null ? null : input.replaceAll("§k", "");
    }
}
