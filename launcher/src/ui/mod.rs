#![allow(clippy::type_complexity)]

mod elements;

use crate::core::GameVersion;
use bevy::{
    asset::embedded_asset,
    prelude::*,
    tasks::{AsyncComputeTaskPool, Task, block_on, poll_once},
    window::RequestRedraw,
    winit::WinitSettings,
};
use std::{
    ops::Deref,
    sync::{Arc, Mutex, mpsc},
};

pub trait UiHandler: Send + Sync + 'static {
    fn fetch_versions_list(
        &self,
    ) -> impl Future<Output = Result<Vec<GameVersion>, anyhow::Error>> + Send + 'static;

    fn is_downloaded(&self, game_version: &GameVersion) -> bool;

    fn download(
        &self,
        game_version: &GameVersion,
        report_progress: std::sync::mpsc::Sender<(usize, usize)>,
    ) -> impl Future<Output = Result<Vec<u8>, anyhow::Error>> + Send + 'static;

    fn init_from_archive(&self, game_version: &GameVersion, compressed_data: Vec<u8>);

    fn start_game(
        &self,
        game_version: &GameVersion,
    ) -> Result<std::process::Command, anyhow::Error>;
}

macro_rules! embed_all_assets {
    ($app: ident) => {
        embedded_asset!($app, "assets/background.png");
        embedded_asset!($app, "assets/Verdana.ttf");
    };
}

macro_rules! asset_path {
    ($asset: literal) => {
        concat!("embedded://launcher/ui/assets/", $asset)
    };
}

struct EmbeddedAssetPlugin;

impl Plugin for EmbeddedAssetPlugin {
    fn build(&self, app: &mut App) {
        embed_all_assets!(app);
    }
}

#[derive(Resource)]
struct UiHandlerRes<H: UiHandler>(H);

impl<H: UiHandler> Deref for UiHandlerRes<H> {
    type Target = H;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

pub fn run<H: UiHandler>(handler: H) {
    App::new()
        .add_plugins(DefaultPlugins.set(WindowPlugin {
            primary_window: Some(Window {
                title: "Hexacraft".to_string(),
                resolution: (720.0, 540.0).into(),
                resizable: false,
                ..Default::default()
            }),
            ..Default::default()
        }))
        .add_plugins(EmbeddedAssetPlugin)
        .add_event::<Action>()
        .insert_resource(MainState::default())
        .insert_resource(UiHandlerRes(handler))
        // Only run the app when there is user input. This will significantly reduce CPU/GPU use.
        .insert_resource(WinitSettings::desktop_app())
        .add_systems(Startup, setup::<H>)
        .add_systems(
            Update,
            (
                handle_http_tasks::<H>,
                button_system,
                perform_actions::<H>,
                update_available_versions_list,
                update_download_progress,
            ),
        )
        .run();
}

#[derive(Resource, Default)]
struct MainState {
    available_versions: Option<Vec<GameVersion>>,
    selected_version: Option<String>,
    is_downloading: bool,
}

#[derive(Event, Clone)]
enum Action {
    Play,
    SelectVersion(GameVersion),
    ShowVersionSelector,
    HideVersionSelector,
}

#[derive(Component)]
struct VersionSelector;

#[derive(Component)]
struct AvailableVersionsList;

#[derive(Component)]
struct VersionButton;

#[derive(Component)]
struct PlayButton;

#[derive(Component)]
struct DownloadProgress(Arc<Mutex<f32>>);

enum HttpTaskResponse {
    FetchGameVersions(Vec<GameVersion>),
    FetchGame(GameVersion, Vec<u8>),
}

#[derive(Component)]
struct HttpTask(Task<anyhow::Result<HttpTaskResponse>>);

fn handle_http_tasks<H: UiHandler>(
    mut commands: Commands,
    mut http_tasks: Query<(Entity, &mut HttpTask)>,
    mut main_state: ResMut<MainState>,
    handler: Res<UiHandlerRes<H>>,
) {
    for (entity, mut task) in &mut http_tasks {
        if !task.0.is_finished() {
            continue;
        }
        if let Some(res) = block_on(poll_once(&mut task.0)) {
            match res {
                Ok(HttpTaskResponse::FetchGameVersions(versions)) => {
                    main_state.available_versions = Some(versions)
                }
                Ok(HttpTaskResponse::FetchGame(game_version, data)) => {
                    println!("Version {} downloaded", game_version.name);
                    handler.init_from_archive(&game_version, data);

                    main_state.is_downloading = false;

                    start_game(&handler.0, &game_version);
                }
                Err(err) => println!("Failed to make http request: {err}"),
            }
            commands.entity(entity).despawn();
        }
    }
}

fn setup<H: UiHandler>(
    mut commands: Commands,
    asset_server: Res<AssetServer>,
    state: Res<MainState>,
    handler: Res<UiHandlerRes<H>>,
) {
    let task_pool = AsyncComputeTaskPool::get();
    let entity = commands.spawn_empty().id();

    let res = handler.fetch_versions_list();

    let task =
        HttpTask(task_pool.spawn(async { Ok(HttpTaskResponse::FetchGameVersions(res.await?)) }));
    commands.entity(entity).insert(task);

    commands.spawn(Camera2dBundle::default());

    commands.spawn(SpriteBundle {
        texture: asset_server.load(asset_path!("background.png")),
        ..Default::default()
    });

    commands
        .spawn(ButtonBundle {
            style: Style {
                width: Val::Percent(100.0),
                height: Val::Percent(100.0),
                justify_content: JustifyContent::SpaceBetween,
                align_items: AlignItems::Center,
                flex_direction: FlexDirection::Column,
                ..default()
            },
            ..default()
        })
        .insert(Action::HideVersionSelector)
        .with_children(|parent| {
            let verdana_font = asset_server.load(asset_path!("Verdana.ttf"));

            elements::make_title_text(parent.spawn_empty(), verdana_font.clone());
            elements::make_play_button(parent.spawn_empty(), verdana_font.clone());
            elements::make_version_selector(
                parent.spawn_empty(),
                verdana_font.clone(),
                state.selected_version.clone(),
            );
        });
}

fn update_available_versions_list(
    mut commands: Commands,
    asset_server: Res<AssetServer>,
    q_list: Query<(Entity, Option<&Children>), With<AvailableVersionsList>>,
    state: Res<MainState>,
) {
    if !state.is_changed() {
        return;
    }

    let verdana_font = asset_server.load(asset_path!("Verdana.ttf"));

    if let Some(ref versions) = state.available_versions {
        let mut items = Vec::new();

        for v in versions.iter() {
            let font = verdana_font.clone();

            let entity = commands.spawn_empty();
            items.push(entity.id());

            elements::make_version_item(entity, font, v);
        }

        if let Ok((list, old_children)) = q_list.get_single() {
            commands.entity(list).replace_children(&items);
            if let Some(old_children) = old_children {
                for ch in old_children {
                    commands.entity(*ch).despawn_recursive();
                }
            }
        }
    }
}

fn update_download_progress(
    mut redraw_request_events: EventWriter<RequestRedraw>,
    q_play_button: Query<&Children, With<PlayButton>>,
    mut q_text: Query<&mut Text>,
    q_progress: Query<&DownloadProgress>,
) {
    if let Ok(progress) = q_progress.get_single() {
        let progress_percent = (*progress.0.lock().unwrap() * 100.0).floor();

        let play_button_children = q_play_button.single();
        let mut play_button_text = q_text.get_mut(play_button_children[0]).unwrap();
        play_button_text.sections[0].value = format!("{progress_percent} %");

        redraw_request_events.send(RequestRedraw);
    }
}

fn button_system(
    mut interaction_query: Query<(&Interaction, &Action), (Changed<Interaction>, With<Button>)>,
    mut redraw_event_writer: EventWriter<RequestRedraw>,
    mut action_event_writer: EventWriter<Action>,
) {
    for (interaction, action) in &mut interaction_query {
        redraw_event_writer.send(RequestRedraw); // Fix for: https://github.com/bevyengine/bevy/issues/11235

        if *interaction == Interaction::Pressed {
            action_event_writer.send(action.clone());
        }
    }
}

fn start_game<H: UiHandler>(handler: &H, game_version: &GameVersion) -> ! {
    let mut cmd = handler.start_game(game_version).unwrap();
    let _ = cmd.spawn().unwrap();
    std::process::exit(0);
}

fn perform_actions<H: UiHandler>(
    mut commands: Commands,
    mut action_event_reader: EventReader<Action>,
    mut q_version_selector: Query<&mut Visibility, With<VersionSelector>>,
    q_version_button: Query<&Children, With<VersionButton>>,
    mut q_text: Query<&mut Text>,
    mut state: ResMut<MainState>,
    handler: Res<UiHandlerRes<H>>,
) {
    for action in &mut action_event_reader.read() {
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
                            let game_version = game_version.clone();

                            let task_pool = AsyncComputeTaskPool::get();
                            let entity = commands.spawn_empty().id();

                            state.is_downloading = true;

                            let download_progress = Arc::new(Mutex::new(0.0));
                            let (progress_tx, progress_rx) = mpsc::channel();

                            task_pool
                                .spawn({
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
                                })
                                .detach();

                            let task = HttpTask(task_pool.spawn({
                                let download_task = handler.download(&game_version, progress_tx);
                                async move {
                                    let buffer = download_task.await?;
                                    Ok(HttpTaskResponse::FetchGame(game_version, buffer))
                                }
                            }));
                            commands
                                .entity(entity)
                                .insert(task)
                                .insert(DownloadProgress(download_progress));
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
                version_button_text.sections[0].value = format!("Version: {}", version.name);

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
