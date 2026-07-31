use std::ops::{Deref, DerefMut};

use rand::SeedableRng as _;

type Rng = rand::rngs::Xoshiro256PlusPlus;

pub struct Random(Rng);

impl Random {
    pub fn from_seed(seed: u64) -> Self {
        Self(Rng::seed_from_u64(seed))
    }
}

impl Deref for Random {
    type Target = Rng;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl DerefMut for Random {
    fn deref_mut(&mut self) -> &mut Self::Target {
        &mut self.0
    }
}
