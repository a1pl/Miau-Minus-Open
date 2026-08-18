package miau;

public final class ClientInfo {
    public static final String NAME = "Miau Minus";
    public static final String VERSION = "1.1.0-beta";
    public static final String MC_VERSION = "1.8.9";
    public static final String GIT_COMMIT = "unknown";
    public static final boolean GITHUB_BUILD = Boolean.parseBoolean("false");

    private ClientInfo() {
    }

    public static String getBuildChannel() {
        return GITHUB_BUILD ? "dev" : "main";
    }

    public static String getDisplayVersion() {
        if (!GITHUB_BUILD) {
            return "Miau Minus 1.1.0-beta Stable";
        }

        String commit = "unknown" != null && !"unknown".isEmpty() && !"unknown".equalsIgnoreCase("unknown")
            ? "unknown"
            : "unknown";
        return "Miau Minus 1.1.0-beta beta +" + commit;
    }

    public static String getClickGuiVersion() {
        return GITHUB_BUILD ? getGitVersion() : "1.1.0-beta";
    }

    public static String getGitVersion() {
        String commit = "unknown" != null && !"unknown".isEmpty() && !"unknown".equalsIgnoreCase("unknown")
            ? "unknown"
            : "unknown";
        return "git-" + commit;
    }
}
