package first.lyra.common.attachment;

import first.lyra.common.armorSet.ArmorSet;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家盔甲套装生效状态附件。
 * <p>
 * 替代 {@link ArmorSet} 的静态缓存：装备变化事件维护的每玩家套装状态表，
 * 逻辑与原静态缓存一致，仅存储位置迁移到玩家附件。
 * </p>
 */
public class ArmorSetData {

    /** 套装生效状态表（armorSet -> 是否穿齐），由装备变化事件维护。 */
    public final Map<ArmorSet, Boolean> activeSets = new HashMap<>();
}
