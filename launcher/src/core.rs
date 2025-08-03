#[derive(Debug, PartialEq, Eq, Clone)]
pub struct GameVersion {
    pub id: String,
    pub name: String,
    pub release_date: String,
    pub distributions: Vec<Distribution>,
}

impl GameVersion {
    pub fn new(
        id: impl Into<String>,
        name: impl Into<String>,
        release_date: impl Into<String>,
    ) -> Self {
        Self {
            id: id.into(),
            name: name.into(),
            release_date: release_date.into(),
            distributions: Vec::new(),
        }
    }

    pub fn distribution(&self, os: OS, arch: Arch) -> Option<Distribution> {
        for (os, arch) in [
            (Some(os), Some(arch)),
            (Some(os), None),
            (None, Some(arch)),
            (None, None),
        ] {
            for d in self.distributions.iter() {
                if d.platform.os == os && d.platform.arch == arch {
                    return Some(d.clone());
                }
            }
        }
        None
    }
}

#[derive(Debug, PartialEq, Eq, Clone)]
pub struct Distribution {
    pub platform: Platform,
    pub archive: String,
    pub exe: ProgramExe,
}

#[derive(Debug, PartialEq, Eq, Clone)]
pub struct Platform {
    pub os: Option<OS>,
    pub arch: Option<Arch>,
}

impl Platform {
    pub fn any() -> Self {
        Platform {
            os: None,
            arch: None,
        }
    }

    pub fn with_os(self, os: OS) -> Self {
        Self {
            os: Some(os),
            ..self
        }
    }

    pub fn with_arch(self, arch: Arch) -> Self {
        Self {
            arch: Some(arch),
            ..self
        }
    }
}

#[derive(Debug, PartialEq, Eq, Clone, Copy)]
pub enum Arch {
    X64,
    Arm64,
}

impl Arch {
    pub fn current() -> Self {
        if cfg!(target_arch = "aarch64") {
            Arch::Arm64
        } else {
            Arch::X64
        }
    }
}

#[derive(Debug, PartialEq, Eq, Clone, Copy)]
pub enum OS {
    Windows,
    Linux,
    Macos,
}

impl OS {
    pub fn current() -> Self {
        if cfg!(target_os = "windows") {
            OS::Windows
        } else if cfg!(target_os = "macos") {
            OS::Macos
        } else {
            OS::Linux
        }
    }
}

#[derive(Debug, PartialEq, Eq, Clone)]
pub enum ProgramExe {
    Jvm {
        jar_file: String,
        java_exe: Option<String>,
    },
    Native {
        executable: String,
    },
}

impl ProgramExe {
    pub fn provided_jvm(jar_file: impl Into<String>) -> Self {
        Self::Jvm {
            jar_file: jar_file.into(),
            java_exe: None,
        }
    }

    pub fn embedded_jvm(jar_file: impl Into<String>, java_exe: impl Into<String>) -> Self {
        Self::Jvm {
            jar_file: jar_file.into(),
            java_exe: Some(java_exe.into()),
        }
    }

    pub fn native(executable: impl Into<String>) -> Self {
        Self::Native {
            executable: executable.into(),
        }
    }
}
