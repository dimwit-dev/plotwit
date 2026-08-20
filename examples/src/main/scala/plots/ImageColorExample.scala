package plotwit.examples.plots

import dimwit.*
import plotwit.*

/** A colour image, given as a tensor of 8 bit pixel intensities per colour channel. */
object ImageColorExample:

  trait Width derives Label
  trait Height derives Label
  trait Channel derives Label

  def spec: VegaLiteSpec =
    val (width, height) = (240, 240)

    /** One radial wave per channel, each a third of a period behind the one before it. */
    def intensity(x: Int, y: Int, channel: Int): Float =
      val (dx, dy) = (x - width / 2.0, y - height / 2.0)
      val radius = Math.sqrt(dx * dx + dy * dy)
      val phase = channel * 2.0 * Math.PI / 3.0
      (127.5 * (1.0 + Math.sin(radius / 6.0 - phase) * Math.exp(-radius / 120.0))).toFloat

    val image = Tensor3(Axis[Width], Axis[Height], Axis[Channel])
      .fromArray(Array.tabulate(width, height, 3)(intensity))
      .asInt(VType[UInt8])

    plots.imagePlot(
      image,
      _.title := "Radial waves, one per colour channel"
    )
