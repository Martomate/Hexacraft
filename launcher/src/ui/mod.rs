#![allow(clippy::type_complexity)]

mod actions;
mod elements;
mod messages;

use crate::{
    core::GameVersion,
    ui::{actions::Action, messages::Message},
};
use bevy::{
    asset::embedded_asset,
    prelude::*,
    tasks::{AsyncComputeTaskPool, Task, block_on, poll_once},
    window::RequestRedraw,
    winit::WinitSettings,
};
use std::{
    ops::Deref,
    sync::{Arc, Mutex},
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
        .add_event::<Message>()
        .insert_resource(MainState::default())
        .insert_resource(UiHandlerRes(handler))
        // Only run the app when there is user input. This will significantly reduce CPU/GPU use.
        .insert_resource(WinitSettings::desktop_app())
        .add_systems(Startup, setup::<H>)
        .add_systems(
            Update,
            (
                handle_http_tasks,
                messages::handle_messages::<H>,
                button_system,
                actions::perform_actions::<H>,
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

#[derive(Component)]
struct HttpTask(Task<Message>);

fn handle_http_tasks(
    mut commands: Commands,
    mut http_tasks: Query<(Entity, &mut HttpTask)>,
    mut messages: EventWriter<Message>,
) {
    for (entity, mut task) in &mut http_tasks {
        if !task.0.is_finished() {
            continue;
        }
        if let Some(msg) = block_on(poll_once(&mut task.0)) {
            messages.send(msg);
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

    commands.spawn(HttpTask(task_pool.spawn({
        let res = handler.fetch_versions_list();
        async { Message::GotVersions(res.await) }
    })));

    commands.spawn(Camera2d);

    commands.spawn(Sprite {
        image: asset_server.load(asset_path!("background.png")),
        ..Default::default()
    });

    commands
        .spawn((
            Button,
            Node {
                width: Val::Percent(100.0),
                height: Val::Percent(100.0),
                justify_content: JustifyContent::SpaceBetween,
                align_items: AlignItems::Center,
                flex_direction: FlexDirection::Column,
                ..default()
            },
        ))
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
        play_button_text.0 = format!("{progress_percent} %");

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
