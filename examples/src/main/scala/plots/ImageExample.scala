package plotwit.examples.plots

import dimwit.*
import plotwit.*

/** A greyscale image, given as a tensor of 8 bit pixel intensities. */
object ImageExample:

  trait Width derives Label
  trait Height derives Label

  def spec: VegaLiteSpec =
    val (width, height) = (240, 240)

    def intensity(x: Int, y: Int): Float =
      val (dx, dy) = (x - width / 2.0, y - height / 2.0)
      val radius = Math.sqrt(dx * dx + dy * dy)
      (127.5 * (1.0 + Math.sin(radius / 6.0) * Math.exp(-radius / 120.0))).toFloat

    val image = Tensor2(Axis[Width], Axis[Height])
      .fromArray(Array.tabulate(width, height)(intensity))
      .asInt(VType[UInt8])

    plots.imagePlot(
      image,
      _.title := "A radial wave"
    )
