package hexacraft.util

class PointerWrapper:
  private val int1 = Array.ofDim[Int](1)
  private val int2 = Array.ofDim[Int](1)

  private val double1 = Array.ofDim[Double](1)
  private val double2 = Array.ofDim[Double](1)

  def ints(f: (Array[Int], Array[Int]) => Unit): (Int, Int) = this.synchronized {
    f(int1, int2)

    (int1(0), int2(0))
  }

  def doubles(f: (Array[Double], Array[Double]) => Unit): (Double, Double) = this.synchronized {
    f(double1, double2)

    (double1(0), double2(0))
  }
