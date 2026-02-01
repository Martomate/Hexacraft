use bevy::ecs::{
    event::{Event, EventReader},
    system::{Res, ResMut},
};

use crate::{core::GameVersion, ui::{MainState, UiHandler, UiHandlerRes, start_game}};

#[derive(Event)]
pub enum Message {
    GotVersions(anyhow::Result<Vec<GameVersion>>),
    GotArchive(GameVersion, anyhow::Result<Vec<u8>>),
}

pub fn handle_messages<H: UiHandler>(
    mut messages: EventReader<Message>,
    mut main_state: ResMut<MainState>,
    handler: Res<UiHandlerRes<H>>,
) {
    for message in messages.read() {
        match message {
            Message::GotVersions(res) => match res {
                Ok(versions) => main_state.available_versions = Some(versions.clone()),
                Err(err) => {
                    println!("Failed to fetch list of versions: {err}")
                }
            },
            Message::GotArchive(game_version, res) => match res {
                Ok(data) => {
                    println!("Version {} downloaded", game_version.name);
                    handler.init_from_archive(game_version, data.clone());

                    main_state.is_downloading = false;

                    start_game(&handler.0, game_version);
                }
                Err(err) => {
                    println!("Failed to download archive: {err}")
                }
            },
        }
    }
}
