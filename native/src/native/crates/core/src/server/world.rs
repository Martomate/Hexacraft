use std::collections::HashMap;
use std::f64::consts::PI;

use glam::{DMat2, DMat3, DVec2, DVec3};
use ordered_float::NotNan;
use uuid::Uuid;

use crate::server::coords::{
    BlockCoords, BlockRelChunk, BlockRelWorld, ChunkRelWorld, ColumnRelWorld, CylCoords, Y60,
};
use crate::server::noise::NoiseGenerator;
use crate::server::random::Random;
use crate::server::{nbt, physics};

pub(crate) const SQRT_3: f64 = 1.732050807568877293527446341505872367_f64;

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

#[derive(PartialEq, Clone, Copy)]
pub struct HexBox {
    pub radius: f32,
    pub bottom: f32,
    pub top: f32,
}

impl HexBox {
    pub fn scaledRadially(&self, scale: f32) -> Self {
        Self {
            radius: self.radius * scale,
            ..*self
        }
    }

    pub fn smallRadius(&self) -> f32 {
        (self.radius as f64 * Y60) as f32
    }

    pub fn base_area(&self) -> f64 {
        self.radius as f64 * self.radius as f64 * CylinderSize::Y60 * 3.0
    }

    pub fn volume(&self) -> f64 {
        self.base_area() * (self.top as f64 - self.bottom as f64)
    }

    pub fn projected_area_in_direction(&self, dir: DVec3) -> f64 {
        let projection = OrthogonalProjection::inDirection(dir).unwrap();
        let projected_vertices: Vec<_> = self
            .vertices()
            .into_iter()
            .map(|v| projection.project(v))
            .collect();
        let polygon = calculate_convex_hull(&projected_vertices);
        polygon.area()
    }

    fn vertices(&self) -> Vec<DVec3> {
        let mut result = Vec::<DVec3>::with_capacity(12);

        for s in 0..2 {
            for i in 0..6 {
                let v = i as f64 * PI / 3.0;
                let x = v.cos();
                let z = v.sin();

                result.push(DVec3::new(
                    x * self.radius as f64,
                    (1 - s) as f64 * (self.top - self.bottom) as f64 + self.bottom as f64,
                    z * self.radius as f64,
                ))
            }
        }
        result
    }

    /** Returns all blocks spaces that would intersect with this HexBox when placed at the given position */
    pub fn cover(&self, pos: CylCoords, cyl_size: CylinderSize) -> Vec<BlockRelWorld> {
        let y_lo = ((pos.y + self.bottom as f64) * 2.0).floor() as i64;
        let y_hi = ((pos.y + self.top as f64) * 2.0).floor() as i64;

        let mut result = Vec::<BlockRelWorld>::new();

        for y in y_lo..=y_hi {
            // TODO: improve this implementation to be more correct (the HexBox radius might be too big)
            for i in 0..9 {
                let dx = (i % 3) - 1;
                let dz = (i / 3) - 1;

                if dx * dz != 1 {
                    // remove corners
                    let origin = BlockCoords::from(pos).offset(dx as f64, 0.0, dz as f64);
                    result.push(
                        CoordUtils::getEnclosingBlock(
                            BlockCoords::new(origin.x, y as f64, origin.z),
                            cyl_size,
                        )
                        .0,
                    );
                }
            }
        }
        result
    }

    pub fn approximateVolumeOfIntersection(
        pos1: CylCoords,
        box1: HexBox,
        pos2: CylCoords,
        box2: HexBox,
    ) -> f64 {
        if box1.radius < box2.radius {
            return HexBox::approximateVolumeOfIntersection(pos2, box2, pos1, box1);
        }

        let r1 = box1.radius;
        let r2 = box2.radius;
        let d = (pos1.x - pos2.x).hypot(pos1.z - pos2.z);
        let a2 = box2.base_area();

        let base_area =
            math_utils::smoothstep(math_utils::remap(r1 - r2, r1 + r2, 1.0, 0.0, d as f32) as f64)
                * a2;

        let t1 = pos1.y + box1.top as f64;
        let b1 = pos1.y + box1.bottom as f64;
        let t2 = pos2.y + box2.top as f64;
        let b2 = pos2.y + box2.bottom as f64;

        let height = (t1.min(t2) - b1.max(b2)).max(0.0);

        base_area * height
    }
}

pub mod CoordUtils {
    use glam::{DVec3, IVec3};

    use crate::server::coords::{BlockCoords, BlockRelWorld, ChunkRelWorld, CylCoords};
    use crate::server::world::CylinderSize;

    pub fn getEnclosingBlock(vec: BlockCoords, cylSize: CylinderSize) -> (BlockRelWorld, DVec3) {
        let (x, y, z) = (vec.x, vec.y, vec.z);

        fn find_block_pos(x: f64, z: f64, x_int: i32, z_int: i32) -> (i32, i32) {
            let xx = x - x_int as f64;
            let zz = z - z_int as f64;

            let xp = xx + 0.5 * zz;
            let zp = zz + 0.5 * xx;
            let wp = zp - xp;

            if xp > 0.5 {
                find_block_pos(x, z, x_int + 1, z_int)
            } else if xp < -0.5 {
                find_block_pos(x, z, x_int - 1, z_int)
            } else if zp > 0.5 {
                find_block_pos(x, z, x_int, z_int + 1)
            } else if zp < -0.5 {
                find_block_pos(x, z, x_int, z_int - 1)
            } else if wp > 0.5 {
                find_block_pos(x, z, x_int - 1, z_int + 1)
            } else if wp < -0.5 {
                find_block_pos(x, z, x_int + 1, z_int - 1)
            } else {
                (x_int, z_int)
            }
        }

        let (x_int, z_int) = find_block_pos(x, z, x.round() as i32, z.round() as i32);

        let xx = x - x_int as f64;
        let zz = z - z_int as f64;
        let y_int = y.floor() as i32;

        (
            BlockRelWorld::new(x_int, y_int, z_int),
            DVec3::new(xx, y - y_int as f64, zz),
        )
    }

    fn approximateIntCoords(coords: BlockCoords, cyl_size: CylinderSize) -> BlockRelWorld {
        let X = coords.x.round() as i32;
        let Y = coords.y.round() as i32;
        let Z = coords.z.round() as i32;
        BlockRelWorld::new(X, Y, Z)
    }

    fn approximateChunkCoords(coords: CylCoords, cyl_size: CylinderSize) -> ChunkRelWorld {
        ChunkRelWorld::from(approximateIntCoords(BlockCoords::from(coords), cyl_size))
    }

    fn vectorToOffset(vec: DVec3) -> IVec3 {
        let blockCoords = BlockCoords::from(CylCoords::from(vec));
        IVec3::new(
            blockCoords.x.round() as i32,
            blockCoords.y.round() as i32,
            blockCoords.z.round() as i32,
        )
    }
}

mod math_utils {
    use glam::FloatExt as _;

    pub fn remap(from_lo: f32, from_hi: f32, to_lo: f32, to_hi: f32, value: f32) -> f32 {
        let t = (value - from_lo) / (from_hi - from_lo);
        to_lo.lerp(to_hi, t)
    }

    pub fn smoothstep(x: f64) -> f64 {
        let t = x.clamp(0.0, 1.0);
        ((3.0 - 2.0 * t) * t * t).clamp(0.0, 1.0)
    }
}

struct OrthogonalProjection(DMat3);

impl OrthogonalProjection {
    pub fn inDirection(dir: DVec3) -> Option<Self> {
        if dir.length_squared() == 0.0 {
            // fail fast instead of producing NaN results
            return None;
        }
        let up = unitVectorDifferentFrom(dir);
        let m = DMat3::look_to_rh(dir, up);
        Some(Self(m))
    }

    pub fn project(&self, v: DVec3) -> DVec2 {
        let p = self.0 * v;
        DVec2::new(p.x, p.y)
    }
}

fn unitVectorDifferentFrom(v: DVec3) -> DVec3 {
    if DVec3::X.dot(v).abs() < DVec3::Y.dot(v).abs() {
        DVec3::X
    } else {
        DVec3::Y
    }
}

fn calculate_convex_hull(points: &[DVec2]) -> SimplePolygon {
    let lowest_point = *points
        .iter()
        .min_by_key(|v| (NotNan::new(v.y).unwrap(), NotNan::new(v.x).unwrap()))
        .unwrap();
    let sorted_points = {
        let mut pts: Vec<_> = points.iter().collect();
        pts.sort_by_key(|&&p| {
            if p != lowest_point {
                let v = p - lowest_point;
                NotNan::new(v.y.atan2(v.x)).unwrap()
            } else {
                NotNan::new(-1.0).unwrap()
            }
        });
        pts
    };

    let mut hull = Vec::<DVec2>::new();
    hull.push(lowest_point);
    for &p in sorted_points.iter().skip(1) {
        let mut i = hull.len() - 1;
        while i > 0 && DMat2::from_cols(hull[i - 1] - hull[i], p - hull[i]).determinant() > 0.0 {
            hull.remove(i);
            i -= 1;
        }
        hull.push(*p);
    }

    SimplePolygon(hull)
}

struct SimplePolygon(Vec<DVec2>);

impl SimplePolygon {
    pub fn area(&self) -> f64 {
        let mut a = 0.0;
        let root = self.0[0];
        let mut prev = self.0[1] - root;

        for i in 2..self.0.len() {
            let now = self.0[i] - root;
            a += now.y * prev.x - now.x * prev.y;
            prev = now;
        }

        (a / 2.0).abs()
    }
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Block(pub u8);

impl Block {
    const Air: Block = Block(0);
    const Stone: Block = Block(1);
    const Grass: Block = Block(2);
    pub const Dirt: Block = Block(3);
    const Sand: Block = Block(4);
    pub const Water: Block = Block(5);
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

    pub fn bounds(self, metadata: u8) -> HexBox {
        let block_height = match self {
            Block::Water => 1.0 - (metadata & 0x1f) as f32 / (0x1f + 1) as f32,
            _ => 1.0,
        };
        HexBox {
            radius: 0.5,
            bottom: 0.0,
            top: 0.5 * block_height,
        }
    }

    pub fn is_solid(self) -> bool {
        match self {
            Block::Air => false,
            Block::Water => false,
            _ => true,
        }
    }

    pub fn viscosity(self) -> f64 {
        match self {
            Block::Air => physics::viscosity::AIR,
            Block::Water => physics::viscosity::WATER,
            _ => 0.0,
        }
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
    chunks: HashMap<ChunkRelWorld, ChunkData>,
}

impl World {
    pub fn new() -> Self {
        Self {
            chunks: HashMap::new(),
        }
    }

    pub fn set_chunk(&mut self, coords: ChunkRelWorld, data: ChunkData) {
        self.chunks.insert(coords, data);
    }

    pub fn get_block(&self, coords: BlockRelWorld) -> Option<BlockState> {
        self.chunks.get(&ChunkRelWorld::from(coords)).map(|chunk| {
            let b = BlockRelChunk::from(coords);
            let idx = b.encoded() as usize;
            BlockState {
                block_type: Block(chunk.block_type[idx]),
                metadata: chunk.metadata[idx],
            }
        })
    }
}

#[derive(Clone)]
pub struct ChunkData {
    pub block_type: [u8; 16 * 16 * 16],
    pub metadata: [u8; 16 * 16 * 16],
}

impl ChunkData {
    pub fn getBlock(&self, coords: BlockRelChunk) -> BlockState {
        let idx = coords.encoded() as usize;
        BlockState {
            block_type: Block(self.block_type[idx]),
            metadata: self.metadata[idx],
        }
    }

    pub fn from_blocks(blocks: impl IntoIterator<Item = (BlockRelChunk, BlockState)>) -> Self {
        let mut block_type = [0; 16 * 16 * 16];
        let mut metadata = [0; 16 * 16 * 16];

        for (c, b) in blocks {
            let c = c.encoded() as usize;
            block_type[c] = b.block_type.id();
            metadata[c] = b.metadata;
        }

        Self { block_type, metadata }
    }
}

#[derive(Debug)]
pub struct BlockState {
    pub block_type: Block,
    pub metadata: u8,
}

impl BlockState {
    pub const AIR: Self = Self {
        block_type: Block::Air,
        metadata: 0,
    };

    pub fn of(block_type: Block) -> Self {
        Self {
            block_type,
            metadata: 0,
        }
    }
}

pub struct WorldGenerator {
    block_generator: NoiseGenerator,
    block_density_generator: NoiseGenerator,
    biome_height_variation_generator: NoiseGenerator,
    biome_height_generator: NoiseGenerator,
    height_map_generator: NoiseGenerator,
    cyl: CylinderSize,
}

impl WorldGenerator {
    pub fn new(settings: WorldGenSettings, cyl: CylinderSize) -> Self {
        let mut rand = Random::from_seed(settings.seed ^ 8347658734289375837);
        Self {
            block_generator: NoiseGenerator::new(&mut rand, 8, settings.block_gen_scale),
            block_density_generator: NoiseGenerator::new(
                &mut rand,
                4,
                settings.block_density_gen_scale,
            ),
            biome_height_variation_generator: NoiseGenerator::new(
                &mut rand,
                4,
                settings.biome_height_variation_gen_scale,
            ),
            biome_height_generator: NoiseGenerator::new(
                &mut rand,
                4,
                settings.biome_height_map_gen_scale,
            ),
            height_map_generator: NoiseGenerator::new(&mut rand, 8, settings.height_map_gen_scale),
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
            .gen_wrapped_noise_xz(c.x, c.z, cyl.radius());
        let biome_height_variation =
            self.biome_height_variation_generator
                .gen_wrapped_noise_xz(c.x, c.z, cyl.radius());
        let height_map = self
            .height_map_generator
            .gen_wrapped_noise_xz(c.x, c.z, cyl.radius());

        height_map * biome_height_variation * 100.0 + biome_height * 100.0
    }

    pub fn generate_chunk(&self, coords: ChunkRelWorld) -> ChunkData {
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

        ChunkData {
            block_type,
            metadata,
        }
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
            .gen_wrapped_noise_xyz(c.x, c.y, c.z, cyl.radius());
        let n2 = self
            .block_density_generator
            .gen_wrapped_noise_xyz(c.x, c.y, c.z, cyl.radius());

        n1 + n2 * 0.4
    }
}

fn lerp(from: f64, to: f64, a: f64) -> f64 {
    from + (to - from) * a
}
