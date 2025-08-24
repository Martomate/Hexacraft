use jni::objects::JObject;
use jni::sys::jstring;
use jni::JNIEnv;
use jni_fn::jni_fn;

#[jni_fn("hexacraft.rs.RustLib")]
pub fn hello<'local>(env: JNIEnv<'local>, _object: JObject<'local>) -> jstring {
    let s = env.new_string("Hello from Rust!").unwrap();
    s.into_raw()
}
