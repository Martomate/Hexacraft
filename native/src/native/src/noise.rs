// Improved Perlin Noise: http://mrl.nyu.edu/~perlin/noise/

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

fn int_comps(i: f64) -> (i32, f64, f64) {
    let int_part = i.floor();
    let rest = i - int_part;
    (int_part as i32 & 255, rest, fade(rest))
}

#[allow(clippy::identity_op)]
fn grad(hash: i32, x: f64, y: f64, z: f64, w: f64) -> f64 {
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

pub fn noise(perm: &[i32], xx: f64, yy: f64, zz: f64, ww: f64) -> f64 {
    let (ix, x, tx) = int_comps(xx);
    let (iy, y, ty) = int_comps(yy);
    let (iz, z, tz) = int_comps(zz);
    let (iw, w, tw) = int_comps(ww);

    let a0 = perm[(iw) as usize];
    let a1 = perm[(iw + 1) as usize];

    let a00 = perm[(a0 + iz) as usize];
    let a01 = perm[(a0 + iz + 1) as usize];
    let a10 = perm[(a1 + iz) as usize];
    let a11 = perm[(a1 + iz + 1) as usize];

    let a000 = perm[(a00 + iy) as usize];
    let a001 = perm[(a00 + iy + 1) as usize];
    let a010 = perm[(a01 + iy) as usize];
    let a011 = perm[(a01 + iy + 1) as usize];
    let a100 = perm[(a10 + iy) as usize];
    let a101 = perm[(a10 + iy + 1) as usize];
    let a110 = perm[(a11 + iy) as usize];
    let a111 = perm[(a11 + iy + 1) as usize];

    let a0000 = perm[(a000 + ix) as usize];
    let a0001 = perm[(a000 + ix + 1) as usize];
    let a0010 = perm[(a001 + ix) as usize];
    let a0011 = perm[(a001 + ix + 1) as usize];
    let a0100 = perm[(a010 + ix) as usize];
    let a0101 = perm[(a010 + ix + 1) as usize];
    let a0110 = perm[(a011 + ix) as usize];
    let a0111 = perm[(a011 + ix + 1) as usize];
    let a1000 = perm[(a100 + ix) as usize];
    let a1001 = perm[(a100 + ix + 1) as usize];
    let a1010 = perm[(a101 + ix) as usize];
    let a1011 = perm[(a101 + ix + 1) as usize];
    let a1100 = perm[(a110 + ix) as usize];
    let a1101 = perm[(a110 + ix + 1) as usize];
    let a1110 = perm[(a111 + ix) as usize];
    let a1111 = perm[(a111 + ix + 1) as usize];

    lerp(
        tri_lerp(
            grad(a0000, x - 0.0, y - 0.0, z - 0.0, w - 0.0),
            grad(a0001, x - 1.0, y - 0.0, z - 0.0, w - 0.0),
            grad(a0010, x - 0.0, y - 1.0, z - 0.0, w - 0.0),
            grad(a0011, x - 1.0, y - 1.0, z - 0.0, w - 0.0),
            grad(a0100, x - 0.0, y - 0.0, z - 1.0, w - 0.0),
            grad(a0101, x - 1.0, y - 0.0, z - 1.0, w - 0.0),
            grad(a0110, x - 0.0, y - 1.0, z - 1.0, w - 0.0),
            grad(a0111, x - 1.0, y - 1.0, z - 1.0, w - 0.0),
            tx,
            ty,
            tz,
        ),
        tri_lerp(
            grad(a1000, x - 0.0, y - 0.0, z - 0.0, w - 1.0),
            grad(a1001, x - 1.0, y - 0.0, z - 0.0, w - 1.0),
            grad(a1010, x - 0.0, y - 1.0, z - 0.0, w - 1.0),
            grad(a1011, x - 1.0, y - 1.0, z - 0.0, w - 1.0),
            grad(a1100, x - 0.0, y - 0.0, z - 1.0, w - 1.0),
            grad(a1101, x - 1.0, y - 0.0, z - 1.0, w - 1.0),
            grad(a1110, x - 0.0, y - 1.0, z - 1.0, w - 1.0),
            grad(a1111, x - 1.0, y - 1.0, z - 1.0, w - 1.0),
            tx,
            ty,
            tz,
        ),
        tw,
    )
}

fn lerp(a: f64, b: f64, t: f64) -> f64 {
    a + (b - a) * t
}

#[allow(clippy::too_many_arguments)]
fn tri_lerp(
    q000: f64,
    q100: f64,
    q010: f64,
    q110: f64,
    q001: f64,
    q101: f64,
    q011: f64,
    q111: f64,
    tx: f64,
    ty: f64,
    tz: f64,
) -> f64 {
    let x00 = lerp(q000, q100, tx);
    let x10 = lerp(q010, q110, tx);
    let x01 = lerp(q001, q101, tx);
    let x11 = lerp(q011, q111, tx);
    let y0 = lerp(x00, x10, ty);
    let y1 = lerp(x01, x11, ty);
    lerp(y0, y1, tz)
}
