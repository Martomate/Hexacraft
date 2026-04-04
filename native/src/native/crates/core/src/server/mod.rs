use std::sync::Arc;

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
        let client_id = self.socket.receive().await.unwrap();
        let _message = self.socket.receive().await.unwrap();

        let data = nbt::MapTag::new()
            .set("success", nbt::Tag::Byte(0))
            .set(
                "error",
                nbt::Tag::String("server is shutting down".to_string()),
            )
            .build()
            .to_binary();

        self.socket.send(client_id, data).await.unwrap();
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
            let mut stream = TagStream::new();
            stream.write("", self);
            stream.data
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

    struct TagStream {
        data: Vec<u8>,
    }

    impl TagStream {
        pub fn new() -> Self {
            Self { data: Vec::new() }
        }

        pub fn write(&mut self, name: &str, tag: &Tag) {
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
                        self.write(&name, tag);
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
}
