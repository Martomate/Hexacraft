pub mod net;
pub mod ui;

use std::{
    future::Future,
    path::{Path, PathBuf},
};

use bevy::ecs::system::Resource;
use platform_dirs::AppDirs;

use crate::net::{GameVersion, Platform};

pub trait UiHandler: Resource {
    fn fetch_versions_list(
        &self,
    ) -> impl Future<Output = Result<Vec<GameVersion>, anyhow::Error>> + Send + 'static;

    fn is_downloaded(&self, game_version: &GameVersion) -> bool;

    fn download(
        &self,
        game_version: &GameVersion,
        report_progress: std::sync::mpsc::Sender<(usize, usize)>,
    ) -> impl Future<Output = Result<ZipFile, anyhow::Error>> + Send + 'static;

    fn init_from_archive(&self, game_version: &GameVersion, data: ZipFile);

    fn start_game(&self, game_version: &GameVersion) -> std::process::Child;
}

#[derive(Resource)]
pub struct MainUiHandler {
    pub api_url: String,
    pub versions_dir: PathBuf,
}

impl MainUiHandler {
    pub fn from_env() -> Self {
        Self {
            api_url: std::env::var("API_URL")
                .unwrap_or_else(|_| "https://martomate.com".to_string()),
            versions_dir: AppDirs::new(Some("Hexacraft"), false)
                .unwrap()
                .cache_dir
                .join("versions"),
        }
    }
}

impl UiHandler for MainUiHandler {
    fn fetch_versions_list(
        &self,
    ) -> impl Future<Output = Result<Vec<GameVersion>, anyhow::Error>> + Send + 'static {
        let request = ureq::get(&format!("{}/api/hexacraft/versions", &self.api_url));

        let versions_cache = self.versions_dir.join("versions.json");

        async {
            let body = request
                .call()
                .and_then(|res| Ok(res.into_string()?))
                .or_else(|err| {
                    if versions_cache.exists() {
                        Ok(std::fs::read_to_string(versions_cache.clone())?)
                    } else {
                        Err(err)
                    }
                })?;

            if !versions_cache.exists() {
                std::fs::create_dir_all(versions_cache.parent().unwrap())?;
                std::fs::File::create(versions_cache.clone())?;
            }
            std::fs::write(versions_cache, body.clone())?;

            Ok(serde_json::from_str::<Vec<GameVersion>>(&body)?)
        }
    }

    fn is_downloaded(&self, game_version: &GameVersion) -> bool {
        self.game_dir(game_version).exists()
    }

    fn download(
        &self,
        game_version: &GameVersion,
        report_progress: std::sync::mpsc::Sender<(usize, usize)>,
    ) -> impl Future<Output = Result<ZipFile, anyhow::Error>> + Send + 'static {
        let request = ureq::get(
            &game_version
                .distribution(&Platform::current())
                .unwrap()
                .archive,
        );

        async move {
            println!("Fetching {}", request.url());

            let response = request.call()?;
            let total = response
                .header("Content-Length")
                .and_then(|s| s.parse::<usize>().ok())
                .unwrap_or_default();

            let mut reader = response.into_reader();

            let mut buffer = Vec::new();
            let mut buf = [0; 1024];

            loop {
                let bytes_read = reader.read(&mut buf)?;
                if bytes_read == 0 {
                    break;
                }
                buffer.extend_from_slice(&buf[..bytes_read]);
                let _ = report_progress.send((buffer.len(), total));
            }

            Ok(ZipFile(buffer))
        }
    }

    fn init_from_archive(&self, game_version: &GameVersion, archive: ZipFile) {
        let game_dir = self.game_dir(game_version);

        if game_dir.exists() {
            std::fs::remove_dir_all(&game_dir).unwrap();
        }
        std::fs::create_dir_all(&game_dir).unwrap();
        archive.extract_into_dir(&game_dir).unwrap();
    }

    fn start_game(&self, game_version: &GameVersion) -> std::process::Child {
        let platform = Platform::current();

        let game_dir = self.game_dir(game_version);

        let Some(dist) = game_version.distribution(&platform) else {
            panic!("No distribution is available for this platform ({platform:?})");
        };

        let mut p = if let Some(executable) = dist.executable {
            let exe_cmd = PathBuf::from(executable);
            assert!(exe_cmd.is_relative());
            let exe_cmd = game_dir.join(exe_cmd);

            println!("Running native command: {exe_cmd:?}");

            std::process::Command::new(exe_cmd)
        } else {
            let java_cmd = if let Some(java_exe) =
                dist.java_exe.map(PathBuf::from).filter(|p| p.is_relative())
            {
                game_dir.join(java_exe)
            } else if cfg!(windows) {
                PathBuf::from("javaw")
            } else {
                PathBuf::from("java")
            };

            let Some(jar_file) = dist.jar_file.map(PathBuf::from).filter(|p| p.is_relative())
            else {
                panic!(
                    "Could not run jar file, either because it was not given or because it was not a relative path."
                );
            };
            let file_to_run = game_dir.join(jar_file);

            println!("Running Java command: {java_cmd:?}\n\tJar file: {file_to_run:?}");

            let mut p = std::process::Command::new(java_cmd);
            if cfg!(target_os = "macos") {
                p.arg("-XstartOnFirstThread");
            }
            p.arg("-jar").arg(file_to_run);
            p
        };

        p.spawn().unwrap()
    }
}

impl MainUiHandler {
    fn game_dir(&self, game_version: &GameVersion) -> PathBuf {
        self.versions_dir.join(&game_version.name)
    }
}

pub struct ZipFile(Vec<u8>);

impl ZipFile {
    pub fn extract_into_dir(self, dir: &Path) -> Result<(), zip_extract::ZipExtractError> {
        zip_extract::extract(std::io::Cursor::new(self.0), dir, false)
    }
}
