use std::{
    collections::{HashMap, VecDeque},
    sync::Mutex,
};

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
    players: Mutex<HashMap<u64, PlayerConnectionState>>,
}

struct PlayerConnectionState {
    player: Player,
    messages_to_send: VecDeque<ServerMessage>,
}

#[derive(Clone)]
pub struct ServerMessage {
    pub text: String,
    pub sender: ServerMessageSender,
}

#[derive(Clone)]
pub enum ServerMessageSender {
    Server,
    Player { name: String },
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
                    let message = ServerMessage {
                        text: format!("{} logged in", name),
                        sender: ServerMessageSender::Server,
                    };

                    let mut players = self.players.lock().unwrap();
                    for (_, p) in players.iter_mut() {
                        p.messages_to_send.push_back(message.clone());
                    }
                    players.insert(
                        client_id,
                        PlayerConnectionState {
                            player: Player::new(
                                id,
                                name,
                                Inventory::new(), // TODO: load from disk
                            ),
                            messages_to_send: VecDeque::new(),
                        },
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
                .map(|p| GetPlayerStateResponse { player: &p.player }.into()),
            NetworkPacket::GetEvents => {
                let player_data = {
                    let mut players = self.players.lock().unwrap();

                    players
                        .get_mut(&client_id)
                        .map(|p| p.messages_to_send.drain(..).collect::<Vec<_>>())
                };

                player_data.map(|new_messages| {
                    GetEventsResponse {
                        // TODO: make proper shutdown feature
                        server_shutting_down: *self.is_shutting_down.lock().unwrap(),
                        new_messages,
                    }
                    .into()
                })
            }
            NetworkPacket::GetWorldLoadingEvents { max_chunks_to_load } => {
                Some(GetWorldLoadingEventsResponse {}.into())
            }
            NetworkPacket::PlayerRightClicked => None,
            NetworkPacket::PlayerLeftClicked => None,
            NetworkPacket::PlayerToggledFlying => {
                if let Some(p) = self.players.lock().unwrap().get_mut(&client_id) {
                    let p = &mut p.player;
                    p.flying = !p.flying
                }
                None
            }
            NetworkPacket::PlayerSetSelectedItemSlot { slot } => {
                if let Some(p) = self.players.lock().unwrap().get_mut(&client_id) {
                    let p = &mut p.player;
                    p.selected_item_slot = slot as u8;
                }
                None
            }
            NetworkPacket::PlayerUpdatedInventory { inventory } => {
                if let Some(p) = self.players.lock().unwrap().get_mut(&client_id) {
                    let p = &mut p.player;
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
                let sender_data = {
                    let players = self.players.lock().unwrap();

                    players.get(&client_id).map(|p| p.player.name.clone())
                };

                if let Some(sender_name) = sender_data {
                    match command.as_str() {
                        "chat" => {
                            if args.len() != 1 {
                                println!(
                                    "Wrong number of arguments to chat command: {}",
                                    args.len()
                                );
                            } else {
                                let message = &args[0];
                                for (_, p) in self.players.lock().unwrap().iter_mut() {
                                    p.messages_to_send.push_back(ServerMessage {
                                        text: message.to_string(),
                                        sender: ServerMessageSender::Player {
                                            name: sender_name.clone(),
                                        },
                                    });
                                }
                            }
                        }
                        "spawn" => todo!(),
                        "kill" => todo!(),
                        _ => println!("Received unknown command: {command}"),
                    }
                }
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
