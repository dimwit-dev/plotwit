package plotwit.plots

import dimwit._
import io.circe.Json
import io.circe.syntax._
import io.github.quafadas.plots.SetupVega._
import plotwit.Core._
import viz.macros.ArrField
import viz.macros.ObjField
import viz.macros.SpecMod

object histogramPlot:

  lazy val template = new PlotTemplate(
    VegaPlot.fromString("""
      {
      "$schema": "https://vega.github.io/schema/vega-lite/v6.json",
      "description": "Histogram created from data.",
      "title": "",
      "data": { "values": [] },
      "mark": { "type": "bar" },
      "encoding": {
        "x": {"field": "value", "type": "quantitative", "bin": {"maxbins": 20}},
        "y": {"aggregate": "count", "type": "quantitative"}
      }
    }""")
  )

  import template.{M, spec}

  def apply[L: Label](data: Tensor1[L, Float32], mods: (M => SpecMod)*): UnitSpec = UnitSpec:
    spec.build((
      Seq[M => SpecMod](
        _.data.values := data.toArray.map: v =>
          Map("value" -> v)
        .asJson
      ) ++ mods
    )*)
