use glam::DVec3;

use crate::server::response::{EntityModel, HexPrism, ModelPart};

/// TODO: implement actual model
pub fn boat_model() -> EntityModel {
    EntityModel {
        parts: vec![ModelPart {
            name: "body".to_string(),
            prism: HexPrism {
                radius: 4,
                length: 4,
            },
            parent: None,
            position: DVec3 {
                x: 0.0,
                y: 0.0,
                z: 0.0,
            },
            rotation: DVec3 {
                x: 1.0,
                y: 2.0,
                z: 3.0,
            },
            prism_offset: 0.0,
        }],
    }
}

/// TODO: implement actual model
pub fn player_model() -> EntityModel {
    EntityModel {
        parts: vec![ModelPart {
            name: "body".to_string(),
            prism: HexPrism {
                radius: 4,
                length: 6,
            },
            parent: None,
            position: DVec3 {
                x: 0.0,
                y: 0.0,
                z: 0.0,
            },
            rotation: DVec3 {
                x: 1.0,
                y: 2.0,
                z: 3.0,
            },
            prism_offset: 0.0,
        }],
    }
}

/// TODO: implement actual model
pub fn sheep_model() -> EntityModel {
    EntityModel {
        parts: vec![ModelPart {
            name: "body".to_string(),
            prism: HexPrism {
                radius: 4,
                length: 8,
            },
            parent: None,
            position: DVec3 {
                x: 0.0,
                y: 0.0,
                z: 0.0,
            },
            rotation: DVec3 {
                x: 1.0,
                y: 2.0,
                z: 3.0,
            },
            prism_offset: 0.0,
        }],
    }
}
