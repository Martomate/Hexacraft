mod noise;

use jni::objects::{JIntArray, ReleaseMode};
use jni::sys::{jclass, jdouble, jlong, jstring};
use jni::JNIEnv;
use jni_fn::jni_fn;

#[jni_fn("hexacraft.rs.RustLib")]
pub fn hello<'local>(env: JNIEnv<'local>, _class: jclass) -> jstring {
    let s = env.new_string("Hello from Rust!").unwrap();
    s.into_raw()
}

#[jni_fn("hexacraft.rs.RustLib$PerlinNoise4D")]
pub fn init<'local>(mut env: JNIEnv<'local>, _class: jclass, perm: JIntArray<'local>) -> jlong {
    let elements = unsafe {
        env.get_array_elements(&perm, ReleaseMode::NoCopyBack)
            .expect("failed to read from perm array")
    };

    let perm = (0..512).map(|i| elements[i]).collect::<Vec<_>>();
    let perm = Box::new(perm);
    (Box::into_raw(perm) as *mut i32) as jlong
}

#[jni_fn("hexacraft.rs.RustLib$PerlinNoise4D")]
pub fn noise<'local>(
    _env: JNIEnv<'local>,
    _class: jclass,
    handle: jlong,
    xx: jdouble,
    yy: jdouble,
    zz: jdouble,
    ww: jdouble,
) -> jdouble {
    let perm: Box<Vec<i32>> = unsafe { Box::from_raw(handle as *mut Vec<i32>) };
    assert_eq!(perm.len(), 512);

    let res = noise::noise(&perm, xx, yy, zz, ww);

    std::mem::forget(perm);

    res
}
