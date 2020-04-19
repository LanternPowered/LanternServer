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
package org.lanternpowered.server.script.function.value.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.lanternpowered.api.script.function.value.DoubleValueProvider;

import java.lang.reflect.Type;

public class ConstantDoubleValueProviderJsonSerializer implements JsonSerializer<DoubleValueProvider.Constant>,
        JsonDeserializer<DoubleValueProvider.Constant> {

    @Override
    public JsonElement serialize(DoubleValueProvider.Constant src, Type typeOfSrc, JsonSerializationContext context) {
        //noinspection ConstantConditions
        return new JsonPrimitive(src.get(null));
    }

    @Override
    public DoubleValueProvider.Constant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return DoubleValueProvider.constant(json.getAsDouble());
    }
}
