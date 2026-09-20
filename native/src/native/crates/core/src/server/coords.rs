use std::ops::Add;

use glam::DVec3;

use crate::server::world::SQRT_3;

pub const Y60: f64 = SQRT_3 / 2.0;

#[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
pub struct BlockRelChunk {
    pub x: u8,
    pub y: u8,
    pub z: u8,
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

#[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
pub struct ChunkRelWorld {
    pub x: i32,
    pub y: i32,
    pub z: i32,
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

impl From<BlockRelWorld> for ChunkRelWorld {
    fn from(coords: BlockRelWorld) -> Self {
        Self::new(coords.x >> 4, coords.y >> 4, coords.z >> 4)
    }
}

impl From<BlockRelWorld> for BlockRelChunk {
    fn from(coords: BlockRelWorld) -> Self {
        Self::new(coords.x as u8 & 0xf, coords.y as u8 & 0xf, coords.z as u8 & 0xf)
    }
}

impl From<ChunkRelWorld> for ColumnRelWorld {
    fn from(coords: ChunkRelWorld) -> Self {
        Self { x: coords.x, z: coords.z }
    }
}

#[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
pub struct ColumnRelWorld {
    pub x: i32,
    pub z: i32,
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

#[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
pub struct BlockRelWorld {
    pub x: i32,
    pub y: i32,
    pub z: i32,
}

impl BlockRelWorld {
    pub fn new(x: i32, y: i32, z: i32) -> Self {
        Self { x, y, z }
    }
}

#[derive(Debug, PartialEq, Clone, Copy)]
pub struct BlockCoords {
    pub x: f64,
    pub y: f64,
    pub z: f64,
}

impl BlockCoords {
    pub const fn new(x: f64, y: f64, z: f64) -> Self {
        Self { x, y, z }
    }

    pub fn offset(&self, dx: f64, dy: f64, dz: f64) -> Self {
        Self {
            x: self.x + dx,
            y: self.y + dy,
            z: self.z + dz,
        }
    }

    pub const fn to_vec3(self) -> DVec3 {
        let Self { x, y, z } = self;
        DVec3 { x, y, z }
    }

    pub const fn to_cyl_coords(self) -> CylCoords {
        CylCoords::from_vec3(conversion::skew_to_cyl(conversion::block_to_skew(
            self.to_vec3(),
        )))
    }
}

impl From<BlockRelWorld> for BlockCoords {
    fn from(coords: BlockRelWorld) -> Self {
        Self {
            x: coords.x as f64,
            y: coords.y as f64,
            z: coords.z as f64,
        }
    }
}

#[derive(Debug, PartialEq, Clone, Copy)]
pub struct SkewCylCoords {
    pub x: f64,
    pub y: f64,
    pub z: f64,
}

impl SkewCylCoords {
    pub fn new(x: f64, y: f64, z: f64) -> Self {
        Self { x, y, z }
    }

    pub const fn from_vec3(DVec3 { x, y, z }: DVec3) -> Self {
        Self { x, y, z }
    }
}

impl CylCoords {
    pub fn new(x: f64, y: f64, z: f64) -> Self {
        Self { x, y, z }
    }
}

impl Add for BlockRelWorld {
    type Output = BlockRelWorld;

    fn add(self, rhs: Self) -> Self::Output {
        Self::Output {
            x: self.x + rhs.x,
            y: self.y + rhs.y,
            z: self.z + rhs.z,
        }
    }
}

impl Add for BlockCoords {
    type Output = BlockCoords;

    fn add(self, rhs: Self) -> Self::Output {
        Self::Output {
            x: self.x + rhs.x,
            y: self.y + rhs.y,
            z: self.z + rhs.z,
        }
    }
}

impl Add for SkewCylCoords {
    type Output = SkewCylCoords;

    fn add(self, rhs: Self) -> Self::Output {
        Self::Output {
            x: self.x + rhs.x,
            y: self.y + rhs.y,
            z: self.z + rhs.z,
        }
    }
}

impl Add for CylCoords {
    type Output = CylCoords;

    fn add(self, rhs: Self) -> Self::Output {
        Self::Output {
            x: self.x + rhs.x,
            y: self.y + rhs.y,
            z: self.z + rhs.z,
        }
    }
}

impl From<SkewCylCoords> for CylCoords {
    fn from(coords: SkewCylCoords) -> Self {
        conversion::skew_to_cyl(coords.into()).into()
    }
}

impl From<CylCoords> for SkewCylCoords {
    fn from(coords: CylCoords) -> Self {
        coords.toSkewCylCoords()
    }
}

impl From<BlockCoords> for SkewCylCoords {
    fn from(coords: BlockCoords) -> Self {
        conversion::block_to_skew(coords.into()).into()
    }
}


#[derive(Debug, PartialEq, Clone, Copy)]
pub struct CylCoords {
    pub x: f64,
    pub y: f64,
    pub z: f64,
}

impl CylCoords {
    pub const fn from_vec3(DVec3 { x, y, z }: DVec3) -> Self {
        Self { x, y, z }
    }

    pub const fn to_vec3(self) -> DVec3 {
        let Self { x, y, z } = self;
        DVec3 { x, y, z }
    }

    pub const fn toSkewCylCoords(self) -> SkewCylCoords {
        SkewCylCoords::from_vec3(conversion::cyl_to_skew(self.to_vec3()))
    }
}

fn i20_to_i32(value: u64) -> i32 {
    (value as i32) << 12 >> 12
}

impl From<DVec3> for BlockCoords {
    fn from(DVec3 { x, y, z }: DVec3) -> Self {
        Self { x, y, z }
    }
}

impl From<DVec3> for SkewCylCoords {
    fn from(DVec3 { x, y, z }: DVec3) -> Self {
        Self { x, y, z }
    }
}

impl From<BlockCoords> for CylCoords {
    fn from(block: BlockCoords) -> Self {
        block.to_cyl_coords()
    }
}

impl From<CylCoords> for BlockCoords {
    fn from(coords: CylCoords) -> Self {
        Self::from(conversion::skew_to_block(conversion::cyl_to_skew(
            coords.into(),
        )))
    }
}

impl From<BlockCoords> for DVec3 {
    fn from(BlockCoords { x, y, z }: BlockCoords) -> Self {
        Self { x, y, z }
    }
}

impl From<SkewCylCoords> for DVec3 {
    fn from(SkewCylCoords { x, y, z }: SkewCylCoords) -> Self {
        Self { x, y, z }
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

#[derive(Debug, PartialEq, Clone, Copy)]
pub struct Offset {
    pub dx: i32,
    pub dy: i32,
    pub dz: i32,
}

impl Offset {
    pub const fn new(dx: i32, dy: i32, dz: i32) -> Self {
        Self { dx, dy, dz }
    }
}

mod conversion {
    use glam::DVec3;

    use crate::server::coords::Y60;

    pub const fn block_to_skew(DVec3 { x, y, z }: DVec3) -> DVec3 {
        DVec3 {
            x: x * Y60,
            y: y * 0.5,
            z: z * Y60,
        }
    }

    pub const fn skew_to_cyl(DVec3 { x, y, z }: DVec3) -> DVec3 {
        DVec3 {
            x: x * Y60,
            y,
            z: z + x * 0.5,
        }
    }

    pub const fn skew_to_block(DVec3 { x, y, z }: DVec3) -> DVec3 {
        DVec3 {
            x: x / Y60,
            y: y / 0.5,
            z: z / Y60,
        }
    }

    pub const fn cyl_to_skew(DVec3 { x, y, z }: DVec3) -> DVec3 {
        DVec3 {
            x: x / Y60,
            y,
            z: z - x / Y60 * 0.5,
        }
    }
}
