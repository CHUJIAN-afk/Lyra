package first.lyra.common.damageInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 伤害数字样式管理器（客户端持有）。
 * <p>
 * 从数据包 JSON 加载样式定义，/reload 时自动重载。
 * 客户端收到网络包后根据 damageType 查询样式重建渲染参数。
 * 若 JSON 未定义 default，则 defaultStyle 为 null，未匹配的伤害类型将被跳过不渲染。
 * </p>
 */
public class DamageInfoStyleManager implements PreparableReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final DamageInfoStyleManager INSTANCE = new DamageInfoStyleManager();

    /** 默认样式（null 表示 JSON 未定义 default，未匹配的伤害类型将被跳过） */
    @Nullable
    private DamageInfoStyle defaultStyle;
    /** 伤害类型 → 样式缓存 */
    private Map<Identifier, DamageInfoStyle> styleMap = new HashMap<>();

    private DamageInfoStyleManager() {
    }

    /**
     * 26.2: SimpleJsonResourceReloadListener 泛型化且 apply 移除,改为直接实现 PreparableReloadListener,
     * 手动读取 damage_info 目录 JSON。reload 签名改为 (SharedState, Executor, PreparationBarrier, Executor)。
     */
    @Override
    public @NonNull CompletableFuture<Void> reload(PreparableReloadListener.SharedState sharedState, @NonNull Executor backgroundExecutor, PreparableReloadListener.PreparationBarrier barrier, @NonNull Executor gameExecutor) {
        ResourceManager resourceManager = sharedState.resourceManager();
        return CompletableFuture.supplyAsync(() -> {
            Map<Identifier, JsonElement> resources = new HashMap<>();
            for (Map.Entry<Identifier, Resource> entry : resourceManager.listResources("damage_info", path -> path.getPath().endsWith(".json")).entrySet()) {
                try (BufferedReader reader = entry.getValue().openAsReader()) {
                    resources.put(entry.getKey(), GSON.fromJson(reader, JsonElement.class));
                } catch (IOException e) {
                    LOGGER.warn("Failed to read damage_info file {}: {}", entry.getKey(), e.getMessage());
                }
            }
            return resources;
        }, backgroundExecutor).thenCompose(barrier::wait).thenAcceptAsync(this::apply, gameExecutor);
    }

    protected void apply(Map<Identifier, JsonElement> resources) {
        Map<Identifier, DamageInfoStyle> newMap = new HashMap<>();
        DamageInfoStyle newDefault = null;

        for (Map.Entry<Identifier, JsonElement> entry : resources.entrySet()) {
            Identifier fileId = entry.getKey();
            try {
                JsonObject root = GsonHelper.convertToJsonObject(entry.getValue(), "damage_info");

                // 解析 default
                if (root.has("default")) {
                    DamageInfoStyle style = parseStyle(GsonHelper.getAsJsonObject(root, "default"));
                    if (style != null) {
                        newDefault = style;
                    }
                }

                // 解析 entries
                if (root.has("entries")) {
                    for (JsonElement elem : GsonHelper.getAsJsonArray(root, "entries")) {
                        JsonObject obj = elem.getAsJsonObject();
                        DamageInfoStyle style = parseStyle(obj);
                        if (style != null) {
                            Identifier key = Identifier.parse(style.damageType());
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

    /**
     * 解析单条样式，校验所有字段完整性。
     *
     * @return 解析后的样式，字段不完整则返回 null 并打印警告
     */
    @Nullable
    private DamageInfoStyle parseStyle(JsonObject obj) {
        try {
            // 校验所有必需字段存在
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

            // 基本值校验
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
                    Identifier.parse(textureStr),
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

    /**
     * 根据伤害类型 ID 查询样式。
     * <p>
     * 先查 styleMap，未命中时返回 defaultStyle。
     * 若 defaultStyle 也为 null（JSON 未定义 default），返回 null，调用方应跳过该条目。
     * </p>
     */
    @Nullable
    public DamageInfoStyle getStyle(Identifier damageTypeId) {
        DamageInfoStyle exact = styleMap.get(damageTypeId);
        if (exact != null) return exact;
        return defaultStyle;
    }

    /** 获取默认样式，可能为 null */
    @Nullable
    public DamageInfoStyle getDefault() {
        return defaultStyle;
    }
}
