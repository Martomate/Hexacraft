use glam::DVec3;

use crate::server::coords::{ChunkRelWorld, CylCoords};
use crate::server::nbt;
use crate::server::state::{ServerMessage, ServerMessageSender};
use crate::server::world::{Inventory, Player, WorldInfo};

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
    pub new_messages: Vec<ServerMessage>,
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
            .set(
                "messages",
                nbt::Tag::List(
                    res.new_messages
                        .iter()
                        .map(|m| {
                            let text = nbt::Tag::String(m.text.clone());
                            let sender = {
                                let (kind, tag) = match &m.sender {
                                    ServerMessageSender::Server => ("server", nbt::MapTag::new()),
                                    ServerMessageSender::Player { name } => (
                                        "player",
                                        nbt::MapTag::new()
                                            .set("name", nbt::Tag::String(name.to_string())),
                                    ),
                                };
                                tag.set("kind", nbt::Tag::String(kind.to_string())).build()
                            };
                            nbt::MapTag::new()
                                .set("text", text)
                                .set("sender", sender)
                                .build()
                        })
                        .collect(),
                ),
            )
            .build()
    }
}

pub struct GetWorldLoadingEventsResponse {
    pub loaded: Vec<LoadedChunk>,
    pub unloaded: Vec<ChunkRelWorld>,
}

pub struct LoadedChunk {
    pub coords: ChunkRelWorld,
    pub data: LoadedChunkData,
}

impl From<LoadedChunk> for nbt::Tag {
    fn from(c: LoadedChunk) -> Self {
        nbt::MapTag::new()
            .set("coords", nbt::Tag::Long(c.coords.encoded() as i64))
            .set("data", nbt::Tag::from(c.data))
            .build()
    }
}

pub struct LoadedChunkData {
    pub blocks: Vec<u8>,
    pub metadata: Vec<u8>,
    pub entities: Vec<LoadedChunkEntity>,
    pub is_decorated: bool,
}

impl From<LoadedChunkData> for nbt::Tag {
    fn from(data: LoadedChunkData) -> Self {
        let blocks = unsafe { std::mem::transmute::<Vec<u8>, Vec<i8>>(data.blocks) };
        let metadata = unsafe { std::mem::transmute::<Vec<u8>, Vec<i8>>(data.metadata) };
        let entities = data.entities.into_iter().map(nbt::Tag::from).collect();
        let is_decorated = if data.is_decorated { 1 } else { 0 };

        nbt::MapTag::new()
            .set("blocks", nbt::Tag::ByteArray(blocks))
            .set("metadata", nbt::Tag::ByteArray(metadata))
            .set("entities", nbt::Tag::List(entities))
            .set("isDecorated", nbt::Tag::Byte(is_decorated))
            .build()
    }
}

pub struct LoadedChunkEntity {
    pub _type: String,
    pub id: String,
    pub pos: CylCoords,
    pub velocity: DVec3,
    pub rotation: DVec3,
    pub ai: Option<LoadedChunkEntityAI>,
}

impl From<LoadedChunkEntity> for nbt::Tag {
    fn from(e: LoadedChunkEntity) -> Self {
        nbt::MapTag::new()
            .set("type", nbt::Tag::String(e._type))
            .set("id", nbt::Tag::String(e.id))
            .set("pos", nbt::make_vector_tag(e.pos.into()))
            .set("velocity", nbt::make_vector_tag(e.velocity))
            .set("rotation", nbt::make_vector_tag(e.rotation))
            .set_opt("ai", e.ai.map(nbt::Tag::from))
            .build()
    }
}

pub enum LoadedChunkEntityAI {
    Simple {
        target_x: f64,
        target_z: f64,
        timeout: i16,
    },
}

impl From<LoadedChunkEntityAI> for nbt::Tag {
    fn from(ai: LoadedChunkEntityAI) -> Self {
        match ai {
            LoadedChunkEntityAI::Simple {
                target_x,
                target_z,
                timeout,
            } => nbt::MapTag::new()
                .set("type", nbt::Tag::String("simple".to_string()))
                .set("targetX", nbt::Tag::Double(target_x))
                .set("targetZ", nbt::Tag::Double(target_z))
                .set("timeout", nbt::Tag::Short(timeout))
                .build(),
        }
    }
}

impl From<GetWorldLoadingEventsResponse> for nbt::Tag {
    fn from(res: GetWorldLoadingEventsResponse) -> Self {
        nbt::MapTag::new()
            .set(
                "chunks_loaded",
                nbt::Tag::List(res.loaded.into_iter().map(nbt::Tag::from).collect()),
            )
            .set(
                "chunks_unloaded",
                nbt::Tag::List(
                    res.unloaded
                        .into_iter()
                        .map(|c| nbt::Tag::Long(c.encoded() as i64))
                        .collect(),
                ),
            )
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
                    .slots()
                    .map(|(slot, block)| {
                        nbt::MapTag::new()
                            .set("slot", nbt::Tag::Byte(slot as i8))
                            .set("id", nbt::Tag::Byte(block.id() as i8))
                            .build()
                    })
                    .collect(),
            ),
        )
        .build()
}
