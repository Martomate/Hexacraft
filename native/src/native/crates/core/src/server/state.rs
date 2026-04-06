use std::{collections::HashMap, sync::Mutex};

use crate::server::{
    RequestHandler, nbt,
    request::NetworkPacket,
    response::*,
    world::{CylinderSize, Inventory, Player, WorldGenSettings, WorldInfo},
};

pub struct GameState {
    is_online: bool,
    path: String,

    is_shutting_down: Mutex<bool>,
    world_info: WorldInfo,
    players: Mutex<HashMap<u64, Player>>,
}

impl GameState {
    pub fn create(is_online: bool, path: String) -> Self {
        Self {
            is_online,
            path: path.to_string(),

            is_shutting_down: Mutex::new(false),
            world_info: WorldInfo {
                version: 2,
                world_name: "Test 123".to_string(),
                world_size: CylinderSize(8),
                _gen: WorldGenSettings {
                    seed: 42,
                    block_gen_scale: 0.1,
                    height_map_gen_scale: 0.02,
                    block_density_gen_scale: 0.01,
                    biome_height_map_gen_scale: 0.002,
                    biome_height_variation_gen_scale: 0.002,
                },
            },
            players: Mutex::new(HashMap::new()),
        }
    }
}

impl RequestHandler for GameState {
    fn handle(&self, client_id: u64, packet: NetworkPacket) -> Option<nbt::Tag> {
        match packet {
            NetworkPacket::Login { id, name } => {
                let is_shutting_down = { *self.is_shutting_down.lock().unwrap() };
                if is_shutting_down {
                    Some(LoginResponse::failure("server is shutting down").into())
                    // TODO: handle more cases
                } else {
                    self.players.lock().unwrap().insert(
                        client_id,
                        Player::new(
                            id,
                            name,
                            Inventory::new(), // TODO: load from disk
                        ),
                    );
                    Some(LoginResponse::success().into())
                }
            }
            NetworkPacket::Logout => {
                self.players.lock().unwrap().remove(&client_id);
                None
            }
            NetworkPacket::GetWorldInfo => Some(
                GetWorldInfoResponse {
                    info: &self.world_info,
                }
                .into(),
            ),
            NetworkPacket::LoadColumnData { coords } => Some(nbt::MapTag::new().build()),
            NetworkPacket::GetPlayerState => self
                .players
                .lock()
                .unwrap()
                .get(&client_id)
                .map(|p| GetPlayerStateResponse { player: p }.into()),
            NetworkPacket::GetEvents => {
                Some(
                    GetEventsResponse {
                        // TODO: make proper shutdown feature
                        server_shutting_down: *self.is_shutting_down.lock().unwrap(),
                    }
                    .into(),
                )
            }
            NetworkPacket::GetWorldLoadingEvents { max_chunks_to_load } => {
                Some(GetWorldLoadingEventsResponse {}.into())
            }
            NetworkPacket::PlayerRightClicked => None,
            NetworkPacket::PlayerLeftClicked => None,
            NetworkPacket::PlayerToggledFlying => {
                if let Some(p) = self.players.lock().unwrap().get_mut(&client_id) {
                    p.flying = !p.flying
                }
                None
            }
            NetworkPacket::PlayerSetSelectedItemSlot { slot } => {
                if let Some(p) = self.players.lock().unwrap().get_mut(&client_id) {
                    p.selected_item_slot = slot as u8;
                }
                None
            }
            NetworkPacket::PlayerUpdatedInventory { inventory } => {
                if let Some(p) = self.players.lock().unwrap().get_mut(&client_id) {
                    p.inventory = inventory;
                    Some(
                        PlayerUpdatedInventoryResponse {
                            inventory: &p.inventory,
                        }
                        .into(),
                    )
                } else {
                    None
                }
            }
            NetworkPacket::PlayerMovedMouse { distance } => None,
            NetworkPacket::PlayerPressedKeys { keys } => None,
            NetworkPacket::RunCommand { command, args } => {
                println!("Received command '{command}' with args: {args:?}");
                None
            }
        }
    }

    async fn shutdown(&self) {
        {
            let mut is_shutting_down = self.is_shutting_down.lock().unwrap();
            *is_shutting_down = true;
        }
        // TODO: wait for clients to get disconnected (max N seconds), then shut down
    }
}
