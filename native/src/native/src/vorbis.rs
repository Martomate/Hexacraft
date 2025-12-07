use std::io::Cursor;

use vorbis_rs::{VorbisDecoder, VorbisError};

pub struct VorbisData {
    pub sample_rate: i32,
    pub samples: Vec<i16>,
}

pub fn decode(bytes: &[u8]) -> Result<VorbisData, VorbisError> {
    let mut decoder = VorbisDecoder::new(Cursor::new(bytes))?;
    
    let sample_rate: i32 = decoder.sampling_frequency().cast_signed().into();

    let mut samples = Vec::new();
    while let Some(block) = decoder.decode_audio_block()? {
        samples.extend(
            block
                .samples()
                .iter()
                .flat_map(|&chunk| chunk.iter())
                .map(|f| (*f * 0x8000 as f32).floor() as i16),
        );
    }

    Ok(VorbisData {
        sample_rate,
        samples,
    })
}
