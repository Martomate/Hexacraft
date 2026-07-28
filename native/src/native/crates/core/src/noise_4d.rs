// Based on some code from the Internet

#[rustfmt::skip]
const GRAD4: [i8; 128] = [
    0,  1,  1,  1,    0,  1,  1, -1,    0,  1, -1,  1,    0,  1, -1, -1,
    0, -1,  1,  1,    0, -1,  1, -1,    0, -1, -1,  1,    0, -1, -1, -1,
    1,  0,  1,  1,    1,  0,  1, -1,    1,  0, -1,  1,    1,  0, -1, -1,
   -1,  0,  1,  1,   -1,  0,  1, -1,   -1,  0, -1,  1,   -1,  0, -1, -1,
    1,  1,  0,  1,    1,  1,  0, -1,    1, -1,  0,  1,    1, -1,  0, -1,
   -1,  1,  0,  1,   -1,  1,  0, -1,   -1, -1,  0,  1,   -1, -1,  0, -1,
    1,  1,  1,  0,    1,  1, -1,  0,    1, -1,  1,  0,    1, -1, -1,  0,
   -1,  1,  1,  0,   -1,  1, -1,  0,   -1, -1,  1,  0,   -1, -1, -1,  0,
];

fn int_comps(i: f64) -> (u8, f64, f64) {
    let int_part = i.floor();
    let rest = i - int_part;
    (int_part as i32 as u8, rest, fade(rest))
}

#[allow(clippy::identity_op)]
fn grad(hash: u8, x: f64, y: f64, z: f64, w: f64) -> f64 {
    let h = hash & 31;

    let gx = GRAD4[(h * 4 + 0) as usize] as f64 * x;
    let gy = GRAD4[(h * 4 + 1) as usize] as f64 * y;
    let gz = GRAD4[(h * 4 + 2) as usize] as f64 * z;
    let gw = GRAD4[(h * 4 + 3) as usize] as f64 * w;

    gx + gy + gz + gw
}

fn fade(t: f64) -> f64 {
    t * t * t * (t * (t * 6.0 - 15.0) + 10.0)
}

fn lerp(a: f64, b: f64, t: f64) -> f64 {
    a + (b - a) * t
}

fn permutate(perm: &[u8], iw: u16) -> (u8, u8) {
    (perm[(iw) as usize], perm[(iw + 1) as usize])
}

pub fn noise(perm: &[u8], xx: f64, yy: f64, zz: f64, ww: f64) -> f64 {
    let (ix, x, tx) = int_comps(xx);
    let (iy, y, ty) = int_comps(yy);
    let (iz, z, tz) = int_comps(zz);
    let (iw, w, tw) = int_comps(ww);

    let (a0, a1) = permutate(perm, iw as u16);

    let (a00, a01) = permutate(perm, a0 as u16 + iz as u16);
    let (a10, a11) = permutate(perm, a1 as u16 + iz as u16);

    let (a000, a001) = permutate(perm, a00 as u16 + iy as u16);
    let (a010, a011) = permutate(perm, a01 as u16 + iy as u16);
    let (a100, a101) = permutate(perm, a10 as u16 + iy as u16);
    let (a110, a111) = permutate(perm, a11 as u16 + iy as u16);

    let (a0000, a0001) = permutate(perm, a000 as u16 + ix as u16);
    let (a0010, a0011) = permutate(perm, a001 as u16 + ix as u16);
    let (a0100, a0101) = permutate(perm, a010 as u16 + ix as u16);
    let (a0110, a0111) = permutate(perm, a011 as u16 + ix as u16);
    let (a1000, a1001) = permutate(perm, a100 as u16 + ix as u16);
    let (a1010, a1011) = permutate(perm, a101 as u16 + ix as u16);
    let (a1100, a1101) = permutate(perm, a110 as u16 + ix as u16);
    let (a1110, a1111) = permutate(perm, a111 as u16 + ix as u16);

    let [g00, g01, g10, g11] = [
        [
            grad(a0000, x - 0.0, y - 0.0, z - 0.0, w - 0.0),
            grad(a0001, x - 1.0, y - 0.0, z - 0.0, w - 0.0),
            grad(a0010, x - 0.0, y - 1.0, z - 0.0, w - 0.0),
            grad(a0011, x - 1.0, y - 1.0, z - 0.0, w - 0.0),
        ],
        [
            grad(a0100, x - 0.0, y - 0.0, z - 1.0, w - 0.0),
            grad(a0101, x - 1.0, y - 0.0, z - 1.0, w - 0.0),
            grad(a0110, x - 0.0, y - 1.0, z - 1.0, w - 0.0),
            grad(a0111, x - 1.0, y - 1.0, z - 1.0, w - 0.0),
        ],
        [
            grad(a1000, x - 0.0, y - 0.0, z - 0.0, w - 1.0),
            grad(a1001, x - 1.0, y - 0.0, z - 0.0, w - 1.0),
            grad(a1010, x - 0.0, y - 1.0, z - 0.0, w - 1.0),
            grad(a1011, x - 1.0, y - 1.0, z - 0.0, w - 1.0),
        ],
        [
            grad(a1100, x - 0.0, y - 0.0, z - 1.0, w - 1.0),
            grad(a1101, x - 1.0, y - 0.0, z - 1.0, w - 1.0),
            grad(a1110, x - 0.0, y - 1.0, z - 1.0, w - 1.0),
            grad(a1111, x - 1.0, y - 1.0, z - 1.0, w - 1.0),
        ],
    ]
    .map(|[gwz00, gwz01, gwz10, gwz11]| {
        let gwz0 = lerp(gwz00, gwz01, tx);
        let gwz1 = lerp(gwz10, gwz11, tx);

        lerp(gwz0, gwz1, ty)
    });

    let g0 = lerp(g00, g01, tz);
    let g1 = lerp(g10, g11, tz);

    lerp(g0, g1, tw)
}

pub fn noise_with_octaves(perms: &[&[u8]], scale: f64, x: f64, y: f64, z: f64, w: f64) -> f64 {
    let mut amp = 1.0;
    let mut result = 0.0;
    for perm in perms {
        let mult = scale / amp;
        result += amp * noise(perm, x * mult, y * mult, z * mult, w * mult);
        amp /= 2.0;
    }
    result
}
