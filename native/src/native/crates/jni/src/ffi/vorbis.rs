use crate::handle::Handle;

use jni::JNIEnv;
use jni::objects::{JByteArray, JClass, ReleaseMode};
use jni::sys::{jint, jshortArray};
use jni_fn::jni_fn;

#[jni_fn("hexacraft.rs.RustLib$VorbisDecoder")]
pub fn decode<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    bytes: JByteArray<'local>,
) -> Handle<hexacraft::vorbis::VorbisData> {
    let elements = unsafe {
        env.get_array_elements(&bytes, ReleaseMode::NoCopyBack)
            .expect("failed to read from bytes array")
    };
    let bytes: Vec<u8> = elements
        .iter()
        .cloned()
        .map(|b| b as u8)
        .collect::<Vec<_>>();

    let state: hexacraft::vorbis::VorbisData = hexacraft::vorbis::decode(&bytes).unwrap();
    Handle::create(state)
}

#[jni_fn("hexacraft.rs.RustLib$VorbisDecoder")]
pub fn getSamples<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<hexacraft::vorbis::VorbisData>,
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
    handle: Handle<hexacraft::vorbis::VorbisData>,
) -> jint {
    handle.use_handle(|data| data.sample_rate)
}

#[jni_fn("hexacraft.rs.RustLib$VorbisDecoder")]
pub fn destroy<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: Handle<hexacraft::vorbis::VorbisData>,
) {
    handle.destroy();
}
