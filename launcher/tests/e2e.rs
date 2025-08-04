use std::{env::temp_dir, time::UNIX_EPOCH};

use launcher::core::{Arch, OS};
use launcher::UiHandler;

fn make_temp_dir() -> std::path::PathBuf {
    temp_dir().join(format!(
        "{}",
        std::time::SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_millis()
    ))
}

// TODO: find good ways of testing each part separately so we can get rid of this test.
#[test]
#[ignore]
fn test_e2e() {
    bevy::tasks::block_on(async {
        let handler = launcher::MainUiHandler {
            api_url: "https://martomate.com".to_string(),
            versions_dir: make_temp_dir(),
            os: OS::current(),
            arch: Arch::current(),
        };

        let versions = handler.fetch_versions_list().await.unwrap();

        let version = versions.last().unwrap();
        let (report_progress, _) = std::sync::mpsc::channel();
        let zip_file = handler.download(version, report_progress).await.unwrap();

        assert!(!handler.is_downloaded(version));
        handler.init_from_archive(version, zip_file);
        assert!(handler.is_downloaded(version));

        let mut p = handler.start_game(version).unwrap().spawn().unwrap();
        assert_eq!(p.try_wait().unwrap(), None);
        p.kill().unwrap();
    });
}

#[test]
#[ignore]
fn test_works_when_registry_is_down_after_initial_run() {
    bevy::tasks::block_on(async {
        let shared_versions_dir = make_temp_dir();

        let handler = launcher::MainUiHandler {
            api_url: "https://martomate.com".to_string(),
            versions_dir: shared_versions_dir.clone(),
            os: OS::current(),
            arch: Arch::current(),
        };

        let versions = handler.fetch_versions_list().await.unwrap();

        let version = versions.last().unwrap();
        let (report_progress, _) = std::sync::mpsc::channel();
        let zip_file = handler.download(version, report_progress).await.unwrap();

        assert!(!handler.is_downloaded(version));
        handler.init_from_archive(version, zip_file);
        assert!(handler.is_downloaded(version));

        let mut p = handler.start_game(version).unwrap().spawn().unwrap();
        assert_eq!(p.try_wait().unwrap(), None);
        p.kill().unwrap();

        // Run the application again and expect the list of versions to be retained

        let handler = launcher::MainUiHandler {
            // simulate the registry being down by using a non-existent server
            api_url: "http://localhost:47358".to_string(),
            versions_dir: shared_versions_dir.clone(),
            os: OS::current(),
            arch: Arch::current(),
        };

        let versions = handler.fetch_versions_list().await.unwrap();

        let version = versions.last().unwrap();

        // this version should already be downloaded
        assert!(handler.is_downloaded(version));

        let mut p = handler.start_game(version).unwrap().spawn().unwrap();
        assert_eq!(p.try_wait().unwrap(), None);
        p.kill().unwrap();

        // Try to download another version (which should work because GitHub is still reachable)

        let next_last_version = versions.iter().rev().nth(1).unwrap();

        assert!(!handler.is_downloaded(next_last_version));
        let (report_progress, _) = std::sync::mpsc::channel();
        assert!(
            handler
                .download(next_last_version, report_progress)
                .await
                .is_ok()
        );
        assert!(!handler.is_downloaded(next_last_version));
    });
}
