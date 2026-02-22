use std::{ffi::c_void, time::Duration};

use jni::{
    JavaVM,
    sys::{JNI_VERSION_1_1, jint},
};

mod ffi {
    mod client_socket;
    mod noise;
    mod server_socket;
    mod vorbis;
}
mod handle;
mod util;

use util::*;

static RT: std::sync::Mutex<Option<tokio::runtime::Runtime>> = std::sync::Mutex::new(None);

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

fn run_with_timeout<R: Send + 'static>(
    timeout: Duration,
    future: impl Future<Output = R> + Send + 'static,
) -> Option<R> {
    let (tx, rx) = std::sync::mpsc::channel();
    {
        let rt = RT.lock().unwrap();
        rt.as_ref().unwrap().spawn(async move {
            tx.send(tokio::time::timeout(timeout, future).await)
                .unwrap();
        });
        drop(rt);
    }
    rx.recv().expect("task failed").ok()
}

fn run_and_wait<R: Send + 'static>(future: impl Future<Output = R> + Send + 'static) -> R {
    let (tx, rx) = std::sync::mpsc::channel();
    {
        let rt = RT.lock().unwrap();
        rt.as_ref().unwrap().spawn(async move {
            tx.send(future.await).unwrap();
        });
        drop(rt);
    }
    rx.recv().expect("task failed")
}
