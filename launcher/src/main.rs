#![windows_subsystem = "windows"]

fn main() {
    launcher::ui::run(launcher::MainUiHandler::from_env());
}
