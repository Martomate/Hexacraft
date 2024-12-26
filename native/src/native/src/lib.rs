use jni::objects::JObject;
use jni::sys::jstring;
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_hexacraft_rs_RustLib_00024_hello<'local>(
    env: JNIEnv<'local>,
    _object: JObject<'local>,
) -> jstring {
    let s = env.new_string("Hello from Rust!").unwrap();
    s.into_raw()
}
