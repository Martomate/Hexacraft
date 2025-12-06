fn intComps(i: f64) -> (i32, f64, f64) {
    let intPart = i.floor();
    let rest = i - intPart;
    (intPart as i32 & 255, rest, fade(rest))
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
    let (ix, x, tx) = intComps(xx);
    let (iy, y, ty) = intComps(yy);
    let (iz, z, tz) = intComps(zz);

    let (q0, q1) = permutate(perm, ix);

    let (q00, q01) = permutate(perm, q0 + iy);
    let (q10, q11) = permutate(perm, q1 + iy);

    let (q000, q100) = permutate(perm, q00 + iz);
    let (q001, q101) = permutate(perm, q10 + iz);
    let (q010, q110) = permutate(perm, q01 + iz);
    let (q011, q111) = permutate(perm, q11 + iz);

    let x00 = lerp(
        grad(q000, x - 0.0, y - 0.0, z - 0.0),
        grad(q001, x - 1.0, y - 0.0, z - 0.0),
        tx,
    );
    let x10 = lerp(
        grad(q010, x - 0.0, y - 1.0, z - 0.0),
        grad(q011, x - 1.0, y - 1.0, z - 0.0),
        tx,
    );
    let x01 = lerp(
        grad(q100, x - 0.0, y - 0.0, z - 1.0),
        grad(q101, x - 1.0, y - 0.0, z - 1.0),
        tx,
    );
    let x11 = lerp(
        grad(q110, x - 0.0, y - 1.0, z - 1.0),
        grad(q111, x - 1.0, y - 1.0, z - 1.0),
        tx,
    );
    let y0 = lerp(x00, x10, ty);
    let y1 = lerp(x01, x11, ty);

    lerp(y0, y1, tz)
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
