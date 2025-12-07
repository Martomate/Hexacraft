#[cfg(test)]
mod tests {
    mod vorbis;
}

mod handle;
mod noise_3d;
mod noise_4d;
mod vorbis;

use handle::Handle;
use jni::objects::{JByteArray, JClass, JIntArray, JLongArray, ReleaseMode};
use jni::sys::{jdouble, jint, jshortArray, jstring};
use jni::JNIEnv;
use jni_fn::jni_fn;

pub struct NoiseState {
    perm: Vec<i32>,
}

#[jni_fn("hexacraft.rs.RustLib")]
pub fn hello<'local>(env: JNIEnv<'local>, _class: JClass<'local>) -> jstring {
    let s = env.new_string("Hello from Rust!").unwrap();
    s.into_raw()
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
