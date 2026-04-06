use crate::server::{
    nbt,
    world::{Inventory, Player, WorldInfo},
};

pub struct LoginResponse<'r> {
    pub success: bool,
    pub error: Option<&'r str>,
}

impl<'r> LoginResponse<'r> {
    pub fn success() -> Self {
        Self {
            success: true,
            error: None,
        }
    }

    pub fn failure(error: &'r str) -> Self {
        Self {
            success: false,
            error: Some(error),
        }
    }
}

impl<'r> From<LoginResponse<'r>> for nbt::Tag {
    fn from(res: LoginResponse) -> Self {
        let success = nbt::Tag::Byte(if res.success { 1 } else { 0 });
        let error = res.error.map(|error| nbt::Tag::String(error.to_string()));

        nbt::MapTag::new()
            .set("success", success)
            .set_opt("error", error)
            .build()
    }
}

pub struct GetWorldInfoResponse<'r> {
    pub info: &'r WorldInfo,
}

impl<'r> From<GetWorldInfoResponse<'r>> for nbt::Tag {
    fn from(res: GetWorldInfoResponse) -> Self {
        let info = res.info;

        nbt::MapTag::new()
            .set("version", nbt::Tag::Short(info.version as i16))
            .set(
                "general",
                nbt::MapTag::new()
                    .set("worldSize", nbt::Tag::Byte(info.world_size.0 as i8))
                    .set("name", nbt::Tag::String(info.world_name.clone()))
                    .build(),
            )
            .set("gen", {
                let s = &info._gen;
                nbt::MapTag::new()
                    .set("seed", nbt::Tag::Long(s.seed as i64))
                    .set("blockGenScale", nbt::Tag::Double(s.block_gen_scale))
                    .set(
                        "heightMapGenScale",
                        nbt::Tag::Double(s.height_map_gen_scale),
                    )
                    .set(
                        "blockDensityGenScale",
                        nbt::Tag::Double(s.block_density_gen_scale),
                    )
                    .set(
                        "biomeHeightGenScale",
                        nbt::Tag::Double(s.biome_height_map_gen_scale),
                    )
                    .set(
                        "biomeHeightVariationGenScale",
                        nbt::Tag::Double(s.biome_height_variation_gen_scale),
                    )
                    .build()
            })
            .build()
    }
}

pub struct GetPlayerStateResponse<'r> {
    pub player: &'r Player,
}

impl<'r> From<GetPlayerStateResponse<'r>> for nbt::Tag {
    fn from(res: GetPlayerStateResponse<'r>) -> Self {
        let p = res.player;

        nbt::MapTag::new()
            .set("position", nbt::make_vector_tag(p.position))
            .set("rotation", nbt::make_vector_tag(p.rotation))
            .set("velocity", nbt::make_vector_tag(p.velocity))
            .set("flying", nbt::Tag::Byte(if p.flying { 1 } else { 0 }))
            .set(
                "selectedItemSlot",
                nbt::Tag::Short(p.selected_item_slot as i16),
            )
            .set("inventory", encode_inventory(&p.inventory))
            .build()
    }
}

pub struct GetEventsResponse {
    pub server_shutting_down: bool,
}

impl From<GetEventsResponse> for nbt::Tag {
    fn from(res: GetEventsResponse) -> Self {
        nbt::MapTag::new()
            .set("block_updates", nbt::Tag::List(Vec::new()))
            .set(
                "entity_events",
                nbt::MapTag::new()
                    .set("ids", nbt::Tag::List(Vec::new()))
                    .set("events", nbt::Tag::List(Vec::new()))
                    .build(),
            )
            .set(
                "server_shutting_down",
                nbt::Tag::Byte(if res.server_shutting_down { 1 } else { 0 }),
            )
            .set("messages", nbt::Tag::List(Vec::new()))
            .build()
    }
}

pub struct GetWorldLoadingEventsResponse {}

impl From<GetWorldLoadingEventsResponse> for nbt::Tag {
    fn from(_res: GetWorldLoadingEventsResponse) -> Self {
        nbt::MapTag::new()
            .set("chunks_loaded", nbt::Tag::List(Vec::new()))
            .set("chunks_unloaded", nbt::Tag::List(Vec::new()))
            .build()
    }
}

pub struct PlayerUpdatedInventoryResponse<'r> {
    pub inventory: &'r Inventory,
}

impl<'r> From<PlayerUpdatedInventoryResponse<'r>> for nbt::Tag {
    fn from(res: PlayerUpdatedInventoryResponse<'r>) -> Self {
        encode_inventory(res.inventory)
    }
}

fn encode_inventory(inventory: &Inventory) -> nbt::Tag {
    nbt::MapTag::new()
        .set(
            "slots",
            nbt::Tag::List(
                inventory
                    .iter()
                    .map(|(&slot, &block)| {
                        nbt::MapTag::new()
                            .set("slot", nbt::Tag::Byte(slot as i8))
                            .set("id", nbt::Tag::Byte(block as i8))
                            .build()
                    })
                    .collect(),
            ),
        )
        .build()
}
