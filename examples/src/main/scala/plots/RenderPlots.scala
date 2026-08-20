package plotwit.examples.plots

import plotwit.*

/** The example plots, by the name of the files they are rendered to. */
val galleryPlots: Seq[(String, () => VegaLiteSpec)] = Seq(
  "line" -> (() => LineExample.spec),
  "scatter" -> (() => ScatterExample.spec),
  "histogram" -> (() => HistogramExample.spec),
  "heatmap" -> (() => HeatmapExample.spec),
  "image" -> (() => ImageExample.spec),
  "image_color" -> (() => ImageColorExample.spec),
  "tensor-tree" -> (() => TensorTreeExample.spec),
  "overlay" -> (() => OverlayExample.spec),
  "grid" -> (() => GridExample.spec)
)

/** Renders every example plot to a PNG, next to the Vega JSON it was rendered from.
  *
  * Run it with `sbt renderPlots`, which passes `docs/plots` as the output directory.
  */
@main def renderPlots(outputDirectory: String): Unit =
  dimwit.initialize()

  val outDir = os.Path(outputDirectory, os.pwd)
  os.makeDir.all(outDir)

  for (name, spec) <- galleryPlots do
    val _ = display(spec())(using plotFiles(outDir, name))
    println(s"[plots] rendered $name")

/** Opens a single plot in the browser, e.g. `sbt "examples/runMain plotwit.examples.plots.showPlot line"`. */
@main def showPlot(name: String): Unit =
  dimwit.initialize()

  val spec = galleryPlots.toMap.getOrElse(
    name,
    throw new IllegalArgumentException(s"Unknown plot '$name'. Available: ${galleryPlots.map(_._1).mkString(", ")}")
  )

  import plotwit.PlotTargets.desktopBrowser
  val _ = display(spec())
