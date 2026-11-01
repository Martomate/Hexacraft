use std::f32::consts::PI;

use glam::DVec3;

use crate::server::coords::Y60;
use crate::server::response::{EntityModel, HexPrism, ModelPart};

fn pos(x: f64, y: f64, z: f64) -> DVec3 {
    DVec3::new(x, y, z)
}

fn rot(x: f32, y: f32, z: f32) -> DVec3 {
    DVec3::new(x as f64, y as f64, z as f64)
}

/// The distance between the centers of two neighbouring hexagons of the given radius (in a row)
fn hex_step(radius: f64) -> f64 {
    2.0 * radius * Y60
}

fn prism(radius: u32, length: u32) -> HexPrism {
    HexPrism { radius, length }
}

/// A prism that is not rendered. Parts with this prism only serve as pivots for their children.
fn empty_prism() -> HexPrism {
    prism(0, 0)
}

fn part(
    name: impl Into<String>,
    prism: HexPrism,
    position: DVec3,
    rotation: DVec3,
    parent: Option<&str>,
    prism_offset: f32,
) -> ModelPart {
    ModelPart {
        name: name.into(),
        prism,
        position,
        rotation,
        parent: parent.map(str::to_string),
        prism_offset,
    }
}

pub fn boat_model() -> EntityModel {
    let rod_length: u32 = 128;
    let rod_radius: u32 = 4;
    let bottom_rod_count: i32 = 9; // should be odd so that the boat is symmetric
    let side_row_count: i32 = 2; // the number of rows of rods above the bottom row

    // Distances between the centers of neighbouring hexagonal rods
    let rod_spacing = hex_step(rod_radius as f64); // within a row
    let row_height = 1.5 * rod_radius as f64; // between rows (each row is shifted half a rod sideways)

    // In each row the cross rods reach from the center of the leftmost rod to the center of the rightmost one.
    // Every side row is half a rod wider on each side than the row below it. (rounded to whole pixels)
    let cross_rod_length =
        |row: i32| ((bottom_rod_count - 1 + row) as f64 * rod_spacing).round() as u32;

    let elevation = 6.0; // a hack that ensures that no water is in the boat

    let half_bottom_rod_count = bottom_rod_count / 2;
    let bottom_rods = (-half_bottom_rod_count..=half_bottom_rod_count).map(|col| (col, 0));
    // Each side row sits half a rod further out than the row below it, so the sides lean outwards
    let side_rods = (1..=side_row_count).flat_map(|row| {
        [
            (-half_bottom_rod_count - row, row),
            (half_bottom_rod_count, row),
        ]
    });

    let mut parts = vec![
        // Pivots (not rendered)
        part(
            "body",
            empty_prism(),
            pos(0.0, elevation, 0.0),
            rot(0.0, PI / 2.0, 0.0),
            None,
            0.0,
        ),
        part(
            "crossBody",
            empty_prism(),
            pos(0.0, 8.0 - 4.0 * Y60, 0.0),
            rot(0.0, PI / 2.0, 0.0),
            Some("body"),
            0.0,
        ),
    ];

    for (col, row) in bottom_rods.chain(side_rods) {
        let position = pos(
            -0.2 * rod_length as f64, // centered along the rod's length
            row_height * row as f64,
            rod_spacing * (col as f64 + 0.5 * row as f64),
        );
        parts.push(part(
            format!("rod_{col}_{row}"),
            prism(rod_radius, rod_length),
            position,
            rot(0.0, 0.0, -PI / 2.0),
            Some("body"),
            0.0,
        ));
    }

    for row in 0..side_row_count {
        let length = cross_rod_length(row);
        let ends = [
            ("back", -0.2 * rod_length as f64 + rod_radius as f64 * 0.5),
            ("front", 0.8 * rod_length as f64 - rod_radius as f64 * 0.5),
        ];
        for (end, d) in ends {
            // the cross rods are centered around their attachment point
            parts.push(part(
                format!("{end}CrossRod_{row}"),
                prism(rod_radius, length),
                pos(0.0, row_height * row as f64, d),
                rot(0.0, PI / 2.0, -PI / 2.0),
                Some("crossBody"),
                -0.5 * length as f32,
            ));
        }
    }

    EntityModel { parts }
}

pub fn player_model() -> EntityModel {
    let leg_length: u32 = 48;
    let leg_radius: u32 = 8;
    let body_length: u32 = 40;
    let body_radius: u32 = 8;
    let arm_length: u32 = 40;
    let arm_radius: u32 = 8;
    let head_depth: u32 = 20;
    let head_radius: u32 = 16;

    let head_prism = prism(head_radius, head_depth);
    let body_prism = prism(body_radius, body_length);
    let arm_prism = prism(arm_radius, arm_length);
    let leg_prism = prism(leg_radius, leg_length);

    let head_prism_offset = -(head_depth as f32) / 2.0;
    let arm_prism_offset = (-(arm_radius as f64) * Y60) as f32;

    let head_base_y = (body_length + leg_length) as f64;
    let head_y = head_radius as f64 * Y60;
    let head_base_pos = pos(0.0, head_base_y, 0.0);
    let head_pos = pos(0.0, head_y, 0.0);

    // The body halves are neighbouring hexagons, so are the arms and the body
    let body_step = hex_step(body_radius as f64);
    let right_body_pos = pos(0.0, leg_length as f64, 0.5 * body_step);
    let left_body_pos = pos(0.0, leg_length as f64, -0.5 * body_step);

    let arm_y = (leg_length + body_length) as f64 - arm_radius as f64 * Y60;
    let arm_z = hex_step(body_radius as f64 + 0.5 * arm_radius as f64);
    let right_arm_pos = pos(0.0, arm_y, arm_z);
    let left_arm_pos = pos(0.0, arm_y, -arm_z);

    // The legs are moved a tiny bit apart to avoid z-fighting
    let leg_gap = 0.05;
    let leg_step = hex_step(leg_radius as f64);
    let right_leg_pos = pos(0.0, leg_length as f64, 0.5 * leg_step + leg_gap);
    let left_leg_pos = pos(0.0, leg_length as f64, -0.5 * leg_step - leg_gap);

    let zero = DVec3::ZERO;

    EntityModel {
        parts: vec![
            // Pivots (not rendered)
            part(
                "base",
                empty_prism(),
                zero,
                rot(0.0, PI / 2.0, 0.0),
                None,
                0.0,
            ),
            part(
                "headYawBase",
                empty_prism(),
                head_base_pos,
                zero,
                Some("base"),
                0.0,
            ),
            part(
                "headBase",
                empty_prism(),
                zero,
                zero,
                Some("headYawBase"),
                0.0,
            ),
            // Visible parts
            part(
                "head",
                head_prism,
                head_pos,
                rot(0.0, PI / 2.0, PI / 2.0),
                Some("headBase"),
                head_prism_offset,
            ),
            part(
                "leftBodyHalf",
                body_prism.clone(),
                left_body_pos,
                zero,
                Some("base"),
                0.0,
            ),
            part(
                "rightBodyHalf",
                body_prism,
                right_body_pos,
                zero,
                Some("base"),
                0.0,
            ),
            part(
                "rightArm",
                arm_prism.clone(),
                right_arm_pos,
                rot(PI, 0.0, 0.0),
                Some("base"),
                arm_prism_offset,
            ),
            part(
                "leftArm",
                arm_prism,
                left_arm_pos,
                rot(PI, 0.0, 0.0),
                Some("base"),
                arm_prism_offset,
            ),
            part(
                "rightLeg",
                leg_prism.clone(),
                right_leg_pos,
                rot(PI, 0.0, 0.0),
                Some("base"),
                0.0,
            ),
            part(
                "leftLeg",
                leg_prism,
                left_leg_pos,
                rot(PI, 0.0, 0.0),
                Some("base"),
                0.0,
            ),
        ],
    }
}

pub fn sheep_model() -> EntityModel {
    let leg_length: u32 = 32;
    let leg_radius: u32 = 6;
    let body_length: u32 = 48;
    let body_radius: u32 = 16;
    let head_depth: u32 = 16;
    let head_radius: u32 = 12;

    let head_offset = 2.0;
    let head_y_offset = 2.0;
    let leg_side_offset = 0.5 * body_radius as f64;
    let leg_y_offset = 3.0;

    let head_prism = prism(head_radius, head_depth);
    let body_prism = prism(body_radius, body_length);
    let leg_prism = prism(leg_radius, leg_length);

    let head_prism_offset = -(head_depth as f32) / 2.0;

    let head_dist_x = 0.5 * body_length as f64 + head_offset;
    let body_dist_x = 0.5 * body_length as f64;
    let leg_dist_x = 0.5 * body_length as f64 - leg_radius as f64;

    let head_y = leg_length as f64 + leg_y_offset + (head_radius as f64 + head_y_offset) * Y60;
    let body_y = leg_length as f64 + leg_y_offset;
    let leg_y = leg_length as f64;

    let head_pos = pos(head_dist_x, head_y, 0.0);
    let body_pos = pos(-body_dist_x, body_y, 0.0);
    let front_right_leg_pos = pos(leg_dist_x, leg_y, leg_side_offset);
    let front_left_leg_pos = pos(leg_dist_x, leg_y, -leg_side_offset);
    let back_right_leg_pos = pos(-leg_dist_x, leg_y, leg_side_offset);
    let back_left_leg_pos = pos(-leg_dist_x, leg_y, -leg_side_offset);

    let leg_rotation = rot(PI, 0.0, 0.0);

    EntityModel {
        parts: vec![
            part(
                "head",
                head_prism,
                head_pos,
                rot(0.0, PI / 2.0, PI / 2.0),
                None,
                head_prism_offset,
            ),
            part(
                "body",
                body_prism,
                body_pos,
                rot(0.0, PI / 2.0, -PI / 2.0),
                None,
                0.0,
            ),
            part(
                "frontRightLeg",
                leg_prism.clone(),
                front_right_leg_pos,
                leg_rotation,
                None,
                0.0,
            ),
            part(
                "frontLeftLeg",
                leg_prism.clone(),
                front_left_leg_pos,
                leg_rotation,
                None,
                0.0,
            ),
            part(
                "backRightLeg",
                leg_prism.clone(),
                back_right_leg_pos,
                leg_rotation,
                None,
                0.0,
            ),
            part(
                "backLeftLeg",
                leg_prism,
                back_left_leg_pos,
                leg_rotation,
                None,
                0.0,
            ),
        ],
    }
}

#[cfg(test)]
mod tests {
    use std::collections::HashMap;

    use super::*;
    use crate::test_utils::assert_vec3_eq;

    /// The same checks as the Scala `EntityModel` constructor: unique names, and parents before children
    fn assert_valid(model: &EntityModel) {
        let mut indices = HashMap::new();
        for (idx, p) in model.parts.iter().enumerate() {
            assert!(
                indices.insert(p.name.as_str(), idx).is_none(),
                "duplicate part '{}'",
                p.name
            );
            if let Some(parent) = &p.parent {
                let parent_idx = indices.get(parent.as_str());
                assert!(
                    parent_idx.is_some(),
                    "the parent of '{}' must come before it",
                    p.name
                );
            }
        }
    }

    fn find<'a>(model: &'a EntityModel, name: &str) -> &'a ModelPart {
        model.parts.iter().find(|p| p.name == name).unwrap()
    }

    #[test]
    fn models_are_valid() {
        assert_valid(&boat_model());
        assert_valid(&player_model());
        assert_valid(&sheep_model());
    }

    #[test]
    fn boat_has_expected_parts() {
        let model = boat_model();
        let names: Vec<&str> = model.parts.iter().map(|p| p.name.as_str()).collect();
        assert_eq!(
            names,
            vec![
                "body",
                "crossBody",
                "rod_-4_0",
                "rod_-3_0",
                "rod_-2_0",
                "rod_-1_0",
                "rod_0_0",
                "rod_1_0",
                "rod_2_0",
                "rod_3_0",
                "rod_4_0",
                "rod_-5_1",
                "rod_4_1",
                "rod_-6_2",
                "rod_4_2",
                "backCrossRod_0",
                "frontCrossRod_0",
                "backCrossRod_1",
                "frontCrossRod_1",
            ]
        );

        let cross_rod = find(&model, "frontCrossRod_1");
        assert_eq!(cross_rod.prism.length, 62);
        assert_eq!(cross_rod.prism_offset, -31.0);
        assert_vec3_eq!(
            cross_rod.position,
            DVec3::new(0.0, 6.0, 100.4f32 as f64),
            epsilon = 1e-5
        );
        assert_eq!(find(&model, "backCrossRod_0").prism.length, 55);
    }

    #[test]
    fn player_has_expected_parts() {
        let model = player_model();
        assert_eq!(model.parts.len(), 10);
        assert_eq!(find(&model, "head").prism_offset, -10.0);
        assert_vec3_eq!(
            find(&model, "headYawBase").position,
            DVec3::new(0.0, 88.0, 0.0),
            epsilon = 1e-5
        );
        assert_eq!(find(&model, "leftArm").parent.as_deref(), Some("base"));
    }

    #[test]
    fn sheep_has_expected_parts() {
        let model = sheep_model();
        assert_eq!(model.parts.len(), 6);
        assert_eq!(find(&model, "head").prism_offset, -8.0);
        assert_eq!(find(&model, "body").position, DVec3::new(-24.0, 35.0, 0.0));
        assert_eq!(
            find(&model, "backLeftLeg").position,
            DVec3::new(-18.0, 32.0, -8.0)
        );
    }
}
