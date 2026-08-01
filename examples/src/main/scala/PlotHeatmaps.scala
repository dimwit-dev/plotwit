package src.main.scala

import dimwit.*
import plotwit.*
import plotwit.plotting.Plotting.*

@main
def plotHeatmaps(): Unit =

  dimwit.initialize()

  trait A derives Label
  trait B derives Label
  trait C derives Label

  val data = Tensor3(Axis[A], Axis[B], Axis[C]).fromArray(
    Array(
      Array(
        Array(1.0f, 2.0f),
        Array(3.0f, 4.0f)
      ),
      Array(
        Array(2.0f, 5.0f),
        Array(0.0f, 3.0f)
      ),
      Array(
        Array(99.0f, 13.0f),
        Array(0.0f, 22.0f)
      )
    )
  )

  val specs = data.unstack(Axis[A]).zipWithIndex.map:
    case (t, c) =>
      heatmap.plot(
        t,
        _.title := f"Heatmap Matrix (c=$c)",
        _.encoding.x.title := "Beniboy",
        _.encoding.x.axis.labelAngle := 90
      )

  import viz.PlotTargets.desktopBrowser
  display(hconcat(specs.toList))
