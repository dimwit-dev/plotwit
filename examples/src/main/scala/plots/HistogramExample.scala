package plotwit.examples.plots

import dimwit.*
import dimwit.stats.Normal
import plotwit.*

/** The empirical distribution of 2000 samples drawn from a standard normal. */
object HistogramExample:

  trait Sample derives Label

  def spec: VegaLiteSpec =
    val samples = Normal
      .standardNormal(Shape(Axis[Sample] -> 2000))
      .sample(Key(1))

    plots.histogramPlot(
      samples,
      _.title := "2000 samples of a standard normal",
      _.encoding.x.bin.maxbins := 40
    )
