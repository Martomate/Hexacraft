use std::mem::transmute;

    use bytes::BufMut;

    #[derive(Debug, Clone)]
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

        pub fn set_opt(mut self, name: &str, tag: Option<Tag>) -> Self {
            if let Some(tag) = tag {
                self.items.push((name.to_string(), tag));
            }
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
                        self.write_tag(name, tag);
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