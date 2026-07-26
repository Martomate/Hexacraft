use std::{collections::HashMap, f64::consts::PI};

use glam::DVec3;
use uuid::Uuid;

use crate::server::nbt;

const SQRT_3: f64 = 1.732050807568877293527446341505872367_f64;

pub struct WorldInfo {
    pub version: u16,
    pub world_name: String,
    pub world_size: CylinderSize,
    pub _gen: WorldGenSettings,
}

pub struct WorldGenSettings {
    pub seed: u64,
    pub block_gen_scale: f64,
    pub height_map_gen_scale: f64,
    pub block_density_gen_scale: f64,
    pub biome_height_map_gen_scale: f64,
    pub biome_height_variation_gen_scale: f64,
}

/// The real cylinder size (the number of chunks around the cylinder) is:<br> <code>ringSize =
/// 2&#94;sizeExponent</code>
///
/// @param worldSize
///   the size exponent, <b>max-value: 20</b>
#[derive(Clone, Copy)]
pub struct CylinderSize(pub u8);

impl CylinderSize {
    const Y60: f64 = SQRT_3 / 2.0;

    /** The number of chunks around the cylinder */
    pub fn ring_size(self) -> u32 {
        1 << self.0
    }

    /** ringSize - 1 */
    pub fn ring_size_mask(self) -> u32 {
        self.ring_size() - 1
    }

    /** The number of blocks around the cylinder */
    pub fn total_size(self) -> u32 {
        16 * self.ring_size()
    }

    /** totalSize - 1 */
    pub fn total_size_mask(self) -> u32 {
        self.total_size() - 1
    }

    /** The angle (in radians) of half a block seen from the center of the cylinder */
    pub fn hex_angle(self) -> f64 {
        (2.0 * PI) / self.total_size() as f64
    }

    /** The radius of the cylinder */
    pub fn radius(self) -> f64 {
        CylinderSize::Y60 / self.hex_angle()
    }

    /** The circumference of the cylinder.<br><br>This is NOT the number of blocks, for that see
     * <code>totalSize</code>.
     */
    pub fn circumference(self) -> f64 {
        self.total_size() as f64 * CylinderSize::Y60
    }
}

pub struct HexBox {
    pub radius: f32,
    pub bottom: f32,
    pub top: f32,
}

#[derive(Clone, Copy, PartialEq)]
pub struct Block(u8);

impl Block {
    const Air: Block = Block(0);
    const Stone: Block = Block(1);
    const Grass: Block = Block(2);
    const Dirt: Block = Block(3);
    const Sand: Block = Block(4);
    const Water: Block = Block(5);
    const OakLog: Block = Block(6);
    const OakLeaves: Block = Block(7);
    const Planks: Block = Block(8);
    const BirchLog: Block = Block(9);
    const BirchLeaves: Block = Block(10);
    const Tnt: Block = Block(11);
    const Glass: Block = Block(12);

    pub fn id(&self) -> u8 {
        self.0
    }
}

pub struct Inventory(HashMap<u8, Block>);

impl From<HashMap<u8, u8>> for Inventory {
    fn from(value: HashMap<u8, u8>) -> Self {
        Self(value.iter().map(|(&slot, &id)| (slot, Block(id))).collect())
    }
}

impl Inventory {
    pub fn default() -> Self {
        Self(HashMap::from_iter([
            (0, Block::Dirt),
            (1, Block::Grass),
            (2, Block::Sand),
            (3, Block::Stone),
            (4, Block::Water),
            (5, Block::OakLog),
            (6, Block::OakLeaves),
            (7, Block::Planks),
            (8, Block::BirchLog),
            (9, Block::BirchLeaves),
            (10, Block::Tnt),
        ]))
    }

    pub fn at_slot(&self, slot: u8) -> Option<Block> {
        self.0.get(&slot).cloned()
    }

    pub fn slots(&self) -> impl Iterator<Item = (u8, Block)> {
        self.0.iter().map(|(&idx, &block)| (idx, block))
    }
}

const AIR: Block = Block::Air;

pub struct Player {
    pub id: Uuid,
    pub name: String,
    pub inventory: Inventory,
    pub bounds: HexBox,
    pub velocity: DVec3,
    pub position: DVec3,
    pub rotation: DVec3,
    pub flying: bool,
    pub selected_item_slot: u8,
}

impl Player {
    pub fn new(id: Uuid, name: String, inventory: Inventory) -> Self {
        Self {
            id,
            name,
            inventory,
            bounds: HexBox {
                radius: 0.2,
                bottom: -1.65,
                top: 0.1,
            },
            velocity: DVec3::ZERO,
            position: DVec3::ZERO,
            rotation: DVec3::ZERO,
            flying: false,
            selected_item_slot: 0,
        }
    }

    pub fn block_in_hand(&self) -> Block {
        self.inventory
            .at_slot(self.selected_item_slot)
            .unwrap_or(AIR)
    }
}

#[derive(PartialEq, Eq, Hash)]
pub enum WorldProviderPath {
    ChunkData(ChunkRelWorld),
    ColumnData(ColumnRelWorld),
    PlayerData(Uuid),
    WorldData,
}

pub trait NbtDecoder: Sized {
    fn decode(tag: &nbt::Tag) -> Result<Self, String>;
}

pub trait NbtEncoder {
    fn encode(&self) -> nbt::Tag;
}

impl NbtDecoder for Player {
    fn decode(tag: &nbt::Tag) -> Result<Self, String> {
        let tag = tag.as_map().ok_or("tag was not a map tag")?;

        let inventory = match tag.get("inventory") {
            Some(tag) => Inventory::decode(tag)?,
            None => Inventory::default(),
        };

        let mut player = Player::new(Uuid::nil(), "".to_string(), inventory);

        player.position = tag
            .get("position")
            .and_then(|t| t.as_vector())
            .ok_or("missing position")?;
        player.rotation = tag
            .get("rotation")
            .and_then(|t| t.as_vector())
            .ok_or("missing rotation")?;
        player.velocity = tag
            .get("velocity")
            .and_then(|t| t.as_vector())
            .ok_or("missing velocity")?;

        player.flying = tag.get("flying").and_then(|t| t.as_byte()).unwrap_or(0) != 0;
        player.selected_item_slot = tag
            .get("selectedItemSlot")
            .and_then(|t| t.as_short())
            .unwrap_or(0) as u8;

        Ok(player)
    }
}

impl NbtEncoder for Player {
    fn encode(&self) -> nbt::Tag {
        nbt::MapTag::new()
            .set("position", nbt::make_vector_tag(self.position))
            .set("rotation", nbt::make_vector_tag(self.rotation))
            .set("velocity", nbt::make_vector_tag(self.velocity))
            .set("flying", nbt::Tag::Byte(if self.flying { 1 } else { 0 }))
            .set(
                "selectedItemSlot",
                nbt::Tag::Short(self.selected_item_slot as i16),
            )
            .set("inventory", self.inventory.encode())
            .build()
    }
}

impl NbtDecoder for Inventory {
    fn decode(tag: &nbt::Tag) -> Result<Self, String> {
        let tag = tag.as_map().ok_or("not a map")?;

        match tag.get("slots").and_then(|t| t.as_list()) {
            Some(slot_tags) =>{
                let slots = HashMap::from_iter(slot_tags.iter().filter_map(|s| s.as_map()).filter_map(|s| {
                    let idx = s.get("slot").and_then(|t| t.as_byte()).unwrap_or(-1);
                    let id = s.get("id").and_then(|t| t.as_byte()).unwrap_or(-1);

                    if idx != -1 && id != -1 {
                        Some((idx as u8, Block(id as u8)))
                    } else {
                        None
                    }
                }));
                Ok(Inventory(slots))
            }
            None => Ok(Inventory(HashMap::new())),
        }
    }
}

impl NbtEncoder for Inventory {
    fn encode(&self) -> nbt::Tag {
        let slots = self
            .slots()
            .filter(|(_, b)| *b != Block::Air)
            .map(|(idx, block)| {
                nbt::MapTag::new()
                    .set("slot", nbt::Tag::Byte(idx as i8))
                    .set("id", nbt::Tag::Byte(block.id() as i8))
                    .build()
            })
            .collect();

        nbt::MapTag::new()
            .set("slots", nbt::Tag::List(slots))
            .build()
    }
}

pub trait WorldProvider {
    fn load_state(&self, path: WorldProviderPath) -> Option<nbt::Tag>;
    fn save_state(&mut self, path: WorldProviderPath, tag: nbt::Tag);
}

pub struct InMemoryWorldProvider {
    data: HashMap<WorldProviderPath, nbt::Tag>,
}

impl InMemoryWorldProvider {
    pub fn new() -> Self {
        Self {
            data: HashMap::new(),
        }
    }
}

impl WorldProvider for InMemoryWorldProvider {
    fn load_state(&self, path: WorldProviderPath) -> Option<nbt::Tag> {
        self.data.get(&path).cloned()
    }

    fn save_state(&mut self, path: WorldProviderPath, tag: nbt::Tag) {
        self.data.insert(path, tag);
    }
}

pub struct World {}

impl World {
    pub fn height(&self, x: i32, z: i32) -> i32 {
        17 // todo!()
    }
}

#[derive(PartialEq, Eq, Hash)]
pub struct ChunkRelWorld {
    x: i32,
    y: i32,
    z: i32,
}

#[derive(PartialEq, Eq, Hash)]
pub struct ColumnRelWorld {
    x: i32,
    z: i32,
}

pub struct BlockCoords(DVec3);
pub struct CylCoords(DVec3);

impl From<DVec3> for BlockCoords {
    fn from(value: DVec3) -> Self {
        Self(value)
    }
}

impl From<BlockCoords> for CylCoords {
    fn from(block: BlockCoords) -> Self {
        Self(conversion::skew_to_cyl(conversion::block_to_skew(block.0)))
    }
}

impl From<CylCoords> for DVec3 {
    fn from(value: CylCoords) -> Self {
        value.0
    }
}

mod conversion {
    use crate::server::world::SQRT_3;
    use glam::DVec3;

    const Y60: f64 = SQRT_3 / 2.0;

    pub fn block_to_skew(DVec3 { x, y, z }: DVec3) -> DVec3 {
        DVec3 {
            x: x * Y60,
            y: y * 0.5,
            z: z * Y60,
        }
    }

    pub fn skew_to_cyl(DVec3 { x, y, z }: DVec3) -> DVec3 {
        DVec3 {
            x: x * Y60,
            y,
            z: z + x * 0.5,
        }
    }
}
