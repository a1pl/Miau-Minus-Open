package miau.module.modules.render;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import miau.Miau;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

public class Capes extends Module {
    public static final List<ResourceLocation> LOADED_CAPES = new ArrayList<>();
    public static String[] CAPES_NAME = getBuiltinCapes().toArray(new String[0]);
    public final ModeProperty capeMode = new ModeProperty("Cape", 0, CAPES_NAME);
    public final BooleanProperty allPlayer = new BooleanProperty("All player", false);
    public final BooleanProperty btnLoadCapes = new BooleanProperty("Load capes", false);
    public final BooleanProperty btnOpenFolder = new BooleanProperty("Open folder", false);
    private static File directory;

    private static List<String> getBuiltinCapes() {
        List<String> capes = new ArrayList<>();

        try {
            URL url = Miau.class.getResource("/assets/keystrokesmod/textures/capes/");
            if (url != null) {
                if (url.getProtocol().equals("file")) {
                    File dir = new File(url.toURI());
                    if (dir.exists() && dir.listFiles() != null) {
                        for (File f : dir.listFiles()) {
                            if (f.getName().endsWith(".png")) {
                                capes.add(f.getName().replace(".png", ""));
                            }
                        }
                    }
                } else if (url.getProtocol().equals("jar")) {
                    String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
                    JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));

                    try {
                        Enumeration<JarEntry> entries = jar.entries();

                        while (entries.hasMoreElements()) {
                            String name = entries.nextElement().getName();
                            if (name.startsWith("assets/keystrokesmod/textures/capes/") && name.endsWith(".png")) {
                                String capeName = name.substring(name.lastIndexOf("/") + 1).replace(".png", "");
                                capes.add(capeName);
                            }
                        }
                    } catch (Throwable var8) {
                        try {
                            jar.close();
                        } catch (Throwable var7) {
                            var8.addSuppressed(var7);
                        }

                        throw var8;
                    }

                    jar.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (capes.isEmpty()) {
            capes.add("anime");
        }

        return capes;
    }

    public Capes() {
        super("Capes", false);
        directory = new File(Minecraft.func_71410_x().field_71412_D + File.separator + "keystrokes", "customCapes");
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            if (!success) {
                System.out.println("There was an issue creating customCapes directory.");
            }
        }

        this.loadCapes();
    }

    @Override
    public void verifyValue(String name) {
        if (name.equals("Load capes") && this.btnLoadCapes.getValue()) {
            this.btnLoadCapes.setValue(false);
            this.loadCapes();
        } else if (name.equals("Open folder") && this.btnOpenFolder.getValue()) {
            this.btnOpenFolder.setValue(false);

            try {
                Desktop.getDesktop().open(directory);
            } catch (IOException ex) {
                directory.mkdirs();
                ChatUtil.display("&cError locating folder, recreated.");
            }
        }
    }

    public void loadCapes() {
        File[] files;
        try {
            files = Objects.requireNonNull(directory.listFiles());
        } catch (NullPointerException e) {
            ChatUtil.display("&cFail to load custom capes.");
            return;
        }

        String[] builtinCapes = getBuiltinCapes().toArray(new String[0]);
        CAPES_NAME = new String[files.length + builtinCapes.length];
        LOADED_CAPES.clear();
        System.arraycopy(builtinCapes, 0, CAPES_NAME, 0, builtinCapes.length);

        for (String s : builtinCapes) {
            String name = s.toLowerCase();

            try {
                InputStream stream = Miau.class
                    .getResourceAsStream("/assets/keystrokesmod/textures/capes/" + name + ".png");
                if (stream == null) {
                    stream = Miau.class.getResourceAsStream("/assets/keystrokesmod/textures/capes/" + s + ".png");
                }

                if (stream != null) {
                    BufferedImage bufferedImage = ImageIO.read(stream);
                    LOADED_CAPES.add(
                        Minecraft.func_71410_x().field_71446_o.func_110578_a(name, new DynamicTexture(bufferedImage))
                    );
                    stream.close();
                }
            } catch (Exception e) {
                ChatUtil.display("&cFailed to load cape '&r" + s + "&c'");
            }
        }

        int i = 0;

        for (int filesLength = files.length; i < filesLength; i++) {
            File file = files[i];
            if (file.exists() && file.isFile() && file.getName().endsWith(".png")) {
                String fileName = file.getName().substring(0, file.getName().length() - 4);
                CAPES_NAME[builtinCapes.length + i] = fileName;

                try {
                    BufferedImage bufferedImage = ImageIO.read(file);
                    LOADED_CAPES.add(
                        Minecraft.func_71410_x()
                            .field_71446_o
                            .func_110578_a(fileName, new DynamicTexture(bufferedImage))
                    );
                } catch (IOException e) {
                    ChatUtil.display("&cFailed to load cape '&r" + fileName + "&c'");
                }
            }
        }

        this.capeMode.setModes(CAPES_NAME);
        ChatUtil.display("&aLoaded &r" + CAPES_NAME.length + "&a capes.");
    }

    public ResourceLocation getCape() {
        int index = this.capeMode.getValue();
        return index >= 0 && index < LOADED_CAPES.size() ? LOADED_CAPES.get(index) : null;
    }
}
