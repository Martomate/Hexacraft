use std::sync::Arc;
use std::time::Duration;

use crate::handle::Handle;
use crate::{run_with_timeout, throw_rte};

use jni::JNIEnv;
use jni::objects::{AsJArrayRaw, JByteArray, JClass, JObject, JString};
use jni::sys::{jbyteArray, jint};
use jni_fn::jni_fn;

#[jni_fn("hexacraft.rs.RustLib$ClientSocket")]
pub fn create<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    client_id: JByteArray<'local>,
) -> Handle<Arc<hexacraft::zmq::ClientSocket>> {
    let client_id = env
        .convert_byte_array(client_id)
        .expect("failed to convert byte array");

    Handle::create(Arc::new(hexacraft::zmq::ClientSocket::new(client_id)))
}

#[jni_fn("hexacraft.rs.RustLib$ClientSocket")]
pub fn connect<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<Arc<hexacraft::zmq::ClientSocket>>,
    host: JString<'local>,
    port: jint,
) {
    assert!((0..1 << 16).contains(&port), "port is out of range");

    let host = env.get_string(&host).expect("failed to read string");
    let host = host.to_str().expect("invalid utf8").to_string();

    match handle.use_handle(|socket| {
        let socket = socket.clone();
        run_with_timeout(Duration::from_millis(3000), async move {
            socket.connect(&host, port as u16).await
        })
    }) {
        None => throw_rte(&mut env, "timed out connecting to server"),
        Some(Err(err)) => throw_rte(&mut env, format!("failed to connect to server: {err}")),
        _ => {}
    };
}

#[jni_fn("hexacraft.rs.RustLib$ClientSocket")]
pub fn send<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<Arc<hexacraft::zmq::ClientSocket>>,
    data: JByteArray<'local>,
) {
    let data = env
        .convert_byte_array(data)
        .expect("failed to convert byte array");

    match handle.use_handle(|socket| {
        let socket = socket.clone();
        run_with_timeout(Duration::from_millis(3000), async move {
            socket.send(data).await
        })
    }) {
        None => throw_rte(&mut env, "timed out sending data"),
        Some(Err(err)) => throw_rte(&mut env, format!("failed to send data: {err}")),
        _ => {}
    };
}

#[jni_fn("hexacraft.rs.RustLib$ClientSocket")]
pub fn receive<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<Arc<hexacraft::zmq::ClientSocket>>,
) -> jbyteArray {
    match handle.use_handle(|socket| {
        let socket = socket.clone();
        run_with_timeout(Duration::from_millis(3000), async move {
            socket.receive().await
        })
    }) {
        None => {
            throw_rte(&mut env, "timed out receiving");
            *JObject::null()
        }
        Some(Err(err)) => {
            throw_rte(&mut env, format!("failed to receive data: {err}"));
            *JObject::null()
        }
        Some(Ok(data)) => env
            .byte_array_from_slice(&data)
            .expect("failed to create byte array")
            .as_jarray_raw(),
    }
}

#[jni_fn("hexacraft.rs.RustLib$ClientSocket")]
pub fn close<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<Arc<hexacraft::zmq::ClientSocket>>,
) {
    handle.destroy();
}
