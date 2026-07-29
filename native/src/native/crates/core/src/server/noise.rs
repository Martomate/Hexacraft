use crate::{noise_3d, noise_4d};

pub struct NoiseGenerator3D {
    perms: Vec<[u8; 512]>,
    scale: f64,
}

impl NoiseGenerator3D {
    pub fn new(octaves: u8, scale: f64) -> Self {
        Self {
            perms: (0..octaves).map(|_| make_perm()).collect(),
            scale,
        }
    }

    pub fn gen_wrapped_noise(&self, x: f64, z: f64, radius: f64) -> f64 {
        let perms = self
            .perms
            .iter()
            .map(|perm| perm.as_slice())
            .collect::<Vec<_>>();

        let angle = z / radius;
        noise_3d::noise_with_octaves(
            perms.as_slice(),
            self.scale,
            x,
            angle.sin() * radius,
            angle.cos() * radius,
        )
    }
}

pub struct NoiseGenerator4D {
    perms: Vec<[u8; 512]>,
    scale: f64,
}

impl NoiseGenerator4D {
    pub fn new(octaves: u8, scale: f64) -> Self {
        Self {
            perms: (0..octaves).map(|_| make_perm()).collect(),
            scale,
        }
    }

    pub fn gen_wrapped_noise(&self, x: f64, y: f64, z: f64, radius: f64) -> f64 {
        let perms = self
            .perms
            .iter()
            .map(|perm| perm.as_slice())
            .collect::<Vec<_>>();

        let angle = z / radius;
        noise_4d::noise_with_octaves(
            perms.as_slice(),
            self.scale,
            x,
            y,
            angle.sin() * radius,
            angle.cos() * radius,
        )
    }
}

fn make_perm() -> [u8; 512] {
    let mut perm: [u8; 256] = std::array::from_fn(|i| i as u8);

    shuffle_array(&mut perm);

    let mut res = [0; 512];
    res[..256].copy_from_slice(&perm);
    res[256..].copy_from_slice(&perm);
    res
}

fn shuffle_array<T, const N: usize>(arr: &mut [T; N]) {
    let len = arr.len();
    for i in 0..len {
        arr.swap(i, rand::random_range(0..(len - i)) + i);
    }
}
