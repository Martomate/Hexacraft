use std::{
    collections::{HashMap, VecDeque},
    sync::Mutex,
};

use crate::server::{
    GracefulShutdown, RequestHandler, nbt,
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

    fn access_player_state<R>(
        &self,
        client_id: u64,
        access: impl FnOnce(&mut PlayerConnectionState) -> R,
    ) -> Option<R> {
        let mut players = self.players.lock().unwrap();
        players.get_mut(&client_id).map(access)
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
                let message = self.access_player_state(client_id, |p| ServerMessage {
                    text: format!("{} logged out", p.player.name),
                    sender: ServerMessageSender::Server,
                })?;

                let mut players = self.players.lock().unwrap();
                players.remove(&client_id);

                for (_, p) in players.iter_mut() {
                    p.messages_to_send.push_back(message.clone());
                }

                None
            }
            NetworkPacket::GetWorldInfo => Some(
                GetWorldInfoResponse {
                    info: &self.world_info,
                }
                .into(),
            ),
            NetworkPacket::LoadColumnData { coords } => Some(nbt::MapTag::new().build()),
            NetworkPacket::GetPlayerState => self.access_player_state(client_id, |p| {
                GetPlayerStateResponse { player: &p.player }.into()
            }),
            NetworkPacket::GetEvents => {
                let new_messages = self.access_player_state(client_id, |p| {
                    p.messages_to_send.drain(..).collect::<Vec<_>>()
                })?;

                Some(
                    GetEventsResponse {
                        // TODO: make proper shutdown feature
                        server_shutting_down: *self.is_shutting_down.lock().unwrap(),
                        new_messages,
                    }
                    .into(),
                )
            }
            NetworkPacket::GetWorldLoadingEvents { max_chunks_to_load } => {
                Some(GetWorldLoadingEventsResponse {}.into())
            }
            NetworkPacket::PlayerRightClicked => {
                // TODO: player right clicked
                None
            }
            NetworkPacket::PlayerLeftClicked => {
                // TODO: player left clicked
                None
            }
            NetworkPacket::PlayerToggledFlying => {
                self.access_player_state(client_id, |p| {
                    let p = &mut p.player;
                    p.flying = !p.flying
                });
                None
            }
            NetworkPacket::PlayerSetSelectedItemSlot { slot } => {
                self.access_player_state(client_id, |p| {
                    let p = &mut p.player;
                    p.selected_item_slot = slot as u8;
                });
                None
            }
            NetworkPacket::PlayerUpdatedInventory { inventory } => {
                self.access_player_state(client_id, |p| {
                    let p = &mut p.player;
                    p.inventory = inventory;

                    PlayerUpdatedInventoryResponse {
                        inventory: &p.inventory,
                    }
                    .into()
                })
            }
            NetworkPacket::PlayerMovedMouse { distance } => {
                // TODO: player mouse moved
                None
            }
            NetworkPacket::PlayerPressedKeys { keys } => {
                // TODO: player pressed keys
                None
            }
            NetworkPacket::RunCommand { command, args } => {
                let sender_name = self.access_player_state(client_id, |p| p.player.name.clone())?;

                match command.as_str() {
                    "chat" => {
                        if args.len() != 1 {
                            println!("Wrong number of arguments to chat command: {}", args.len());
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

                None
            }
        }
    }
}

impl GracefulShutdown for GameState {
    fn initiate(&self) {
        let mut is_shutting_down = self.is_shutting_down.lock().unwrap();
        *is_shutting_down = true;
    }

    fn done(&self) -> bool {
        let is_shutting_down = { *self.is_shutting_down.lock().unwrap() };
        if !is_shutting_down {
            return false;
        }
        let has_no_players = { self.players.lock().unwrap().is_empty() };
        if !has_no_players {
            return false;
        }
        true
    }
}
