#![windows_subsystem = "windows"]

fn main() {
    launcher::run(launcher::MainUiHandler::from_env());
}
