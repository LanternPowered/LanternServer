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
package org.lanternpowered.server.data.io.store.misc;

import org.lanternpowered.server.effect.potion.LanternPotionEffectType;
import org.lanternpowered.server.game.Lantern;
import org.lanternpowered.server.game.registry.type.effect.PotionEffectTypeRegistryModule;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectType;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class PotionEffectSerializer {

    private static final DataQuery IDENTIFIER = DataQuery.of("Id");
    private static final DataQuery AMPLIFIER = DataQuery.of("Amplifier");
    private static final DataQuery DURATION = DataQuery.of("Duration");
    private static final DataQuery SHOW_PARTICLES = DataQuery.of("ShowParticles");
    private static final DataQuery SHOW_ICON = DataQuery.of("ShowIcon");
    private static final DataQuery AMBIENT = DataQuery.of("Ambient");

    public static DataView serialize(PotionEffect potionEffect) {
        final DataView dataView = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
        dataView.set(AMPLIFIER, (byte) potionEffect.getAmplifier());
        dataView.set(DURATION, potionEffect.getDuration());
        dataView.set(AMBIENT, (byte) (potionEffect.isAmbient() ? 1 : 0));
        if (potionEffect.showsParticles())
            dataView.set(SHOW_PARTICLES, (byte) 1);
        if (potionEffect.showsIcon())
            dataView.set(SHOW_ICON, (byte) 1);
        final LanternPotionEffectType potionEffectType = (LanternPotionEffectType) potionEffect.getType();
        final int internalId = potionEffectType.getInternalId();
        if (internalId > 0xff) {
            dataView.set(IDENTIFIER, internalId);
        } else {
            dataView.set(IDENTIFIER, (byte) internalId);
        }
        return dataView;
    }

    @Nullable
    public static PotionEffect deserialize(DataView dataView) {
        final int internalId;

        if (dataView.get(IDENTIFIER).get() instanceof Byte) {
            internalId = dataView.getByte(IDENTIFIER).get() & 0xff;
        } else {
            internalId = dataView.getInt(IDENTIFIER).get();
        }

        final PotionEffectType effectType = PotionEffectTypeRegistryModule.INSTANCE.getByInternalId(internalId).orElse(null);
        if (effectType == null) {
            Lantern.getLogger().warn("Unknown potion effect type: " + internalId);
            return null;
        }

        final int amplifier = dataView.getInt(AMPLIFIER).get();
        final int duration = dataView.getInt(DURATION).get();
        final boolean ambient = dataView.getInt(AMBIENT).orElse(0) > 0;
        final boolean particles = dataView.getInt(SHOW_PARTICLES).orElse(0) > 0;
        final boolean icon = dataView.getInt(SHOW_ICON).orElse(1) > 0;
        return PotionEffect.builder()
                .potionType(effectType)
                .ambient(ambient)
                .amplifier(amplifier)
                .duration(duration)
                .showParticles(particles)
                .showIcon(icon)
                .build();
    }

    private PotionEffectSerializer() {
    }
}
