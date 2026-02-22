use std::sync::Arc;
use std::time::Duration;

use crate::handle::Handle;
use crate::{run_and_wait, run_with_timeout, throw_ie, throw_rte};

use hexacraft::ZmqError;
use jni::JNIEnv;
use jni::objects::{AsJArrayRaw, JByteArray, JClass, JObject};
use jni::sys::{jbyteArray, jint};
use jni_fn::jni_fn;

#[jni_fn("hexacraft.rs.RustLib$ServerSocket")]
pub fn create<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> Handle<Arc<hexacraft::zmq::ServerSocket>> {
    Handle::create(Arc::new(hexacraft::zmq::ServerSocket::new()))
}

#[jni_fn("hexacraft.rs.RustLib$ServerSocket")]
pub fn bind<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<Arc<hexacraft::zmq::ServerSocket>>,
    port: jint,
) {
    assert!((0..1 << 16).contains(&port), "port is out of range");

    match handle.use_handle(|socket| {
        let socket = socket.clone();
        run_with_timeout(Duration::from_millis(3000), async move {
            socket.bind(port as u16).await
        })
    }) {
        None => throw_rte(&mut env, "timed out connecting to server"),
        Some(Err(err)) => throw_rte(&mut env, format!("failed to connect to server: {err}")),
        _ => {}
    };
}

#[jni_fn("hexacraft.rs.RustLib$ServerSocket")]
pub fn send<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<Arc<hexacraft::zmq::ServerSocket>>,
    client_id: JByteArray<'local>,
    data: JByteArray<'local>,
) {
    let client_id = env
        .convert_byte_array(client_id)
        .expect("failed to convert byte array");

    let data = env
        .convert_byte_array(data)
        .expect("failed to convert byte array");

    match handle.use_handle(|socket| {
        let socket = socket.clone();
        run_with_timeout(Duration::from_millis(3000), async move {
            socket.send(client_id, data).await
        })
    }) {
        None => {
            throw_rte(&mut env, "timed out sending data");
        }
        Some(Err(ZmqError::Other("cancelled"))) => {
            throw_ie(&mut env, "cancelled");
        }
        Some(Err(err)) => {
            throw_rte(&mut env, format!("failed to send data: {err}"));
        }
        _ => {}
    };
}

#[jni_fn("hexacraft.rs.RustLib$ServerSocket")]
pub fn receive<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<Arc<hexacraft::zmq::ServerSocket>>,
) -> jbyteArray {
    match handle.use_handle(|socket| {
        let socket = socket.clone();
        run_and_wait(async move { socket.receive().await })
    }) {
        Err(ZmqError::Other("cancelled")) => {
            throw_ie(&mut env, "cancelled");
            *JObject::null()
        }
        Err(err) => {
            throw_rte(&mut env, format!("failed to receive data: {err}"));
            *JObject::null()
        }
        Ok(data) => env
            .byte_array_from_slice(&data)
            .expect("failed to create byte array")
            .as_jarray_raw(),
    }
}

#[jni_fn("hexacraft.rs.RustLib$ServerSocket")]
pub fn close<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<Arc<hexacraft::zmq::ServerSocket>>,
) {
    handle.use_handle(|socket| socket.cancel());
    handle.destroy();
}
