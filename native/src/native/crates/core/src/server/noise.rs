use rand::RngExt as _;

use crate::server::random::Random;
use crate::{noise_3d, noise_4d};

pub struct NoiseGenerator3D {
    perms: Vec<[u8; 512]>,
    scale: f64,
}

impl NoiseGenerator3D {
    pub fn new(rand: &mut Random, octaves: u8, scale: f64) -> Self {
        Self {
            perms: (0..octaves).map(|_| make_perm(rand)).collect(),
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
    pub fn new(rand: &mut Random, octaves: u8, scale: f64) -> Self {
        Self {
            perms: (0..octaves).map(|_| make_perm(rand)).collect(),
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

fn make_perm(rand: &mut Random) -> [u8; 512] {
    let mut perm: [u8; 256] = std::array::from_fn(|i| i as u8);

    shuffle_array(&mut perm, rand);

    let mut res = [0; 512];
    res[..256].copy_from_slice(&perm);
    res[256..].copy_from_slice(&perm);
    res
}

fn shuffle_array<T, const N: usize>(arr: &mut [T; N], rand: &mut Random) {
    let len = arr.len();
    for i in 0..len {
        arr.swap(i, rand.random_range(0..(len - i)) + i);
    }
}

#[cfg(test)]
mod tests_3d {
    use rand::RngExt;

    use super::NoiseGenerator3D;
    use super::test_utils::*;
    use crate::server::random::Random;

    #[test]
    fn fixed_for_same_input() {
        let seed = 123456789123456789_u64;
        let _gen = make_gen(seed);

        let coords: [_; 4] = std::array::from_fn(|i| {
            let i = i as i8;
            ((i / 2 % 2) - 1, (i % 2) - 1)
        })
        .map(|(x, z)| (x as f64 * 321.0, z as f64 * 321.0));

        let expected_noise: [_; 4] = [
            0.15390935087522514,
            0.48707075476407985,
            0.13387749820415404,
            -0.18212522700800027,
        ];

        // These assertions act as regression tests
        assert_eq!(
            coords.map(|(x, z)| _gen.gen_wrapped_noise(x, z, 1234.0)),
            expected_noise
        );
    }

    #[test]
    fn same_for_same_input() {
        let mut rand = Random::from_seed(42);
        let seed = rand.random::<u64>();
        let gen1 = make_gen(seed);
        let gen2 = make_gen(seed);

        let scale = 100.0;
        for _ in 0..10 {
            let x = next_double(&mut rand, scale);
            let z = next_double(&mut rand, scale);

            assert_eq!(
                gen1.gen_wrapped_noise(x, z, 123.0),
                gen2.gen_wrapped_noise(x, z, 123.0)
            );
        }
    }

    #[test]
    fn different_for_different_radii() {
        let seed = 123456789_u64;
        let _gen = make_gen(seed);

        assert_ne!(
            _gen.gen_wrapped_noise(0.1, 0.2, 1.234),
            _gen.gen_wrapped_noise(0.1, 0.2, 2.234),
        );
    }

    #[test]
    fn not_constant() {
        let mut rand = Random::from_seed(42);
        let _gen = make_gen(rand.random::<u64>());

        let scale = 100.0;

        let values = unique_f64s((0..10).map(|_| {
            let x = next_double(&mut rand, scale);
            let z = next_double(&mut rand, scale);

            _gen.gen_wrapped_noise(x, z, 123.4)
        }));

        assert!(values.len() > 1);
    }

    fn make_gen(seed: u64) -> NoiseGenerator3D {
        NoiseGenerator3D::new(&mut Random::from_seed(seed), 4, 0.01)
    }
}

#[cfg(test)]
mod tests_4d {
    use rand::RngExt;

    use super::NoiseGenerator4D;
    use super::test_utils::*;
    use crate::server::random::Random;

    #[test]
    fn fixed_for_same_input() {
        let seed = 123456789123456789_u64;
        let _gen = make_gen(seed);

        let coords: [_; 8] = std::array::from_fn(|i| {
            let i = i as i8;
            ((i / 4) - 1, (i / 2 % 2) - 1, (i % 2) - 1)
        })
        .map(|(x, y, z)| (x as f64 * 321.0, y as f64 * 321.0, z as f64 * 321.0));

        let expected_noise: [_; 8] = [
            -0.12121297840261352,
            0.7920653866495418,
            -0.2471291482724508,
            0.2682094089653293,
            -0.04695392270259044,
            -0.21946891763839488,
            -0.1673367016346573,
            0.5647166842879996,
        ];

        // These assertions act as regression tests
        assert_eq!(
            coords.map(|(x, y, z)| _gen.gen_wrapped_noise(x, y, z, 1234.0)),
            expected_noise
        );
    }

    #[test]
    fn same_for_same_input() {
        let mut rand = Random::from_seed(42);
        let seed = rand.random::<u64>();
        let gen1 = make_gen(seed);
        let gen2 = make_gen(seed);

        let scale = 100.0;
        for _ in 0..10 {
            let x = next_double(&mut rand, scale);
            let y = next_double(&mut rand, scale);
            let z = next_double(&mut rand, scale);

            assert_eq!(
                gen1.gen_wrapped_noise(x, y, z, 123.0),
                gen2.gen_wrapped_noise(x, y, z, 123.0)
            );
        }
    }

    #[test]
    fn different_for_different_radii() {
        let seed = 123456789_u64;
        let _gen = make_gen(seed);

        assert_ne!(
            _gen.gen_wrapped_noise(0.1, 0.2, 0.3, 1.234),
            _gen.gen_wrapped_noise(0.1, 0.2, 0.3, 2.234),
        );
    }

    #[test]
    fn not_constant() {
        let mut rand = Random::from_seed(42);
        let _gen = make_gen(rand.random::<u64>());

        let scale = 100.0;

        let values = unique_f64s((0..10).map(|_| {
            let x = next_double(&mut rand, scale);
            let y = next_double(&mut rand, scale);
            let z = next_double(&mut rand, scale);

            _gen.gen_wrapped_noise(x, y, z, 123.4)
        }));

        assert!(values.len() > 1);
    }

    fn make_gen(seed: u64) -> NoiseGenerator4D {
        NoiseGenerator4D::new(&mut Random::from_seed(seed), 4, 0.01)
    }
}

#[cfg(test)]
mod test_utils {
    use rand::RngExt;

    use crate::server::random::Random;

    pub fn next_double(rand: &mut Random, scale: f64) -> f64 {
        rand.random::<f64>() * scale
    }

    pub fn unique_f64s(values: impl Iterator<Item = f64>) -> Vec<f64> {
        let mut values = values.collect::<Vec<f64>>();
        values.sort_by(f64::total_cmp);
        values.dedup();
        values
    }
}
