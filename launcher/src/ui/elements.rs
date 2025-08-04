use crate::{
    core::GameVersion,
    ui::{Action, AvailableVersionsList, PlayButton, VersionButton, VersionSelector},
};
use bevy::{ecs::system::EntityCommands, prelude::*};

pub fn make_title_text(mut entity: EntityCommands, font: Handle<Font>) {
    entity
        .insert(NodeBundle { ..default() })
        .with_children(|parent| {
            let (outline_bundles, main_bundle) = make_outlined_text(
                "Hexacraft",
                TextStyle {
                    font,
                    font_size: 72.0,
                    color: Color::WHITE,
                },
                Color::srgba(0.1, 0.1, 0.1, 1.0),
            );

            for outline_bundle in outline_bundles {
                parent.spawn(outline_bundle);
            }
            parent.spawn(main_bundle);
        });
}

pub fn make_play_button(mut entity: EntityCommands, font: Handle<Font>) {
    entity
        .insert(ButtonBundle {
            style: Style {
                width: Val::Px(128.0),
                height: Val::Px(64.0),
                border: UiRect::all(Val::Px(2.0)),
                justify_content: JustifyContent::Center,
                align_items: AlignItems::Center,
                margin: UiRect::bottom(Val::Percent(20.0)),
                ..default()
            },
            border_radius: BorderRadius::all(Val::Px(10.0)),
            border_color: Color::srgba(1.0, 1.0, 1.0, 0.5).into(),
            background_color: Color::srgba(0.15, 0.15, 0.15, 0.4).into(),
            ..default()
        })
        .insert(PlayButton)
        .insert(Action::Play)
        .with_children(|parent| {
            parent.spawn(TextBundle::from_section(
                "Play",
                TextStyle {
                    font,
                    font_size: 32.0,
                    color: Color::srgb(0.9, 0.9, 0.9),
                },
            ));
        });
}

pub fn make_version_selector(
    mut entity: EntityCommands,
    font: Handle<Font>,
    selected_version: Option<String>,
) {
    entity
        .insert(NodeBundle {
            style: Style {
                flex_direction: FlexDirection::Column,
                justify_content: JustifyContent::FlexEnd,
                align_items: AlignItems::Start,
                //row_gap: Val::Px(4.0),
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
                        align_items: AlignItems::Stretch,
                        margin: UiRect::all(Val::Px(8.0)),
                        border: UiRect::all(Val::Px(2.0)),
                        ..default()
                    },
                    background_color: Color::srgba(0.15, 0.15, 0.15, 0.7).into(),
                    border_color: BorderColor(Color::srgba(0.0, 0.0, 0.0, 0.7)),
                    visibility: Visibility::Hidden,
                    ..default()
                })
                .insert(VersionSelector)
                .insert(AvailableVersionsList);

            parent
                .spawn(ButtonBundle {
                    style: Style {
                        justify_content: JustifyContent::FlexStart,
                        margin: UiRect::all(Val::Px(8.0)),
                        ..default()
                    },
                    background_color: Color::srgba(0.15, 0.15, 0.15, 0.0).into(),
                    ..default()
                })
                .insert(VersionButton)
                .insert(Action::ShowVersionSelector)
                .with_children(|parent| {
                    parent.spawn(TextBundle::from_section(
                        match selected_version {
                            Some(v) => format!("Version: {v}"),
                            None => "Version: Latest".to_string(),
                        },
                        TextStyle {
                            font,
                            font_size: 20.0,
                            ..default()
                        },
                    ));
                });
        });
}

pub fn make_version_item(mut entity: EntityCommands, font: Handle<Font>, v: &GameVersion) {
    entity
        .insert(ButtonBundle {
            style: Style {
                height: Val::Px(24.0),
                width: Val::Px(120.0),
                justify_content: JustifyContent::FlexStart,
                margin: UiRect::axes(Val::Px(8.0), Val::Px(2.0)),
                ..default()
            },
            ..default()
        })
        .insert(Action::SelectVersion(v.clone()))
        .with_children(|parent| {
            parent.spawn(TextBundle::from_section(
                v.name.clone(),
                TextStyle {
                    font,
                    font_size: 20.0,
                    ..default()
                },
            ));
        });
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
