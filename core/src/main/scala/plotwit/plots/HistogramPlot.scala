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

  /** Every property is spelled out with the value Vega-Lite would choose, so that each of them can be changed with a
    * mod. The only exception is `maxbins`, which is 20 rather than Vega-Lite's 10.
    */
  lazy val template = new PlotTemplate(
    VegaPlot.fromString("""
      {
      "$schema": "https://vega.github.io/schema/vega-lite/v6.json",
      "description": "Histogram created from data.",
      "title": "",
      "width": 200,
      "height": 200,
      "data": { "values": [] },
      "mark": {
        "type": "bar",
        "orient": "vertical",
        "color": "#4c78a8",
        "opacity": 1,
        "binSpacing": 1,
        "cornerRadius": 0,
        "tooltip": false,
        "clip": false
      },
      "encoding": {
        "x": {
          "field": "value",
          "type": "quantitative",
          "title": "value (binned)",
          "bin": {
            "maxbins": 20,
            "base": 10,
            "divide": [5, 2],
            "nice": true
          },
          "scale": {
            "type": "linear",
            "zero": false,
            "nice": false,
            "reverse": false,
            "clamp": false
          },
          "axis": {
            "orient": "bottom",
            "grid": false,
            "labels": true,
            "ticks": true,
            "domain": true,
            "labelAngle": 0,
            "labelFlush": true,
            "labelOverlap": true,
            "labelFontSize": 10,
            "labelColor": "#000",
            "labelPadding": 2,
            "labelLimit": 180,
            "tickSize": 5,
            "tickWidth": 1,
            "tickColor": "#888",
            "domainColor": "#888",
            "domainWidth": 1,
            "gridColor": "#ddd",
            "gridWidth": 1,
            "gridOpacity": 1,
            "gridDash": [],
            "titleFontSize": 11,
            "titleFontWeight": "bold",
            "titleColor": "#000",
            "titlePadding": 4
          }
        },
        "y": {
          "aggregate": "count",
          "type": "quantitative",
          "title": "Count of Records",
          "scale": {
            "type": "linear",
            "domain": null,
            "zero": true,
            "nice": true,
            "reverse": false,
            "clamp": false
          },
          "axis": {
            "orient": "left",
            "grid": true,
            "labels": true,
            "ticks": true,
            "domain": true,
            "labelAngle": 0,
            "labelFlush": false,
            "labelOverlap": true,
            "labelFontSize": 10,
            "labelColor": "#000",
            "labelPadding": 2,
            "labelLimit": 180,
            "tickSize": 5,
            "tickWidth": 1,
            "tickColor": "#888",
            "domainColor": "#888",
            "domainWidth": 1,
            "gridColor": "#ddd",
            "gridWidth": 1,
            "gridOpacity": 1,
            "gridDash": [],
            "titleFontSize": 11,
            "titleFontWeight": "bold",
            "titleColor": "#000",
            "titlePadding": 4
          }
        }
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
