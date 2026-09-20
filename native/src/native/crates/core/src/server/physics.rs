use glam::DVec3;

use crate::server::collision::CollisionDetector;
use crate::server::coords::{BlockCoords, CylCoords};
use crate::server::world::{Block, BlockState, CylinderSize, HexBox, Player, World};

pub mod viscosity {
    pub const AIR: f64 = 18.5e-6;
    pub const WATER: f64 = 1e-3;
}

mod density {
    pub const WATER: f64 = 1000.0;
}

mod drag_coefficient {
    pub const HUMAN: f64 = 1.0;
}

mod fluid_dynamics {
    use glam::DVec3;

    /// @param area usually the orthographic projection of the object into the direction of movement
    /// @return the fluid drag (calculated using the drag equation)
    pub fn drag_force(
        velocity: DVec3,
        drag_coefficient: f64,
        area: f64,
        fluid_density: f64,
    ) -> DVec3 {
        let rho = fluid_density;
        let cd = drag_coefficient;
        let a = area;

        // (0.5 * rho * v^2 * Cd * A) in the opposite direction of velocity
        velocity * (-0.5 * rho * velocity.length() * cd * a)
    }
}

pub fn tick(
    world: &World,
    player: &mut Player,
    max_speed: f64,
    effective_viscosity: f64,
    volume_submerged_in_water: f64,
    collision_detector: CollisionDetector,
) {
    let vel_len = player.velocity.x.hypot(player.velocity.z);
    if vel_len > max_speed {
        player.velocity.x *= max_speed / vel_len;
        player.velocity.z *= max_speed / vel_len;
    }

    if player.flying {
        player.position += player.velocity / 60.0;
        player.velocity.x *= 0.8;
        player.velocity.z *= 0.8;
        return;
    }

    let friction_factor = if effective_viscosity < viscosity::AIR * 2.0 {
        0.8
    } else {
        0.95
    };
    player.velocity.x *= friction_factor;
    player.velocity.z *= friction_factor;

    let is_moving = player.velocity.length_squared() > 0.0;
    if is_moving {
        let total_area = player.bounds.projected_area_in_direction(player.velocity);
        let adjusted_area = total_area * (volume_submerged_in_water / player.bounds.volume());
        apply_drag(&mut player.velocity, 75.0, adjusted_area)
    };

    apply_buoyancy(
        &mut player.velocity,
        75.0,
        volume_submerged_in_water,
        density::WATER,
    );
    apply_gravity(&mut player.velocity);
    apply_collision(
        world,
        &mut player.position,
        &mut player.velocity,
        player.bounds,
        collision_detector,
    );
}

fn apply_drag(velocity: &mut DVec3, object_mass: f64, object_projected_area: f64) {
    let drag = fluid_dynamics::drag_force(
        *velocity,
        drag_coefficient::HUMAN,
        object_projected_area,
        density::WATER,
    );

    // dv = a * dt = (F / m) * (1 / 60) = F / (m * 60)
    *velocity += drag / (object_mass * 60.0);
}

fn apply_buoyancy(
    velocity: &mut DVec3,
    object_mass: f64,
    submerged_volume: f64,
    fluid_density: f64,
) {
    velocity.y += (submerged_volume * fluid_density * 9.82) / (object_mass * 60.0);
}

fn apply_gravity(velocity: &mut DVec3) {
    velocity.y -= 9.82 / 60.0;
}

fn apply_collision(
    world: &World,
    position: &mut DVec3,
    velocity: &mut DVec3,
    bounds: HexBox,
    collision_detector: CollisionDetector,
) {
    *velocity /= 60.0;
    let (pos, vel) =
        collision_detector.position_and_velocity_after_collision(world, bounds, *position, *velocity);
    *position = pos;
    *velocity = vel;
    *velocity *= 60.0;
}
