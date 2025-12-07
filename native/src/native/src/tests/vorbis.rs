static EXAMPLE_SOUND: &[u8] = include_bytes!("example_sound.ogg");

#[test]
fn load_ogg_file() {
    let sound = crate::vorbis::decode(EXAMPLE_SOUND).unwrap();

    assert_eq!(sound.sample_rate, 44100);
    assert_eq!(sound.samples.len(), 11040);

    assert_eq!(sound.samples[0], -9);
    assert_eq!(sound.samples[123], -6);
    assert_eq!(sound.samples[sound.samples.len() / 2], 886);
    assert_eq!(sound.samples[sound.samples.len() - 1], 0);
}
