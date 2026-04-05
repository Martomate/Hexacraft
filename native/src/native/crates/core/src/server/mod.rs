use std::{collections::HashMap, mem::transmute, sync::Arc};

use crate::zmq::ServerSocket;

pub struct GameServer {
    is_online: bool,
    socket: Arc<ServerSocket>,
    path: String,
}

impl GameServer {
    pub async fn start(is_online: bool, port: u16, path: String) -> Self {
        let socket = ServerSocket::new();
        socket.bind(port).await.unwrap();
        Self {
            is_online,
            socket: Arc::new(socket),
            path: path.to_string(),
        }
    }

    pub async fn run_receiver(self: Arc<Self>) {
        loop {
            let client_id = self.socket.receive().await.unwrap();
            let message = self.socket.receive().await.unwrap();

            match nbt::Tag::from_binary(&message).and_then(|(_, tag)| match tag {
                nbt::Tag::Map(vs) => {
                    if vs.len() != 1 {
                        return Err("packet did not have exactly 1 field")?;
                    }
                    let (name, tag) = &vs[0];
                    NetworkPacket::decode(name, tag.clone())
                }
                _ => Err("packet was not a Map tag")?,
            }) {
                Err(err) => eprintln!("Got invalid message: {err}"),
                Ok(packet) => {
                    let response: Option<nbt::Tag> = match packet {
                        NetworkPacket::Login { id, name } => {
                            println!("User {name} is trying to login with id {id}");

                            Some(
                                nbt::MapTag::new()
                                    .set("success", nbt::Tag::Byte(0))
                                    .set(
                                        "error",
                                        nbt::Tag::String("server is shutting down".to_string()),
                                    )
                                    .build(),
                            )
                        }
                        NetworkPacket::Logout => {
                            todo!()
                        }
                        NetworkPacket::GetWorldInfo => {
                            todo!()
                        }
                        NetworkPacket::LoadColumnData { coords } => {
                            todo!()
                        }
                        NetworkPacket::LoadWorldData => {
                            todo!()
                        }
                        NetworkPacket::GetPlayerState => {
                            todo!()
                        }
                        NetworkPacket::GetEvents => {
                            todo!()
                        }
                        NetworkPacket::GetWorldLoadingEvents { max_chunks_to_load } => {
                            todo!()
                        }
                        NetworkPacket::PlayerRightClicked => {
                            todo!()
                        }
                        NetworkPacket::PlayerLeftClicked => {
                            todo!()
                        }
                        NetworkPacket::PlayerToggledFlying => {
                            todo!()
                        }
                        NetworkPacket::PlayerSetSelectedItemSlot { slot } => {
                            todo!()
                        }
                        NetworkPacket::PlayerUpdatedInventory { inventory } => {
                            todo!()
                        }
                        NetworkPacket::PlayerMovedMouse { distance } => {
                            todo!()
                        }
                        NetworkPacket::PlayerPressedKeys { keys } => {
                            todo!()
                        }
                        NetworkPacket::RunCommand { command, args } => {
                            todo!()
                        }
                    };

                    if let Some(data) = response {
                        self.socket.send(client_id, data.to_binary()).await.unwrap();
                    }
                }
            }
        }
    }
}

enum NetworkPacket {
    Login { id: u128, name: String },
    Logout,

    GetWorldInfo,
    LoadColumnData { coords: u64 },
    LoadWorldData,

    GetPlayerState,
    GetEvents,
    GetWorldLoadingEvents { max_chunks_to_load: u32 },

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
            "logout" => {
                todo!()
            }
            "get_world_info" => {
                todo!()
            }
            "load_column_data" => {
                todo!()
            }
            "load_world_data" => {
                todo!()
            }
            "get_player_state" => {
                todo!()
            }
            "get_events" => {
                todo!()
            }
            "get_world_loading_events" => {
                todo!()
            }
            "right_mouse_clicked" => {
                todo!()
            }
            "left_mouse_clicked" => {
                todo!()
            }
            "toggle_flying" => {
                todo!()
            }
            "set_selected_inventory_slot" => {
                todo!()
            }
            "inventory_updated" => {
                todo!()
            }
            "mouse_moved" => {
                todo!()
            }
            "keys_pressed" => {
                todo!()
            }
            "run_command" => {
                todo!()
            }
            _ => return Err(format!("Got unknown packet: {name}"))?,
        };

        Ok(packet)
    }
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
