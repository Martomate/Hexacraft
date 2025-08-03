use crate::core::{Arch, GameVersion, OS};
use crate::ui::UiHandler;
use crate::{cmd::create_game_command, registry};
use anyhow::bail;
use platform_dirs::AppDirs;
use std::{future::Future, path::PathBuf};

pub struct MainUiHandler {
    pub api_url: String,
    pub versions_dir: PathBuf,
    pub os: OS,
    pub arch: Arch,
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
            os: OS::current(),
            arch: Arch::current(),
        }
    }

    fn game_dir(&self, game_version: &GameVersion) -> PathBuf {
        self.versions_dir.join(&game_version.name)
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

            Ok(registry::parse_json_str(&body)?)
        }
    }

    fn is_downloaded(&self, game_version: &GameVersion) -> bool {
        self.game_dir(game_version).exists()
    }

    fn download(
        &self,
        game_version: &GameVersion,
        report_progress: std::sync::mpsc::Sender<(usize, usize)>,
    ) -> impl Future<Output = Result<Vec<u8>, anyhow::Error>> + Send + 'static {
        let request = ureq::get(
            &game_version
                .distribution(self.os, self.arch)
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

            Ok(buffer)
        }
    }

    fn init_from_archive(&self, game_version: &GameVersion, compressed_data: Vec<u8>) {
        let game_dir = self.game_dir(game_version);

        if game_dir.exists() {
            std::fs::remove_dir_all(&game_dir).unwrap();
        }
        std::fs::create_dir_all(&game_dir).unwrap();
        zip_extract::extract(std::io::Cursor::new(compressed_data), &game_dir, false).unwrap();
    }

    fn start_game(
        &self,
        game_version: &GameVersion,
    ) -> Result<std::process::Command, anyhow::Error> {
        let Some(dist) = game_version.distribution(self.os, self.arch) else {
            bail!(
                "No distribution is available for this platform ({:?}, {:?})",
                self.os,
                self.arch
            );
        };
        create_game_command(self.os, dist.exe, &self.game_dir(game_version))
    }
}
