use std::{collections::HashMap, f64::consts::PI};

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

pub type UUID = u128;
pub type Block = u8;
pub type Vector3d = [f64; 3];
pub type Inventory = HashMap<u8, Block>;

const AIR: Block = 0;

pub struct Player {
    pub id: UUID,
    pub name: String,
    pub inventory: Inventory,
    pub bounds: HexBox,
    pub velocity: Vector3d,
    pub position: Vector3d,
    pub rotation: Vector3d,
    pub flying: bool,
    pub selected_item_slot: u8,
}

impl Player {
    pub fn new(id: UUID, name: String, inventory: Inventory) -> Self {
        Self {
            id,
            name,
            inventory,
            bounds: HexBox {
                radius: 0.2,
                bottom: -1.65,
                top: 0.1,
            },
            velocity: [0.0; 3],
            position: [0.0; 3],
            rotation: [0.0; 3],
            flying: false,
            selected_item_slot: 0,
        }
    }

    pub fn block_in_hand(&self) -> Block {
        self.inventory
            .get(&self.selected_item_slot)
            .cloned()
            .unwrap_or(AIR)
    }
}
