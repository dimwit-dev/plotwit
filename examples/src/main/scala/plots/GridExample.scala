package plotwit.examples.plots

import dimwit.*
import plotwit.*

/** Four gaussian kernels, arranged in a 2x2 grid. */
object GridExample:

  trait Kernel derives Label
  trait Row derives Label
  trait Column derives Label

  def spec: VegaLiteSpec =
    val size = 15
    val sigmas = Seq(1.0, 2.0, 3.0, 5.0)

    def gaussian(sigma: Double)(row: Int, column: Int): Float =
      val (dx, dy) = (column - size / 2, row - size / 2)
      Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma)).toFloat

    val kernels = Tensor3(Axis[Kernel], Axis[Row], Axis[Column]).fromArray(
      sigmas.map(sigma => Array.tabulate(size, size)(gaussian(sigma))).toArray
    )

    val heatmaps = kernels
      .unstack(Axis[Kernel])
      .zip(sigmas)
      .map((kernel, sigma) => plots.heatmapPlot(kernel, _.title := f"σ = $sigma%.0f"))

    grid(heatmaps.grouped(2).toSeq)
