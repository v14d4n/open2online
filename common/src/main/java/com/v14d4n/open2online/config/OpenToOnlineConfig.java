package com.v14d4n.open2online.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Plain JSON config, read and written with the Gson that Minecraft already ships.
 *
 * <p>1.16.5 used {@code ForgeConfigSpec}, and the port carried that over through Forge Config API
 * Port so Fabric had the same class. That meant every Fabric player needed an extra mod installed for
 * a handful of settings, so the spec was dropped in favour of this. The trade is that JSON has no
 * comments — the descriptions that used to sit in the file now live in the tooltips next to each
 * option, which is where they are actually read.
 *
 * <p>{@link Value} keeps the {@code get}/{@code set}/{@code save} shape the rest of the mod was
 * already written against.
 */
public final class OpenToOnlineConfig {
    public static final int AUTO_START_MIN_DELAY = 3;
    public static final int AUTO_START_MAX_DELAY = 60;

    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");
    private static final String FILE_NAME = "open2online.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final List<Value<?>> VALUES = new ArrayList<>();

    public static final Value<Integer> port = integer("port", 25565, 0, 65535);
    public static final Value<Integer> maxPlayers = integer("maxPlayers", 8, 1, Integer.MAX_VALUE);
    public static final Value<String> lastIP = string("lastIP", "0.0.0.0");
    public static final Value<Boolean> allowPvp = bool("allowPvp", true);
    public static final Value<Boolean> requireLicense = bool("requireLicense", false);
    public static final Value<Boolean> hideIP = bool("hideIP", true);

    /** -1 means Auto, which walks the backends in turn. */
    public static final Value<Integer> libraryId = integer("library", -1, -1, 2);
    /** Backend that last succeeded in Auto mode, so the next run can start with it. */
    public static final Value<Integer> autoLibraryId = integer("autoLibrary", -1, -1, 2);
    /** Mapper index PortMapper succeeded with last time; -1 when nothing is remembered. */
    public static final Value<Integer> portMapperIndex = integer("portMapperIndex", -1, -1, Integer.MAX_VALUE);

    public static final Value<Boolean> whitelistMode = bool("whitelist", false);
    public static final Value<List<String>> friends = stringList("friendList");

    public static final Value<Boolean> updateNotifications = bool("updateNotifications", true);
    public static final Value<Boolean> licenseNotifications = bool("licenseNotifications", true);
    public static final Value<Boolean> whitelistNotifications = bool("whitelistNotifications", true);

    public static final Value<Boolean> autoStart = bool("autoStart", false);
    public static final Value<Boolean> autoStartOnline = bool("autoStartOnline", true);
    public static final Value<Integer> autoStartDelay =
            integer("autoStartDelay", 5, AUTO_START_MIN_DELAY, AUTO_START_MAX_DELAY);

    private OpenToOnlineConfig() {
    }

    /** Reads the file if it exists, then writes it back so any missing key gains its default. */
    public static synchronized void load() {
        Path file = file();

        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (Value<?> value : VALUES) {
                    value.readFrom(root);
                }
            } catch (Exception e) {
                LOGGER.warn("Could not read {}, falling back to defaults", FILE_NAME, e);
            }
        }

        save();
    }

    /** Synchronized because option screens and the publish worker both write. */
    public static synchronized void save() {
        JsonObject root = new JsonObject();
        for (Value<?> value : VALUES) {
            root.add(value.key, value.toJson());
        }

        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Could not write {}", FILE_NAME, e);
        }
    }

    /** Mutating the returned list has no effect; the whole list has to be handed back. */
    public static void setFriends(List<String> updated) {
        friends.set(List.copyOf(updated));
        friends.save();
    }

    private static Path file() {
        return Platform.getConfigFolder().resolve(FILE_NAME);
    }

    private static Value<Integer> integer(String key, int defaultValue, int min, int max) {
        return register(new Value<>(key, defaultValue,
                json -> Math.clamp(json.getAsInt(), min, max),
                value -> new com.google.gson.JsonPrimitive(value)));
    }

    private static Value<Boolean> bool(String key, boolean defaultValue) {
        return register(new Value<>(key, defaultValue,
                JsonElement::getAsBoolean,
                value -> new com.google.gson.JsonPrimitive(value)));
    }

    private static Value<String> string(String key, String defaultValue) {
        return register(new Value<>(key, defaultValue,
                JsonElement::getAsString,
                com.google.gson.JsonPrimitive::new));
    }

    private static Value<List<String>> stringList(String key) {
        return register(new Value<>(key, Collections.emptyList(),
                json -> {
                    List<String> items = new ArrayList<>();
                    json.getAsJsonArray().forEach(element -> items.add(element.getAsString()));
                    return List.copyOf(items);
                },
                value -> {
                    JsonArray array = new JsonArray();
                    value.forEach(array::add);
                    return array;
                }));
    }

    private static <T> Value<T> register(Value<T> value) {
        VALUES.add(value);
        return value;
    }

    public static final class Value<T> {
        private final String key;
        private final T defaultValue;
        private final Function<JsonElement, T> reader;
        private final Function<T, JsonElement> writer;

        /** Written from option screens and read from the publish worker. */
        private volatile T value;

        private Value(String key, T defaultValue, Function<JsonElement, T> reader, Function<T, JsonElement> writer) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.reader = reader;
            this.writer = writer;
            this.value = defaultValue;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }

        public void save() {
            OpenToOnlineConfig.save();
        }

        private void readFrom(JsonObject root) {
            JsonElement element = root.get(key);
            if (element == null || element.isJsonNull()) {
                return;
            }

            try {
                value = reader.apply(element);
            } catch (RuntimeException e) {
                // A hand-edited file should not stop the mod from loading.
                LOGGER.warn("Ignoring malformed value for '{}' in {}", key, FILE_NAME);
                value = defaultValue;
            }
        }

        private JsonElement toJson() {
            return writer.apply(value);
        }
    }
}
