package plotwit.examples.plots

import dimwit.*
import dimwit.stats.Normal
import plotwit.*

/** The densities of three normal distributions, one line per distribution. */
object LineExample:

  trait X derives Label
  trait Y derives Label

  def spec: VegaLiteSpec =
    val numPoints = 300
    val parameters = Seq((0.0f, 1.0f), (-2.0f, 0.5f), (1.5f, 2.0f)) // (mean, standard deviation)

    val xs = Tensor1(Axis[X]).fromArray(
      Array.tabulate(numPoints)(i => -6.0f + 12.0f * i / (numPoints - 1))
    )

    def pdf(mean: Float, stdDev: Float): Tensor1[X, Float32] =
      Normal(
        Tensor(xs.shape).fill(mean),
        Tensor(xs.shape).fill(stdDev)
      ).elementWiseProb(xs).asFloat

    val densities: Tensor2[Y, X, Float32] = stack(
      parameters.map((mean, stdDev) => pdf(mean, stdDev)),
      Axis[Y]
    )

    plots.linePlot(
      xs,
      densities,
      parameters.map((mean, stdDev) => f"μ=$mean%.1f, σ=$stdDev%.1f"),
      _.title := "Normal distribution PDFs",
      _.encoding.x.title := "x",
      _.encoding.y.title := "density",
      _.encoding.color.legend.title := "Parameters"
    )
