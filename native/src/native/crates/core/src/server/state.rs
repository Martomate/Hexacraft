use std::collections::{HashMap, HashSet, VecDeque};
use std::sync::Mutex;
use std::time::Duration;

use glam::{DVec3, Vec2};

use crate::server::coords::{BlockCoords, ChunkRelWorld, ColumnRelWorld, CylCoords};
use crate::server::request::NetworkPacket;
use crate::server::response::*;
use crate::server::world::{
    CylinderSize, Inventory, NbtDecoder as _, NbtEncoder as _, Player, World, WorldGenSettings,
    WorldInfo, WorldProvider, WorldProviderPath,
};
use crate::server::{GracefulShutdown, RequestHandler, input, nbt};

pub struct GameState<P> {
    is_online: bool,
    path: String,

    is_shutting_down: Mutex<bool>,
    world_info: WorldInfo,
    players: Mutex<HashMap<u64, PlayerConnectionState>>,

    world: World,
    world_provider: Mutex<P>,
}

struct PlayerConnectionState {
    player: Player,
    messages_to_send: VecDeque<ServerMessage>,
    mouse_movement: Vec2,
    pressed_keys: Vec<String>,

    chunks_loaded: HashSet<ChunkRelWorld>,
    new_chunks_loaded: Vec<ChunkRelWorld>,
    new_chunks_unloaded: Vec<ChunkRelWorld>,
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

impl<P: WorldProvider> GameState<P> {
    pub fn create(is_online: bool, path: String, world_provider: P) -> Self {
        let gen_settings = WorldGenSettings {
            seed: 42,
            block_gen_scale: 0.1,
            height_map_gen_scale: 0.02,
            block_density_gen_scale: 0.01,
            biome_height_map_gen_scale: 0.002,
            biome_height_variation_gen_scale: 0.002,
        };
        let world_size = CylinderSize(8);
        Self {
            is_online,
            path: path.to_string(),

            is_shutting_down: Mutex::new(false),
            world_info: WorldInfo {
                version: 2,
                world_name: "Test 123".to_string(),
                world_size,
                _gen: gen_settings,
            },
            players: Mutex::new(HashMap::new()),

            world: World::new(gen_settings, world_size),
            world_provider: Mutex::new(world_provider),
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

    pub async fn run_ticks(&self) {
        let mut interval = tokio::time::interval(Duration::from_millis(1000 / 60));
        interval.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Delay); // Skip would be fine too

        while !{ *self.is_shutting_down.lock().unwrap() } {
            interval.tick().await;
            self.tick();
        }
    }

    fn tick(&self) {
        {
            let mut players = self.players.lock().unwrap();
            for (_, p) in players.iter_mut() {
                input::update_player(
                    &mut p.player,
                    p.mouse_movement,
                    &p.pressed_keys
                        .iter()
                        .map(|s| s.as_str())
                        .collect::<Vec<_>>(),
                );
                p.mouse_movement = Vec2::new(0.0, 0.0);

                // Temporary:
                {
                    let d = 6;
                    let s = 2 * d + 1;
                    let first_chunks_to_load: Vec<_> = (0..s * s * s)
                        .map(|i| {
                            let dx = (i % s) - d;
                            let dy = (i / s % s) - d;
                            let dz = (i / s / s) - d;
                            ChunkRelWorld::new(
                                dx,
                                dy,
                                (dz as u32 & self.world_info.world_size.ring_size_mask()) as i32,
                            )
                        })
                        .collect();

                    if !p.chunks_loaded.contains(&first_chunks_to_load[0]) {
                        for &c in &first_chunks_to_load {
                            p.chunks_loaded.insert(c);
                            p.new_chunks_loaded.push(c);
                        }
                    }
                }
            }
        }
    }
}

impl<P: WorldProvider> RequestHandler for GameState<P> {
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
                    let player = if let Some(tag) = self
                        .world_provider
                        .lock()
                        .unwrap()
                        .load_state(WorldProviderPath::PlayerData(id))
                    {
                        Player::decode(&tag)
                    } else {
                        let start_x = rand::random_range(-5..=5);
                        let start_z = rand::random_range(-5..=5);
                        let start_y = (self.world.height(start_x, start_z) as f64) + 4.0;

                        let start_pos = BlockCoords::new(start_x as f64, start_y, start_z as f64);

                        Ok(Player {
                            position: DVec3::from(CylCoords::from(start_pos)),
                            ..Player::new(id, name, Inventory::initial())
                        })
                    };

                    match player {
                        Ok(player) => {
                            self.world_provider
                                .lock()
                                .unwrap()
                                .save_state(WorldProviderPath::PlayerData(id), player.encode());

                            players.insert(
                                client_id,
                                PlayerConnectionState {
                                    player,
                                    messages_to_send: VecDeque::new(),
                                    mouse_movement: Vec2::new(0.0, 0.0),
                                    pressed_keys: Vec::new(),
                                    chunks_loaded: HashSet::new(),
                                    new_chunks_loaded: Vec::new(),
                                    new_chunks_unloaded: Vec::new(),
                                },
                            );
                            Some(LoginResponse::success().into())
                        }
                        Err(msg) => Some(LoginResponse::failure(&msg).into()),
                    }
                }
            }
            NetworkPacket::Logout => {
                self.access_player_state(client_id, |p| {
                    self.world_provider.lock().unwrap().save_state(
                        WorldProviderPath::PlayerData(p.player.id),
                        p.player.encode(),
                    );
                })?;

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
            NetworkPacket::LoadColumnData { coords } => {
                match self
                    .world
                    .height_map_of_column(ColumnRelWorld::decode(coords))
                {
                    Some(height_map) => Some(
                        nbt::MapTag::new()
                            .set(
                                "heightMap",
                                nbt::Tag::ShortArray(
                                    (0..16 * 16).map(|i| height_map[i % 16][i / 16]).collect(),
                                ),
                            )
                            .build(),
                    ),
                    None => Some(nbt::MapTag::new().build()),
                }
            }
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
                let (loaded, unloaded) = self.access_player_state(client_id, |p| {
                    (
                        p.new_chunks_loaded.drain(..).collect::<Vec<_>>(),
                        p.new_chunks_unloaded.drain(..).collect::<Vec<_>>(),
                    )
                })?;
                let loaded = loaded
                    .iter()
                    .map(|&c| {
                        let (blocks, metadata) = self.world.generate_chunk(c);
                        LoadedChunk {
                            coords: c,
                            data: LoadedChunkData {
                                blocks: blocks.into_iter().collect(),
                                metadata: metadata.into_iter().collect(),
                                entities: Vec::new(),
                                is_decorated: true,
                            },
                        }
                    })
                    .collect();
                Some(GetWorldLoadingEventsResponse { loaded, unloaded }.into())
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
                    p.inventory = Inventory::from(inventory);

                    PlayerUpdatedInventoryResponse {
                        inventory: &p.inventory,
                    }
                    .into()
                })
            }
            NetworkPacket::PlayerMovedMouse { distance: d } => {
                self.access_player_state(client_id, |p| {
                    let m = p.mouse_movement;
                    p.mouse_movement = Vec2::new(m.x + d.x, m.y + d.y);
                })?;
                None
            }
            NetworkPacket::PlayerPressedKeys { keys } => {
                self.access_player_state(client_id, |p| {
                    p.pressed_keys = keys;
                })?;
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

impl<P: WorldProvider> GracefulShutdown for GameState<P> {
    fn initiate(&self) {
        let mut is_shutting_down = self.is_shutting_down.lock().unwrap();
        *is_shutting_down = true;

        {
            for (_, p) in self.players.lock().unwrap().iter() {
                self.world_provider.lock().unwrap().save_state(
                    WorldProviderPath::PlayerData(p.player.id),
                    p.player.encode(),
                );
            }
        }
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
