package dev.nohus.rift.network.esi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UniverseIdsResponse(
    @SerialName("characters")
    val characters: List<UniverseIdsCharacter>? = null,
)

@Serializable
data class UniverseName(
    @SerialName("category")
    val category: UniverseNamesCategory,
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
)

@Serializable
enum class UniverseNamesCategory {
    @SerialName("character")
    Character,

    @SerialName("constellation")
    Constellation,

    @SerialName("corporation")
    Corporation,

    @SerialName("inventory_type")
    InventoryType,

    @SerialName("region")
    Region,

    @SerialName("solar_system")
    SolarSystem,

    @SerialName("station")
    Station,

    @SerialName("faction")
    Faction,

    @SerialName("alliance")
    Alliance,
}

@Serializable
data class UniverseIdsCharacter(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
)

@Serializable
data class CharactersIdCharacter(
    @SerialName("alliance_id")
    val allianceId: Int? = null,
    @SerialName("corporation_id")
    val corporationId: Int,
    @SerialName("name")
    val name: String,
    @SerialName("title")
    val title: String? = null,
)

@Serializable
data class CorporationsIdCorporation(
    @SerialName("name")
    val name: String,
    @SerialName("ticker")
    val ticker: String,
)

@Serializable
data class AlliancesIdAlliance(
    @SerialName("name")
    val name: String,
    @SerialName("ticker")
    val ticker: String,
)

@Serializable
data class CharacterIdOnline(
    @SerialName("last_login")
    val lastLogin: String? = null,
    @SerialName("last_logout")
    val lastLogout: String? = null,
    @SerialName("logins")
    val logins: Int? = null,
    @SerialName("online")
    val isOnline: Boolean,
)

@Serializable
data class CharacterIdLocation(
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("station_id")
    val stationId: Int? = null,
    @SerialName("structure_id")
    val structureId: Long? = null,
)

@Serializable
data class UniverseStationsId(
    @SerialName("name")
    val name: String,
    @SerialName("system_id")
    val systemId: Int,
    @SerialName("type_id")
    val typeId: Int,
)

@Serializable
data class UniverseStructuresId(
    @SerialName("name")
    val name: String,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("type_id")
    val typeId: Int? = null,
)

@Serializable
data class CharactersIdSearch(
    @SerialName("structure")
    val structure: List<Long> = emptyList(),
)

@Serializable
data class CharactersIdAsset(
    @SerialName("is_blueprint_copy")
    val isBlueprintCopy: Boolean? = null,
    @SerialName("is_singleton")
    val isSingleton: Boolean,
    @SerialName("item_id")
    val itemId: Long,
    @SerialName("location_flag")
    val locationFlag: String,
    @SerialName("location_id")
    val locationId: Long,
    @SerialName("location_type")
    val locationType: CharactersIdAssetLocationType,
    @SerialName("quantity")
    val quantity: Int,
    @SerialName("type_id")
    val typeId: Int,
)

@Serializable
enum class CharactersIdAssetLocationType {
    @SerialName("station")
    Station,

    @SerialName("solar_system")
    SolarSystem,

    @SerialName("item")
    Item,

    @SerialName("other")
    Other,
}

@Serializable
data class CharactersIdAssetsName(
    @SerialName("item_id")
    val itemId: Long,
    @SerialName("name")
    val name: String,
)
