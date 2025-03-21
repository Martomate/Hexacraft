#![allow(clippy::type_complexity)]

use bevy::{
    prelude::*,
    tasks::{block_on, IoTaskPool, Task},
    window::RequestRedraw,
    winit::WinitSettings,
};
use futures_lite::future;
use serde::Deserialize;

fn main() {
    App::new()
        .add_plugins(DefaultPlugins.set(WindowPlugin {
            primary_window: Some(Window {
                title: "Game".to_string(),
                resolution: (720.0, 540.0).into(),
                resizable: false,
                ..Default::default()
            }),
            ..Default::default()
        }))
        .add_event::<Action>()
        .insert_resource(MainState::default())
        // Only run the app when there is user input. This will significantly reduce CPU/GPU use.
        .insert_resource(WinitSettings::desktop_app())
        .add_systems(Startup, setup)
        .add_systems(
            Update,
            (
                handle_http_tasks,
                button_system,
                perform_actions,
                update_available_versions_list,
            ),
        )
        .run();
}

#[derive(Debug, PartialEq, Eq, Deserialize)]
struct GameVersion {
    id: String,
    name: String,
    release_date: String,
    url: String,
    file_to_run: String,
}

#[derive(Resource, Default)]
struct MainState {
    available_versions: Option<Vec<GameVersion>>,
    selected_version: Option<String>,
}

#[derive(Component, Event, Clone)]
enum Action {
    Play,
    SelectVersion(String),
    ShowVersionSelector,
}

#[derive(Component)]
struct VersionSelector;

#[derive(Component)]
struct AvailableVersionsList;

#[derive(Component)]
struct VersionButton;

enum HttpTaskResponse {
    FetchGameVersions(Vec<GameVersion>),
}

#[derive(Component)]
struct HttpTask(Task<anyhow::Result<HttpTaskResponse>>);

fn handle_http_tasks(
    mut commands: Commands,
    mut http_tasks: Query<(Entity, &mut HttpTask)>,
    mut main_state: ResMut<MainState>,
) {
    for (entity, mut task) in &mut http_tasks {
        if let Some(res) = block_on(future::poll_once(&mut task.0)) {
            match res {
                Ok(res) => match res {
                    HttpTaskResponse::FetchGameVersions(versions) => {
                        main_state.available_versions = Some(versions)
                    }
                },
                Err(err) => println!("Failed to make http request: {}", err),
            }
            commands.entity(entity).despawn();
        }
    }
}

fn setup(mut commands: Commands, asset_server: Res<AssetServer>, state: Res<MainState>) {
    let task_pool = IoTaskPool::get();
    let entity = commands.spawn_empty().id();
    let task: Task<anyhow::Result<_>> = task_pool.spawn(async {
        let url = "https://martomate.com/api/hexacraft/versions";

        let response = ureq::get(url).call()?;
        let body = response.into_string()?;

        let versions = serde_json::from_str::<Vec<GameVersion>>(&body)?;
        Ok(HttpTaskResponse::FetchGameVersions(versions))
    });
    commands.entity(entity).insert(HttpTask(task));

    commands.spawn(Camera2dBundle::default());

    commands.spawn(SpriteBundle {
        texture: asset_server.load("background.png"),
        ..Default::default()
    });

    commands
        .spawn(NodeBundle {
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
        .with_children(|parent| {
            parent
                .spawn(NodeBundle { ..default() })
                .with_children(|parent| {
                    let (outline_bundles, main_bundle) = make_outlined_text(
                        "Hexacraft",
                        TextStyle {
                            font: asset_server.load("Verdana.ttf"),
                            font_size: 72.0,
                            color: Color::WHITE,
                        },
                        Color::rgba(0.1, 0.1, 0.1, 1.0),
                    );

                    for outline_bundle in outline_bundles {
                        parent.spawn(outline_bundle);
                    }
                    parent.spawn(main_bundle);
                });

            parent
                .spawn(ButtonBundle {
                    style: Style {
                        width: Val::Px(128.0),
                        height: Val::Px(64.0),
                        border: UiRect::all(Val::Px(2.0)),
                        // TODO: add border_radius when this is released: https://github.com/bevyengine/bevy/pull/8973
                        justify_content: JustifyContent::Center,
                        align_items: AlignItems::Center,
                        margin: UiRect::bottom(Val::Percent(20.0)),
                        ..default()
                    },
                    border_color: Color::rgba(1.0, 1.0, 1.0, 0.5).into(),
                    background_color: Color::rgba(0.15, 0.15, 0.15, 0.4).into(),
                    ..default()
                })
                .insert(Action::Play)
                .with_children(|parent| {
                    parent.spawn(TextBundle::from_section(
                        "Play",
                        TextStyle {
                            font: asset_server.load("Verdana.ttf"),
                            font_size: 32.0,
                            color: Color::rgb(0.9, 0.9, 0.9),
                        },
                    ));
                });

            parent
                .spawn(NodeBundle {
                    style: Style {
                        flex_direction: FlexDirection::Row,
                        justify_content: JustifyContent::FlexStart,
                        align_items: AlignItems::Center,
                        width: Val::Percent(100.0),
                        height: Val::Px(64.0),
                        padding: UiRect::all(Val::Px(4.0)),
                        ..default()
                    },
                    ..default()
                })
                .with_children(|parent| {
                    parent
                        .spawn(NodeBundle {
                            style: Style {
                                flex_direction: FlexDirection::Column,
                                ..default()
                            },
                            background_color: Color::rgb(0.10, 0.10, 0.10).into(),
                            visibility: Visibility::Hidden,
                            ..default()
                        })
                        .insert(VersionSelector)
                        .with_children(|parent| {
                            parent
                                .spawn(NodeBundle {
                                    style: Style {
                                        position_type: PositionType::Absolute,
                                        left: Val::Px(5.0),
                                        bottom: Val::Px(10.0),
                                        flex_direction: FlexDirection::Column,
                                        align_items: AlignItems::Stretch,
                                        ..default()
                                    },
                                    background_color: Color::rgba(0.15, 0.15, 0.15, 0.8).into(),
                                    ..default()
                                })
                                .insert(AvailableVersionsList)
                                .with_children(|_| {}); // make sure there is a Children component
                        });

                    parent
                        .spawn(ButtonBundle {
                            style: Style {
                                height: Val::Px(24.0),
                                justify_content: JustifyContent::FlexStart,
                                padding: UiRect::all(Val::Px(4.0)),
                                ..default()
                            },
                            background_color: Color::rgba(0.15, 0.15, 0.15, 0.0).into(),
                            ..default()
                        })
                        .insert(VersionButton)
                        .insert(Action::ShowVersionSelector)
                        .with_children(|parent| {
                            parent.spawn(TextBundle::from_section(
                                match state.selected_version {
                                    Some(ref v) => format!("Version: {}", *v),
                                    None => "Latest version".to_string(),
                                },
                                TextStyle {
                                    font: asset_server.load("Verdana.ttf"),
                                    font_size: 20.0,
                                    ..default()
                                },
                            ));
                        });
                });
        });
}

fn update_available_versions_list(
    mut commands: Commands,
    asset_server: Res<AssetServer>,
    q_list: Query<(Entity, &Children), With<AvailableVersionsList>>,
    state: Res<MainState>,
) {
    if !state.is_changed() {
        return;
    }

    if let Some(ref versions) = state.available_versions {
        let mut items = Vec::new();

        for v in versions.iter() {
            let item = commands
                .spawn(ButtonBundle {
                    style: Style {
                        height: Val::Px(24.0),
                        width: Val::Px(100.0),
                        justify_content: JustifyContent::FlexStart,
                        margin: UiRect::axes(Val::Px(8.0), Val::Px(2.0)),
                        ..default()
                    },
                    background_color: Color::rgba(0.15, 0.15, 0.15, 0.0).into(),
                    ..default()
                })
                .insert(Action::SelectVersion(v.name.clone()))
                .with_children(|parent| {
                    parent.spawn(TextBundle::from_section(
                        v.name.clone(),
                        TextStyle {
                            font: asset_server.load("Verdana.ttf"),
                            font_size: 20.0,
                            ..default()
                        },
                    ));
                })
                .id();

            items.push(item);
        }

        let (list, old_children) = q_list.single();
        commands.entity(list).replace_children(&items);
        for ch in old_children {
            commands.entity(*ch).despawn_recursive();
        }
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

fn perform_actions(
    mut action_event_reader: EventReader<Action>,
    mut q_version_selector: Query<&mut Visibility, With<VersionSelector>>,
    q_version_button: Query<&Children, With<VersionButton>>,
    mut q_text: Query<&mut Text>,
    mut state: ResMut<MainState>,
) {
    for action in &mut action_event_reader.read() {
        match action {
            Action::Play => println!("Play pressed!"),
            Action::SelectVersion(version) => {
                state.selected_version = Some(version.clone());

                let version_button_children = q_version_button.single();
                let mut version_button_text = q_text.get_mut(version_button_children[0]).unwrap();
                version_button_text.sections[0].value = format!("Version: {}", *version);

                let mut version_selector = q_version_selector.single_mut();
                *version_selector = Visibility::Hidden;
            }
            Action::ShowVersionSelector => {
                let mut version_selector = q_version_selector.single_mut();
                *version_selector = Visibility::Visible;
            }
        };
    }
}

fn make_outlined_text(
    text: &str,
    style: TextStyle,
    outline_color: Color,
) -> (Vec<TextBundle>, TextBundle) {
    let outline_text = Text::from_section(
        text,
        TextStyle {
            color: outline_color,
            ..style.clone()
        },
    );

    let mut outline_bundles = Vec::new();

    for dy in -2..=2 {
        for dx in -2..=2 {
            if (dx != 0 || dy != 0) && dx * dx + dy * dy <= 5 {
                outline_bundles.push(TextBundle {
                    text: outline_text.clone(),
                    style: Style {
                        position_type: PositionType::Absolute,
                        left: Val::Px(dx as f32),
                        bottom: Val::Px(dy as f32),
                        ..default()
                    },
                    ..default()
                });
            }
        }
    }

    let main_bundle = TextBundle::from_section(text, style);

    (outline_bundles, main_bundle)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_game_version() -> serde_json::Result<()> {
        let json_str = r#"{
            "id": "0.1",
            "name": "0.1",
            "release_date": "2015-06-20T00:37:30+02:00",
            "url": "https://a.com/b/c.zip",
            "file_to_run": "Game.jar"
        }"#;

        assert_eq!(
            serde_json::from_str::<GameVersion>(json_str)?,
            GameVersion {
                id: "0.1".into(),
                name: "0.1".into(),
                release_date: "2015-06-20T00:37:30+02:00".into(),
                url: "https://a.com/b/c.zip".into(),
                file_to_run: "Game.jar".into(),
            }
        );

        Ok(())
    }
}
