use crate::core::{OS, ProgramExe};
use anyhow::bail;
use std::path::{Path, PathBuf};

pub fn create_game_command(
    os: OS,
    program: ProgramExe,
    game_dir: &Path,
) -> Result<std::process::Command, anyhow::Error> {
    match program {
        crate::core::ProgramExe::Native { executable } => {
            let exe_cmd = PathBuf::from(executable);
            assert!(exe_cmd.is_relative());
            let exe_cmd = game_dir.join(exe_cmd);

            println!("Running native command: {exe_cmd:?}");

            let mut p = std::process::Command::new(exe_cmd);
            p.current_dir(game_dir);
            Ok(p)
        }
        crate::core::ProgramExe::Jvm { jar_file, java_exe } => {
            let java_cmd =
                if let Some(java_exe) = java_exe.map(PathBuf::from).filter(|p| p.is_relative()) {
                    game_dir.join(java_exe)
                } else if os == OS::Windows {
                    PathBuf::from("javaw")
                } else {
                    PathBuf::from("java")
                };

            let Some(jar_file) = Some(PathBuf::from(jar_file)).filter(|p| p.is_relative()) else {
                bail!("Could not run jar file because it was not a relative path");
            };
            let file_to_run = game_dir.join(jar_file);

            println!("Running Java command: {java_cmd:?}\n\tJar file: {file_to_run:?}");

            let mut p = std::process::Command::new(java_cmd);
            p.current_dir(game_dir);
            if os == OS::Macos {
                p.arg("-XstartOnFirstThread");
            }
            p.arg("-jar").arg(file_to_run);
            Ok(p)
        }
    }
}

#[cfg(test)]
mod tests {
    use super::{OS, create_game_command};
    use crate::core::ProgramExe;
    use std::env::temp_dir;

    #[test]
    fn windows_provided_jvm() {
        let dir = temp_dir();

        let program = ProgramExe::provided_jvm("a.jar");
        let cmd = create_game_command(OS::Windows, program, &dir).unwrap();

        assert_eq!(cmd.get_current_dir(), Some(dir.as_path()));
        assert_eq!(cmd.get_program(), "javaw");
        assert_eq!(
            cmd.get_args().collect::<Vec<_>>(),
            vec!["-jar", &dir.join("a.jar").display().to_string()]
        );
    }

    #[test]
    fn linux_provided_jvm() {
        let dir = temp_dir();

        let program = ProgramExe::provided_jvm("a.jar");
        let cmd = create_game_command(OS::Linux, program, &dir).unwrap();

        assert_eq!(cmd.get_current_dir(), Some(dir.as_path()));
        assert_eq!(cmd.get_program(), "java");
        assert_eq!(
            cmd.get_args().collect::<Vec<_>>(),
            vec!["-jar", &dir.join("a.jar").display().to_string()]
        );
    }

    #[test]
    fn macos_provided_jvm() {
        let dir = temp_dir();

        let program = ProgramExe::provided_jvm("a.jar");
        let cmd = create_game_command(OS::Macos, program, &dir).unwrap();

        assert_eq!(cmd.get_current_dir(), Some(dir.as_path()));
        assert_eq!(cmd.get_program(), "java");
        assert_eq!(
            cmd.get_args().collect::<Vec<_>>(),
            vec![
                "-XstartOnFirstThread",
                "-jar",
                &dir.join("a.jar").display().to_string()
            ]
        );
    }

    #[test]
    fn windows_embedded_jvm() {
        let dir = temp_dir();

        let program = ProgramExe::embedded_jvm("a.jar", "jre/bin/java.exe");
        let cmd = create_game_command(OS::Windows, program, &dir).unwrap();

        assert_eq!(cmd.get_current_dir(), Some(dir.as_path()));
        assert_eq!(cmd.get_program(), dir.join("jre/bin/java.exe"));
        assert_eq!(
            cmd.get_args().collect::<Vec<_>>(),
            vec!["-jar", &dir.join("a.jar").display().to_string()]
        );
    }

    #[test]
    fn linux_embedded_jvm() {
        let dir = temp_dir();

        let program = ProgramExe::embedded_jvm("a.jar", "jre/bin/java");
        let cmd = create_game_command(OS::Linux, program, &dir).unwrap();

        assert_eq!(cmd.get_current_dir(), Some(dir.as_path()));
        assert_eq!(cmd.get_program(), dir.join("jre/bin/java"));
        assert_eq!(
            cmd.get_args().collect::<Vec<_>>(),
            vec!["-jar", &dir.join("a.jar").display().to_string()]
        );
    }

    #[test]
    fn macos_embedded_jvm() {
        let dir = temp_dir();

        let program = ProgramExe::embedded_jvm("a.jar", "jre/bin/java");
        let cmd = create_game_command(OS::Macos, program, &dir).unwrap();

        assert_eq!(cmd.get_current_dir(), Some(dir.as_path()));
        assert_eq!(cmd.get_program(), dir.join("jre/bin/java"));
        assert_eq!(
            cmd.get_args().collect::<Vec<_>>(),
            vec![
                "-XstartOnFirstThread",
                "-jar",
                &dir.join("a.jar").display().to_string()
            ]
        );
    }

    #[test]
    fn windows_native() {
        let dir = temp_dir();

        let program = ProgramExe::native("bin/a.exe");
        let cmd = create_game_command(OS::Windows, program, &dir).unwrap();

        assert_eq!(cmd.get_current_dir(), Some(dir.as_path()));
        assert_eq!(cmd.get_program(), dir.join("bin/a.exe"));
        assert_eq!(cmd.get_args().collect::<Vec<_>>(), vec![] as Vec<&str>);
    }

    #[test]
    fn linux_native() {
        let dir = temp_dir();

        let program = ProgramExe::native("bin/a");
        let cmd = create_game_command(OS::Linux, program, &dir).unwrap();

        assert_eq!(cmd.get_current_dir(), Some(dir.as_path()));
        assert_eq!(cmd.get_program(), dir.join("bin/a"));
        assert_eq!(cmd.get_args().collect::<Vec<_>>(), vec![] as Vec<&str>);
    }

    #[test]
    fn macos_native() {
        let dir = temp_dir();

        let program = ProgramExe::native("bin/a");
        let cmd = create_game_command(OS::Macos, program, &dir).unwrap();

        assert_eq!(cmd.get_current_dir(), Some(dir.as_path()));
        assert_eq!(cmd.get_program(), dir.join("bin/a"));
        assert_eq!(cmd.get_args().collect::<Vec<_>>(), vec![] as Vec<&str>);
    }
}
