package com.viaversion.viaversion.util;

import com.viaversion.viaversion.compatibility.YamlCompat;
import com.viaversion.viaversion.compatibility.unsafe.Yaml1Compat;
import com.viaversion.viaversion.compatibility.unsafe.Yaml2Compat;
import com.viaversion.viaversion.libs.gson.JsonElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public abstract class Config {
    protected static final Logger LOGGER = Logger.getLogger("ViaVersion Config");
    private static final YamlCompat YAMP_COMPAT = YamlCompat.isVersion1() ? new Yaml1Compat() : new Yaml2Compat();
    private static final ThreadLocal<Yaml> YAML = ThreadLocal.withInitial(() -> {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(false);
        options.setIndent(2);
        return new Yaml(YAMP_COMPAT.createSafeConstructor(), YAMP_COMPAT.createRepresenter(options), options);
    });
    private final CommentStore commentStore = new CommentStore('.', 2);
    private final File configFile;
    private Map<String, Object> config;

    protected Config(File configFile) {
        this.configFile = configFile;
    }

    public URL getDefaultConfigURL() {
        return this.getClass().getClassLoader().getResource("assets/viaversion/config.yml");
    }

    public InputStream getDefaultConfigInputStream() {
        return this.getClass().getClassLoader().getResourceAsStream("assets/viaversion/config.yml");
    }

    public Map<String, Object> loadConfig(File location) {
        URL defaultConfigUrl = this.getDefaultConfigURL();
        return defaultConfigUrl != null
            ? this.loadConfig(location, defaultConfigUrl)
            : this.loadConfig(location, this::getDefaultConfigInputStream);
    }

    public synchronized Map<String, Object> loadConfig(File location, URL jarConfigFile) {
        return this.loadConfig(location, jarConfigFile::openStream);
    }

    private synchronized Map<String, Object> loadConfig(File location, InputStreamSupplier configSupplier) {
        List<String> unsupported = this.getUnsupportedOptions();

        try {
            this.commentStore.storeComments(configSupplier.get());

            for (String option : unsupported) {
                List<String> comments = this.commentStore.header(option);
                if (comments != null) {
                    comments.clear();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Object> config = null;
        if (location.exists()) {
            try (FileInputStream input = new FileInputStream(location)) {
                config = YAML.get().load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (config == null) {
            config = new HashMap<>();
        }

        Map<String, Object> defaults = config;

        try (InputStream stream = configSupplier.get()) {
            defaults = YAML.get().load(stream);

            for (String option : unsupported) {
                defaults.remove(option);
            }

            for (Entry<String, Object> entry : config.entrySet()) {
                if (defaults.containsKey(entry.getKey()) && !unsupported.contains(entry.getKey())) {
                    defaults.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.handleConfig(defaults);
        this.save(location, defaults);
        return defaults;
    }

    protected abstract void handleConfig(Map<String, Object> var1);

    public synchronized void save(File location, Map<String, Object> config) {
        try {
            this.commentStore.writeComments(YAML.get().dump(config), location);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract List<String> getUnsupportedOptions();

    public void set(String path, Object value) {
        this.config.put(path, value);
    }

    public void save() {
        if (this.configFile.getParentFile() != null) {
            this.configFile.getParentFile().mkdirs();
        }

        this.save(this.configFile, this.config);
    }

    public void save(File file) {
        this.save(file, this.config);
    }

    public void reload() {
        if (this.configFile.getParentFile() != null) {
            this.configFile.getParentFile().mkdirs();
        }

        this.config = new ConcurrentSkipListMap<>(this.loadConfig(this.configFile));
    }

    public Map<String, Object> getValues() {
        return this.config;
    }

    public <T> @Nullable T get(String key, T def) {
        Object o = this.config.get(key);
        return (T)(o != null ? o : def);
    }

    public boolean getBoolean(String key, boolean def) {
        Object o = this.config.get(key);
        return o instanceof Boolean ? (Boolean)o : def;
    }

    public @Nullable String getString(String key, @Nullable String def) {
        Object o = this.config.get(key);
        return o instanceof String ? (String)o : def;
    }

    public int getInt(String key, int def) {
        Object o = this.config.get(key);
        return o instanceof Number ? ((Number)o).intValue() : def;
    }

    public double getDouble(String key, double def) {
        Object o = this.config.get(key);
        return o instanceof Number ? ((Number)o).doubleValue() : def;
    }

    public List<Integer> getIntegerList(String key) {
        Object o = this.config.get(key);
        return o != null ? (List)o : new ArrayList<>();
    }

    public List<String> getStringList(String key) {
        Object o = this.config.get(key);
        return o != null ? (List)o : new ArrayList<>();
    }

    public <T> List<T> getListSafe(String key, Class<T> type, String invalidValueMessage) {
        Object o = this.config.get(key);
        if (o instanceof List) {
            List<?> list = (List<?>)o;
            List<T> filteredValues = new ArrayList<>();

            for (Object o1 : list) {
                if (type.isInstance(o1)) {
                    filteredValues.add(type.cast(o1));
                } else if (invalidValueMessage != null) {
                    LOGGER.warning(String.format(invalidValueMessage, o1));
                }
            }

            return filteredValues;
        } else {
            return new ArrayList<>();
        }
    }

    public @Nullable JsonElement getSerializedComponent(String key) {
        Object o = this.config.get(key);
        return o != null && !((String)o).isEmpty() ? ComponentUtil.legacyToJson((String)o) : null;
    }
}
