package plotwit.examples.plots

import dimwit.Conversions.given
import dimwit.*
import dimwit.stats.Normal
import plotwit.*

/** A noisy linear relationship, where the size of each point encodes how far it is off the trend. */
object ScatterExample:

  trait Sample derives Label

  def spec: VegaLiteSpec =
    val numPoints = 150

    val xs = Tensor1(Axis[Sample]).fromArray(
      Array.tabulate(numPoints)(i => -3.0f + 6.0f * i / (numPoints - 1))
    )
    val noise = Normal.standardNormal(xs.shape).sample(Key(0))
    val ys = (xs *! 1.5f) + noise
    val sizes = noise.abs *! 150.0f

    plots.scatterPlot(
      xs,
      ys,
      sizes,
      _.title := "y = 1.5 x + ε",
      _.encoding.x.title := "x",
      _.encoding.y.title := "y"
    )
