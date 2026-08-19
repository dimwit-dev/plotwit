package plotwit.examples.plots

import dimwit.Conversions.given
import dimwit.*
import dimwit.stats.Normal
import plotwit.*

/** A line, layered on top of the observations it was fitted to. */
object OverlayExample:

  trait Sample derives Label

  def spec: VegaLiteSpec =
    val numPoints = 60
    val slope = 1.5f

    val xs = Tensor1(Axis[Sample]).fromArray(
      Array.tabulate(numPoints)(i => -3.0f + 6.0f * i / (numPoints - 1))
    )
    val noise = Normal.standardNormal(xs.shape).sample(Key(2))
    val observations = (xs *! slope) + noise
    val fitted = xs *! slope

    overlay(
      plots.scatterPlot(xs, observations, _.encoding.x.title := "x", _.encoding.y.title := "y"),
      plots.linePlot(xs, fitted, _.title := "A line, fitted to noisy observations", _.encoding.x.title := "x", _.encoding.y.title := "y")
    )
