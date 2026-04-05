use std::sync::Arc;
use std::time::Duration;

use crate::handle::Handle;
use crate::run_with_timeout;

use hexacraft::server::GameServer;
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
) -> Handle<Arc<GameServer>> {
    let is_online = is_online == 1;

    assert!((0..1 << 16).contains(&port), "port is out of range");
    let port = port as u16;

    let path = env.get_string(&path).expect("failed to read string");
    let path = path.to_str().expect("invalid utf8").to_string();

    let server = run_with_timeout(Duration::from_millis(1000), async move {
        let server = Arc::new(hexacraft::server::GameServer::start(is_online, port, path).await);
        tokio::spawn(server.clone().run_receiver());
        server
    })
    .unwrap();

    Handle::create(server)
}

#[jni_fn("hexacraft.rs.RustLib$GameServer")]
pub fn stop<'local>(_env: JNIEnv<'local>, _class: JClass<'local>, handle: Handle<Arc<GameServer>>) {
    handle.use_handle(|server| {
        let _ = run_with_timeout(Duration::from_millis(1000), server.clone().shutdown());
    });
    handle.destroy(); // this stops the server
}
