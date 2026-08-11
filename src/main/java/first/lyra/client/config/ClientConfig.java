package first.lyra.client.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {

    private static final ModConfigSpec.Builder Builder = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DynamicLight = Builder.define("dynamic_light", true);

    public static final ModConfigSpec.BooleanValue AlphaModify = Builder.define("alpha_modify", true);

    public static final ModConfigSpec.BooleanValue DebugMode = Builder.define("debug_mode", false);

    public static final ModConfigSpec Spec = Builder.build();
}
