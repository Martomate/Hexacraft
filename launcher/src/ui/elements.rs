use crate::{
    core::GameVersion,
    ui::{Action, AvailableVersionsList, PlayButton, VersionButton, VersionSelector},
};
use bevy::{ecs::system::EntityCommands, prelude::*};

pub fn make_title_text(mut entity: EntityCommands, font: Handle<Font>) {
    entity.insert(Node { ..default() }).with_children(|parent| {
        spawn_outlined_text(
            parent,
            "Hexacraft",
            TextFont {
                font,
                font_size: 72.0,
                ..default()
            },
            Color::WHITE,
            Color::srgba(0.1, 0.1, 0.1, 1.0),
        );
    });
}

pub fn make_play_button(mut entity: EntityCommands, font: Handle<Font>) {
    entity
        .insert((
            Button,
            Node {
                width: Val::Px(128.0),
                height: Val::Px(64.0),
                border: UiRect::all(Val::Px(2.0)),
                justify_content: JustifyContent::Center,
                align_items: AlignItems::Center,
                margin: UiRect::bottom(Val::Percent(20.0)),
                ..default()
            },
            BorderRadius::all(Val::Px(10.0)),
            BorderColor(Color::srgba(1.0, 1.0, 1.0, 0.5)),
            BackgroundColor(Color::srgba(0.15, 0.15, 0.15, 0.4)),
        ))
        .insert(PlayButton)
        .insert(Action::Play)
        .with_children(|parent| {
            parent.spawn((
                Text::new("Play"),
                TextFont {
                    font,
                    font_size: 32.0,
                    ..default()
                },
                TextColor(Color::srgb(0.9, 0.9, 0.9)),
            ));
        });
}

pub fn make_version_selector(
    mut entity: EntityCommands,
    font: Handle<Font>,
    selected_version: Option<String>,
) {
    entity
        .insert(Node {
            flex_direction: FlexDirection::Column,
            justify_content: JustifyContent::FlexEnd,
            align_items: AlignItems::Start,
            //row_gap: Val::Px(4.0),
            width: Val::Percent(100.0),
            height: Val::Px(64.0),
            padding: UiRect::all(Val::Px(4.0)),
            ..default()
        })
        .with_children(|parent| {
            parent
                .spawn((
                    Node {
                        flex_direction: FlexDirection::Column,
                        align_items: AlignItems::Stretch,
                        margin: UiRect::all(Val::Px(8.0)),
                        border: UiRect::all(Val::Px(2.0)),
                        ..default()
                    },
                    BackgroundColor(Color::srgba(0.15, 0.15, 0.15, 0.7)),
                    BorderColor(Color::srgba(0.0, 0.0, 0.0, 0.7)),
                    Visibility::Hidden,
                ))
                .insert(VersionSelector)
                .insert(AvailableVersionsList);

            parent
                .spawn((
                    Button,
                    Node {
                        justify_content: JustifyContent::FlexStart,
                        margin: UiRect::all(Val::Px(8.0)),
                        ..default()
                    },
                    BackgroundColor(Color::srgba(0.15, 0.15, 0.15, 0.0)),
                ))
                .insert(VersionButton)
                .insert(Action::ShowVersionSelector)
                .with_children(|parent| {
                    parent.spawn((
                        Text::new(match selected_version {
                            Some(v) => format!("Version: {v}"),
                            None => "Version: Latest".to_string(),
                        }),
                        TextFont {
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
        .insert((
            Button,
            Node {
                height: Val::Px(24.0),
                width: Val::Px(120.0),
                justify_content: JustifyContent::FlexStart,
                margin: UiRect::axes(Val::Px(8.0), Val::Px(2.0)),
                ..default()
            },
        ))
        .insert(Action::SelectVersion(v.clone()))
        .with_children(|parent| {
            parent.spawn((
                Text::new(v.name.clone()),
                TextFont {
                    font,
                    font_size: 20.0,
                    ..default()
                },
            ));
        });
}

fn spawn_outlined_text(
    parent: &mut ChildBuilder<'_>,
    text: &str,
    font: TextFont,
    color: Color,
    outline_color: Color,
) {
    let outline_text = (Text::new(text), font.clone(), TextColor(outline_color));

    for dy in -2..=2 {
        for dx in -2..=2 {
            if (dx != 0 || dy != 0) && dx * dx + dy * dy <= 5 {
                parent.spawn((
                    outline_text.clone(),
                    Node {
                        position_type: PositionType::Absolute,
                        left: Val::Px(dx as f32),
                        bottom: Val::Px(dy as f32),
                        ..default()
                    },
                ));
            }
        }
    }

    parent.spawn((Text::new(text), font, TextColor(color)));
}
