use glam::DVec3;

use crate::server::coords::{BlockCoords, BlockRelWorld, CylCoords, Offset, SkewCylCoords, Y60};
use crate::server::world::{CoordUtils, CylinderSize, HexBox, World};

struct MovingBox {
    bounds: HexBox,
    pos: CylCoords,
    velocity: CylCoords,
}

pub struct CollisionDetector {
    cyl_size: CylinderSize,
}

impl CollisionDetector {
    pub fn new(cyl_size: CylinderSize) -> Self {
        Self { cyl_size }
    }
}

impl CollisionDetector {
    const REFLECTION_DIRS: [Offset; 8] = [
        Offset::new(0, -1, 0),
        Offset::new(0, 1, 0),
        Offset::new(-1, 0, 0),
        Offset::new(1, 0, 0),
        Offset::new(0, 0, -1),
        Offset::new(0, 0, 1),
        Offset::new(1, 0, -1),
        Offset::new(-1, 0, 1),
    ];

    fn refl_dirs_cyl(i: usize) -> CylCoords {
        let Offset { dx, dy, dz } = Self::REFLECTION_DIRS[i];
        CylCoords::from(SkewCylCoords::new(dx as f64, dy as f64, dz as f64))
    }

    pub fn collides(
        &self,
        object_bounds: HexBox,
        object_coords: CylCoords,
        target_bounds: HexBox,
        target_coords: CylCoords,
    ) -> bool {
        let _box = MovingBox {
            bounds: object_bounds,
            pos: object_coords,
            velocity: CylCoords::new(0.0, 0.0, 0.0),
        };
        self.distance_to_collision(&_box, target_bounds, SkewCylCoords::from(target_coords))
            .0
            == 0.0
    }

    /** pos and velocity should be CylCoords in vector form. Velocity is per tick. */
    pub fn position_and_velocity_after_collision(
        &self,
        world: &World,
        _box: HexBox,
        pos: DVec3,
        velocity: DVec3,
    ) -> (DVec3, DVec3) {
        let mut result = (pos, velocity);
        let parts = (velocity.length() * 10.0) as i32 + 1;
        result.1 /= parts as f64;

        for _ in 0..parts {
            let current_pos = CylCoords::from(result.0);
            let current_velocity = CylCoords::from(result.1);
            result = self._collides(
                world,
                &MovingBox {
                    bounds: _box,
                    pos: current_pos,
                    velocity: current_velocity,
                },
                100,
            )
        }

        result.1 *= parts as f64;
        result
    }

    fn _collides(&self, world: &World, _box: &MovingBox, ttl: i32) -> (DVec3, DVec3) {
        if ttl < 0 {
            // TODO: this is a temporary fix for the StackOverflow problem
            return (DVec3::from(_box.pos), DVec3::ZERO);
        }

        if _box.velocity.x == 0.0 && _box.velocity.y == 0.0 && _box.velocity.z == 0.0 {
            // velocity is 0
            return (DVec3::from(_box.pos), DVec3::from(_box.velocity));
        }

        let future_coords = BlockCoords::from(_box.pos + _box.velocity);
        let (bc, _) = CoordUtils::getEnclosingBlock(future_coords, self.cyl_size);

        match self.min_dist_and_reflection_dir(world, _box, bc) {
            Some((min_dist, reflection_dir)) => {
                self.result_after_collision(world, _box, min_dist, reflection_dir, ttl)
            }
            None => (
                DVec3::from(_box.pos + _box.velocity),
                DVec3::from(_box.velocity),
            ), // no collision found
        }
    }

    /** This will check all blocks that could intersect the bounds of the object. If any block
     * intersects the bounds of the object it will return (dist: 0, side: -1) If no blocks are in the
     * way it will return None Otherwise it will return (dist: 'distance to the closest block', side:
     * 'side of collision for that block')
     */
    fn min_dist_and_reflection_dir(
        &self,
        world: &World,
        _box: &MovingBox,
        bc: BlockRelWorld,
    ) -> Option<(f64, i32)> {
        let y_lo =
            ((_box.pos.y + _box.velocity.y + _box.bounds.bottom as f64) * 2.0).floor() as i32;
        let y_hi = ((_box.pos.y + _box.velocity.y + _box.bounds.top as f64) * 2.0).floor() as i32;

        // min by dist, with dir as extra data
        let mut min_dist: f64 = f64::MAX;
        let mut min_dir: i32 = 0;

        for y in y_lo..=y_hi {
            for i in 0..9 {
                let dx = (i % 3) - 1;
                let dz = (i / 3) - 1;

                if dx * dz != 1 {
                    // remove corners
                    // keep track of the closest block we are about to collide with
                    if let Some((dist, dir)) = self.distance_to_block(
                        world,
                        _box,
                        BlockRelWorld::new(bc.x + dx, y, bc.z + dz),
                    ) {
                        if dist < min_dist {
                            min_dist = dist;
                            min_dir = dir;
                        }
                    }
                }
            }
        }

        // only return the distance if there was in fact a collision
        if min_dist != f64::MAX {
            Some((min_dist, min_dir))
        } else {
            None
        }
    }

    /** Returns the distance to the target block along `vec` and the side of the collision.
     *
     * If the chunk of the target block is not loaded it will return (dist: 0, side: -1), which is
     * the same as if the object was intersecting the block.
     *
     * If the target block is air it will return None
     */
    fn distance_to_block(
        &self,
        world: &World,
        _box: &MovingBox,
        target_block: BlockRelWorld,
    ) -> Option<(f64, i32)> {
        let Some(block_state) = world.get_block(if target_block.z < 0 {
            BlockRelWorld {
                z: target_block.z + self.cyl_size.total_size() as i32,
                ..target_block
            }
        } else {
            target_block
        }) else {
            // Chunk isn't loaded, you're stuck (so that you don't fall into the void or something)
            return Some((0.0, -1));
        };
        if !block_state.block_type.is_solid() {
            return None;
        }

        let target_bounds = block_state.block_type.bounds(block_state.metadata);
        let target_coords = SkewCylCoords::from(BlockCoords::from(target_block));

        Some(self.distance_to_collision(_box, target_bounds, target_coords))
    }

    fn result_after_collision(
        &self,
        world: &World,
        _box: &MovingBox,
        min_dist: f64,
        reflection_dir: i32,
        ttl: i32,
    ) -> (DVec3, DVec3) {
        if min_dist >= 1.0 {
            // no collision found
            return (
                DVec3::from(_box.pos + _box.velocity),
                DVec3::from(_box.velocity),
            );
        }

        if reflection_dir == -1 {
            // inside a block
            return (DVec3::from(_box.pos), DVec3::ZERO);
        }

        let normal = DVec3::from(Self::refl_dirs_cyl(reflection_dir as usize)).normalize();
        let new_pos = _box.pos
            + CylCoords::new(
                _box.velocity.x * min_dist,
                _box.velocity.y * min_dist,
                _box.velocity.z * min_dist,
            );
        let mut vel = DVec3::from(_box.velocity) * (1.0 - min_dist);
        let dot = vel.dot(normal);
        vel -= normal * dot;
        let mut result = self._collides(
            world,
            &MovingBox {
                bounds: _box.bounds,
                pos: new_pos,
                velocity: CylCoords::from(vel),
            },
            ttl - 1,
        );
        result.1 *= 1.0 / (1.0 - min_dist);
        result
    }

    /** Returns the distance to the other object along the vector `vec`. Also returns the side of the
     * other object that will be collided with.
     *
     * If the objects are already intersecting, it will return (dist: 0, side: -1) The maximum
     * distance returned is 1 (meaning the full length of `vec`). If no collision is found within
     * that distance it will return (dist: 1, side: -1) Otherwise it will return (dist: 'distance in
     * units of `vec.length`, side: 'side of collision')
     */
    fn distance_to_collision(
        &self,
        box1: &MovingBox,
        box2: HexBox,
        _pos2: SkewCylCoords,
    ) -> (f64, i32) {
        let vel1 = SkewCylCoords::from(box1.velocity);
        let pos1 = SkewCylCoords::from(box1.pos) + vel1; // pos after moving
        // The following line ensures that the code works when z is close to 0
        let pos2 = SkewCylCoords::new(
            _pos2.x,
            _pos2.y,
            absmin(_pos2.z - pos1.z, self.cyl_size.circumference()) + pos1.z,
        );

        let x1 = pos1.x + 0.5 * pos1.z;
        let y1 = pos1.y;
        let z1 = pos1.z + 0.5 * pos1.x;
        let x2 = pos2.x + 0.5 * pos2.z;
        let y2 = pos2.y;
        let z2 = pos2.z + 0.5 * pos2.x;

        let r1 = box1.bounds.radius as f64 * Y60;
        let r2 = box2.radius as f64 * Y60;
        let b1 = box1.bounds.bottom as f64;
        let b2 = box2.bottom as f64;
        let t1 = box1.bounds.top as f64;
        let t2 = box2.top as f64;

        let dx = x2 - x1;
        let dy = y2 - y1;
        let dz = z2 - z1;
        let d = r2 + r1;

        let vx = vel1.x + 0.5 * vel1.z;
        let vy = vel1.y;
        let vz = vel1.z + 0.5 * vel1.x;

        // index corresponds to `reflectionDirs`
        let distances = [
            t2 - b1 + dy, // (  y2    + t2) - (  y1    + b1),
            t1 - b2 - dy, // (  y1    + t1) - (  y2    + b2),
            d + dx,       //       (     x2 + r2) - (     x1 - r1),
            d - dx,       //       (     x1 + r1) - (     x2 - r2),
            d + dz,       //       (z2      + r2) - (z1      - r1),
            d - dz,       //       (z1      + r1) - (z2      - r2),
            d + dz - dx,  //  (z2 - x2 + r2) - (z1 - x1 - r1),
            d - dz + dx,  //   (z1 - x1 + r1) - (z2 - x2 - r2)
        ];

        for d in distances {
            if d < 0.0 {
                // the box is not colliding after moving
                return (1.0, -1);
            }
        }

        let mut min_dist = 1.0;
        let mut min_dist_dir: i32 = -1;

        for i in 0..8 {
            let t = Self::REFLECTION_DIRS[i];
            let vel_dist = t.dx as f64 * vx + t.dy as f64 * vy + t.dz as f64 * vz; // the length of v along the normals
            let dist_after = ((vel_dist - distances[i]) * 1.0e9) as i64 as f64 / 1.0e9;

            if vel_dist > 0.0 && dist_after >= 0.0 {
                let a = dist_after / vel_dist;
                if a < min_dist {
                    min_dist = a;
                    min_dist_dir = i as i32;
                }
            }
        }

        if min_dist_dir != -1 {
            (min_dist.min(1.0), min_dist_dir)
        } else {
            (0.0, -1) // the box was colliding even before moving
        }
    }
}

fn fit_z(z: f64, circumference: f64) -> f64 {
    let zz = z % circumference;

    if zz < 0.0 { zz + circumference } else { zz }
}

/** @return x or (x - C) depending on which one is closest to 0 on the modulo circle */
fn absmin(x: f64, circumference: f64) -> f64 {
    fit_z(x + circumference / 2.0, circumference) - circumference / 2.0
}

#[cfg(test)]
mod tests {
    use std::collections::HashMap;

    use approx::{RelativeEq, assert_relative_eq};
    use glam::DVec3;

    use crate::server::collision::CollisionDetector;
    use crate::server::coords::{
        BlockCoords, BlockRelChunk, BlockRelWorld, ChunkRelWorld, ColumnRelWorld, CylCoords,
        SkewCylCoords,
    };
    use crate::server::world::{
        Block, BlockState, ChunkData, CylinderSize, HexBox, World, WorldGenSettings, WorldGenerator,
    };

    const cylSize: CylinderSize = CylinderSize(8);

    const box1: HexBox = HexBox {
        radius: 0.4,
        bottom: 0.1,
        top: 0.3,
    };
    const box2: HexBox = HexBox {
        radius: 0.5,
        bottom: 0.15,
        top: 0.45,
    };
    const pos: CylCoords = BlockCoords::new(1.0, 27.0, -3.0).to_cyl_coords();

    #[test]
    fn collides_should_return_true_for_boxes_at_the_same_location() {
        let detector = CollisionDetector::new(cylSize);

        assert!(detector.collides(box1, pos, box2, pos));
    }

    #[test]
    fn collides_should_work_in_the_y_direction() {
        let detector = CollisionDetector::new(cylSize);

        let pos2a = CylCoords::from(
            pos.toSkewCylCoords()
                + SkewCylCoords::new(0.0, (box1.top - box2.bottom - 0.001) as f64, 0.0),
        );
        let pos2b = CylCoords::from(
            pos.toSkewCylCoords()
                + SkewCylCoords::new(0.0, (box1.top - box2.bottom + 0.001) as f64, 0.0),
        );
        let pos2c = CylCoords::from(
            pos.toSkewCylCoords()
                + SkewCylCoords::new(0.0, (box1.bottom - box2.top + 0.001) as f64, 0.0),
        );
        let pos2d = CylCoords::from(
            pos.toSkewCylCoords()
                + SkewCylCoords::new(0.0, (box1.bottom - box2.top - 0.001) as f64, 0.0),
        );

        assert!(detector.collides(box1, pos, box2, pos2a));
        assert!(!detector.collides(box1, pos, box2, pos2b));
        assert!(detector.collides(box1, pos, box2, pos2c));
        assert!(!detector.collides(box1, pos, box2, pos2d));
    }

    #[test]
    fn collides_should_work_in_the_z_direction() {
        let detector = CollisionDetector::new(cylSize);

        let d = box1.smallRadius() as f64 + box2.smallRadius() as f64;

        let pos2a =
            CylCoords::from(pos.toSkewCylCoords() + SkewCylCoords::new(0.0, 0.0, d - 0.001));
        let pos2b =
            CylCoords::from(pos.toSkewCylCoords() + SkewCylCoords::new(0.0, 0.0, d + 0.001));
        let pos2c =
            CylCoords::from(pos.toSkewCylCoords() + SkewCylCoords::new(0.0, 0.0, -d + 0.001));
        let pos2d =
            CylCoords::from(pos.toSkewCylCoords() + SkewCylCoords::new(0.0, 0.0, -d - 0.001));

        assert!(detector.collides(box1, pos, box2, pos2a));
        assert!(!detector.collides(box1, pos, box2, pos2b));
        assert!(detector.collides(box1, pos, box2, pos2c));
        assert!(!detector.collides(box1, pos, box2, pos2d));
    }

    #[test]
    fn collides_should_work_in_the_x_direction() {
        let detector = CollisionDetector::new(cylSize);

        let d = box1.smallRadius() as f64 + box2.smallRadius() as f64;

        let pos2a =
            CylCoords::from(SkewCylCoords::from(pos) + SkewCylCoords::new(d - 0.001, 0.0, 0.0));
        let pos2b =
            CylCoords::from(SkewCylCoords::from(pos) + SkewCylCoords::new(d + 0.001, 0.0, 0.0));
        let pos2c =
            CylCoords::from(SkewCylCoords::from(pos) + SkewCylCoords::new(-d + 0.001, 0.0, 0.0));
        let pos2d =
            CylCoords::from(SkewCylCoords::from(pos) + SkewCylCoords::new(-d - 0.001, 0.0, 0.0));

        assert!(detector.collides(box1, pos, box2, pos2a));
        assert!(!detector.collides(box1, pos, box2, pos2b));
        assert!(detector.collides(box1, pos, box2, pos2c));
        assert!(!detector.collides(box1, pos, box2, pos2d));
    }

    #[test]
    fn collides_should_work_in_the_w_direction() {
        let detector = CollisionDetector::new(cylSize);

        let d = box1.smallRadius() as f64 + box2.smallRadius() as f64;

        let pos2a = CylCoords::from(
            SkewCylCoords::from(pos) + SkewCylCoords::new(d - 0.001, 0.0, -(d - 0.001)),
        );
        let pos2b = CylCoords::from(
            SkewCylCoords::from(pos) + SkewCylCoords::new(d + 0.001, 0.0, -(d + 0.001)),
        );
        let pos2c = CylCoords::from(
            SkewCylCoords::from(pos) + SkewCylCoords::new(-d + 0.001, 0.0, -(-d + 0.001)),
        );
        let pos2d = CylCoords::from(
            SkewCylCoords::from(pos) + SkewCylCoords::new(-d - 0.001, 0.0, -(-d - 0.001)),
        );

        assert!(detector.collides(box1, pos, box2, pos2a));
        assert!(!detector.collides(box1, pos, box2, pos2b));
        assert!(detector.collides(box1, pos, box2, pos2c));
        assert!(!detector.collides(box1, pos, box2, pos2d));
    }

    fn check_collision(
        detector: &CollisionDetector,
        world: &World,
        _box: HexBox,
        _pos: SkewCylCoords,
        velocity: SkewCylCoords,
        should_stop_after: Option<SkewCylCoords>,
    ) {
        let (new_pos, new_vel) = detector.position_and_velocity_after_collision(
            world,
            _box,
            CylCoords::from(_pos).to_vec3(),
            CylCoords::from(velocity).to_vec3(),
        );

        let (expected_new_pos, expected_new_vel) = match should_stop_after {
            Some(coords) => (_pos + coords, SkewCylCoords::new(0.0, 0.0, 0.0)),
            None => (_pos + velocity, velocity),
        };

        assert_relative_eq!(
            new_pos.distance(CylCoords::from(expected_new_pos).to_vec3()),
            0.0,
            epsilon = 1e-6
        );
        assert_relative_eq!(
            new_vel.distance(CylCoords::from(expected_new_vel).to_vec3()),
            0.0,
            epsilon = 1e-6
        );
    }

    #[test]
    fn position_and_velocity_after_collision_should_do_nothing_if_velocity_is_0() {
        let coords = BlockRelWorld::new(17, -48, 3);

        let mut world = World::new();
        world.set_chunk(
            ChunkRelWorld::from(coords),
            ChunkData::from_blocks([(BlockRelChunk::from(coords), BlockState::AIR)]),
        );

        // Ensure the chunk is loaded
        assert!(world.get_block(coords).is_some());

        // Check for collision
        let detector = CollisionDetector::new(cylSize);
        assert_eq!(
            detector.position_and_velocity_after_collision(
                &world,
                box1,
                CylCoords::from(BlockCoords::from(coords)).to_vec3(),
                DVec3::ZERO
            ),
            (
                BlockCoords::from(coords).to_cyl_coords().to_vec3(),
                DVec3::ZERO
            )
        );
    }

    #[test]
    fn position_and_velocity_after_collision_should_do_nothing_if_inside_a_block() {
        let coords = BlockRelWorld::new(17, -48, 3);

        let mut world = World::new();
        world.set_chunk(
            ChunkRelWorld::from(coords),
            ChunkData::from_blocks([(BlockRelChunk::from(coords), BlockState::of(Block::Dirt))]),
        );
        let detector = CollisionDetector::new(cylSize);

        // Ensure the chunk is loaded
        assert!(world.get_block(coords).is_some());

        // Check for collision (it should not move)
        let position = SkewCylCoords::from(BlockCoords::from(coords));
        let velocity = SkewCylCoords::new(3.2, 1.4, -0.9);
        let zero_movement = SkewCylCoords::new(0.0, 0.0, 0.0);
        check_collision(
            &detector,
            &world,
            box1,
            position,
            velocity,
            Some(zero_movement),
        )
    }

    #[test]
    fn position_and_velocity_after_collision_should_do_nothing_if_the_chunk_is_not_loaded() {
        let world = World::new();
        let detector = CollisionDetector::new(cylSize);

        // Ensure the chunk is NOT loaded
        let coords = BlockRelWorld::new(17, -48, 3);
        assert!(world.get_block(coords).is_none());

        // Check for collision (it should not move)
        let position = SkewCylCoords::from(BlockCoords::from(coords));
        let velocity = SkewCylCoords::new(3.2, 1.4, -0.9);
        let zero_movement = SkewCylCoords::new(0.0, 0.0, 0.0);
        check_collision(
            &detector,
            &world,
            box1,
            position,
            velocity,
            Some(zero_movement),
        );
    }

    #[test]
    fn position_and_velocity_after_collision_should_add_velocity_to_position_if_there_is_no_collision()
     {
        let coords = BlockRelWorld::new(1, -7, 7);

        let mut world = World::new();
        world.set_chunk(
            ChunkRelWorld::from(coords),
            ChunkData::from_blocks(
                (-1..=1)
                    .flat_map(|dz| {
                        (-1..=1).flat_map(move |dy| (-1..=1).map(move |dx| (dx, dy, dz)))
                    })
                    .map(|(dx, dy, dz)| {
                        (
                            BlockRelChunk::from(coords + BlockRelWorld::new(dx, dy, dz)),
                            BlockState::AIR,
                        )
                    }),
            ),
        );

        // Ensure the chunk is loaded
        assert!(world.get_block(coords).is_some());

        // Check for collision
        let _box = HexBox {
            radius: 0.15,
            bottom: 0.1,
            top: 0.3,
        };
        let velocity = SkewCylCoords::from(BlockCoords::new(0.2, 0.39, 0.71));
        let detector = CollisionDetector::new(cylSize);

        check_collision(
            &detector,
            &world,
            _box,
            SkewCylCoords::from(BlockCoords::from(coords)),
            velocity,
            None,
        );
    }

    #[test]
    fn position_and_velocity_after_collision_should_work_in_the_x_direction() {
        let coords = BlockRelWorld::new(5, 7, 9);

        let mut world = World::new();
        world.set_chunk(
            ChunkRelWorld::from(coords),
            ChunkData::from_blocks((-1..=3).map(|dx| {
                let b = match dx {
                    -1 | 3 => BlockState::of(Block::Dirt),
                    _ => BlockState::AIR,
                };
                (
                    BlockRelChunk::from(coords + BlockRelWorld::new(dx, 0, 0)),
                    b,
                )
            })),
        );

        // Ensure the chunk is loaded
        assert!(world.get_block(coords).is_some());

        let detector = CollisionDetector::new(cylSize);
        let _box = HexBox {
            radius: 0.15,
            bottom: 0.1,
            top: 0.3,
        };

        // Check for collision forward
        let back = SkewCylCoords::from(BlockCoords::from(coords)); // right at the beginning of the first Air
        let forward_max = SkewCylCoords::from(BlockCoords::new(2.5, 0.0, 0.0))
            + SkewCylCoords::new(-_box.smallRadius() as f64, 0.0, 0.0); // maximal movement from back to upper Dirt

        // Just before colliding
        check_collision(
            &detector,
            &world,
            _box,
            back,
            forward_max + SkewCylCoords::new(-0.001, 0.0, 0.0),
            None,
        );

        // Too far
        check_collision(
            &detector,
            &world,
            _box,
            back,
            forward_max + SkewCylCoords::new(0.001, 0.0, 0.0),
            Some(forward_max),
        );

        // Check for collision backward
        let front =
            SkewCylCoords::from(BlockCoords::from(coords) + BlockCoords::new(2.0, 0.0, 0.0)); // right at the beginning of the last Air
        let backward_max = SkewCylCoords::from(BlockCoords::new(-2.5, 0.0, 0.0))
            + SkewCylCoords::new(_box.smallRadius() as f64, 0.0, 0.0); // maximal movement from front to lower Dirt

        // Just before colliding
        check_collision(
            &detector,
            &world,
            _box,
            front,
            backward_max + SkewCylCoords::new(0.001, 0.0, 0.0),
            None,
        );

        // Too far
        check_collision(
            &detector,
            &world,
            _box,
            front,
            backward_max + SkewCylCoords::new(-0.001, 0.0, 0.0),
            Some(backward_max),
        );
    }

    #[test]
    fn position_and_velocity_after_collision_should_work_in_the_y_direction() {
        let coords = BlockRelWorld::new(5, 7, 9);

        let mut world = World::new();
        world.set_chunk(
            ChunkRelWorld::from(coords),
            ChunkData::from_blocks((-1..=3).map(|dy| {
                let b = match dy {
                    -1 | 3 => BlockState::of(Block::Dirt),
                    _ => BlockState::AIR,
                };
                (
                    BlockRelChunk::from(coords + BlockRelWorld::new(0, dy, 0)),
                    b,
                )
            })),
        );

        // Ensure the chunk is loaded
        assert!(world.get_block(coords).is_some());

        let detector = CollisionDetector::new(cylSize);
        let _box = HexBox {
            radius: 0.15,
            bottom: 0.1,
            top: 0.3,
        };

        // Check for collision up
        let bottom = SkewCylCoords::from(BlockCoords::from(coords)); // right at the beginning of the first Air
        let up_max = SkewCylCoords::from(BlockCoords::new(0.0, 3.0, 0.0))
            + SkewCylCoords::new(0.0, -0.3, 0.0); // maximal movement from back to upper Dirt

        // Just before colliding
        check_collision(
            &detector,
            &world,
            _box,
            bottom,
            up_max + SkewCylCoords::new(0.0, -0.001, 0.0),
            None,
        );

        // Too far
        check_collision(
            &detector,
            &world,
            _box,
            bottom,
            up_max + SkewCylCoords::new(0.0, 0.001, 0.0),
            Some(up_max),
        );

        // Check for collision down
        let top = SkewCylCoords::from(BlockCoords::from(coords) + BlockCoords::new(0.0, 2.0, 0.0)); // right at the beginning of the last Air
        let down_max = SkewCylCoords::from(BlockCoords::new(0.0, -2.0, 0.0))
            + SkewCylCoords::new(0.0, -0.1, 0.0); // maximal movement from front to lower Dirt

        // Just before colliding
        check_collision(
            &detector,
            &world,
            _box,
            top,
            down_max + SkewCylCoords::new(0.0, 0.001, 0.0),
            None,
        );

        // Too far
        check_collision(
            &detector,
            &world,
            _box,
            top,
            down_max + SkewCylCoords::new(0.0, -0.001, 0.0),
            Some(down_max),
        );
    }

    #[test]
    fn position_and_velocity_after_collision_should_work_in_the_z_direction() {
        let coords = BlockRelWorld::new(5, 7, 9);

        let mut world = World::new();
        world.set_chunk(
            ChunkRelWorld::from(coords),
            ChunkData::from_blocks((-1..=3).map(|dz| {
                let b = match dz {
                    -1 | 3 => BlockState::of(Block::Dirt),
                    _ => BlockState::AIR,
                };
                (
                    BlockRelChunk::from(coords + BlockRelWorld::new(0, 0, dz)),
                    b,
                )
            })),
        );

        // Ensure the chunk is loaded
        assert!(world.get_block(coords).is_some());

        let detector = CollisionDetector::new(cylSize);
        let _box = HexBox {
            radius: 0.15,
            bottom: 0.1,
            top: 0.3,
        };

        // Check for collision forward
        let back = SkewCylCoords::from(BlockCoords::from(coords)); // right at the beginning of the first Air
        let forward_max = SkewCylCoords::from(BlockCoords::new(0.0, 0.0, 2.5))
            + SkewCylCoords::new(0.0, 0.0, -_box.smallRadius() as f64); // maximal movement from back to upper Dirt

        // Just before colliding
        check_collision(
            &detector,
            &world,
            _box,
            back,
            forward_max + SkewCylCoords::new(0.0, 0.0, -0.001),
            None,
        );

        // Too far
        check_collision(
            &detector,
            &world,
            _box,
            back,
            forward_max + SkewCylCoords::new(0.0, 0.0, 0.001),
            Some(forward_max),
        );

        // Check for collision backward
        let front =
            SkewCylCoords::from(BlockCoords::from(coords) + BlockCoords::new(0.0, 0.0, 2.0)); // right at the beginning of the last Air
        let backward_max = SkewCylCoords::from(BlockCoords::new(0.0, 0.0, -2.5))
            + SkewCylCoords::new(0.0, 0.0, _box.smallRadius() as f64); // maximal movement from front to lower Dirt

        // Just before colliding
        check_collision(
            &detector,
            &world,
            _box,
            front,
            backward_max + SkewCylCoords::new(0.0, 0.0, 0.001),
            None,
        );

        // Too far
        check_collision(
            &detector,
            &world,
            _box,
            front,
            backward_max + SkewCylCoords::new(0.0, 0.0, -0.001),
            Some(backward_max),
        );
    }

    #[test]
    fn position_and_velocity_after_collision_should_work_in_the_w_direction() {
        let coords = BlockRelWorld::new(5, 7, 9);

        let mut world = World::new();
        world.set_chunk(
            ChunkRelWorld::from(coords),
            ChunkData::from_blocks((-1..=3).map(|dw| {
                let b = match dw {
                    -1 | 3 => BlockState::of(Block::Dirt),
                    _ => BlockState::AIR,
                };
                (
                    BlockRelChunk::from(coords + BlockRelWorld::new(dw, 0, -dw)),
                    b,
                )
            })),
        );

        // Ensure the chunk is loaded
        assert!(world.get_block(coords).is_some());

        let detector = CollisionDetector::new(cylSize);
        let _box = HexBox {
            radius: 0.15,
            bottom: 0.1,
            top: 0.3,
        };

        // Check for collision forward
        let back = SkewCylCoords::from(BlockCoords::from(coords)); // right at the beginning of the first Air
        let forward_max = SkewCylCoords::from(BlockCoords::new(2.5, 0.0, -2.5))
            + SkewCylCoords::new(-_box.smallRadius() as f64, 0.0, _box.smallRadius() as f64); // maximal movement from back to upper Dirt

        // Just before colliding
        check_collision(
            &detector,
            &world,
            _box,
            back,
            forward_max + SkewCylCoords::new(-0.001, 0.0, 0.001),
            None,
        );

        // Too far
        check_collision(
            &detector,
            &world,
            _box,
            back,
            forward_max + SkewCylCoords::new(0.001, 0.0, -0.001),
            Some(forward_max),
        );

        // Check for collision backward
        let front =
            SkewCylCoords::from(BlockCoords::from(coords) + BlockCoords::new(2.0, 0.0, -2.0)); // right at the beginning of the last Air
        let backward_max = SkewCylCoords::from(BlockCoords::new(-2.5, 0.0, 2.5))
            + SkewCylCoords::new(_box.smallRadius() as f64, 0.0, -_box.smallRadius() as f64); // maximal movement from front to lower Dirt

        // Just before colliding
        check_collision(
            &detector,
            &world,
            _box,
            front,
            backward_max + SkewCylCoords::new(0.001, 0.0, -0.001),
            None,
        );

        // Too far
        check_collision(
            &detector,
            &world,
            _box,
            front,
            backward_max + SkewCylCoords::new(-0.001, 0.0, 0.001),
            Some(backward_max),
        );
    }
}
