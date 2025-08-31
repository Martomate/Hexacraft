mod noise;

use jni::objects::{JIntArray, JLongArray, ReleaseMode};
use jni::sys::{jclass, jdouble, jlong, jstring};
use jni::JNIEnv;
use jni_fn::jni_fn;

struct NoiseState {
    perm: Vec<i32>
}

#[jni_fn("hexacraft.rs.RustLib")]
pub fn hello<'local>(env: JNIEnv<'local>, _class: jclass) -> jstring {
    let s = env.new_string("Hello from Rust!").unwrap();
    s.into_raw()
}

#[jni_fn("hexacraft.rs.RustLib$NoiseGenerator4D")]
pub fn storePerms<'local>(mut env: JNIEnv<'local>, _class: jclass, perm: JIntArray<'local>) -> jlong {
    let elements = unsafe {
        env.get_array_elements(&perm, ReleaseMode::NoCopyBack)
            .expect("failed to read from perm array")
    };

    let state = Box::new(NoiseState {
        perm: (0..512).map(|i| elements[i]).collect::<Vec<_>>()
    });
    Box::into_raw(state) as jlong
}

#[jni_fn("hexacraft.rs.RustLib$NoiseGenerator4D")]
pub fn createLayeredNoiseGenerator<'local>(
    mut env: JNIEnv<'local>,
    _class: jclass,
    noiseHandles: JLongArray<'local>,
) -> jlong {
    let elements = unsafe {
        env.get_array_elements(&noiseHandles, ReleaseMode::NoCopyBack)
            .expect("failed to read from noiseHandles array")
    };

    let genHandle: Vec<i64> = elements.iter().cloned().collect::<Vec<_>>();
    (Box::into_raw(Box::new(genHandle)) as *mut i32) as jlong
}

#[jni_fn("hexacraft.rs.RustLib$NoiseGenerator4D")]
pub fn genNoise<'local>(
    _env: JNIEnv<'local>,
    _class: jclass,
    genHandle: jlong,
    scale: jdouble,
    x: jdouble,
    y: jdouble,
    z: jdouble,
    w: jdouble,
) -> jdouble {
    use_handle::<Vec<i64>, _>(genHandle, |noiseStates| {
        use_handles::<NoiseState, _>(noiseStates.as_slice(), |noise_states| {
            let perms = noise_states.iter().map(|state| state.perm.as_slice()).collect::<Vec<_>>();
            noise::noise_with_octaves(perms.as_slice(), scale, x, y, z, w)
        })
    })
}

fn use_handle<S, R>(handle: jlong, f: impl FnOnce(&S) -> R) -> R {
    let state: Box<S> = unsafe { Box::from_raw(handle as *mut S) };
    let res = f(&state);
    std::mem::forget(state);
    res
}

fn use_handles<S, R>(handles: &[jlong], f: impl FnOnce(&[&S]) -> R) -> R {
    let state: Vec<Box<S>> = handles.iter().map(|&handle| unsafe { Box::from_raw(handle as *mut S) }).collect();
    let state_refs: Vec<&S> = state.iter().map(|s| s.as_ref()).collect();
    let res = f(&state_refs);
    std::mem::forget(state);
    res
}
