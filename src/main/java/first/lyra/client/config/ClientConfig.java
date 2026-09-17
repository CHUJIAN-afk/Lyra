package first.lyra.client.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    private static final ForgeConfigSpec.Builder Builder = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue AlphaModify = Builder.define("alpha_modify", true);

    public static final ForgeConfigSpec.BooleanValue DebugMode = Builder.define("debug_mode", false);

    public static final ForgeConfigSpec Spec = Builder.build();
}
