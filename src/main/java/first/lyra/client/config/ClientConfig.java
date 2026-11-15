package first.lyra.client.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {

    private static final ModConfigSpec.Builder Builder = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue AlphaModify = Builder.define("alpha_modify", true);

    public static final ModConfigSpec.BooleanValue DebugMode = Builder.define("debug_mode", false);

    public static final ModConfigSpec.BooleanValue DynamicLights = Builder.define("dynamic_lights", true);
    public static final ModConfigSpec.IntValue MaxLights = Builder.defineInRange("max_lights", 128, 1, 512);
    public static final ModConfigSpec.BooleanValue Bloom = Builder.define("bloom", true);
    public static final ModConfigSpec.BooleanValue BloomWithShaders = Builder.define("bloom_with_shaders", false);
    public static final ModConfigSpec.DoubleValue BloomStrength = Builder.defineInRange("bloom_strength", 0.3, 0.0, 2.0);

    public static final ModConfigSpec Spec = Builder.build();
}
