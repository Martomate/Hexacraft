use std::{
    any::TypeId,
    collections::HashMap,
    marker::PhantomData,
    sync::{Arc, LazyLock, Mutex},
};

const DEBUG: bool = false;

static HANDLES: LazyLock<Arc<Mutex<HashMap<i64, TypeId>>>> =
    LazyLock::new(|| Arc::new(Mutex::new(HashMap::new())));

#[repr(transparent)]
pub struct Handle<S> {
    raw: i64,
    _phantom: PhantomData<S>,
}

impl<S: 'static> Handle<S> {
    pub fn create(state: S) -> Self {
        let raw = Box::into_raw(Box::new(state)) as i64;
        if DEBUG {
            println!("Created handle: {raw}");
        }
        HANDLES.lock().unwrap().insert(raw, TypeId::of::<S>());
        Self {
            raw,
            _phantom: PhantomData {},
        }
    }

    pub unsafe fn wrap(raw: i64) -> Self {
        if DEBUG {
            println!("Wrapped handle: {raw}");
        }
        assert_handle_type::<S>(HANDLES.lock().unwrap().get(&raw)).unwrap();
        Self {
            raw,
            _phantom: PhantomData {},
        }
    }

    pub fn use_handle<R>(&self, f: impl FnOnce(&S) -> R) -> R {
        if DEBUG {
            println!("Using handle: {}", self.raw);
        }
        assert_handle_type::<S>(HANDLES.lock().unwrap().get(&self.raw)).unwrap();
        let state: Box<S> = unsafe { Box::from_raw(self.raw as *mut S) };
        let res = f(&state);
        std::mem::forget(state);
        res
    }

    pub fn use_handles<R>(handles: &[Handle<S>], f: impl FnOnce(&[&S]) -> R) -> R {
        if DEBUG {
            println!(
                "Using handles: {:?}",
                handles.iter().map(|h| h.raw).collect::<Vec<_>>()
            );
        }
        for h in handles {
            assert_handle_type::<S>(HANDLES.lock().unwrap().get(&h.raw)).unwrap();
        }
        let state: Vec<Box<S>> = handles
            .iter()
            .map(|handle| unsafe { Box::from_raw(handle.raw as *mut S) })
            .collect();
        let state_refs: Vec<&S> = state.iter().map(|s| s.as_ref()).collect();
        let res = f(&state_refs);
        std::mem::forget(state);
        res
    }

    /// Drops the data and invalidates the handle
    pub fn destroy(self) {
        if DEBUG {
            println!("Destroying handle: {}", self.raw);
        }

        let mut type_map = HANDLES.lock().unwrap();
        assert_handle_type::<S>(type_map.get(&self.raw)).unwrap();
        type_map.remove(&self.raw);

        let state: Box<S> = unsafe { Box::from_raw(self.raw as *mut S) };
        drop(state);
    }
}

fn assert_handle_type<S: 'static>(raw_type: Option<&TypeId>) -> Result<(), &'static str> {
    let Some(type_id) = raw_type else {
        return Err("handle does not exist");
    };
    if *type_id != TypeId::of::<S>() {
        return Err("handle has wrong type");
    }
    Ok(())
}
