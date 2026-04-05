use std::{collections::HashMap, f64::consts::PI, mem::transmute, sync::Arc};

use tokio::sync::Mutex;

use crate::zmq::ServerSocket;

const SQRT_3: f64 = 1.732050807568877293527446341505872367_f64;

pub struct GameServer {
    is_online: bool,
    socket: Arc<ServerSocket>,
    path: String,

    is_shutting_down: Mutex<bool>,
    world_info: WorldInfo,
}

struct WorldInfo {
    version: u16,
    world_name: String,
    world_size: CylinderSize,
    _gen: WorldGenSettings,
}

struct WorldGenSettings {
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
struct CylinderSize(u8);

impl CylinderSize {
    const Y60: f64 = SQRT_3 / 2.0;

    /** The number of chunks around the cylinder */
    fn ring_size(self) -> u32 {
        1 << self.0
    }

    /** ringSize - 1 */
    fn ring_size_mask(self) -> u32 {
        self.ring_size() - 1
    }

    /** The number of blocks around the cylinder */
    fn total_size(self) -> u32 {
        16 * self.ring_size()
    }

    /** totalSize - 1 */
    fn total_size_mask(self) -> u32 {
        self.total_size() - 1
    }

    /** The angle (in radians) of half a block seen from the center of the cylinder */
    fn hex_angle(self) -> f64 {
        (2.0 * PI) / self.total_size() as f64
    }

    /** The radius of the cylinder */
    fn radius(self) -> f64 {
        CylinderSize::Y60 / self.hex_angle()
    }

    /** The circumference of the cylinder.<br><br>This is NOT the number of blocks, for that see
     * <code>totalSize</code>.
     */
    fn circumference(self) -> f64 {
        self.total_size() as f64 * CylinderSize::Y60
    }
}

struct HexBox {
    radius: f32,
    bottom: f32,
    top: f32,
}

type UUID = u128;
type Block = u8;
type Vector3d = [f64; 3];
type Inventory = HashMap<u8, Block>;

const AIR: Block = 0;

struct Player {
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
    fn new(id: UUID, name: String, inventory: Inventory) -> Self {
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

    fn block_in_hand(&self) -> Block {
        self.inventory
            .get(&self.selected_item_slot)
            .cloned()
            .unwrap_or(AIR)
    }
}

impl GameServer {
    pub async fn start(is_online: bool, port: u16, path: String) -> Self {
        let socket = ServerSocket::new();
        socket.bind(port).await.unwrap();
        Self {
            is_online,
            socket: Arc::new(socket),
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
        }
    }

    pub async fn shutdown(self: Arc<Self>) {
        {
            let mut is_shutting_down = self.is_shutting_down.lock().await;
            *is_shutting_down = true;
        }
        // TODO: wait for clients to get disconnected (max N seconds), then shut down
    }

    pub async fn run_receiver(self: Arc<Self>) {
        let mut players: HashMap<u64, Player> = HashMap::new();

        loop {
            let client_id_bytes = self.socket.receive().await.unwrap();
            let message = self.socket.receive().await.unwrap();

            match nbt::Tag::from_binary(&message).and_then(|(_, tag)| match tag {
                nbt::Tag::Map(vs) => {
                    if vs.len() != 1 {
                        return Err("packet did not have exactly 1 field")?;
                    }
                    let (name, tag) = &vs[0];
                    let client_id = String::from_utf8(client_id_bytes.clone())
                        .map_err(|_| "client id was not utf8")?
                        .parse::<u64>()
                        .map_err(|_| "client id was not a positive integer")?;
                    Ok((client_id, NetworkPacket::decode(name, tag.clone())?))
                }
                _ => Err("packet was not a Map tag")?,
            }) {
                Err(err) => eprintln!("Got invalid message: {err}"),
                Ok((client_id, packet)) => {
                    let response: Option<nbt::Tag> = match packet {
                        NetworkPacket::Login { id, name } => {
                            let is_shutting_down = { *self.is_shutting_down.lock().await };
                            if is_shutting_down {
                                Some(
                                    nbt::MapTag::new()
                                        .set("success", nbt::Tag::Byte(0))
                                        .set(
                                            "error",
                                            nbt::Tag::String("server is shutting down".to_string()),
                                        )
                                        .build(),
                                )
                                // TODO: handle more cases
                            } else {
                                players.insert(
                                    client_id,
                                    Player::new(
                                        id,
                                        name,
                                        Inventory::new(), // TODO: load from disk
                                    ),
                                );
                                Some(nbt::MapTag::new().set("success", nbt::Tag::Byte(1)).build())
                            }
                        }
                        NetworkPacket::Logout => {
                            players.remove(&client_id);
                            None
                        }
                        NetworkPacket::GetWorldInfo => {
                            let info = &self.world_info;
                            Some(
                                nbt::MapTag::new()
                                    .set("version", nbt::Tag::Short(info.version as i16))
                                    .set(
                                        "general",
                                        nbt::MapTag::new()
                                            .set(
                                                "worldSize",
                                                nbt::Tag::Byte(info.world_size.0 as i8),
                                            )
                                            .set("name", nbt::Tag::String(info.world_name.clone()))
                                            .build(),
                                    )
                                    .set("gen", {
                                        let s = &info._gen;
                                        nbt::MapTag::new()
                                            .set("seed", nbt::Tag::Long(s.seed as i64))
                                            .set(
                                                "blockGenScale",
                                                nbt::Tag::Double(s.block_gen_scale),
                                            )
                                            .set(
                                                "heightMapGenScale",
                                                nbt::Tag::Double(s.height_map_gen_scale),
                                            )
                                            .set(
                                                "blockDensityGenScale",
                                                nbt::Tag::Double(s.block_density_gen_scale),
                                            )
                                            .set(
                                                "biomeHeightGenScale",
                                                nbt::Tag::Double(s.biome_height_map_gen_scale),
                                            )
                                            .set(
                                                "biomeHeightVariationGenScale",
                                                nbt::Tag::Double(
                                                    s.biome_height_variation_gen_scale,
                                                ),
                                            )
                                            .build()
                                    })
                                    .build(),
                            )
                        }
                        NetworkPacket::LoadColumnData { coords } => {
                            Some(nbt::MapTag::new().build())
                        }
                        NetworkPacket::GetPlayerState => players.get(&client_id).map(|p| {
                            nbt::MapTag::new()
                                .set("position", nbt::make_vector_tag(p.position))
                                .set("rotation", nbt::make_vector_tag(p.rotation))
                                .set("velocity", nbt::make_vector_tag(p.velocity))
                                .set("flying", nbt::Tag::Byte(if p.flying { 1 } else { 0 }))
                                .set(
                                    "selectedItemSlot",
                                    nbt::Tag::Short(p.selected_item_slot as i16),
                                )
                                .set("inventory", {
                                    encode_inventory(&p.inventory)
                                })
                                .build()
                        }),
                        NetworkPacket::GetEvents => {
                            Some(
                                nbt::MapTag::new()
                                    .set("block_updates", nbt::Tag::List(Vec::new()))
                                    .set(
                                        "entity_events",
                                        nbt::MapTag::new()
                                            .set("ids", nbt::Tag::List(Vec::new()))
                                            .set("events", nbt::Tag::List(Vec::new()))
                                            .build(),
                                    )
                                    .set(
                                        "server_shutting_down",
                                        nbt::Tag::Byte(if *self.is_shutting_down.lock().await {
                                            1
                                        } else {
                                            0
                                        }),
                                    ) // TODO: make proper shutdown feature
                                    .set("messages", nbt::Tag::List(Vec::new()))
                                    .build(),
                            )
                        }
                        NetworkPacket::GetWorldLoadingEvents { max_chunks_to_load } => Some(
                            nbt::MapTag::new()
                                .set("chunks_loaded", nbt::Tag::List(Vec::new()))
                                .set("chunks_unloaded", nbt::Tag::List(Vec::new()))
                                .build(),
                        ),
                        NetworkPacket::PlayerRightClicked => {
                            None
                        }
                        NetworkPacket::PlayerLeftClicked => {
                            None
                        }
                        NetworkPacket::PlayerToggledFlying => {
                            if let Some(p) = players.get_mut(&client_id) {
                                p.flying = !p.flying
                            }
                            None
                        }
                        NetworkPacket::PlayerSetSelectedItemSlot { slot } => {
                            if let Some(p) = players.get_mut(&client_id) {
                                p.selected_item_slot = slot as u8;
                            }
                            None
                        }
                        NetworkPacket::PlayerUpdatedInventory { inventory } => {
                            if let Some(p) = players.get_mut(&client_id) {
                                p.inventory = inventory;
                                Some(encode_inventory(&p.inventory))
                            } else {
                                None
                            }
                        }
                        NetworkPacket::PlayerMovedMouse { distance } => {
                            None
                        }
                        NetworkPacket::PlayerPressedKeys { keys } => {
                            None
                        }
                        NetworkPacket::RunCommand { command, args } => {
                            println!("Received command '{command}' with args: {args:?}");
                            None
                        }
                    };

                    if let Some(data) = response {
                        self.socket
                            .send(client_id_bytes, data.to_binary())
                            .await
                            .unwrap();
                    }
                }
            }
        }
    }
}

fn encode_inventory(inventory: &Inventory) -> nbt::Tag {
    nbt::MapTag::new()
        .set(
            "slots",
            nbt::Tag::List(
                inventory
                    .iter()
                    .map(|(&slot, &block)| {
                        nbt::MapTag::new()
                            .set("slot", nbt::Tag::Byte(slot as i8))
                            .set("id", nbt::Tag::Byte(block as i8))
                            .build()
                    })
                    .collect(),
            ),
        )
        .build()
}

enum NetworkPacket {
    Login { id: u128, name: String },
    Logout,

    GetWorldInfo,
    LoadColumnData { coords: u64 },

    GetPlayerState,
    GetEvents,
    GetWorldLoadingEvents { max_chunks_to_load: u16 },

    PlayerRightClicked,
    PlayerLeftClicked,
    PlayerToggledFlying,
    PlayerSetSelectedItemSlot { slot: u16 },
    PlayerUpdatedInventory { inventory: HashMap<u8, u8> },
    PlayerMovedMouse { distance: (f32, f32) },
    PlayerPressedKeys { keys: Vec<String> },

    RunCommand { command: String, args: Vec<String> },
}

impl NetworkPacket {
    fn decode(name: &str, tag: nbt::Tag) -> Result<Self, String> {
        let tag = match tag {
            nbt::Tag::Map(items) => items.into_iter().collect::<HashMap<_, _>>(),
            _ => return Err("package was not a map tag")?,
        };

        let packet = match name {
            "login" => {
                let id = match tag.get("id").ok_or("missing id field")? {
                    nbt::Tag::ByteArray(v) => {
                        let bytes: [i8; 16] = *v.as_array().ok_or("invalid id: not 16 bytes")?;
                        let bytes: [u8; 16] = unsafe { transmute(bytes) };
                        u128::from_be_bytes(bytes)
                    }
                    _ => return Err("wrong type for id field")?,
                };
                let name = match tag.get("name").ok_or("missing name field")? {
                    nbt::Tag::String(v) => v.clone(),
                    _ => return Err("wrong type for name field")?,
                };
                NetworkPacket::Login { id, name }
            }
            "logout" => NetworkPacket::Logout,
            "get_world_info" => NetworkPacket::GetWorldInfo,
            "load_column_data" => NetworkPacket::LoadColumnData {
                coords: match tag.get("coords").ok_or("missing field coords")? {
                    nbt::Tag::Long(v) => *v as u64,
                    _ => return Err("wrong type for coords field")?,
                },
            },
            "get_player_state" => NetworkPacket::GetPlayerState,
            "get_events" => NetworkPacket::GetEvents,
            "get_world_loading_events" => NetworkPacket::GetWorldLoadingEvents {
                max_chunks_to_load: match tag.get("max_chunks").ok_or("missing max_chunks")? {
                    nbt::Tag::Short(v) => *v as u16,
                    _ => return Err("wrong type for max_chunks field")?,
                },
            },
            "right_mouse_clicked" => NetworkPacket::PlayerRightClicked,
            "left_mouse_clicked" => NetworkPacket::PlayerLeftClicked,
            "toggle_flying" => NetworkPacket::PlayerToggledFlying,
            "set_selected_inventory_slot" => NetworkPacket::PlayerSetSelectedItemSlot {
                slot: match tag.get("slot").ok_or("missing field slot")? {
                    nbt::Tag::Short(v) => *v as u16,
                    _ => return Err("wrong type for slot field")?,
                },
            },
            "inventory_updated" => match tag.get("inventory").ok_or("missing field inventory")? {
                nbt::Tag::Map(vs) => {
                    let inventory = match vs
                        .iter()
                        .find(|(name, _)| name == "slots")
                        .ok_or("missing field slots")?
                    {
                        (_, nbt::Tag::List(vs)) => vs
                            .iter()
                            .map(|key| match key {
                                nbt::Tag::Map(vs) => aa(vs.as_slice()),
                                _ => Err("wrong type for slots item".into()),
                            })
                            .collect::<Result<HashMap<_, _>, _>>()?,
                        _ => return Err("wrong type for slots field")?,
                    };
                    NetworkPacket::PlayerUpdatedInventory { inventory }
                }
                _ => return Err("wrong type for inventory field")?,
            },
            "mouse_moved" => NetworkPacket::PlayerMovedMouse {
                distance: (
                    match tag.get("dx").ok_or("missing dx")? {
                        nbt::Tag::Float(v) => *v,
                        _ => return Err("wrong type for dx field")?,
                    },
                    match tag.get("dy").ok_or("missing dy")? {
                        nbt::Tag::Float(v) => *v,
                        _ => return Err("wrong type for dy field")?,
                    },
                ),
            },
            "keys_pressed" => NetworkPacket::PlayerPressedKeys {
                keys: match tag.get("keys").ok_or("missing field keys")? {
                    nbt::Tag::List(vs) => vs
                        .iter()
                        .map(|key| match key {
                            nbt::Tag::String(v) => Ok(v.clone()),
                            _ => Err("wrong type for key"),
                        })
                        .collect::<Result<Vec<_>, _>>()?,
                    _ => return Err("wrong type for keys field")?,
                },
            },
            "run_command" => match tag.get("command").ok_or("missing field command")? {
                nbt::Tag::Map(vs) => {
                    let command = match vs
                        .iter()
                        .find(|(name, _)| name == "name")
                        .ok_or("missing name field")?
                    {
                        (_, nbt::Tag::String(v)) => v.clone(),
                        _ => return Err("wrong type for name field")?,
                    };
                    let args = match vs
                        .iter()
                        .find(|(name, _)| name == "args")
                        .ok_or("missing args field")?
                    {
                        (_, nbt::Tag::List(vs)) => vs
                            .iter()
                            .map(|key| match key {
                                nbt::Tag::String(v) => Ok(v.clone()),
                                _ => Err("wrong type for arg"),
                            })
                            .collect::<Result<Vec<_>, _>>()?,
                        _ => return Err("wrong type for args field")?,
                    };
                    NetworkPacket::RunCommand { command, args }
                }
                _ => return Err("wrong type for command field")?,
            },
            _ => return Err(format!("Got unknown packet: {name}"))?,
        };

        Ok(packet)
    }
}

fn aa(vs: &[(String, nbt::Tag)]) -> Result<(u8, u8), String> {
    let slot = match vs
        .iter()
        .find(|(name, _)| name == "slot")
        .ok_or("missing field slot")?
    {
        (_, nbt::Tag::Byte(v)) => *v as u8,
        _ => return Err("wrong type for slot")?,
    };
    let id = match vs
        .iter()
        .find(|(name, _)| name == "id")
        .ok_or("missing field id")?
    {
        (_, nbt::Tag::Byte(v)) => *v as u8,
        _ => return Err("wrong type for id")?,
    };
    Ok((slot, id))
}

mod nbt {
    use std::mem::transmute;

    use bytes::BufMut;

    #[derive(Clone)]
    pub enum Tag {
        End,
        Byte(i8),
        Short(i16),
        Int(i32),
        Long(i64),
        Float(f32),
        Double(f64),
        ByteArray(Vec<i8>),
        String(String),
        List(Vec<Tag>),
        Map(Vec<(String, Tag)>),
        IntArray(Vec<i32>),
        ShortArray(Vec<i16>),
    }

    impl Tag {
        fn tag_id(&self) -> u8 {
            match self {
                Tag::End => 0,
                Tag::Byte(_) => 1,
                Tag::Short(_) => 2,
                Tag::Int(_) => 3,
                Tag::Long(_) => 4,
                Tag::Float(_) => 5,
                Tag::Double(_) => 6,
                Tag::ByteArray(_) => 7,
                Tag::String(_) => 8,
                Tag::List(_) => 9,
                Tag::Map(_) => 10,
                Tag::IntArray(_) => 11,
                Tag::ShortArray(_) => 100, // yes, it should be 100
            }
        }

        pub fn to_binary(&self) -> Vec<u8> {
            let mut stream = TagOutputStream::new();
            stream.write_tag("", self);
            stream.data
        }

        pub fn from_binary(data: &[u8]) -> Result<(String, Tag), String> {
            TagInputStream { data }.read_tag()
        }
    }

    pub fn make_vector_tag([x, y, z]: [f64; 3]) -> Tag {
        MapTag::new()
            .set("x", Tag::Double(x))
            .set("y", Tag::Double(y))
            .set("z", Tag::Double(z))
            .build()
    }

    pub struct MapTag {
        items: Vec<(String, Tag)>,
    }

    impl MapTag {
        pub fn new() -> Self {
            Self { items: Vec::new() }
        }

        pub fn set(mut self, name: &str, tag: Tag) -> Self {
            self.items.push((name.to_string(), tag));
            self
        }

        pub fn build(self) -> Tag {
            Tag::Map(self.items)
        }
    }

    struct TagOutputStream {
        data: Vec<u8>,
    }

    impl TagOutputStream {
        pub fn new() -> Self {
            Self { data: Vec::new() }
        }

        pub fn write_tag(&mut self, name: &str, tag: &Tag) {
            assert_ne!(tag.tag_id(), Tag::End.tag_id());

            self.data.put_u8(tag.tag_id());
            self.data.put_u16(name.len() as u16);
            self.data.extend_from_slice(name.as_bytes());

            self.write_payload(tag);
        }

        fn write_payload(&mut self, tag: &Tag) {
            match tag {
                Tag::End => {}
                Tag::Byte(v) => {
                    self.data.put_i8(*v);
                }
                Tag::Short(v) => {
                    self.data.put_i16(*v);
                }
                Tag::Int(v) => {
                    self.data.put_i32(*v);
                }
                Tag::Long(v) => {
                    self.data.put_i64(*v);
                }
                Tag::Float(v) => {
                    self.data.put_f32(*v);
                }
                Tag::Double(v) => {
                    self.data.put_f64(*v);
                }
                Tag::ByteArray(v) => {
                    self.data.put_u32(v.len() as u32);
                    self.data
                        .extend_from_slice(unsafe { transmute::<&[i8], &[u8]>(v.as_slice()) });
                }
                Tag::String(v) => {
                    self.data.put_u16(v.len() as u16);
                    self.data.extend_from_slice(v.as_bytes());
                }
                Tag::List(v) => {
                    let item_tag_id = if v.is_empty() {
                        Tag::End.tag_id()
                    } else {
                        v[0].tag_id()
                    };
                    self.data.put_u8(item_tag_id);
                    self.data.put_u32(v.len() as u32);
                    for item in v {
                        self.write_payload(item);
                    }
                }
                Tag::Map(v) => {
                    for (name, tag) in v {
                        self.write_tag(&name, tag);
                    }
                    self.data.put_u8(Tag::End.tag_id());
                }
                Tag::IntArray(v) => {
                    self.data.put_u32(v.len() as u32);
                    for item in v {
                        self.data.put_i32(*item);
                    }
                }
                Tag::ShortArray(v) => {
                    self.data.put_u32(v.len() as u32);
                    for item in v {
                        self.data.put_i16(*item);
                    }
                }
            }
        }
    }

    struct TagInputStream<'a> {
        data: &'a [u8],
    }

    impl<'a> TagInputStream<'a> {
        pub fn read_tag(&mut self) -> Result<(String, Tag), String> {
            let tag_id = self.read_u8()?;

            let name = if tag_id == Tag::End.tag_id() {
                "".to_string()
            } else {
                let name_len = self.read_u16()?;
                let name_bytes = self.take_n_bytes(name_len as usize)?;
                String::from_utf8_lossy(name_bytes).to_string()
            };

            let tag = self.read_payload(tag_id)?;

            Ok((name, tag))
        }

        fn read_payload(&mut self, tag_id: u8) -> Result<Tag, String> {
            let tag = match tag_id {
                0 => Tag::End,
                1 => Tag::Byte(self.read_u8()? as i8),
                2 => Tag::Short(self.read_u16()? as i16),
                3 => Tag::Int(self.read_u32()? as i32),
                4 => Tag::Long(self.read_u64()? as i64),
                5 => Tag::Float(self.read_f32()?),
                6 => Tag::Double(self.read_f64()?),
                7 => {
                    let len = self.read_u32()? as usize;
                    let bytes = self.take_n_bytes(len)?;
                    Tag::ByteArray(unsafe { transmute::<&[u8], &[i8]>(bytes).to_vec() })
                }
                8 => {
                    let len = self.read_u16()? as usize;
                    let bytes = self.take_n_bytes(len)?;
                    Tag::String(String::from_utf8_lossy(bytes).to_string())
                }
                9 => {
                    let item_tag_id = self.read_u8()?;
                    let len = self.read_u32()?;

                    if len != 0 && item_tag_id == Tag::End.tag_id() {
                        return Err("non-empty list of end tags is not allowed")?;
                    }

                    let mut items = Vec::with_capacity(len as usize);
                    for _ in 0..len {
                        items.push(self.read_payload(item_tag_id)?);
                    }
                    Tag::List(items)
                }
                10 => {
                    let mut items = Vec::new();
                    loop {
                        let (name, item) = self.read_tag()?;
                        if item.tag_id() == Tag::End.tag_id() {
                            break;
                        }
                        items.push((name, item));
                    }
                    Tag::Map(items)
                }
                11 => {
                    let len = self.read_u32()? as usize;
                    let mut items = Vec::with_capacity(len);
                    for _ in 0..len {
                        items.push(self.read_u32()? as i32);
                    }
                    Tag::IntArray(items)
                }
                100 => {
                    let len = self.read_u32()? as usize;
                    let mut items = Vec::with_capacity(len);
                    for _ in 0..len {
                        items.push(self.read_u16()? as i16);
                    }
                    Tag::ShortArray(items)
                }
                n => Err(format!("unknown tag id: {n}"))?,
            };

            Ok(tag)
        }

        fn take_n_bytes(&mut self, n: usize) -> Result<&'a [u8], String> {
            if self.data.len() < n {
                return Err("not enough bytes")?;
            }
            let (taken, rest) = self.data.split_at(n);
            self.data = rest;
            Ok(taken)
        }

        fn read_u8(&mut self) -> Result<u8, String> {
            let b = self.take_n_bytes(1)?;
            Ok(b[0])
        }

        fn read_u16(&mut self) -> Result<u16, String> {
            let b = self.take_n_bytes(2)?;
            Ok(u16::from_be_bytes(<[u8; 2]>::try_from(b).unwrap()))
        }

        fn read_u32(&mut self) -> Result<u32, String> {
            let b = self.take_n_bytes(4)?;
            Ok(u32::from_be_bytes(<[u8; 4]>::try_from(b).unwrap()))
        }

        fn read_u64(&mut self) -> Result<u64, String> {
            let b = self.take_n_bytes(8)?;
            Ok(u64::from_be_bytes(<[u8; 8]>::try_from(b).unwrap()))
        }

        fn read_f32(&mut self) -> Result<f32, String> {
            let b = self.take_n_bytes(4)?;
            Ok(f32::from_be_bytes(<[u8; 4]>::try_from(b).unwrap()))
        }

        fn read_f64(&mut self) -> Result<f64, String> {
            let b = self.take_n_bytes(8)?;
            Ok(f64::from_be_bytes(<[u8; 8]>::try_from(b).unwrap()))
        }
    }
}
