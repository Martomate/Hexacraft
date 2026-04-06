use std::f64::consts::PI;

use crate::server::world::{Player, Vector3d};

pub fn update_player(player: &mut Player, mouse_movement: (f32, f32)) {
    update_rotation(&mut player.rotation, mouse_movement, 0.05);
}

fn update_rotation(
      rotation: &mut Vector3d,
      mouse_movement: (f32, f32),
      r_speed: f32
) {
    rotation[1] += (mouse_movement.0 * r_speed * 0.05) as f64;
    rotation[0] -= (mouse_movement.1 * r_speed * 0.05) as f64;

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