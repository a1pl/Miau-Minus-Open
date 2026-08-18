package de.florianmichael.vialoadingbase;

import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocol.ProtocolManagerImpl;
import de.florianmichael.vialoadingbase.model.Platform;
import de.florianmichael.vialoadingbase.platform.ViaBackwardsPlatformImpl;
import de.florianmichael.vialoadingbase.platform.ViaRewindPlatformImpl;
import de.florianmichael.vialoadingbase.platform.ViaVersionPlatformImpl;
import de.florianmichael.vialoadingbase.platform.viaversion.VLBViaCommandHandler;
import de.florianmichael.vialoadingbase.platform.viaversion.VLBViaInjector;
import de.florianmichael.vialoadingbase.platform.viaversion.VLBViaProviders;
import de.florianmichael.vialoadingbase.util.JLoggerToLog4j;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;

public class ViaLoadingBase {
    public static final String VERSION = "${vialoadingbase_version}";
    public static final Logger LOGGER = new JLoggerToLog4j(LogManager.getLogger("ViaLoadingBase"));
    public static final Platform PSEUDO_VIA_VERSION = new Platform(
        "ViaVersion",
        () -> true,
        () -> {},
        protocolVersions -> protocolVersions.addAll(ViaVersionPlatformImpl.createVersionList())
    );
    public static final Platform PLATFORM_VIA_BACKWARDS = new Platform(
        "ViaBackwards",
        () -> inClassPath("com.viaversion.viabackwards.api.ViaBackwardsPlatform"),
        () -> new ViaBackwardsPlatformImpl(Via.getManager().getPlatform().getDataFolder())
    );
    public static final Platform PLATFORM_VIA_REWIND = new Platform(
        "ViaRewind",
        () -> inClassPath("com.viaversion.viarewind.api.ViaRewindPlatform"),
        () -> new ViaRewindPlatformImpl(Via.getManager().getPlatform().getDataFolder())
    );
    public static final List<ProtocolVersion> PROTOCOLS = new ArrayList<>();
    private static ViaLoadingBase instance;
    private final LinkedList<Platform> platforms;
    private final File runDirectory;
    private final int nativeVersion;
    private final BooleanSupplier forceNativeVersionCondition;
    private final Supplier<JsonObject> dumpSupplier;
    private final Consumer<ViaProviders> providers;
    private final Consumer<ViaManagerImpl.ViaManagerBuilder> managerBuilderConsumer;
    private final Consumer<ProtocolVersion> onProtocolReload;
    private ProtocolVersion nativeProtocolVersion;
    private ProtocolVersion targetProtocolVersion;

    public ViaLoadingBase(
        LinkedList<Platform> platforms,
        File runDirectory,
        int nativeVersion,
        BooleanSupplier forceNativeVersionCondition,
        Supplier<JsonObject> dumpSupplier,
        Consumer<ViaProviders> providers,
        Consumer<ViaManagerImpl.ViaManagerBuilder> managerBuilderConsumer,
        Consumer<ProtocolVersion> onProtocolReload
    ) {
        this.platforms = platforms;
        this.runDirectory = new File(runDirectory, "ViaLoadingBase");
        this.nativeVersion = nativeVersion;
        this.forceNativeVersionCondition = forceNativeVersionCondition;
        this.dumpSupplier = dumpSupplier;
        this.providers = providers;
        this.managerBuilderConsumer = managerBuilderConsumer;
        this.onProtocolReload = onProtocolReload;
        instance = this;
        this.initPlatform();
    }

    public ProtocolVersion getTargetVersion() {
        return this.forceNativeVersionCondition != null && this.forceNativeVersionCondition.getAsBoolean()
            ? this.nativeProtocolVersion
            : this.targetProtocolVersion;
    }

    public void reload(ProtocolVersion protocolVersion) {
        this.targetProtocolVersion = protocolVersion;
        if (this.onProtocolReload != null) {
            this.onProtocolReload.accept(this.targetProtocolVersion);
        }
    }

    public void initPlatform() {
        for (Platform platform : this.platforms) {
            platform.createProtocolPath();
        }

        this.nativeProtocolVersion = ProtocolVersion.getProtocol(this.nativeVersion);
        this.targetProtocolVersion = this.nativeProtocolVersion;
        ViaVersionPlatformImpl viaVersionPlatform = new ViaVersionPlatformImpl(LOGGER);
        ViaManagerImpl.ViaManagerBuilder builder = ViaManagerImpl.builder()
            .platform(viaVersionPlatform)
            .loader(new VLBViaProviders())
            .injector(new VLBViaInjector())
            .commandHandler(new VLBViaCommandHandler());
        if (this.managerBuilderConsumer != null) {
            this.managerBuilderConsumer.accept(builder);
        }

        Via.init(builder.build());
        ViaManagerImpl manager = (ViaManagerImpl)Via.getManager();
        manager.addEnableListener(() -> {
            for (Platform platform : this.platforms) {
                platform.build(LOGGER);
            }
        });
        manager.init();
        manager.onServerLoaded();
        manager.getProtocolManager().setMaxProtocolPathSize(Integer.MAX_VALUE);
        manager.getProtocolManager().setMaxPathDeltaIncrease(-1);
        ((ProtocolManagerImpl)manager.getProtocolManager()).refreshVersions();
        LOGGER.info("ViaLoadingBase has loaded " + Platform.COUNT + "/" + this.platforms.size() + " platforms");
    }

    public static ViaLoadingBase getInstance() {
        return instance;
    }

    public List<Platform> getSubPlatforms() {
        return this.platforms;
    }

    public File getRunDirectory() {
        return this.runDirectory;
    }

    public int getNativeVersion() {
        return this.nativeVersion;
    }

    public Supplier<JsonObject> getDumpSupplier() {
        return this.dumpSupplier;
    }

    public Consumer<ViaProviders> getProviders() {
        return this.providers;
    }

    public static boolean inClassPath(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Deprecated
    public static List<ProtocolVersion> getProtocols() {
        return PROTOCOLS;
    }

    public static class ViaLoadingBaseBuilder {
        private final LinkedList<Platform> platforms = new LinkedList<>();
        private File runDirectory;
        private Integer nativeVersion;
        private BooleanSupplier forceNativeVersionCondition;
        private Supplier<JsonObject> dumpSupplier;
        private Consumer<ViaProviders> providers;
        private Consumer<ViaManagerImpl.ViaManagerBuilder> managerBuilderConsumer;
        private Consumer<ProtocolVersion> onProtocolReload;

        public ViaLoadingBaseBuilder() {
            this.platforms.add(ViaLoadingBase.PSEUDO_VIA_VERSION);
            this.platforms.add(ViaLoadingBase.PLATFORM_VIA_BACKWARDS);
            this.platforms.add(ViaLoadingBase.PLATFORM_VIA_REWIND);
        }

        public static ViaLoadingBase.ViaLoadingBaseBuilder create() {
            return new ViaLoadingBase.ViaLoadingBaseBuilder();
        }

        public ViaLoadingBase.ViaLoadingBaseBuilder platform(Platform platform) {
            this.platforms.add(platform);
            return this;
        }

        public ViaLoadingBase.ViaLoadingBaseBuilder platform(Platform platform, int position) {
            this.platforms.add(position, platform);
            return this;
        }

        public ViaLoadingBase.ViaLoadingBaseBuilder runDirectory(File runDirectory) {
            this.runDirectory = runDirectory;
            return this;
        }

        public ViaLoadingBase.ViaLoadingBaseBuilder nativeVersion(int nativeVersion) {
            this.nativeVersion = nativeVersion;
            return this;
        }

        public ViaLoadingBase.ViaLoadingBaseBuilder forceNativeVersionCondition(
            BooleanSupplier forceNativeVersionCondition
        ) {
            this.forceNativeVersionCondition = forceNativeVersionCondition;
            return this;
        }

        public ViaLoadingBase.ViaLoadingBaseBuilder dumpSupplier(Supplier<JsonObject> dumpSupplier) {
            this.dumpSupplier = dumpSupplier;
            return this;
        }

        public ViaLoadingBase.ViaLoadingBaseBuilder providers(Consumer<ViaProviders> providers) {
            this.providers = providers;
            return this;
        }

        public ViaLoadingBase.ViaLoadingBaseBuilder managerBuilderConsumer(
            Consumer<ViaManagerImpl.ViaManagerBuilder> managerBuilderConsumer
        ) {
            this.managerBuilderConsumer = managerBuilderConsumer;
            return this;
        }

        public ViaLoadingBase.ViaLoadingBaseBuilder onProtocolReload(Consumer<ProtocolVersion> onProtocolReload) {
            this.onProtocolReload = onProtocolReload;
            return this;
        }

        public void build() {
            if (ViaLoadingBase.getInstance() != null) {
                ViaLoadingBase.LOGGER.severe("ViaLoadingBase has already started the platform!");
            } else if (this.runDirectory != null && this.nativeVersion != null) {
                new ViaLoadingBase(
                    this.platforms,
                    this.runDirectory,
                    this.nativeVersion,
                    this.forceNativeVersionCondition,
                    this.dumpSupplier,
                    this.providers,
                    this.managerBuilderConsumer,
                    this.onProtocolReload
                );
            } else {
                ViaLoadingBase.LOGGER.severe("Please check your ViaLoadingBaseBuilder arguments!");
            }
        }
    }
}
