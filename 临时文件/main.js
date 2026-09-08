let minionWeapon = [
    { id: "weapon_id", data: { damage: 1, knockback: 0.5, armorPierce: 0 } }
]
let $MinionWeaponModifyEvent = Java.loadClass("first.lyra.api.MinionWeaponModifyEvent")
NativeEvents.onEvent($MinionWeaponModifyEvent, event => {
    minionWeapon.forEach(config => {
        if (event.item === config.id) {
            event.damage = config.data.damage;
            event.knockback = config.data.knockback;
            event.armorPierce = config.data.armorPierce;
        }
    })
})
