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
package org.lanternpowered.server.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.lanternpowered.server.game.Lantern.getLogger;
import static org.spongepowered.api.data.persistence.DataQuery.of;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import org.lanternpowered.server.data.element.Element;
import org.lanternpowered.server.data.key.LanternKeys;
import org.lanternpowered.server.data.manipulator.DataManipulatorRegistration;
import org.lanternpowered.server.data.manipulator.mutable.IDataManipulator;
import org.lanternpowered.server.data.persistence.DataTypeSerializer;
import org.lanternpowered.server.data.persistence.DataTypeSerializerContext;
import org.lanternpowered.server.game.Lantern;
import org.lanternpowered.server.game.registry.type.data.DataSerializerRegistry;
import org.spongepowered.api.CatalogKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.value.CompositeValueStore;
import org.spongepowered.api.data.value.ValueContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("ALL")
public final class DataHelper {

    // Old data class
    private static final DataQuery DATA_CLASS = of("DataClass");

    public static DataView checkDataExists(DataView dataView, DataQuery query) throws InvalidDataException {
        checkNotNull(dataView);
        checkNotNull(query);
        if (!dataView.contains(query)) {
            throw new InvalidDataException("Missing data for query: " + query.asString('.'));
        } else {
            return dataView;
        }
    }

    public static DataView getOrCreateView(DataView dataView, DataQuery query) {
        return dataView.getView(query).orElseGet(() -> dataView.createView(query));
    }

    public static DataContainer toContainer(ValueContainerBase valueContainer) {
        final DataContainer dataContainer = DataContainer.createNew();
        final LocalKeyRegistry localKeyRegistry = valueContainer.getValueCollection();
        final LanternDataManager dataManager = Lantern.getGame().getDataManager();
        for (KeyRegistration<?,?> registration : localKeyRegistry.getAll()) {
            if (!(registration instanceof Element)) {
                continue;
            }
            final Key<?> key = registration.getKey();
            final DataQuery dataQuery = key.getQuery();
            final TypeToken<?> typeToken = key.getElementToken();
            final DataTypeSerializer typeSerializer = DataSerializerRegistry.INSTANCE.getTypeSerializer(typeToken)
                    .orElseThrow(() -> new IllegalStateException("Wasn't able to find a type serializer for the element type: " + typeToken.toString()));
            final DataTypeSerializerContext context = DataSerializerRegistry.INSTANCE.getTypeSerializerContext();
            // The value's shouldn't be null inside a data manipulator,
            // since it doesn't support removal of values
            dataContainer.set(dataQuery, typeSerializer.serialize(typeToken, context,
                    checkNotNull(((Element) registration).get(), "element")));
        }
        return dataContainer;
    }

    public static <T extends ValueContainerBase> Optional<T> buildContent(DataView container, Supplier<T> manipulatorSupplier)
            throws InvalidDataException {
        final T manipulator = manipulatorSupplier.get();
        deserializeRawData(container, manipulator);
        return Optional.of(manipulator);
    }

    public static void serializeRawData(DataView dataView, ValueContainerBase valueContainer) {
        serializeRawRegisteredKeyData(dataView, valueContainer);
        serializeRawContainerData(dataView, valueContainer);
    }

    public static void serializeRawContainerData(DataView dataView, ValueContainerBase valueContainer) {
        serializeRawContainerData(dataView, valueContainer, DataQueries.DATA_MANIPULATORS);
    }

    public static void serializeRawContainerData(DataView dataView, ValueContainerBase valueContainer, DataQuery query) {
        if (!(valueContainer instanceof AdditionalContainerHolder)) {
            return;
        }
        final LocalKeyRegistry localKeyRegistry = valueContainer.getValueCollection();
        final AdditionalContainerCollection<ValueContainer<?>> containers =
                ((AdditionalContainerHolder) valueContainer).getAdditionalContainers();
        final ImmutableList.Builder<DataView> builder = ImmutableList.builder();
        final LanternDataManager dataManager = Lantern.getGame().getDataManager();
        for (ValueContainer<?> manipulator : containers.getAll()) {
            if (!(manipulator instanceof DataManipulator)) {
                continue;
            }
            final Class<?> manipulatorType;
            if (manipulator instanceof IDataManipulator) {
                manipulatorType = ((IDataManipulator) manipulator).getMutableType();
            } else {
                manipulatorType = manipulator.getClass();
            }
            final Optional<DataRegistration> optRegistration = dataManager.get(manipulatorType);
            if (!optRegistration.isPresent()) {
                getLogger().error("Could not serialize {}. No registration could be found.", manipulatorType.getName());
            } else {
                builder.add(DataContainer.createNew()
                        .set(DataQueries.MANIPULATOR_ID, optRegistration.get().getKey().toString())
                        .set(DataQueries.MANIPULATOR_DATA, ((DataManipulator) manipulator).toContainer()));
            }
        }
        Element<List<DataView>> holder = localKeyRegistry.getElement(LanternKeys.FAILED_DATA_MANIPULATORS).orElse(null);
        if (holder != null) {
            builder.addAll(holder.get());
        }
        dataView.set(query, builder.build());
    }

    public static void serializeRawRegisteredKeyData(DataView dataView, ValueContainerBase valueContainer) {
        serializeRawRegisteredKeyData(dataView, valueContainer, Collections.emptySet());
    }

    public static void serializeRawRegisteredKeyData(DataView dataView, ValueContainerBase valueContainer,
            Set<Key> ignoredKeys) {
        DataView view = null;
        final LocalKeyRegistry localKeyRegistry = valueContainer.getValueCollection();
        final DataTypeSerializerContext context = DataSerializerRegistry.INSTANCE.getTypeSerializerContext();
        for (KeyRegistration<?,?> registration : localKeyRegistry.getAll()) {
            final Key<?> key = registration.getKey();
            if (!(registration instanceof Element)
                    || ignoredKeys.contains(key)
                    || key == LanternKeys.FAILED_DATA_MANIPULATORS
                    || key == LanternKeys.FAILED_DATA_VALUES) {
                continue;
            }
            final Element element = (Element) registration;
            final TypeToken<?> typeToken = key.getElementToken();
            final DataTypeSerializer typeSerializer = DataSerializerRegistry.INSTANCE.getTypeSerializer(typeToken)
                    .orElseThrow(() -> new IllegalStateException(
                            "Wasn't able to find a type serializer for the element type: " + typeToken.toString()));
            final Object object = element.get();
            if (object == null) {
                continue;
            }
            final Object value = typeSerializer.serialize(typeToken, context, object);
            if (view == null) {
                view = dataView.createView(DataQueries.DATA_VALUES);
            }
            view.set(key.getQuery(), value);
        }
        Element<DataView> holder = localKeyRegistry.getElement(LanternKeys.FAILED_DATA_VALUES).orElse(null);
        if (holder != null) {
            if (view == null) {
                view = dataView.createView(DataQueries.DATA_VALUES);
            }
            for (Map.Entry<DataQuery, Object> entry : holder.get().getValues(false).entrySet()) {
                if (!view.contains(entry.getKey())) {
                    view.set(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public static void deserializeRawData(DataView dataView, ValueContainerBase valueContainer)
            throws InvalidDataException {
        deserializeRawRegisteredKeyData(dataView, valueContainer);
        deserializeRawContainerData(dataView, valueContainer);
    }

    public static void deserializeRawContainerData(DataView dataView, ValueContainerBase valueContainer)
            throws InvalidDataException {
        deserializeRawContainerData(dataView, valueContainer, DataQueries.DATA_MANIPULATORS);
    }

    public static void deserializeRawContainerData(DataView dataView, ValueContainerBase valueContainer, DataQuery query)
            throws InvalidDataException {
        final List<DataView> dataViews = dataView.getViewList(query).orElse(null);
        if (dataViews == null) {
            return;
        }
        if (!(valueContainer instanceof AdditionalContainerHolder)) {
            getLogger().warn("{} is not a AdditionalContainerHolder, but data manipulators were found.", valueContainer);
            return;
        }
        final LocalKeyRegistry localKeyRegistry = valueContainer.getValueCollection();
        final AdditionalContainerCollection<ValueContainer<?>> containers =
                ((AdditionalContainerHolder) valueContainer).getAdditionalContainers();
        final List<DataView> failedData = new ArrayList<>();
        final LanternDataManager dataManager = Lantern.getGame().getDataManager();
        for (DataView view : dataViews) {
            Optional<DataRegistration> optRegistration;
            String id;
            if (view.contains(DataQueries.MANIPULATOR_ID)) {
                id = view.getString(DataQueries.MANIPULATOR_ID).get();
                optRegistration = (Optional) DataRegistrationRegistryModule.INSTANCE.get(CatalogKey.resolve(id));
            } else if (view.contains(DATA_CLASS)) {
                id = view.getString(DATA_CLASS).get();
                optRegistration = dataManager.getLegacyRegistration(id);
            } else {
                getLogger().warn("Manipulator with missing id.");
                continue;
            }
            final Optional<DataView> manipulatorView = view.getView(DataQueries.MANIPULATOR_DATA);
            if (manipulatorView.isPresent()) {
                getLogger().warn("Missing manipulator data for id: {}", id);
            }
            if (optRegistration.isPresent()) {
                try {
                    final Optional<DataManipulator> optManipulator = optRegistration.get()
                            .getDataManipulatorBuilder().build(manipulatorView.get());
                    if (optManipulator.isPresent()) {
                        containers.offer(optManipulator.get());
                    }
                } catch (InvalidDataException e) {
                    getLogger().error("Could not deserialize " + id
                            + "! Don't worry though, we'll try to deserialize the rest of the data.", e);
                }
            } else {
                getLogger().warn("Missing DataRegistration for ID: " + id + ". Don't worry, the data will be kept safe.");
                failedData.add(view);
            }
        }
        if (!failedData.isEmpty()) {
            // Should be safe to cast, at least if nobody touches this key
            Element<List<DataView>> holder = localKeyRegistry.getElement(LanternKeys.FAILED_DATA_MANIPULATORS).orElse(null);
            if (holder == null) {
                holder = localKeyRegistry.register(LanternKeys.FAILED_DATA_MANIPULATORS, null);
            }
            holder.set(failedData);
        }
    }

    public static void deserializeRawRegisteredKeyData(DataView dataView, ValueContainerBase valueContainer)
            throws InvalidDataException {
        dataView = dataView.getView(DataQueries.DATA_VALUES).orElse(null);
        if (dataView == null) {
            return;
        }
        final LocalKeyRegistry localKeyRegistry = valueContainer.getValueCollection();
        final DataTypeSerializerContext context = DataSerializerRegistry.INSTANCE.getTypeSerializerContext();
        for (KeyRegistration<?,?> registration : localKeyRegistry.getAll()) {
            final Key<?> key = registration.getKey();
            if (!(registration instanceof Element)
                    || key == LanternKeys.FAILED_DATA_MANIPULATORS
                    || key == LanternKeys.FAILED_DATA_VALUES) {
                continue;
            }
            final Optional<Object> data = dataView.get(key.getQuery());
            if (!data.isPresent()) {
                continue;
            }
            dataView.remove(key.getQuery());
            final TypeToken<?> typeToken = key.getElementToken();
            final DataTypeSerializer typeSerializer = DataSerializerRegistry.INSTANCE.getTypeSerializer(typeToken)
                    .orElseThrow(() -> new IllegalStateException(
                            "Wasn't able to find a type serializer for the element type: " + typeToken.toString()));
            ((Element) registration).set(typeSerializer.deserialize(typeToken, context, data.get()));
        }
        if (valueContainer instanceof CompositeValueStore) {
            final CompositeValueStore store = (CompositeValueStore) valueContainer;
            for (Map.Entry<DataQuery, Object> entry : dataView.getValues(false).entrySet()) {
                final Key<?> key = KeyRegistryModule.get().getByQuery(entry.getKey()).orElse(null);
                if (key == null) {
                    continue;
                }
                final TypeToken<?> typeToken = key.getElementToken();
                final DataTypeSerializer typeSerializer = DataSerializerRegistry.INSTANCE.getTypeSerializer(typeToken)
                        .orElseThrow(() -> new IllegalStateException(
                                "Wasn't able to find a type serializer for the element type: " + typeToken.toString()));
                store.offer(key, typeSerializer.deserialize(typeToken, context, entry.getValue()));
                dataView.remove(entry.getKey());
            }
        }
        if (!dataView.isEmpty()) {
            // Should be safe to cast, at least if nobody touches this key
            Element<DataView> holder = localKeyRegistry.getElement(LanternKeys.FAILED_DATA_VALUES).orElse(null);
            if (holder == null) {
                holder = localKeyRegistry.register(LanternKeys.FAILED_DATA_VALUES, null);
            }
            holder.set(dataView);
        }
    }

    public static String camelToSnake(String value) {
        final char[] name = value.toCharArray();
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < name.length; i++) {
            if (Character.isUpperCase(name[i]) || name[i] == '.' || name[i] == '$') {
                if (i != 0 && name[i - 1] != '.' && name[i - 1] != '$') {
                    builder.append('_');
                }
                if (name[i] != '.' && name[i] != '$') {
                    builder.append(Character.toLowerCase(name[i]));
                }
            } else {
                builder.append(name[i]);
            }
        }

        return builder.toString();
    }

    // Internal Methods: Don't use them outside the package

    @SuppressWarnings("unchecked")
    static boolean supports(ValueContainer<?> valueContainer, DataManipulatorRegistration registration) {
        for (Key key : (Set<Key>) registration.getRequiredKeys()) {
            if (!valueContainer.supports(key)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    static DataManipulator create(ValueContainer<?> valueContainer, DataManipulatorRegistration registration) {
        DataManipulator manipulator = (DataManipulator<?, ?>) registration.createMutable();
        for (Key key : (Set<Key>) registration.getRequiredKeys()) {
            final Optional value = valueContainer.get(key);
            if (value.isPresent()) {
                manipulator.set(key, value.get());
            } else if (!valueContainer.supports(key)) {
                manipulator = null;
                break;
            }
        }
        return manipulator;
    }
}
