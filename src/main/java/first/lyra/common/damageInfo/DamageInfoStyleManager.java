package first.lyra.common.damageInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class DamageInfoStyleManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final DamageInfoStyleManager INSTANCE = new DamageInfoStyleManager();

    @Nullable
    private DamageInfoStyle defaultStyle;

    private Map<ResourceLocation, DamageInfoStyle> styleMap = new HashMap<>();

    private DamageInfoStyleManager() {
        super(GSON, "damage_info");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, DamageInfoStyle> newMap = new HashMap<>();
        DamageInfoStyle newDefault = null;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                JsonObject root = GsonHelper.convertToJsonObject(entry.getValue(), "damage_info");

                if (root.has("default")) {
                    DamageInfoStyle style = parseStyle(GsonHelper.getAsJsonObject(root, "default"));
                    if (style != null) {
                        newDefault = style;
                    }
                }

                if (root.has("entries")) {
                    for (JsonElement elem : GsonHelper.getAsJsonArray(root, "entries")) {
                        JsonObject obj = elem.getAsJsonObject();
                        DamageInfoStyle style = parseStyle(obj);
                        if (style != null) {
                            ResourceLocation key = ResourceLocation.parse(style.damageType());
                            newMap.put(key, style);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to parse damage_info file {}: {}", fileId, e.getMessage());
            }
        }

        if (newDefault == null) {
            LOGGER.warn("No default style defined in damage_info JSON, undefined damage types will be skipped");
        }

        this.defaultStyle = newDefault;
        this.styleMap = newMap;
        LOGGER.info("Loaded {} damage info styles, default: {}", newMap.size(), newDefault != null ? newDefault.damageType() : "null");
    }

    @Nullable
    private DamageInfoStyle parseStyle(JsonObject obj) {
        try {

            String[] requiredFields = {"damage_type", "texture", "texture_width", "texture_height",
                    "glyph_spacing", "render_size", "max_life", "color", "critical_color"};
            for (String field : requiredFields) {
                if (!obj.has(field)) {
                    LOGGER.warn("Missing required field '{}' in damage_info entry, skipping", field);
                    return null;
                }
            }

            String damageType = GsonHelper.getAsString(obj, "damage_type");
            String textureStr = GsonHelper.getAsString(obj, "texture");
            int textureWidth = GsonHelper.getAsInt(obj, "texture_width");
            int textureHeight = GsonHelper.getAsInt(obj, "texture_height");
            int glyphSpacing = GsonHelper.getAsInt(obj, "glyph_spacing");
            float renderSize = GsonHelper.getAsFloat(obj, "render_size");
            int maxLife = GsonHelper.getAsInt(obj, "max_life");
            int color = DamageInfoStyle.parseHexColor(GsonHelper.getAsString(obj, "color"));
            int criticalColor = DamageInfoStyle.parseHexColor(GsonHelper.getAsString(obj, "critical_color"));

            if (textureWidth <= 0 || textureHeight <= 0) {
                LOGGER.warn("Invalid texture dimensions {}x{} for damage_type '{}', skipping", textureWidth, textureHeight, damageType);
                return null;
            }
            if (renderSize <= 0) {
                LOGGER.warn("Invalid render_size {} for damage_type '{}', skipping", renderSize, damageType);
                return null;
            }
            if (maxLife <= 0) {
                LOGGER.warn("Invalid max_life {} for damage_type '{}', skipping", maxLife, damageType);
                return null;
            }

            return new DamageInfoStyle(
                    damageType,
                    ResourceLocation.parse(textureStr),
                    textureWidth,
                    textureHeight,
                    glyphSpacing,
                    renderSize,
                    maxLife,
                    color,
                    criticalColor
            );
        } catch (Exception e) {
            LOGGER.warn("Failed to parse damage_info entry: {}", e.getMessage());
            return null;
        }
    }

    @Nullable
    public DamageInfoStyle getStyle(ResourceLocation damageTypeId) {
        DamageInfoStyle exact = styleMap.get(damageTypeId);
        if (exact != null) return exact;
        return defaultStyle;
    }

    @Nullable
    public DamageInfoStyle getDefault() {
        return defaultStyle;
    }
}
