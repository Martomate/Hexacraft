package com.martomate.hexacraft.util

import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

object GzipAlgorithm {
  def compress(bytes: Array[Byte]): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val stream = new BufferedOutputStream(new GZIPOutputStream(out))
    try
      stream.write(bytes)
      stream.flush()
    finally stream.close()
    out.toByteArray

  def decompress(bytes: Array[Byte]): Array[Byte] =
    val stream = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))
    try
      stream.readAllBytes()
    finally stream.close()
}
