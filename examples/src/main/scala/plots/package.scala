package plotwit.examples.plots

import viz.ChartLibrary
import viz.LowPriorityPlotTarget
import viz.VizReturn

/** A plot target that writes a plot to `<directory>/<name>.json` and renders it to `<directory>/<name>.png`.
  *
  * This is the minimum needed to keep the plots of the README up to date, and deliberately lives here rather than in
  * plotwit: the only plot target that renders images, `viz.PlotTargets.png`, shells out to `vg2png`, which understands
  * plain Vega specs only, while every plot but `tensorTreeShapePlot` is a Vega-Lite one. Those need `vl2png`. Both
  * commands come with the vega command line tools: `npm install -g vega-cli vega-lite`.
  */
private[plots] def plotFiles(directory: os.Path, name: String, scale: Double = 2.0): LowPriorityPlotTarget =
  new LowPriorityPlotTarget:
    def show(spec: ujson.Value, library: ChartLibrary): VizReturn =
      os.write.over(directory / s"$name.json", spec.render(indent = 2) + "\n")

      val cli = if isVegaSpec(spec) then "vg2png" else "vl2png"
      val rendered = os.proc(cli, "-s", scale.toString).call(stdin = spec.render(), stderr = os.Pipe, check = false)
      if rendered.exitCode != 0 then
        throw new RuntimeException(s"$cli failed with exit code ${rendered.exitCode}: ${rendered.err.text()}")

      val png = directory / s"$name.png"
      os.write.over(png, rendered.out.bytes)
      png

/** True for plain Vega specs, false for Vega-Lite ones (the default when no `$schema` is declared). */
private def isVegaSpec(spec: ujson.Value): Boolean =
  spec.objOpt
    .flatMap(_.get("$schema"))
    .flatMap(_.strOpt)
    .exists(_.contains("/schema/vega/"))
