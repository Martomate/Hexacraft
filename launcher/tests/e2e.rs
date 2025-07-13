use std::{env::temp_dir, time::UNIX_EPOCH};

use launcher::UiHandler;

// TODO: find good ways of testing each part separately so we can get rid of this test.
#[test]
#[ignore]
fn test_e2e() {
    bevy::tasks::block_on(async {
        let handler = launcher::MainUiHandler {
            api_url: "http://martomate.com".to_string(),
            versions_dir: temp_dir().join(format!(
                "{}",
                std::time::SystemTime::now()
                    .duration_since(UNIX_EPOCH)
                    .unwrap()
                    .as_millis()
            )),
        };

        let versions = handler.fetch_versions_list().await.unwrap();

        let version = versions.last().unwrap();
        let (report_progress, _) = std::sync::mpsc::channel();
        let zip_file = handler.download(version, report_progress).await.unwrap();

        assert!(!handler.is_downloaded(version));
        handler.init_from_archive(version, zip_file);
        assert!(handler.is_downloaded(version));

        let mut p = handler.start_game(version);
        assert_eq!(p.try_wait().unwrap(), None);
        p.kill().unwrap();
    });
}
