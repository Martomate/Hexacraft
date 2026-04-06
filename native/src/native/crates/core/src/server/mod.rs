use crate::server::request::NetworkPacket;
use crate::zmq::ServerSocket;
use std::sync::Arc;
use std::{collections::HashMap, f64::consts::PI};

pub use state::GameState;

mod state;
mod nbt;
mod request;
mod response;

const SQRT_3: f64 = 1.732050807568877293527446341505872367_f64;

pub trait RequestHandler {
    fn handle(&self, client_id: u64, packet: NetworkPacket) -> Option<nbt::Tag>;
    fn shutdown(&self) -> impl Future<Output = ()> + Send;
}

pub struct GameServer<H> {
    socket: Arc<ServerSocket>,
    handler: H,
}

impl<H> GameServer<H> {
    pub async fn start(port: u16, handler: H) -> Self {
        let socket = ServerSocket::new();
        socket.bind(port).await.unwrap();
        Self {
            socket: Arc::new(socket),
            handler,
        }
    }
}

impl<H: RequestHandler> GameServer<H> {
    pub async fn shutdown(&self) {
        self.handler.shutdown().await;
    }

    pub async fn run_receiver(&self) {
        loop {
            let client_id_bytes = self.socket.receive().await.unwrap();
            let message = self.socket.receive().await.unwrap();

            match decode_request(&client_id_bytes, &message) {
                Err(err) => eprintln!("{err}"),
                Ok((client_id, packet)) => {
                    if let Some(response) = self.handler.handle(client_id, packet) {
                        self.socket
                            .send(client_id_bytes, response.to_binary())
                            .await
                            .unwrap();
                    }
                }
            }
        }
    }
}

fn decode_request(
    client_id_bytes: &[u8],
    message_bytes: &[u8],
) -> Result<(u64, NetworkPacket), String> {
    let client_id =
        decode_client_id(client_id_bytes).map_err(|err| format!("Got invalid client id: {err}"))?;
    let packet =
        decode_message(message_bytes).map_err(|err| format!("Got invalid message: {err}"))?;
    Ok((client_id, packet))
}

fn decode_client_id(client_id_bytes: &[u8]) -> Result<u64, &'static str> {
    String::from_utf8(client_id_bytes.to_vec())
        .map_err(|_| "client id was not utf8")?
        .parse::<u64>()
        .map_err(|_| "client id was not a positive integer")
}

fn decode_message(message_bytes: &[u8]) -> Result<NetworkPacket, String> {
    let (_, tag) = nbt::Tag::from_binary(message_bytes)?;

    let packet = match tag {
        nbt::Tag::Map(vs) => {
            if vs.len() != 1 {
                Err("packet did not have exactly 1 field")?;
            }
            let (name, tag) = &vs[0];
            NetworkPacket::decode(name, tag.clone())?
        }
        _ => Err("packet was not a Map tag")?,
    };

    Ok(packet)
}

pub struct WorldInfo {
    version: u16,
    world_name: String,
    world_size: CylinderSize,
    _gen: WorldGenSettings,
}

pub struct WorldGenSettings {
    seed: u64,
    block_gen_scale: f64,
    height_map_gen_scale: f64,
    block_density_gen_scale: f64,
    biome_height_map_gen_scale: f64,
    biome_height_variation_gen_scale: f64,
}

/// The real cylinder size (the number of chunks around the cylinder) is:<br> <code>ringSize =
/// 2&#94;sizeExponent</code>
///
/// @param worldSize
///   the size exponent, <b>max-value: 20</b>
#[derive(Clone, Copy)]
pub struct CylinderSize(u8);

impl CylinderSize {
    const Y60: f64 = SQRT_3 / 2.0;

    /** The number of chunks around the cylinder */
    pub fn ring_size(self) -> u32 {
        1 << self.0
    }

    /** ringSize - 1 */
    pub fn ring_size_mask(self) -> u32 {
        self.ring_size() - 1
    }

    /** The number of blocks around the cylinder */
    pub fn total_size(self) -> u32 {
        16 * self.ring_size()
    }

    /** totalSize - 1 */
    pub fn total_size_mask(self) -> u32 {
        self.total_size() - 1
    }

    /** The angle (in radians) of half a block seen from the center of the cylinder */
    pub fn hex_angle(self) -> f64 {
        (2.0 * PI) / self.total_size() as f64
    }

    /** The radius of the cylinder */
    pub fn radius(self) -> f64 {
        CylinderSize::Y60 / self.hex_angle()
    }

    /** The circumference of the cylinder.<br><br>This is NOT the number of blocks, for that see
     * <code>totalSize</code>.
     */
    pub fn circumference(self) -> f64 {
        self.total_size() as f64 * CylinderSize::Y60
    }
}

pub struct HexBox {
    pub radius: f32,
    pub bottom: f32,
    pub top: f32,
}

pub type UUID = u128;
pub type Block = u8;
pub type Vector3d = [f64; 3];
pub type Inventory = HashMap<u8, Block>;

const AIR: Block = 0;

pub struct Player {
    id: UUID,
    name: String,
    inventory: Inventory,
    bounds: HexBox,
    velocity: Vector3d,
    position: Vector3d,
    rotation: Vector3d,
    flying: bool,
    selected_item_slot: u8,
}

impl Player {
    pub fn new(id: UUID, name: String, inventory: Inventory) -> Self {
        Self {
            id,
            name,
            inventory,
            bounds: HexBox {
                radius: 0.2,
                bottom: -1.65,
                top: 0.1,
            },
            velocity: [0.0; 3],
            position: [0.0; 3],
            rotation: [0.0; 3],
            flying: false,
            selected_item_slot: 0,
        }
    }

    pub fn block_in_hand(&self) -> Block {
        self.inventory
            .get(&self.selected_item_slot)
            .cloned()
            .unwrap_or(AIR)
    }
}
