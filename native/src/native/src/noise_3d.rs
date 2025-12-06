// Based on "Improved Perlin Noise" (https://cs.nyu.edu/~perlin/noise)

fn int_comps(i: f64) -> (i32, f64, f64) {
    let int_part = i.floor();
    let rest = i - int_part;
    (int_part as i32 & 255, rest, fade(rest))
}

fn grad(hash: i32, x: f64, y: f64, z: f64) -> f64 {
    let h = hash & 15;
    let u = if h < 8 { x } else { y };
    let v = if h < 4 {
        y
    } else if h == 12 || h == 14 {
        x
    } else {
        z
    };

    let gu = if (h & 1) == 0 { u } else { -u };
    let gv = if (h & 2) == 0 { v } else { -v };

    gu + gv
}

fn fade(t: f64) -> f64 {
    t * t * t * (t * (t * 6.0 - 15.0) + 10.0)
}

fn lerp(a: f64, b: f64, t: f64) -> f64 {
    a + (b - a) * t
}

fn permutate(perm: &[i32], iw: i32) -> (i32, i32) {
    (perm[(iw) as usize], perm[(iw + 1) as usize])
}

pub fn noise(perm: &[i32], xx: f64, yy: f64, zz: f64) -> f64 {
    let (ix, x, tx) = int_comps(xx);
    let (iy, y, ty) = int_comps(yy);
    let (iz, z, tz) = int_comps(zz);

    let (a0, a1) = permutate(perm, ix);

    let (a00, a10) = permutate(perm, a0 + iy);
    let (a01, a11) = permutate(perm, a1 + iy);

    let (a000, a100) = permutate(perm, a00 + iz);
    let (a001, a101) = permutate(perm, a01 + iz);
    let (a010, a110) = permutate(perm, a10 + iz);
    let (a011, a111) = permutate(perm, a11 + iz);

    let [g00, g01, g10, g11] = [
        [
            grad(a000, x - 0.0, y - 0.0, z - 0.0),
            grad(a001, x - 1.0, y - 0.0, z - 0.0),
        ],
        [
            grad(a010, x - 0.0, y - 1.0, z - 0.0),
            grad(a011, x - 1.0, y - 1.0, z - 0.0),
        ],
        [
            grad(a100, x - 0.0, y - 0.0, z - 1.0),
            grad(a101, x - 1.0, y - 0.0, z - 1.0),
        ],
        [
            grad(a110, x - 0.0, y - 1.0, z - 1.0),
            grad(a111, x - 1.0, y - 1.0, z - 1.0),
        ],
    ]
    .map(|[gzy0, gzy1]| lerp(gzy0, gzy1, tx));

    let g0 = lerp(g00, g01, ty);
    let g1 = lerp(g10, g11, ty);

    lerp(g0, g1, tz)
}

pub fn noise_with_octaves(perms: &[&[i32]], scale: f64, x: f64, y: f64, z: f64) -> f64 {
    let mut amp = 1.0;
    let mut result = 0.0;
    for perm in perms {
        let mult = scale / amp;
        result += amp * noise(perm, x * mult, y * mult, z * mult);
        amp /= 2.0;
    }
    result
}
