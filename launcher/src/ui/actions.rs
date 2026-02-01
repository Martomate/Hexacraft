use crate::{
    UiHandler,
    core::GameVersion,
    ui::{
        DownloadProgress, HttpTask, MainState, Message, UiHandlerRes, VersionButton,
        VersionSelector, start_game,
    },
};
use bevy::{prelude::*, tasks::AsyncComputeTaskPool};
use std::sync::{Arc, Mutex, mpsc};

#[derive(Event, Clone)]
pub enum Action {
    Play,
    SelectVersion(GameVersion),
    ShowVersionSelector,
    HideVersionSelector,
}

pub fn perform_actions<H: UiHandler>(
    mut commands: Commands,
    mut action_event_reader: EventReader<Action>,
    mut q_version_selector: Query<&mut Visibility, With<VersionSelector>>,
    q_version_button: Query<&Children, With<VersionButton>>,
    mut q_text: Query<&mut Text>,
    mut state: ResMut<MainState>,
    handler: Res<UiHandlerRes<H>>,
) {
    for action in action_event_reader.read() {
        match action {
            Action::Play => {
                if state.is_downloading {
                } else if let Some(ref versions) = state.available_versions {
                    let game_version = match state.selected_version {
                        Some(ref s) => versions.iter().find(|&v| v.name == *s),
                        None => versions.last(),
                    };
                    if let Some(game_version) = game_version {
                        if handler.is_downloaded(game_version) {
                            start_game(&handler.0, game_version);
                        } else {
                            start_download(&mut commands, &handler.0, game_version.clone());
                            state.is_downloading = true;
                        }
                    }
                } else {
                    println!(
                        "Version list is not available yet. Maybe you don't have an internet connection?"
                    )
                }
            }
            Action::SelectVersion(version) => {
                state.selected_version = Some(version.name.clone());

                let version_button_children = q_version_button.single();
                let mut version_button_text = q_text.get_mut(version_button_children[0]).unwrap();
                version_button_text.0 = format!("Version: {}", version.name);

                let mut version_selector = q_version_selector.single_mut();
                *version_selector = Visibility::Hidden;
            }
            Action::ShowVersionSelector => {
                let mut version_selector = q_version_selector.single_mut();
                *version_selector = Visibility::Visible;
            }
            Action::HideVersionSelector => {
                let mut version_selector = q_version_selector.single_mut();
                *version_selector = Visibility::Hidden;
            }
        };
    }
}

fn start_download<H: UiHandler>(commands: &mut Commands, handler: &H, game_version: GameVersion) {
    let task_pool = AsyncComputeTaskPool::get();

    let (progress_tx, progress_rx) = mpsc::channel();

    let download_task = task_pool.spawn({
        let download_task = handler.download(&game_version, progress_tx);
        async move { Message::GotArchive(game_version, download_task.await) }
    });

    let download_progress = Arc::new(Mutex::new(0.0));

    let progress_task = task_pool.spawn({
        let download_progress = download_progress.clone();
        async move {
            while let Ok((bytes, total)) = progress_rx.recv() {
                let progress = if total != 0 {
                    bytes as f32 / total as f32
                } else {
                    0.0
                };
                *download_progress.lock().unwrap() = progress;
            }
        }
    });
    progress_task.detach();

    commands
        .spawn(HttpTask(download_task))
        .insert(DownloadProgress(download_progress));
}
