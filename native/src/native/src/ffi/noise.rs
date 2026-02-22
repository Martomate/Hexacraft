use crate::handle::Handle;

use jni::JNIEnv;
use jni::objects::{JClass, JIntArray, JLongArray, ReleaseMode};
use jni::sys::jdouble;
use jni_fn::jni_fn;

pub struct NoiseState {
    perm: Vec<i32>,
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
            crate::noise_3d::noise_with_octaves(perms.as_slice(), scale, x, y, z)
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
            crate::noise_4d::noise_with_octaves(perms.as_slice(), scale, x, y, z, w)
        })
    })
}
