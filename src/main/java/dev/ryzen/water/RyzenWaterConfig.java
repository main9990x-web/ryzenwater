package dev.ryzen.water;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RyzenWaterConfig {
    public static final String[] COLOR_NAMES = {"Синий", "Голубой", "Бирюзовый", "Фиолетовый", "Розовый", "Зелёный", "Радужный"};
    public static final int[] COLORS = {0x4696FF, 0x32DCFF, 0x20E0B0, 0xA060FF, 0xFF70C8, 0x50E060};

    public boolean enabled = true;
    public int radius = 8;
    public float opacity = 0.75f;
    public float waveHeight = 0.05f;
    public float waveSpeed = 1.0f;
    public int color = 0;
    public boolean edgeFade = true;

    public static RyzenWaterConfig INSTANCE = new RyzenWaterConfig();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("ryzenwater.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                RyzenWaterConfig c = GSON.fromJson(Files.readString(p), RyzenWaterConfig.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception ignored) {
        }
        INSTANCE.radius = Math.max(3, Math.min(16, INSTANCE.radius));
        if (INSTANCE.color < 0 || INSTANCE.color >= COLOR_NAMES.length) INSTANCE.color = 0;
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(INSTANCE));
        } catch (IOException ignored) {
        }
    }
}
