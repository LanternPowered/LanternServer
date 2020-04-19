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
package org.lanternpowered.server.network.status

import org.lanternpowered.api.util.ToStringHelper
import org.lanternpowered.api.util.optional.emptyOptional
import org.lanternpowered.api.util.optional.optional
import org.lanternpowered.server.game.version.LanternMinecraftVersion
import org.spongepowered.api.MinecraftVersion
import org.spongepowered.api.event.server.ClientPingServerEvent
import org.spongepowered.api.network.status.Favicon
import org.spongepowered.api.text.Text

class LanternStatusResponse(
        private var version: MinecraftVersion,
        private var description: Text,
        private val players: ClientPingServerEvent.Response.Players,
        private var favicon: Favicon?
) : ClientPingServerEvent.Response {

    private var hidePlayers = false

    /**
     * Sets the [MinecraftVersion] of the status response.
     *
     * The name of this version will be displayed on the client when the
     * server or the client is outdated.
     *
     * @param version The version
     */
    fun setVersion(version: MinecraftVersion) = run { this.version = version }

    /**
     * Sets the [MinecraftVersion] of the status response.
     *
     * The name of this version will be displayed on the client when the
     * server or the client is outdated.
     *
     * @param name The name
     * @param protocol The protocol version
     * @param legacy Whether the version is legacy
     */
    fun setVersion(name: String, protocol: Int, legacy: Boolean) {
        setVersion(LanternMinecraftVersion(name, protocol, legacy))
    }

    override fun getDescription() = this.description
    override fun getVersion() = this.version
    override fun getFavicon() = this.favicon.optional()
    override fun getPlayers() = if (this.hidePlayers) emptyOptional() else this.players.optional()

    override fun setDescription(description: Text) = run { this.description = description }
    override fun setHidePlayers(hide: Boolean) = run { this.hidePlayers = hide }
    override fun setFavicon(favicon: Favicon?) = run { this.favicon = favicon }

    override fun toString(): String {
        return ToStringHelper(this)
                .omitNullValues()
                .add("version", this.version)
                .add("description", this.description)
                .add("players", this.players)
                .add("hidePlayers", this.hidePlayers)
                .add("favicon", this.favicon)
                .toString()
    }
}
