package first.lyra.common.dataComponent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import first.lyra.register.LyraDataComponentRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLLoader;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;
import org.mesdag.portlib.client.PortDeltaTicker;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public record LyraRarity(int color) {
    public static final LyraRarity Gray = new LyraRarity(0x828282);
    public static final LyraRarity White = new LyraRarity(0xFFFFFF);
    public static final LyraRarity Blue = new LyraRarity(0x9696FF);
    public static final LyraRarity Green = new LyraRarity(0x96FF96);
    public static final LyraRarity Orange = new LyraRarity(0xFFC896);
    public static final LyraRarity LightRed = new LyraRarity(0xFF9696);
    public static final LyraRarity Pink = new LyraRarity(0xFF96FF);
    public static final LyraRarity LightPurple = new LyraRarity(0xD2A0FF);
    public static final LyraRarity Lime = new LyraRarity(0x96FF0A);
    public static final LyraRarity Yellow = new LyraRarity(0xFFFF0A);
    public static final LyraRarity Cyan = new LyraRarity(0x05C8FF);
    public static final LyraRarity Red = new LyraRarity(0xFF2864);
    public static final LyraRarity Purple = new LyraRarity(0xB428FF);
    public static final LyraRarity Rainbow = new LyraRarity(-1);

    public static final Map<LyraRarity, Function<Float, Integer>> DynamicColorData = new HashMap<>();
    public static final Codec<LyraRarity> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.INT.fieldOf("color").forGetter(lyraRarity -> lyraRarity.color)).apply(instance, LyraRarity::new));
    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, LyraRarity> STREAM_CODEC = PortByteBufCodecs.fromCodecWithRegistries(CODEC);

    @Override
    public int color() {
        Function<Float, Integer> function = DynamicColorData.get(this);
        if (function != null && FMLLoader.getDist().isClient()) {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level != null) {
                long gameTime = level.getGameTime();
                float partialTick = PortDeltaTicker.INSTANCE.getGameTimeDeltaPartialTick(true);
                return function.apply(gameTime + partialTick);
            }
        }
        return color;
    }

    public static Component handler(ItemStack stack, MutableComponent component) {
        LyraRarity lyraRarity = stack.get(LyraDataComponentRegister.RARITY);
        if (lyraRarity != null) {
            return component.withColor(lyraRarity.color());
        }
        return component;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof LyraRarity other) {
            return color == other.color;
        }
        return false;
    }

    static {
        DynamicColorData.put(Rainbow, time -> {
            float hue = (time * 0.005F) % 1.0F;
            hue = (float) (hue - Math.floor(hue));
            float r, g, b;
            int sector = (int) (hue * 6.0F);
            float f = hue * 6.0F - sector;
            float p = 0.0f;
            float q = (1.0F - f);
            float t = (1.0F - (1.0F - f));
            switch (sector % 6) {
                case 0 -> {
                    r = (float) 1.0;
                    g = t;
                    b = p;
                }
                case 1 -> {
                    r = q;
                    g = (float) 1.0;
                    b = p;
                }
                case 2 -> {
                    r = p;
                    g = (float) 1.0;
                    b = t;
                }
                case 3 -> {
                    r = p;
                    g = q;
                    b = (float) 1.0;
                }
                case 4 -> {
                    r = t;
                    g = p;
                    b = (float) 1.0;
                }
                default -> {
                    r = (float) 1.0;
                    g = p;
                    b = q;
                }
            }
            return (((int) (r * 255.0F)) << 16) | (((int) (g * 255.0F)) << 8) | (int) (b * 255.0F);
        });
    }
}
