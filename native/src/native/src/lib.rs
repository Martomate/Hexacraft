#[cfg(test)]
mod tests {
    mod vorbis;
}

mod handle;
mod noise_3d;
mod noise_4d;
mod vorbis;
mod zmq;

use std::ffi::c_void;
use std::sync::Arc;
use std::time::Duration;

use handle::Handle;
use jni::objects::{
    AsJArrayRaw, JByteArray, JClass, JIntArray, JLongArray, JObject, JString, ReleaseMode,
};
use jni::strings::JNIString;
use jni::sys::{JNI_VERSION_1_1, jbyteArray, jdouble, jint, jshortArray, jstring};
use jni::{JNIEnv, JavaVM};
use jni_fn::jni_fn;

static RT: std::sync::Mutex<Option<tokio::runtime::Runtime>> = std::sync::Mutex::new(None);

pub struct NoiseState {
    perm: Vec<i32>,
}

#[jni_fn("hexacraft.rs.RustLib")]
pub fn hello<'local>(env: JNIEnv<'local>, _class: JClass<'local>) -> jstring {
    let s = env.new_string("Hello from Rust!").unwrap();
    s.into_raw()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(_vm: JavaVM, _: *mut c_void) -> jint {
    let mut rt = RT.lock().unwrap();
    *rt = Some(
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("failed to start Tokio runtime"),
    );
    JNI_VERSION_1_1
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnUnload(_vm: JavaVM, _: *mut c_void) {
    let mut rt = RT.lock().unwrap();
    rt.take().unwrap(); // this will drop the runtime which will shut it down
}

#[jni_fn("hexacraft.rs.RustLib$NoiseGenerator3D")]
pub fn storePerms<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    perm: JIntArray<'local>,
) -> Handle<NoiseState> {
    let elements = unsafe {
        env.get_array_elements(&perm, ReleaseMode::NoCopyBack)
            .expect("failed to read from perm array")
    };

    let state = NoiseState {
        perm: (0..512).map(|i| elements[i]).collect::<Vec<_>>(),
    };
    Handle::create(state)
}

#[jni_fn("hexacraft.rs.RustLib$NoiseGenerator3D")]
pub fn createLayeredNoiseGenerator<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    noiseHandles: JLongArray<'local>,
) -> Handle<Vec<Handle<NoiseState>>> {
    let elements = unsafe {
        env.get_array_elements(&noiseHandles, ReleaseMode::NoCopyBack)
            .expect("failed to read from noiseHandles array")
    };

    let genHandle: Vec<Handle<NoiseState>> = elements
        .iter()
        .map(|&h| unsafe { Handle::wrap(h) })
        .collect::<Vec<_>>();
    Handle::create(genHandle)
}

#[jni_fn("hexacraft.rs.RustLib$NoiseGenerator3D")]
pub fn genNoise<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    genHandle: Handle<Vec<Handle<NoiseState>>>,
    scale: jdouble,
    x: jdouble,
    y: jdouble,
    z: jdouble,
) -> jdouble {
    genHandle.use_handle(|noise_state_handles| {
        Handle::use_handles(noise_state_handles, |noise_states| {
            let perms = noise_states
                .iter()
                .map(|state| state.perm.as_slice())
                .collect::<Vec<_>>();
            noise_3d::noise_with_octaves(perms.as_slice(), scale, x, y, z)
        })
    })
}

#[jni_fn("hexacraft.rs.RustLib$NoiseGenerator4D")]
pub fn storePerms<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    perm: JIntArray<'local>,
) -> Handle<NoiseState> {
    let elements = unsafe {
        env.get_array_elements(&perm, ReleaseMode::NoCopyBack)
            .expect("failed to read from perm array")
    };

    let state = NoiseState {
        perm: (0..512).map(|i| elements[i]).collect::<Vec<_>>(),
    };
    Handle::create(state)
}

#[jni_fn("hexacraft.rs.RustLib$NoiseGenerator4D")]
pub fn createLayeredNoiseGenerator<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    noiseHandles: JLongArray<'local>,
) -> Handle<Vec<Handle<NoiseState>>> {
    let elements = unsafe {
        env.get_array_elements(&noiseHandles, ReleaseMode::NoCopyBack)
            .expect("failed to read from noiseHandles array")
    };

    let genHandle: Vec<Handle<NoiseState>> = elements
        .iter()
        .map(|&h| unsafe { Handle::wrap(h) })
        .collect::<Vec<_>>();
    Handle::create(genHandle)
}

#[jni_fn("hexacraft.rs.RustLib$NoiseGenerator4D")]
pub fn genNoise<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    genHandle: Handle<Vec<Handle<NoiseState>>>,
    scale: jdouble,
    x: jdouble,
    y: jdouble,
    z: jdouble,
    w: jdouble,
) -> jdouble {
    genHandle.use_handle(|noise_state_handles| {
        Handle::use_handles(noise_state_handles, |noise_states| {
            let perms = noise_states
                .iter()
                .map(|state| state.perm.as_slice())
                .collect::<Vec<_>>();
            noise_4d::noise_with_octaves(perms.as_slice(), scale, x, y, z, w)
        })
    })
}

#[jni_fn("hexacraft.rs.RustLib$VorbisDecoder")]
pub fn decode<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    bytes: JByteArray<'local>,
) -> Handle<vorbis::VorbisData> {
    let elements = unsafe {
        env.get_array_elements(&bytes, ReleaseMode::NoCopyBack)
            .expect("failed to read from bytes array")
    };
    let bytes: Vec<u8> = elements
        .iter()
        .cloned()
        .map(|b| b as u8)
        .collect::<Vec<_>>();

    let state: vorbis::VorbisData = vorbis::decode(&bytes).unwrap();
    Handle::create(state)
}

#[jni_fn("hexacraft.rs.RustLib$VorbisDecoder")]
pub fn getSamples<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<vorbis::VorbisData>,
) -> jshortArray {
    handle.use_handle(|data| {
        let samples = &data.samples;
        let arr = env
            .new_short_array(samples.len() as i32)
            .expect("failed to create short array");
        env.set_short_array_region(&arr, 0, samples)
            .expect("failed to write samples to short array");
        arr.into_raw()
    })
}

#[jni_fn("hexacraft.rs.RustLib$VorbisDecoder")]
pub fn getSampleRate<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<vorbis::VorbisData>,
) -> jint {
    handle.use_handle(|data| data.sample_rate)
}

#[jni_fn("hexacraft.rs.RustLib$VorbisDecoder")]
pub fn destroy<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<vorbis::VorbisData>,
) {
    handle.destroy();
}

#[jni_fn("hexacraft.rs.RustLib$ClientSocket")]
pub fn create<'local>(env: JNIEnv<'local>, _class: JClass<'local>, client_id: JByteArray<'local>) -> Handle<Arc<zmq::ClientSocket>> {
    let client_id = env
        .convert_byte_array(client_id)
        .expect("failed to convert byte array");

    Handle::create(Arc::new(zmq::ClientSocket::new(client_id)))
}

#[jni_fn("hexacraft.rs.RustLib$ClientSocket")]
pub fn connect<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<Arc<zmq::ClientSocket>>,
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
    handle: Handle<Arc<zmq::ClientSocket>>,
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
    handle: Handle<Arc<zmq::ClientSocket>>,
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
    handle: Handle<Arc<zmq::ClientSocket>>,
) {
    handle.destroy();
}

fn throw_rte<'local>(env: &mut JNIEnv<'local>, err: impl Into<JNIString>) {
    env.throw_new("java/lang/RuntimeException", err)
        .expect("failed to throw exception");
}

fn run_with_timeout<R: Send + 'static>(
    timeout: Duration,
    future: impl Future<Output = R> + Send + 'static,
) -> Option<R> {
    let (tx, rx) = std::sync::mpsc::channel();
    let rt = RT.lock().unwrap();
    rt.as_ref().unwrap().spawn(async move {
        tx.send(tokio::time::timeout(timeout, future).await)
            .unwrap();
    });
    rx.recv().expect("task failed").ok()
}
