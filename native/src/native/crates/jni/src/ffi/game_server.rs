use std::sync::Arc;
use std::time::Duration;

use crate::handle::Handle;
use crate::run_with_timeout;

use hexacraft::server::{GameServer, GameState};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint};
use jni_fn::jni_fn;

#[jni_fn("hexacraft.rs.RustLib$GameServer")]
pub fn start<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    is_online: jboolean,
    port: jint,
    path: JString<'local>,
) -> Handle<Arc<GameServer<GameState>>> {
    let is_online = is_online == 1;

    assert!((0..1 << 16).contains(&port), "port is out of range");
    let port = port as u16;

    let path = env.get_string(&path).expect("failed to read string");
    let path = path.to_str().expect("invalid utf8").to_string();

    let server = run_with_timeout(Duration::from_millis(1000), async move {
        let server = Arc::new(GameServer::start(port, GameState::create(is_online, path)).await);
        tokio::spawn({
            let server = server.clone();
            async move { server.run_receiver().await }
        });
        server
    })
    .unwrap();

    Handle::create(server)
}

#[jni_fn("hexacraft.rs.RustLib$GameServer")]
pub fn stop<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<Arc<GameServer<GameState>>>,
) {
    handle.use_handle(|server| {
        let shutdown_task = {
            let server = server.clone();
            async move { server.shutdown().await }
        };
        if run_with_timeout(Duration::from_millis(1000), shutdown_task).is_none() {
            eprintln!("Server shutdown timed out. The server will now be forced to shut down.");
        }
    });
    handle.destroy(); // this stops the server
}
