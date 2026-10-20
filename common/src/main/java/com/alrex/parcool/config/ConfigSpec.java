package com.alrex.parcool.config;

import com.alrex.parcool.ParCool;
import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Loader-agnostic replacement for {@code net.neoforged.neoforge.common.ModConfigSpec}.
 *
 * <h2>Why a shim instead of rewriting {@link ParCoolConfig}</h2>
 * {@code ParCoolConfig} (953 lines) is already written against a narrow contract: a chainable
 * {@link Builder} that yields typed values, plus {@code configure(Function)}. Only the backing store
 * and the file format are loader specific, so swapping the class keeps the whole configuration
 * surface — every key, default, range, translation key and the {@code writeToBuffer} /
 * {@code readFromBuffer} wire layout used by {@code ClientSetting} / {@code ServerLimitation} —
 * byte-for-byte identical to upstream.
 *
 * <h2>File format</h2>
 * JSON (Gson, which the game already ships), one file per side:
 * {@code <config>/parcool-client.json} and {@code <config>/parcool-server.json}. Values are stored
 * as a nested tree mirroring the {@code push}/{@code pop} structure, so the file is hand-editable
 * and self-documenting. Unknown keys are ignored and missing keys keep their declared default, so a
 * config written by a newer build still loads.
 */
public class ConfigSpec {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final List<ConfigValue<?>> values;

    private Path file;

    private ConfigSpec(List<ConfigValue<?>> values) {
        for (ConfigValue<?> value : values) {
            value.owner = this;
        }
        this.values = values;
    }

    /** Result of {@link Builder#configure(Function)}: the built holder plus its spec. */
    public static final class Built<T> {
        private final T left;
        private final ConfigSpec right;

        Built(T left, ConfigSpec right) {
            this.left = left;
            this.right = right;
        }

        public T getLeft() {
            return this.left;
        }

        public ConfigSpec getRight() {
            return this.right;
        }
    }

    public boolean isLoaded() {
        return this.file != null;
    }

    public Path getFile() {
        return this.file;
    }

    /**
     * Reads the file if it exists. Missing keys keep their defaults; unparsable values are ignored.
     */
    public void load(Path file) {
        this.file = file;
        if (file == null || !Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            apply(JsonParser.parseReader(reader));
        } catch (IOException | RuntimeException e) {
            Path backup = file.resolveSibling(file.getFileName() + ".bak");
            if (Files.exists(backup)) {
                ParCool.LOGGER.warn("Could not read ParCool config {}, falling back to {}", file, backup);
                try (Reader reader = Files.newBufferedReader(backup, StandardCharsets.UTF_8)) {
                    apply(JsonParser.parseReader(reader));
                    return;
                } catch (IOException | RuntimeException backupError) {
                    ParCool.LOGGER.warn("Could not read the backup either: {}", backupError.toString());
                }
            }
            ParCool.LOGGER.warn("Could not read ParCool config {}: {}", file, e.toString());
        }
    }

    /**
     * Applies a parsed document, value by value. Kept separate from the file handling so the backup
     * path reuses it; a partially parsable document still leaves the untouched keys at their defaults,
     * which is what the log message promises.
     */
    private void apply(JsonElement root) {
        if (root == null || !root.isJsonObject()) return;
        for (ConfigValue<?> value : this.values) {
            JsonElement element = descend(root.getAsJsonObject(), value.segments);
            if (element != null && !element.isJsonNull()) {
                value.parseAndSet(element);
            }
        }
    }

    /**
     * Writes the current values, creating parent directories as needed.
     *
     * <p>Through a temporary file and an atomic move, keeping the previous file as {@code .bak}. The old
     * version wrote straight into the target, so an interruption (a crash, a full disk, a kill during
     * world save) truncated the file - and because {@link #load} swallows a parse error and keeps the
     * defaults, the user silently came back to a fully reset configuration with no way to tell.
     */
    public void save(Path file) {
        if (file == null) return;
        JsonObject root = new JsonObject();
        for (ConfigValue<?> value : this.values) {
            insert(root, value.segments, value.toJson());
        }
        Path temp = null;
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            temp = Files.createTempFile(
                    file.getParent(), file.getFileName().toString(), ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
                writer.write('\n');
            }
            if (Files.exists(file)) {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            temp = null;
        } catch (IOException e) {
            ParCool.LOGGER.warn("Could not write ParCool config {}: {}", file, e.toString());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // Nothing useful to do: the target file was not touched.
                }
            }
        }
    }

    /** Re-persists to the file this spec was last loaded from; used by the settings screens. */
    public void persist() {
        if (this.file != null) save(this.file);
    }

    private static JsonElement descend(JsonObject root, List<String> segments) {
        JsonObject current = root;
        for (int i = 0; i < segments.size() - 1; i++) {
            JsonElement next = current.get(segments.get(i));
            if (next == null || !next.isJsonObject()) return null;
            current = next.getAsJsonObject();
        }
        return current.get(segments.get(segments.size() - 1));
    }

    private static void insert(JsonObject root, List<String> segments, JsonElement value) {
        JsonObject current = root;
        for (int i = 0; i < segments.size() - 1; i++) {
            String segment = segments.get(i);
            JsonElement next = current.get(segment);
            if (next == null || !next.isJsonObject()) {
                next = new JsonObject();
                current.add(segment, next);
            }
            current = next.getAsJsonObject();
        }
        current.add(segments.get(segments.size() - 1), value);
    }

    // ------------------------------------------------------------------
    // builder
    // ------------------------------------------------------------------

    public static class Builder {

        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<String> path = new ArrayList<>();

        private String pendingComment;
        private String pendingTranslation;

        /** Comments are kept so a generated file can be annotated; they are not part of the value. */
        public Builder comment(String comment) {
            this.pendingComment = comment;
            return this;
        }

        public Builder comment(String... comment) {
            this.pendingComment = String.join("\n", comment);
            return this;
        }

        public Builder translation(String key) {
            this.pendingTranslation = key;
            return this;
        }

        public Builder worldRestart() {
            return this;
        }

        public Builder push(String name) {
            this.path.add(name);
            return this;
        }

        public Builder pop() {
            if (!this.path.isEmpty()) {
                this.path.remove(this.path.size() - 1);
            }
            return this;
        }

        public BooleanValue define(String key, boolean defaultValue) {
            return finish(new BooleanValue(segments(key), defaultValue));
        }

        public IntValue defineInRange(String key, int defaultValue, int min, int max) {
            return finish(new IntValue(segments(key), defaultValue, min, max));
        }

        public DoubleValue defineInRange(String key, double defaultValue, double min, double max) {
            return finish(new DoubleValue(segments(key), defaultValue, min, max));
        }

        public <T extends Enum<T>> EnumValue<T> defineEnum(String key, T defaultValue) {
            return finish(new EnumValue<>(segments(key), defaultValue));
        }

        public <T> Built<T> configure(Function<Builder, T> factory) {
            T holder = factory.apply(this);
            return new Built<>(holder, build());
        }

        public ConfigSpec build() {
            return new ConfigSpec(new ArrayList<>(this.values));
        }

        private List<String> segments(String key) {
            List<String> result = new ArrayList<>(this.path);
            result.add(key);
            return result;
        }

        private <V extends ConfigValue<?>> V finish(V value) {
            value.comment = this.pendingComment;
            value.translation = this.pendingTranslation;
            this.pendingComment = null;
            this.pendingTranslation = null;
            this.values.add(value);
            return value;
        }
    }

    // ------------------------------------------------------------------
    // values
    // ------------------------------------------------------------------

    public abstract static class ConfigValue<T> {

        final List<String> segments;
        final T defaultValue;

        /** volatile: written by the settings screen on the render thread, read by the action ticks. */
        volatile T value;
        String comment;
        String translation;
        ConfigSpec owner;

        ConfigValue(List<String> segments, T defaultValue) {
            this.segments = segments;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public T get() {
            return this.value;
        }

        public void set(T value) {
            this.value = value;
        }

        public T getDefault() {
            return this.defaultValue;
        }

        /** Dotted path, matching NeoForge's {@code ConfigValue#getPath()} joined form. */
        public String getDottedPath() {
            return String.join(".", this.segments);
        }

        public List<String> getPath() {
            return List.copyOf(this.segments);
        }

        public String getComment() {
            return this.comment;
        }

        public String getTranslation() {
            return this.translation;
        }

        /** Persists the owning spec; the settings screens call this per changed value. */
        public void save() {
            if (this.owner != null) this.owner.persist();
        }

        abstract JsonElement toJson();

        abstract void parseAndSet(JsonElement element);
    }

    public static class BooleanValue extends ConfigValue<Boolean> {

        BooleanValue(List<String> segments, boolean defaultValue) {
            super(segments, defaultValue);
        }

        @Override
        JsonElement toJson() {
            return new JsonPrimitive(get());
        }

        @Override
        void parseAndSet(JsonElement element) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                set(element.getAsBoolean());
            }
        }
    }

    public static class IntValue extends ConfigValue<Integer> {

        private final int min;
        private final int max;

        IntValue(List<String> segments, int defaultValue, int min, int max) {
            super(segments, defaultValue);
            this.min = min;
            this.max = max;
        }

        public int getMin() {
            return this.min;
        }

        public int getMax() {
            return this.max;
        }

        @Override
        public void set(Integer value) {
            super.set(Math.max(this.min, Math.min(this.max, value)));
        }

        @Override
        JsonElement toJson() {
            return new JsonPrimitive(get());
        }

        @Override
        void parseAndSet(JsonElement element) {
            if (element.getAsJsonPrimitive().isNumber()) {
                set(element.getAsInt());
            }
        }
    }

    public static class DoubleValue extends ConfigValue<Double> {

        private final double min;
        private final double max;

        DoubleValue(List<String> segments, double defaultValue, double min, double max) {
            super(segments, defaultValue);
            this.min = min;
            this.max = max;
        }

        public double getMin() {
            return this.min;
        }

        public double getMax() {
            return this.max;
        }

        @Override
        public void set(Double value) {
            super.set(Math.max(this.min, Math.min(this.max, value)));
        }

        @Override
        JsonElement toJson() {
            return new JsonPrimitive(get());
        }

        @Override
        void parseAndSet(JsonElement element) {
            if (element.getAsJsonPrimitive().isNumber()) {
                set(element.getAsDouble());
            }
        }
    }

    public static class EnumValue<T extends Enum<T>> extends ConfigValue<T> {

        private final Class<T> type;

        @SuppressWarnings("unchecked")
        EnumValue(List<String> segments, T defaultValue) {
            super(segments, defaultValue);
            this.type = (Class<T>) defaultValue.getDeclaringClass();
        }

        public T[] getEnumConstants() {
            return this.type.getEnumConstants();
        }

        public Class<T> getEnumClass() {
            return this.type;
        }

        @Override
        JsonElement toJson() {
            return new JsonPrimitive(get().name());
        }

        @Override
        void parseAndSet(JsonElement element) {
            if (!element.getAsJsonPrimitive().isString()) return;
            try {
                set(Enum.valueOf(this.type, element.getAsString()));
            } catch (IllegalArgumentException ignored) {
                // keep the default
            }
        }
    }
}
