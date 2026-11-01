pub mod noise_3d;
pub mod noise_4d;
pub mod server;
pub mod vorbis;
pub mod zmq;

pub use zeromq::ZmqError;

#[cfg(test)]
pub mod test_utils {
    macro_rules! assert_vec3_eq {
        ($lhs:expr, $rhs:expr, epsilon = $epsilon:expr) => {{
            let lhs = $lhs;
            let rhs = $rhs;
            let epsilon = $epsilon;

            for (l, r, c) in [
                (lhs.x, rhs.x, 'x'),
                (lhs.y, rhs.y, 'y'),
                (lhs.z, rhs.z, 'z'),
            ] {
                if !approx::relative_eq!(l, r, epsilon = epsilon) {
                    panic!(
                        "\
assertion `left.{c} ~= right.{c}` failed
  left = {lhs}
 right = {rhs}\
"
                    );
                }
            }
        }};
    }

    pub(crate) use assert_vec3_eq;
}
