package plotwit.examples.normals

import dimwit.*
import dimwit.stats.Normal
import plotwit.*

trait X derives Label
trait Gaussian derives Label

@main
def plotNormalDistributions(): Unit =

  dimwit.initialize()

  // -- CONFIGURATION PARAMETERS --

  val numPoints = 300
  val xMin = -6.0f
  val xMax = 6.0f

  // (mean, standard deviation) of the distributions to plot
  val parameters = Seq(
    (0.0f, 1.0f),
    (-2.0f, 0.5f),
    (1.5f, 2.0f)
  )

  // -- EVALUATE THE DENSITIES AND PLOT THEM --

  val xs = Tensor1(Axis[X]).fromArray(
    Array.tabulate(numPoints)(i => xMin + (xMax - xMin) * i / (numPoints - 1))
  )

  def pdf(mean: Float, stdDev: Float): Tensor1[X, Float32] =
    Normal(
      Tensor(xs.shape).fill(mean),
      Tensor(xs.shape).fill(stdDev)
    ).elementWiseProb(xs).asFloat

  val densities: Tensor2[Gaussian, X, Float32] = stack(
    parameters.map((mean, stdDev) => pdf(mean, stdDev)),
    Axis[Gaussian]
  )

  val names = parameters.map((mean, stdDev) => f"μ=$mean%.1f, σ=$stdDev%.1f")

  import plotwit.PlotTargets.desktopBrowser
  display(
    plots.linePlot(
      xs,
      densities,
      names,
      _.title := "Normal distribution PDFs",
      _.encoding.x.title := "x",
      _.encoding.y.title := "density",
      _.encoding.color.legend.title := "Parameters"
    )
  )
