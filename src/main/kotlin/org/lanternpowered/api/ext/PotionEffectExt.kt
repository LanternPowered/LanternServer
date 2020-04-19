/*
 * Lantern
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
@file:JvmName("PotionEffectHelper")
@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package org.lanternpowered.api.ext

import org.lanternpowered.api.effect.potion.PotionEffect
import org.lanternpowered.api.effect.potion.PotionEffectBuilder
import org.lanternpowered.api.effect.potion.PotionEffectType
import org.lanternpowered.api.registry.builderOf

inline fun potionEffectOf(type: PotionEffectType, amplifier: Int, duration: Int, ambient: Boolean = false, particles: Boolean = true): PotionEffect =
        PotionEffect.builder().potionType(type).amplifier(amplifier).duration(duration).ambient(ambient).particles(particles).build()

fun Collection<PotionEffect>.merge(that: Collection<PotionEffect>): MutableList<PotionEffect> {
    val effectsByType = mutableMapOf<PotionEffectType, PotionEffect>()
    for (effect in this) {
        effectsByType[effect.type] = effect
    }
    val result = mutableListOf<PotionEffect>()
    for (effect in that) {
        val other = effectsByType.remove(effect.type)
        if (other != null) {
            result.add(effect.merge(other))
        } else {
            result.add(effect)
        }
    }
    result.addAll(effectsByType.values)
    return result
}

/**
 * Merges this [PotionEffect] with the other one.
 *
 * @param that The potion effect to merge with
 * @return The merged potion effect
 */
fun PotionEffect.merge(that: PotionEffect): PotionEffect {
    val builder = builderOf<PotionEffectBuilder>().from(this)
    if (that.amplifier > amplifier) {
        builder.amplifier(that.amplifier).duration(that.duration)
    } else if (that.amplifier == amplifier && duration < that.duration) {
        builder.duration(that.duration)
    } else if (!that.isAmbient && isAmbient) {
        builder.ambient(that.isAmbient)
    }
    builder.showParticles(that.showsParticles())
    builder.showIcon(that.showsIcon())
    return builder.build()
}
