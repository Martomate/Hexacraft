use std::marker::PhantomData;
use std::ops::Add;

use glam::DVec3;

use crate::server::coords::systems::Transform3;
use crate::server::world::SQRT_3;

pub const Y60: f64 = SQRT_3 / 2.0;

pub mod systems {
    use crate::server::coords::{Vec3, conversion};

    #[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
    pub struct BlockRelChunk;

    #[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
    pub struct ChunkRelWorld;

    #[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
    pub struct ColumnRelWorld;

    #[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
    pub struct BlockRelWorld;

    #[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
    pub struct BlockCoords;

    #[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
    pub struct SkewCylCoords;

    #[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
    pub struct CylCoords;

    pub trait Transform3<C: Copy, Target: Copy>: Sized + Copy {
        fn convert(v: Vec3<C, Self>) -> Vec3<C, Target>;
    }

    impl Transform3<f64, BlockCoords> for CylCoords {
        fn convert(v: Vec3<f64, Self>) -> Vec3<f64, BlockCoords> {
            conversion::skew_to_block(conversion::cyl_to_skew(v.into())).into()
        }
    }

    impl Transform3<f64, CylCoords> for SkewCylCoords {
        fn convert(v: Vec3<f64, Self>) -> Vec3<f64, CylCoords> {
            conversion::skew_to_cyl(v.into()).into()
        }
    }

    impl Transform3<f64, SkewCylCoords> for CylCoords {
        fn convert(v: Vec3<f64, Self>) -> Vec3<f64, SkewCylCoords> {
            v.toSkewCylCoords()
        }
    }

    impl Transform3<f64, SkewCylCoords> for BlockCoords {
        fn convert(v: Vec3<f64, Self>) -> Vec3<f64, SkewCylCoords> {
            conversion::block_to_skew(v.into()).into()
        }
    }

    impl Transform3<f64, CylCoords> for BlockCoords {
        fn convert(v: Vec3<f64, Self>) -> Vec3<f64, CylCoords> {
            v.to_cyl_coords()
        }
    }

    impl Transform3<f64, BlockCoords> for BlockRelWorld {
        fn convert(v: Vec3<f64, Self>) -> Vec3<f64, BlockCoords> {
            Vec3::new(v.x as f64, v.y as f64, v.z as f64)
        }
    }

    impl Transform3<i32, ChunkRelWorld> for BlockRelWorld {
        fn convert(v: Vec3<i32, Self>) -> Vec3<i32, ChunkRelWorld> {
            Vec3::new(v.x >> 4, v.y >> 4, v.z >> 4)
        }
    }

    impl Transform3<i32, BlockRelChunk> for BlockRelWorld {
        fn convert(v: Vec3<i32, Self>) -> Vec3<i32, BlockRelChunk> {
            Vec3::new(v.x & 0xf, v.y & 0xf, v.z & 0xf)
        }
    }
}

#[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
pub struct Vec3<C: Copy, S: Copy> {
    pub x: C,
    pub y: C,
    pub z: C,
    _coordinate_system: PhantomData<S>,
}

impl<C: Copy, S: Copy> Vec3<C, S> {
    pub fn map_coords<C2: Copy>(self, f: impl Fn(C) -> C2) -> Vec3<C2, S> {
        Vec3::new(f(self.x), f(self.y), f(self.z))
    }
}

impl<C: Copy, S: Copy> Vec3<C, S> {
    pub const fn new(x: C, y: C, z: C) -> Self {
        Self {
            x,
            y,
            z,
            _coordinate_system: PhantomData {},
        }
    }
}

impl<C, S> Vec2<C, S> {
    pub fn new(x: C, z: C) -> Self {
        Self {
            x,
            z,
            _coordinate_system: PhantomData {},
        }
    }
}

impl<S: Copy> Vec3<f64, S> {
    pub const fn from_vec3(DVec3 { x, y, z }: DVec3) -> Self {
        Self::new(x, y, z)
    }

    pub const fn to_vec3(self) -> DVec3 {
        let Self { x, y, z, .. } = self;
        DVec3 { x, y, z }
    }
}

#[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
pub struct Vec2<C, S> {
    pub x: C,
    pub z: C,
    _coordinate_system: PhantomData<S>,
}

pub type BlockRelChunk = Vec3<u8, systems::BlockRelChunk>;

pub type ChunkRelWorld = Vec3<i32, systems::ChunkRelWorld>;

pub type ColumnRelWorld = Vec2<i32, systems::ColumnRelWorld>;

pub type BlockRelWorld = Vec3<i32, systems::BlockRelWorld>;

pub type BlockCoords = Vec3<f64, systems::BlockCoords>;

pub type SkewCylCoords = Vec3<f64, systems::SkewCylCoords>;

pub type CylCoords = Vec3<f64, systems::CylCoords>;

impl BlockRelChunk {
    pub fn encoded(&self) -> u16 {
        let x = (self.x & 0xf) as u16;
        let y = (self.y & 0xf) as u16;
        let z = (self.z & 0xf) as u16;
        x << 8 | y << 4 | z
    }
}

impl ChunkRelWorld {
    pub fn encoded(&self) -> u64 {
        let x = (self.x & 0xfffff) as u64;
        let z = (self.z & 0xfffff) as u64;
        let y = (self.y & 0xfff) as u64;
        x << 32 | z << 12 | y
    }
}

impl ColumnRelWorld {
    pub fn decode(value: u64) -> Self {
        Self::new(
            i20_to_i32((value >> 20) & 0xFFFFF),
            i20_to_i32(value & 0xFFFFF),
        )
    }
}

impl BlockCoords {
    pub fn offset(&self, dx: f64, dy: f64, dz: f64) -> Self {
        Self::new(self.x + dx, self.y + dy, self.z + dz)
    }

    pub const fn to_cyl_coords(self) -> CylCoords {
        CylCoords::from_vec3(conversion::skew_to_cyl(conversion::block_to_skew(
            self.to_vec3(),
        )))
    }
}

impl<C: Copy + Add<Output = C>, S: Copy> Add for Vec3<C, S> {
    type Output = Vec3<C, S>;

    fn add(self, rhs: Self) -> Self::Output {
        Vec3::new(self.x + rhs.x, self.y + rhs.y, self.z + rhs.z)
    }
}

impl CylCoords {
    pub const fn toSkewCylCoords(self) -> SkewCylCoords {
        SkewCylCoords::from_vec3(conversion::cyl_to_skew(self.to_vec3()))
    }
}

fn i20_to_i32(value: u64) -> i32 {
    (value as i32) << 12 >> 12
}

impl<S: Copy> From<DVec3> for Vec3<f64, S> {
    fn from(DVec3 { x, y, z }: DVec3) -> Self {
        Self::new(x, y, z)
    }
}

impl<S: Copy> From<Vec3<f64, S>> for DVec3 {
    fn from(Vec3 { x, y, z, .. }: Vec3<f64, S>) -> Self {
        Self { x, y, z }
    }
}

impl<C: Copy, S: Copy> Vec3<C, S> {
    pub fn convert<T: Copy>(self) -> Vec3<C, T>
    where
        S: Transform3<C, T>,
    {
        Transform3::convert(self)
    }
}

impl From<BlockRelWorld> for BlockCoords {
    fn from(coords: BlockRelWorld) -> Self {
        Self::new(coords.x as f64, coords.y as f64, coords.z as f64)
    }
}

impl From<BlockRelWorld> for BlockRelChunk {
    fn from(coords: BlockRelWorld) -> Self {
        systems::BlockRelWorld::convert(coords).map_coords(|c| c as u8)
    }
}

impl From<ChunkRelWorld> for ColumnRelWorld {
    fn from(coords: ChunkRelWorld) -> Self {
        Self::new(coords.x, coords.z)
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
