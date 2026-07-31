use std::f64::consts::PI;

use glam::{DVec3, Vec2};

use crate::server::world::Player;

pub fn update_player(player: &mut Player, mouse_movement: Vec2, pressed_keys: &[&str]) {
    let max_speed = determine_max_speed(pressed_keys);

    update_velocity(
        &mut player.velocity,
        &player.rotation,
        player.flying,
        false,
        max_speed,
        pressed_keys,
    );
    update_rotation(&mut player.rotation, mouse_movement, pressed_keys, 0.05);

    let vel_len = player.velocity.x.hypot(player.velocity.z);
    if vel_len > max_speed {
        player.velocity.x *= max_speed / vel_len;
        player.velocity.z *= max_speed / vel_len;
    }

    if player.flying {
        player.position += player.velocity / 60.0;

        player.velocity.x *= 0.8;
        player.velocity.z *= 0.8;
    } else {
        let friction_factor = 0.8;
        player.velocity.x *= friction_factor;
        player.velocity.z *= friction_factor;

        player.position += player.velocity / 60.0;
    }
}

fn determine_max_speed(pressed_keys: &[&str]) -> f64 {
    if pressed_keys.contains(&"MoveSlowly") {
        0.075
    } else if pressed_keys.contains(&"MoveFast") {
        12.0
    } else if pressed_keys.contains(&"MoveSuperFast") {
        120.0
    } else {
        4.3
    }
}

fn update_velocity(
    velocity: &mut DVec3,
    rotation: &DVec3,
    flying: bool,
    in_fluid: bool,
    max_speed: f64,
    pressed_keys: &[&str],
) {
    if flying {
        velocity.y = 0.0;
    }

    let cos_move = rotation.y.cos() * max_speed * 0.5;
    let sin_move = rotation.y.sin() * max_speed * 0.5;

    if pressed_keys.contains(&"MoveForward") {
        velocity.z -= cos_move;
        velocity.x += sin_move;
    }

    if pressed_keys.contains(&"MoveBackward") {
        velocity.z += cos_move;
        velocity.x -= sin_move;
    }

    if pressed_keys.contains(&"MoveRight") {
        velocity.x += cos_move;
        velocity.z += sin_move;
    }

    if pressed_keys.contains(&"MoveLeft") {
        velocity.x -= cos_move;
        velocity.z -= sin_move;
    }

    if pressed_keys.contains(&"Jump") {
        if flying {
            velocity.y = max_speed;
        } else if velocity.y == 0.0 {
            velocity.y = 5.0;
        } else if in_fluid {
            velocity.y += max_speed * 0.04;
        }
    }

    if pressed_keys.contains(&"Sneak") {
        if flying {
            velocity.y = -max_speed;
        } else if in_fluid {
            velocity.y -= max_speed * 0.04;
        }
    }
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
