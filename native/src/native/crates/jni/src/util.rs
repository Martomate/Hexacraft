use jni::{JNIEnv, strings::JNIString};

pub fn throw_rte<'local>(env: &mut JNIEnv<'local>, err: impl Into<JNIString>) {
    env.throw_new("java/lang/RuntimeException", err)
        .expect("failed to throw exception");
}

pub fn throw_ie<'local>(env: &mut JNIEnv<'local>, err: impl Into<JNIString>) {
    env.throw_new("java/lang/InterruptedException", err)
        .expect("failed to throw exception");
}
