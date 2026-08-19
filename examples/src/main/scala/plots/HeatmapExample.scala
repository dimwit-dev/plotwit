package plotwit.examples.plots

import dimwit.*
import plotwit.*

/** A 2d function sampled on a 16x16 grid. */
object HeatmapExample:

  trait Row derives Label
  trait Column derives Label

  def spec: VegaLiteSpec =
    val size = 16

    def f(row: Int, column: Int): Float =
      val (x, y) = ((column - size / 2) / 3.0, (row - size / 2) / 3.0)
      (Math.sin(x) * Math.cos(y)).toFloat

    val data = Tensor2(Axis[Row], Axis[Column]).fromArray(
      Array.tabulate(size, size)(f)
    )

    plots.heatmapPlot(
      data,
      _.title := "sin(x) · cos(y)",
      _.encoding.x.title := "column",
      _.encoding.y.title := "row",
      _.encoding.color.scale.scheme := "viridis"
    )
