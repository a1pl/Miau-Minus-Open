package miau.util.misc;

public final class SilentAttackManager {
    private static boolean silent = false;

    private SilentAttackManager() {
    }

    public static boolean isSilent() {
        return silent;
    }

    public static void setSilent(boolean value) {
        silent = value;
    }

    public static void withSilentAttack(Runnable action) {
        if (silent) {
            action.run();
        } else {
            silent = true;

            try {
                action.run();
            } finally {
                silent = false;
            }
        }
    }
}
