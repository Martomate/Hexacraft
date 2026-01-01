use crate::core;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum RegistryError {
    #[error("invalid json: {0}")]
    InvalidJson(#[from] facet_json::DeserializeError<facet_json::JsonError>),
    #[error("parsing failed: {0}")]
    Parsing(String),
}

pub fn parse_json_str(data: &str) -> Result<Vec<core::GameVersion>, RegistryError> {
    let versions: Vec<dto::GameVersion> = facet_json::from_str(data)?;
    let mut res: Vec<core::GameVersion> = Vec::with_capacity(versions.len());
    for v in versions {
        res.push(v.into_core()?);
    }
    Ok(res)
}

mod dto {
    use crate::core;
    use crate::registry::RegistryError;
    use facet::Facet;

    #[derive(Debug, PartialEq, Eq, Clone, Facet)]
    pub struct GameVersion {
        pub id: String,
        pub name: String,
        pub release_date: String,
        pub distributions: Vec<Distribution>,
    }

    impl GameVersion {
        pub fn into_core(self) -> Result<core::GameVersion, RegistryError> {
            let mut dists = Vec::with_capacity(self.distributions.len());
            for d in self.distributions {
                for p in d.platforms {
                    dists.push(core::Distribution {
                        platform: p.into_core(),
                        archive: d.program.archive(),
                        exe: d.program.exe()?,
                    });
                }
            }

            Ok(core::GameVersion {
                id: self.id,
                name: self.name,
                release_date: self.release_date,
                distributions: dists,
            })
        }
    }

    #[derive(Debug, PartialEq, Eq, Clone, Facet)]
    pub struct Distribution {
        pub platforms: Vec<Platform>,
        pub program: Program,
    }

    #[derive(Debug, PartialEq, Eq, Clone, Facet)]
    pub struct Platform {
        pub os: String,
        pub arch: String,
    }

    impl Platform {
        pub fn into_core(self) -> core::Platform {
            core::Platform {
                os: match self.os.as_ref() {
                    "mac" => Some(core::OS::Macos),
                    "linux" => Some(core::OS::Linux),
                    "windows" => Some(core::OS::Windows),
                    "any" => None,
                    _ => None,
                },
                arch: match self.arch.as_ref() {
                    "arm64" => Some(core::Arch::Arm64),
                    "x64" => Some(core::Arch::X64),
                    "any" => None,
                    _ => None,
                },
            }
        }
    }

    #[derive(Debug, PartialEq, Eq, Clone, Facet)]
    pub struct Program {
        pub archive: String,
        pub jar_file: Option<String>,
        pub java_exe: Option<String>,
        pub executable: Option<String>,
    }

    impl Program {
        pub fn archive(&self) -> String {
            self.archive.clone()
        }

        pub fn exe(&self) -> Result<core::ProgramExe, RegistryError> {
            if let Some(ref executable) = self.executable {
                Ok(core::ProgramExe::Native {
                    executable: executable.clone(),
                })
            } else if let Some(ref jar_file) = self.jar_file {
                Ok(core::ProgramExe::Jvm {
                    jar_file: jar_file.clone(),
                    java_exe: self.java_exe.clone(),
                })
            } else {
                Err(RegistryError::Parsing(
                    "must have either program.jar_file or program.executable".to_string(),
                ))
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::dto::*;
    use crate::core;
    use facet_value::value;

    #[test]
    fn parse_game_version() {
        let json_body = value!({
            "id":           "1.2.3",
            "name":         "V1.2.3",
            "release_date": "2018-10-30T20:40:50+01:00",
            "url":          "http://example.com/v1.2.3.zip", // this is ignored
            "file_to_run":  "hexacraft.jar", // this is ignored
            "distributions": [],
        });

        assert_eq!(
            facet_value::from_value::<GameVersion>(json_body)
                .unwrap()
                .into_core()
                .unwrap(),
            core::GameVersion::new("1.2.3", "V1.2.3", "2018-10-30T20:40:50+01:00")
        );
    }

    #[test]
    fn parse_game_version_platforms() {
        fn try_parse(value: facet_value::Value) -> core::Platform {
            let value: Platform = facet_value::from_value(value).unwrap();
            value.into_core()
        }

        assert_eq!(
            try_parse(value!({
                "os":   "windows",
                "arch": "any",
            })),
            core::Platform::any().with_os(core::OS::Windows)
        );
        assert_eq!(
            try_parse(value!({
                "os":   "mac",
                "arch": "any",
            })),
            core::Platform::any().with_os(core::OS::Macos)
        );
        assert_eq!(
            try_parse(value!({
                "os":   "linux",
                "arch": "any",
            })),
            core::Platform::any().with_os(core::OS::Linux)
        );
        assert_eq!(
            try_parse(value!({
                "os":   "any",
                "arch": "x64",
            })),
            core::Platform::any().with_arch(core::Arch::X64)
        );
        assert_eq!(
            try_parse(value!({
                "os":   "any",
                "arch": "arm64",
            })),
            core::Platform::any().with_arch(core::Arch::Arm64)
        );
    }

    #[test]
    fn parse_game_version_program() {
        fn try_parse(value: facet_value::Value) -> (String, core::ProgramExe) {
            let p: Program = facet_value::from_value(value).unwrap();
            (p.archive(), p.exe().unwrap())
        }

        assert_eq!(
            try_parse(value!({
                "archive":  "http://example.com/v1.2.3.zip",
                "jar_file": "hexacraft.jar",
                "java_exe": "jre/bin/java",
            })),
            (
                "http://example.com/v1.2.3.zip".to_string(),
                core::ProgramExe::embedded_jvm("hexacraft.jar", "jre/bin/java",)
            )
        );
        assert_eq!(
            try_parse(value!({
                "archive":    "http://example.com/v1.2.3.zip",
                "executable": "bin/hexacraft",
            })),
            (
                "http://example.com/v1.2.3.zip".to_string(),
                core::ProgramExe::native("bin/hexacraft"),
            )
        );
    }
}
