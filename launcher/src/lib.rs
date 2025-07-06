pub mod ui;

use std::path::{Path, PathBuf};

use platform_dirs::AppDirs;
use serde::Deserialize;

#[derive(Debug, PartialEq, Eq, Deserialize, Clone)]
pub struct GameVersion {
    pub id: String,
    pub name: String,
    pub release_date: String,
    pub url: String,
    pub file_to_run: String,
}

impl GameVersion {
    pub fn download(&self) -> Result<ZipFile, anyhow::Error> {
        let response = ureq::get(&self.url).call()?;

        let mut buffer = Vec::new();
        response.into_reader().read_to_end(&mut buffer)?;

        Ok(ZipFile(buffer))
    }
}

pub struct ZipFile(Vec<u8>);

impl ZipFile {
    fn extract_into_dir(self, dir: &Path) -> Result<(), zip_extract::ZipExtractError> {
        zip_extract::extract(std::io::Cursor::new(self.0), dir, false)
    }
}

pub struct GameDirectory {
    game_dir: PathBuf,
    game_version: GameVersion,
}

impl GameDirectory {
    pub fn new(game_version: GameVersion) -> Self {
        let dirs = AppDirs::new(Some("Hexacraft"), false).unwrap();
        let game_dir = dirs.cache_dir.join("versions").join(&game_version.name);
        Self { game_dir, game_version }
    }

    pub fn is_downloaded(&self) -> bool {
        self.game_dir.exists()
    }

    pub fn init_from_archive(&self, data: ZipFile) {
        if self.game_dir.exists() {
            std::fs::remove_dir_all(&self.game_dir).unwrap();
        }
        std::fs::create_dir_all(&self.game_dir).unwrap();
        data.extract_into_dir(&self.game_dir).unwrap();
    }

    pub fn start_game(&self) -> std::process::Child {
        let file_to_run = self.game_dir.join(&self.game_version.file_to_run);

        let mut p = std::process::Command::new("java");
        if cfg!(target_os = "macos") {
            p.arg("-XstartOnFirstThread");
        }
        p.arg("-jar").arg(file_to_run);

        p.spawn().unwrap()
    }
}

pub fn fetch_versions_list() -> Result<Vec<GameVersion>, anyhow::Error> {
    let url = "https://martomate.com/api/hexacraft/versions";

    let response = ureq::get(url).call()?;
    let body = response.into_string()?;

    Ok(serde_json::from_str::<Vec<GameVersion>>(&body)?)
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
