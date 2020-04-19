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
package org.lanternpowered.server.config;

import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.lanternpowered.server.config.serializer.CatalogKeyTypeSerializer;
import org.lanternpowered.server.config.serializer.CatalogTypeSerializer;
import org.lanternpowered.server.config.serializer.DataViewTypeSerializer;
import org.lanternpowered.server.config.serializer.InetAddressTypeSerializer;
import org.lanternpowered.server.config.serializer.InstantTypeSerializer;
import org.lanternpowered.server.config.serializer.MultimapTypeSerializer;
import org.lanternpowered.server.config.serializer.ProxyTypeSerializer;
import org.lanternpowered.server.config.serializer.TextTypeSerializer;
import org.lanternpowered.server.network.ProxyType;
import org.lanternpowered.server.profile.LanternGameProfile;
import org.lanternpowered.server.profile.LanternProfileProperty;
import org.lanternpowered.server.text.serializer.TextTemplateArgConfigSerializer;
import org.lanternpowered.server.text.serializer.TextTemplateConfigSerializer;
import org.lanternpowered.server.util.IpSet;
import org.spongepowered.api.CatalogKey;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class ConfigBase {

    protected static final ConfigurationOptions DEFAULT_OPTIONS;

    static {
        final TypeSerializerCollection typeSerializers = TypeSerializers.getDefaultSerializers();
        final DataViewTypeSerializer dataViewTypeSerializer = new DataViewTypeSerializer();
        typeSerializers
                .registerType(TypeToken.of(Text.class), new TextTypeSerializer())
                .registerType(TypeToken.of(TextTemplate.class), new TextTemplateConfigSerializer())
                .registerType(TypeToken.of(TextTemplate.Arg.class), new TextTemplateArgConfigSerializer())
                .registerType(TypeToken.of(CatalogType.class), new CatalogTypeSerializer())
                .registerType(TypeToken.of(CatalogKey.class), new CatalogKeyTypeSerializer())
                .registerType(TypeToken.of(IpSet.class), new IpSet.IpSetSerializer())
                .registerType(TypeToken.of(GameProfile.class), (TypeSerializer) typeSerializers.get(
                        TypeToken.of(LanternGameProfile.class)))
                .registerType(TypeToken.of(ProfileProperty.class), (TypeSerializer) typeSerializers.get(
                        TypeToken.of(LanternProfileProperty.class)))
                .registerType(TypeToken.of(InetAddress.class), new InetAddressTypeSerializer())
                .registerType(TypeToken.of(Instant.class), new InstantTypeSerializer())
                .registerType(TypeToken.of(Multimap.class), new MultimapTypeSerializer())
                .registerType(TypeToken.of(DataView.class), dataViewTypeSerializer)
                .registerType(TypeToken.of(DataContainer.class), dataViewTypeSerializer)
                .registerType(TypeToken.of(ProxyType.class), new ProxyTypeSerializer())
                ;
        DEFAULT_OPTIONS = ConfigurationOptions.defaults().setSerializers(typeSerializers);
    }

    private final ObjectMapper<ConfigBase>.BoundInstance configMapper;
    private final ConfigurationLoader<ConfigurationNode> loader;
    private final ConfigurationOptions options;
    private final Path path;
    private final boolean hocon;

    private volatile ConfigurationNode root;

    /**
     * Creates a new config object.
     * 
     * @param path The config path
     */
    public ConfigBase(Path path, boolean hocon) throws IOException {
        this(path, DEFAULT_OPTIONS, hocon);
    }

    /**
     * Creates a new config object.
     * 
     * @param path The config file path
     * @param options The config options
     */
    public ConfigBase(Path path, ConfigurationOptions options, boolean hocon) throws IOException {
        this.hocon = hocon;
        this.loader = createConfigurationLoader(path, options, hocon);
        try {
            this.configMapper = ObjectMapper.forObject(this);
        } catch (ObjectMappingException e) {
            throw new IOException("Unable to create a config mapper for the object.", e);
        }
        this.root = SimpleCommentedConfigurationNode.root(options);
        this.options = options;
        this.path = path;
    }

    private static ConfigurationLoader<ConfigurationNode> createConfigurationLoader(Path path, ConfigurationOptions options, boolean hocon) {
        if (hocon) {
            return (ConfigurationLoader) HoconConfigurationLoader.builder().setPath(path).setDefaultOptions(options).build();
        } else {
            return GsonConfigurationLoader.builder().setPath(path).setDefaultOptions(options).build();
        }
    }

    public Path getPath() {
        return this.path;
    }

    public void load() throws IOException {
        if (!Files.exists(this.path)) {
            this.save();
        } else {
            this.root = this.loader.load(this.options);
            try {
                this.configMapper.populate(this.root);
            } catch (ObjectMappingException e) {
                throw new IOException("An error occurred while serializing the object.", e);
            }
        }
    }

    public void loadFrom(Path path) throws IOException {
        if (Files.exists(path)) {
            ConfigurationLoader<ConfigurationNode> loader = createConfigurationLoader(path, this.options, this.hocon);
            this.root = loader.load(this.options);
            try {
                this.configMapper.populate(this.root);
            } catch (ObjectMappingException e) {
                throw new IOException("An error occurred while serializing the object.", e);
            }
        }
    }

    public void copyFrom(ConfigBase configBase) throws IOException {
        try {
            configBase.configMapper.serialize(this.root);
            this.configMapper.populate(this.root);
        } catch (ObjectMappingException e) {
            throw new IOException("An error occurred while mapping the object.", e);
        }
    }

    public void save() throws IOException {
        if (!Files.exists(this.path.getParent())) {
            Files.createDirectories(this.path.getParent());
        }
        try {
            this.configMapper.serialize(this.root);
        } catch (ObjectMappingException e) {
            throw new IOException("An error occurred while mapping the object.", e);
        }
        this.loader.save(this.root);
    }

}
