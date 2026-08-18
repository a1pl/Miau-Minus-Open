package miau.management;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;

public abstract class PlayerFileManager {
    public static Minecraft mc = Minecraft.func_71410_x();
    public ArrayList<String> players = new ArrayList<>();
    public File file;
    public Color color;

    public PlayerFileManager(File file, Color color) {
        this.file = file;
        this.color = color;
    }

    public void load() {
        if (!this.file.exists()) {
            try {
                if ((this.file.getParentFile().exists() || this.file.getParentFile().mkdirs())
                    && this.file.createNewFile()) {
                    System.out.printf("File created: %s%n", this.file.getName());
                }
            } catch (IOException e) {
                System.err.println("Error creating file: " + e.getMessage());
            }
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.file));

            try {
                Set<String> loaded = reader.lines()
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
                this.players.clear();
                this.players.addAll(loaded);
            } catch (Throwable var5) {
                try {
                    reader.close();
                } catch (Throwable var4) {
                    var5.addSuppressed(var4);
                }

                throw var5;
            }

            reader.close();
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    public void save() {
        try {
            File parent = this.file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            PrintWriter writer = new PrintWriter(new FileWriter(this.file));

            try {
                writer.print(String.join("\n", this.players));
            } catch (Throwable var6) {
                try {
                    writer.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }

                throw var6;
            }

            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }

    public String add(String name) {
        String normalized = name == null ? "" : name.trim();
        if (!normalized.isEmpty() && !this.isFriend(normalized)) {
            this.players.add(normalized);
            this.save();
            return normalized;
        } else {
            return null;
        }
    }

    public String remove(String name) {
        if (name == null) {
            return null;
        }

        for (int i = 0; i < this.players.size(); i++) {
            String player = this.players.get(i);
            if (player.equalsIgnoreCase(name.trim())) {
                this.players.remove(i);
                this.save();
                return player;
            }
        }

        return null;
    }

    public void clear() {
        this.players.clear();
        this.save();
    }

    public boolean isFriend(String string) {
        return string != null && this.players.stream().anyMatch(string2 -> string2.equalsIgnoreCase(string.trim()));
    }

    public ArrayList<String> getPlayers() {
        return this.players;
    }

    public Color getColor() {
        return this.color;
    }
}
