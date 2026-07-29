use std::{collections::HashMap, f64::consts::PI};

use glam::DVec3;
use uuid::Uuid;

use crate::server::{
    nbt,
    noise::{NoiseGenerator3D, NoiseGenerator4D},
};

const SQRT_3: f64 = 1.732050807568877293527446341505872367_f64;

pub struct WorldInfo {
    pub version: u16,
    pub world_name: String,
    pub world_size: CylinderSize,
    pub _gen: WorldGenSettings,
}

#[derive(Clone, Copy)]
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
    pub fn initial() -> Self {
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
            None => Inventory::initial(),
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
            Some(slot_tags) => {
                let slots = HashMap::from_iter(
                    slot_tags.iter().filter_map(|s| s.as_map()).filter_map(|s| {
                        let idx = s.get("slot").and_then(|t| t.as_byte()).unwrap_or(-1);
                        let id = s.get("id").and_then(|t| t.as_byte()).unwrap_or(-1);

                        if idx != -1 && id != -1 {
                            Some((idx as u8, Block(id as u8)))
                        } else {
                            None
                        }
                    }),
                );
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

pub struct World {
    generator: WorldGenerator,
}

impl World {
    pub fn new(_gen: WorldGenSettings, cyl: CylinderSize) -> Self {
        Self {
            generator: WorldGenerator::new(_gen, cyl),
        }
    }

    pub fn height(&self, x: i32, z: i32) -> i16 {
        let column_coords = ColumnRelWorld::new(x >> 4, z >> 4);
        let column_heights = self.generator.height_map_of_column(column_coords);
        column_heights[z as usize & 15][x as usize & 15]
    }

    pub fn height_map_of_column(&self, coords: ColumnRelWorld) -> Option<[[i16; 16]; 16]> {
        Some(self.generator.height_map_of_column(coords))
    }

    pub fn generate_chunk(
        &self,
        coords: ChunkRelWorld,
    ) -> ([u8; 16 * 16 * 16], [u8; 16 * 16 * 16]) {
        self.generator.generate_chunk(coords)
    }
}

struct WorldGenerator {
    block_generator: NoiseGenerator4D,
    block_density_generator: NoiseGenerator4D,
    biome_height_variation_generator: NoiseGenerator3D,
    biome_height_generator: NoiseGenerator3D,
    height_map_generator: NoiseGenerator3D,
    cyl: CylinderSize,
}

impl WorldGenerator {
    pub fn new(settings: WorldGenSettings, cyl: CylinderSize) -> Self {
        // TODO: use settings.seed
        Self {
            block_generator: NoiseGenerator4D::new(8, settings.block_gen_scale),
            block_density_generator: NoiseGenerator4D::new(4, settings.block_density_gen_scale),
            biome_height_variation_generator: NoiseGenerator3D::new(
                4,
                settings.biome_height_variation_gen_scale,
            ),
            biome_height_generator: NoiseGenerator3D::new(4, settings.biome_height_map_gen_scale),
            height_map_generator: NoiseGenerator3D::new(8, settings.height_map_gen_scale),
            cyl,
        }
    }

    pub fn height_map_of_column(&self, coords: ColumnRelWorld) -> [[i16; 16]; 16] {
        let grid_heights: [[f64; 5]; 5] = std::array::from_fn(|iz| {
            std::array::from_fn(|ix| {
                let x = (coords.x << 4) + ((ix as i32) << 2);
                let z = (coords.z << 4) + ((iz as i32) << 2);
                self.raw_height(x, z, self.cyl)
            })
        });
        std::array::from_fn(|dz| {
            std::array::from_fn(|dx| {
                let iz = dz >> 2;
                let ix = dx >> 2;
                let az = (dz & 3) as f64 * 0.25;
                let ax = (dx & 3) as f64 * 0.25;

                let h00 = grid_heights[iz][ix];
                let h01 = grid_heights[iz][ix + 1];
                let h10 = grid_heights[iz + 1][ix];
                let h11 = grid_heights[iz + 1][ix + 1];

                lerp(lerp(h00, h01, ax), lerp(h10, h11, ax), az) as i16
            })
        })
    }

    fn raw_height(&self, x: i32, z: i32, cyl: CylinderSize) -> f64 {
        let c = CylCoords::from(BlockCoords::new(x as f64, 0.0, z as f64));

        let biome_height = self
            .biome_height_generator
            .gen_wrapped_noise(c.x, c.z, cyl.radius());
        let biome_height_variation =
            self.biome_height_variation_generator
                .gen_wrapped_noise(c.x, c.z, cyl.radius());
        let height_map = self
            .height_map_generator
            .gen_wrapped_noise(c.x, c.z, cyl.radius());

        height_map * biome_height_variation * 100.0 + biome_height * 100.0
    }

    pub fn generate_chunk(
        &self,
        coords: ChunkRelWorld,
    ) -> ([u8; 16 * 16 * 16], [u8; 16 * 16 * 16]) {
        let grid_noise: [[[f64; 5]; 5]; 5] = std::array::from_fn(|iz| {
            std::array::from_fn(|iy| {
                std::array::from_fn(|ix| {
                    let x = (coords.x << 4) + ((ix as i32) << 2);
                    let y = (coords.y << 4) + ((iy as i32) << 2);
                    let z = (coords.z << 4) + ((iz as i32) << 2);
                    self.raw_block_noise(x, y, z, self.cyl)
                })
            })
        });
        let noise: [[[f64; 16]; 16]; 16] = std::array::from_fn(|dz| {
            std::array::from_fn(|dy| {
                std::array::from_fn(|dx| {
                    let iz = dz >> 2;
                    let iy = dy >> 2;
                    let ix = dx >> 2;
                    let az = (dz & 3) as f64 * 0.25;
                    let ay = (dy & 3) as f64 * 0.25;
                    let ax = (dx & 3) as f64 * 0.25;

                    let h000 = grid_noise[iz][iy][ix];
                    let h001 = grid_noise[iz][iy][ix + 1];
                    let h010 = grid_noise[iz][iy + 1][ix];
                    let h011 = grid_noise[iz][iy + 1][ix + 1];
                    let h100 = grid_noise[iz + 1][iy][ix];
                    let h101 = grid_noise[iz + 1][iy][ix + 1];
                    let h110 = grid_noise[iz + 1][iy + 1][ix];
                    let h111 = grid_noise[iz + 1][iy + 1][ix + 1];

                    lerp(
                        lerp(lerp(h000, h001, ax), lerp(h010, h011, ax), ay),
                        lerp(lerp(h100, h101, ax), lerp(h110, h111, ax), ay),
                        az,
                    )
                })
            })
        });

        let mut block_type = [0; 16 * 16 * 16];
        let metadata = [0; 16 * 16 * 16];

        let height_map = self.height_map_of_column(ColumnRelWorld::new(coords.x, coords.z));

        for z in 0..16 {
            for y in 0..16 {
                for x in 0..16 {
                    let n = noise[z][y][x];
                    let y_to_go = coords.y * 16 + y as i32 - height_map[z][x] as i32;
                    let limit = self.limit_for_block_noise(y_to_go);
                    if n > limit {
                        block_type
                            [BlockRelChunk::new(x as u8, y as u8, z as u8).encoded() as usize] =
                            self.get_block_at_depth(y_to_go).id();
                    }
                }
            }
        }

        (block_type, metadata)
    }

    fn limit_for_block_noise(&self, y_to_go: i32) -> f64 {
        if y_to_go < -6 {
            -0.4
        } else if y_to_go < 0 {
            -0.4 - (6.0 + y_to_go as f64) * 0.025
        } else {
            4.0
        }
    }

    fn get_block_at_depth(&self, y_to_go: i32) -> Block {
        if y_to_go < -5 {
            Block::Stone
        } else if y_to_go < -1 {
            Block::Dirt
        } else {
            Block::Grass
        }
    }

    fn raw_block_noise(&self, x: i32, y: i32, z: i32, cyl: CylinderSize) -> f64 {
        let c = CylCoords::from(BlockCoords::new(x as f64, y as f64, z as f64));

        let n1 = self
            .block_generator
            .gen_wrapped_noise(c.x, c.y, c.z, cyl.radius());
        let n2 = self
            .block_density_generator
            .gen_wrapped_noise(c.x, c.y, c.z, cyl.radius());

        n1 + n2 * 0.4
    }
}

fn lerp(from: f64, to: f64, a: f64) -> f64 {
    from + (to - from) * a
}

#[derive(PartialEq, Eq, Hash, Clone, Copy)]
pub struct BlockRelChunk {
    x: u8,
    y: u8,
    z: u8,
}

impl BlockRelChunk {
    pub fn new(x: u8, y: u8, z: u8) -> Self {
        Self { x, y, z }
    }

    pub fn encoded(&self) -> u16 {
        let x = (self.x & 0xf) as u16;
        let y = (self.y & 0xf) as u16;
        let z = (self.z & 0xf) as u16;
        x << 8 | y << 4 | z
    }
}

#[derive(PartialEq, Eq, Hash, Clone, Copy)]
pub struct ChunkRelWorld {
    x: i32,
    y: i32,
    z: i32,
}

impl ChunkRelWorld {
    pub fn new(x: i32, y: i32, z: i32) -> Self {
        Self { x, y, z }
    }

    pub fn encoded(&self) -> u64 {
        let x = (self.x & 0xfffff) as u64;
        let z = (self.z & 0xfffff) as u64;
        let y = (self.y & 0xfff) as u64;
        x << 32 | z << 12 | y
    }
}

#[derive(PartialEq, Eq, Hash, Clone, Copy)]
pub struct ColumnRelWorld {
    x: i32,
    z: i32,
}

impl ColumnRelWorld {
    pub fn new(x: i32, z: i32) -> Self {
        Self { x, z }
    }

    pub fn decode(value: u64) -> Self {
        Self {
            x: i20_to_i32((value >> 20) & 0xFFFFF),
            z: i20_to_i32(value & 0xFFFFF),
        }
    }
}

pub struct BlockCoords(DVec3);

impl BlockCoords {
    pub fn new(x: f64, y: f64, z: f64) -> Self {
        Self(DVec3 { x, y, z })
    }
}

pub struct CylCoords {
    x: f64,
    y: f64,
    z: f64,
}

fn i20_to_i32(value: u64) -> i32 {
    (value as i32) << 12 >> 12
}

impl From<DVec3> for BlockCoords {
    fn from(value: DVec3) -> Self {
        Self(value)
    }
}

impl From<BlockCoords> for CylCoords {
    fn from(block: BlockCoords) -> Self {
        Self::from(conversion::skew_to_cyl(conversion::block_to_skew(block.0)))
    }
}

impl From<CylCoords> for DVec3 {
    fn from(CylCoords { x, y, z }: CylCoords) -> Self {
        Self { x, y, z }
    }
}

impl From<DVec3> for CylCoords {
    fn from(DVec3 { x, y, z }: DVec3) -> Self {
        Self { x, y, z }
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
