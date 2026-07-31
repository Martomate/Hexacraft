use glam::DVec3;

#[derive(PartialEq, Eq, Hash, Clone, Copy)]
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

#[derive(PartialEq, Eq, Hash, Clone, Copy)]
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

#[derive(PartialEq, Eq, Hash, Clone, Copy)]
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

pub struct BlockCoords(DVec3);

impl BlockCoords {
    pub fn new(x: f64, y: f64, z: f64) -> Self {
        Self(DVec3 { x, y, z })
    }
}

pub struct CylCoords {
    pub x: f64,
    pub y: f64,
    pub z: f64,
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
    use glam::DVec3;

    use crate::server::world::SQRT_3;

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
