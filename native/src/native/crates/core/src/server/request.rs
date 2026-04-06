use std::{collections::HashMap, mem::transmute};

use crate::server::nbt;


pub enum NetworkPacket {
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
    pub fn decode(name: &str, tag: nbt::Tag) -> Result<Self, String> {
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
                                nbt::Tag::Map(vs) => decode_inventory_slot(vs.as_slice()),
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

fn decode_inventory_slot(vs: &[(String, nbt::Tag)]) -> Result<(u8, u8), String> {
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
