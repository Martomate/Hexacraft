use serde::Deserialize;

#[derive(Debug, PartialEq, Eq, Deserialize, Clone)]
pub struct GameVersion {
    pub id: String,
    pub name: String,
    pub release_date: String,
    pub distributions: Vec<Distribution>,
}

impl GameVersion {
    fn distribution_exact(&self, os: &str, arch: &str) -> Option<Program> {
        self.distributions
            .iter()
            .find(|d| d.platforms.iter().any(|p| p.os == os && p.arch == arch))
            .map(|p| p.program.clone())
    }

    pub fn distribution(&self, platform: &Platform) -> Option<Program> {
        let os = &platform.os;
        let arch = &platform.arch;

        None.or_else(|| self.distribution_exact(os, arch))
            .or_else(|| self.distribution_exact(os, "any"))
            .or_else(|| self.distribution_exact("any", arch))
            .or_else(|| self.distribution_exact("any", "any"))
    }
}

#[derive(Debug, PartialEq, Eq, Deserialize, Clone)]
pub struct Distribution {
    platforms: Vec<Platform>,
    program: Program,
}

#[derive(Debug, PartialEq, Eq, Deserialize, Clone)]
pub struct Platform {
    os: String,
    arch: String,
}

impl Platform {
    pub fn current() -> Self {
        let os = if cfg!(target_os = "windows") {
            "windows"
        } else if cfg!(target_os = "macos") {
            "mac"
        } else {
            "linux"
        };
        let arch = if cfg!(target_arch = "aarch64") {
            "arm64"
        } else {
            "x64"
        };
        Self {
            os: os.to_string(),
            arch: arch.to_string(),
        }
    }
}

#[derive(Debug, PartialEq, Eq, Deserialize, Clone)]
pub struct Program {
    pub archive: String,
    pub jar_file: Option<String>,
    pub java_exe: Option<String>,
    pub executable: Option<String>,
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::*;

    #[test]
    fn parse_game_version() -> serde_json::Result<()> {
        let json_body = json!({
            "id":           "1.2.3",
            "name":         "V1.2.3",
            "release_date": "2018-10-30T20:40:50+01:00",
            "url":          "http://example.com/v1.2.3.zip",
            "file_to_run":  "hexacraft.jar",
            "distributions": [],
        });

        assert_eq!(
            serde_json::from_value::<GameVersion>(json_body)?,
            GameVersion {
                id: "1.2.3".into(),
                name: "V1.2.3".into(),
                release_date: "2018-10-30T20:40:50+01:00".into(),
                distributions: vec![],
            }
        );

        Ok(())
    }

    #[test]
    fn parse_game_version_platforms() -> serde_json::Result<()> {
        assert_eq!(
            serde_json::from_value::<Platform>(json!({
                "os":   "windows",
                "arch": "any",
            }))?,
            Platform {
                os: "windows".to_string(),
                arch: "any".to_string()
            }
        );
        assert_eq!(
            serde_json::from_value::<Platform>(json!({
                "os":   "mac",
                "arch": "x64",
            }))?,
            Platform {
                os: "mac".to_string(),
                arch: "x64".to_string()
            }
        );
        assert_eq!(
            serde_json::from_value::<Platform>(json!({
                "os":   "linux",
                "arch": "x64",
            }))?,
            Platform {
                os: "linux".to_string(),
                arch: "x64".to_string()
            }
        );
        assert_eq!(
            serde_json::from_value::<Platform>(json!({
                "os":   "mac",
                "arch": "arm64",
            }))?,
            Platform {
                os: "mac".to_string(),
                arch: "arm64".to_string()
            }
        );

        Ok(())
    }

    #[test]
    fn parse_game_version_program() -> serde_json::Result<()> {
        assert_eq!(
            serde_json::from_value::<Program>(json!({
                "archive":  "http://example.com/v1.2.3.zip",
                "jar_file": "hexacraft.jar",
                "java_exe": "jre/bin/java",
            }))?,
            Program {
                archive: "http://example.com/v1.2.3.zip".to_string(),
                jar_file: Some("hexacraft.jar".to_string()),
                java_exe: Some("jre/bin/java".to_string()),
                executable: None,
            }
        );
        assert_eq!(
            serde_json::from_value::<Program>(json!({
                "archive":    "http://example.com/v1.2.3.zip",
                "executable": "bin/hexacraft",
            }))?,
            Program {
                archive: "http://example.com/v1.2.3.zip".to_string(),
                jar_file: None,
                java_exe: None,
                executable: Some("bin/hexacraft".to_string()),
            }
        );

        Ok(())
    }
}
