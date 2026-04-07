use std::f64::consts::PI;

use glam::{DVec3, Vec2};

use crate::server::world::{Player, };

pub fn update_player(player: &mut Player, mouse_movement: Vec2, pressed_keys: &[&str]) {
    update_rotation(&mut player.rotation, mouse_movement, pressed_keys, 0.05);
}

fn update_rotation(
    rotation: &mut DVec3,
    mouse_movement: Vec2,
    pressed_keys: &[&str],
    r_speed: f32,
) {
    if pressed_keys.contains(&"LookUp") {
        rotation[0] -= r_speed as f64;
    }
    if pressed_keys.contains(&"LookDown") {
        rotation[0] += r_speed as f64;
    }
    if pressed_keys.contains(&"LookLeft") {
        rotation[1] -= r_speed as f64;
    }
    if pressed_keys.contains(&"LookRight") {
        rotation[1] += r_speed as f64;
    }
    if pressed_keys.contains(&"TurnHeadLeft") {
        rotation[2] -= r_speed as f64;
    }
    if pressed_keys.contains(&"TurnHeadRight") {
        rotation[2] += r_speed as f64;
    }
    if pressed_keys.contains(&"ResetRotation") {
        *rotation = DVec3::ZERO;
    }

    rotation[1] += (mouse_movement.x * r_speed * 0.05) as f64;
    rotation[0] -= (mouse_movement.y * r_speed * 0.05) as f64;

    rotation[0] = rotation[0].clamp(-PI / 2.0, PI / 2.0);

    if rotation[1] < 0.0 {
        rotation[1] += PI * 2.0;
    } else if rotation[1] > PI * 2.0 {
        rotation[1] -= PI * 2.0;
    }

    if rotation[2] < 0.0 {
        rotation[2] += PI * 2.0;
    } else if rotation[2] > PI * 2.0 {
        rotation[2] -= PI * 2.0;
    }
}
