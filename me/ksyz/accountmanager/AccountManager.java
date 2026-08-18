package me.ksyz.accountmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.utils.Nan0EventRegister;
import me.ksyz.accountmanager.utils.SSLUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

public class AccountManager {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final File file = new File(mc.field_71412_D, "openmiau.accounts.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = Logger.getLogger(AccountManager.class.getName());
    public static final ArrayList<Account> accounts = new ArrayList<>();

    public static void init() {
        SSLUtils.getSSLContext();
        Nan0EventRegister.register(MinecraftForge.EVENT_BUS, new Events());
        if (!file.exists()) {
            try {
                if ((file.getParentFile().exists() || file.getParentFile().mkdirs()) && file.createNewFile()) {
                    LOGGER.info("Successfully created openmiau.accounts.json");
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Couldn't create openmiau.accounts.json", e);
            }
        }
    }

    public static void load() {
        accounts.clear();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            try {
                JsonElement json = new JsonParser().parse(reader);
                if (json != null && json.isJsonArray()) {
                    for (JsonElement jsonElement : json.getAsJsonArray()) {
                        if (jsonElement.isJsonObject()) {
                            JsonObject jsonObject = jsonElement.getAsJsonObject();
                            accounts.add(
                                new Account(
                                    Optional.ofNullable(jsonObject.get("refreshToken"))
                                        .<String>map(JsonElement::getAsString)
                                        .orElse(""),
                                    Optional.ofNullable(jsonObject.get("accessToken"))
                                        .<String>map(JsonElement::getAsString)
                                        .orElse(""),
                                    Optional.ofNullable(jsonObject.get("username"))
                                        .<String>map(JsonElement::getAsString)
                                        .orElse(""),
                                    Optional.ofNullable(jsonObject.get("unban"))
                                        .<Long>map(JsonElement::getAsLong)
                                        .orElse(0L),
                                    Optional.ofNullable(jsonObject.get("clientId"))
                                        .<String>map(JsonElement::getAsString)
                                        .orElse(""),
                                    Optional.ofNullable(jsonObject.get("scope"))
                                        .<String>map(JsonElement::getAsString)
                                        .orElse(""),
                                    Optional.ofNullable(jsonObject.get("type"))
                                        .<String>map(JsonElement::getAsString)
                                        .orElse("premium")
                                )
                            );
                        }
                    }
                }
            } catch (Throwable var7) {
                try {
                    reader.close();
                } catch (Throwable var6) {
                    var7.addSuppressed(var6);
                }

                throw var7;
            }

            reader.close();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Couldn't find openmiau.accounts.json", e);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Couldn't load openmiau.accounts.json", e);
        }
    }

    public static void save() {
        try {
            JsonArray jsonArray = new JsonArray();

            for (Account account : accounts) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("refreshToken", account.getRefreshToken());
                jsonObject.addProperty("accessToken", account.getAccessToken());
                jsonObject.addProperty("username", account.getUsername());
                jsonObject.addProperty("unban", account.getUnban());
                jsonObject.addProperty("clientId", account.getClientId());
                jsonObject.addProperty("scope", account.getScope());
                jsonObject.addProperty("type", account.getType());
                jsonArray.add(jsonObject);
            }

            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            PrintWriter printWriter = new PrintWriter(new FileWriter(file));

            try {
                printWriter.println(gson.toJson(jsonArray));
            } catch (Throwable var6) {
                try {
                    printWriter.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }

                throw var6;
            }

            printWriter.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't save openmiau.accounts.json", e);
        }
    }
}
