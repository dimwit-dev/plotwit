package plotwit.plots

import dimwit._
import io.circe.Json
import io.circe.syntax._
import io.github.quafadas.plots.SetupVega._
import plotwit.Core._
import viz.macros.ArrField
import viz.macros.ObjField
import viz.macros.SpecMod

object heatmapPlot:

  lazy val template = new PlotTemplate(
    VegaPlot.fromString("""
      {
        "$schema": "https://vega.github.io/schema/vega-lite/v6.json",
        "description": "Heatmap created from Tensor2 data.",
        "title": "",
        "data": { "values": [] },
        "width": 300,
        "height": 300,
        "mark": "rect",
        "encoding": {
          "x": { 
            "field": "x", 
            "type": "ordinal", 
            "title": "",
            "axis": { "labelAngle": 0, "labels": true, "ticks": true }
          },
          "y": { 
            "field": "y", 
            "type": "ordinal", 
            "title": "",
            "axis": { "labels": true, "ticks": true }
          },
          "color": { 
            "field": "value", 
            "type": "quantitative", 
            "title": "Value",
            "scale": { 
              "scheme": "blues" 
            }
          }
        },
        "config": {
          "scale": {
            "bandPaddingInner": 0,
            "bandPaddingOuter": 0
          },
          "rect": {
            "discreteWidth": 30
          }
        }
      }""")
  )

  import template.{M, spec}

  def apply[L1: Label, L2: Label](data: Tensor2[L1, L2, Float32], mods: (M => SpecMod)*): UnitSpec = UnitSpec:
    val rows = data.shape(Axis[L1])
    val cols = data.shape(Axis[L2])
    val array = data.toArray
    val jsonValues = (
      for
        r <- 0 until rows
        c <- 0 until cols
      yield Map(
        "x" -> c.toFloat,
        "y" -> r.toFloat,
        "value" -> array(r)(c)
      )
    ).asJson
    spec.build((Seq[M => SpecMod](_.data.values := jsonValues) ++ mods)*)
