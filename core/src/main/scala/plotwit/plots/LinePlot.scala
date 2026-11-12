package plotwit.plots

import dimwit._
import io.circe.Json
import io.circe.syntax._
import io.github.quafadas.plots.SetupVega._
import plotwit.Core._
import viz.macros.ArrField
import viz.macros.ObjField
import viz.macros.SpecMod

import scala.annotation.targetName

object linePlot:

  /** Every property is spelled out with the value Vega-Lite would choose, so that each of them can be changed with a
    * mod. Where Vega-Lite derives a value from the data (an axis domain, say), the plot fills it in or leaves it null.
    */
  lazy val template = new PlotTemplate(
    VegaPlot.fromString("""
      {
        "$schema": "https://vega.github.io/schema/vega-lite/v6.json",
        "description": "Line plot",
        "title": "",
        "width": 200,
        "height": 200,
        "data": { "values": [] },
        "mark": {
          "type": "line",
          "point": false,
          "interpolate": "linear",
          "color": "#4c78a8",
          "strokeWidth": 2,
          "strokeDash": [],
          "strokeCap": "butt",
          "opacity": 1,
          "tooltip": false,
          "clip": false
        },
        "encoding": {
          "x": {
            "field": "x",
            "type": "quantitative",
            "title": "X",
            "scale": {
              "type": "linear",
              "domain": [],
              "zero": false,
              "nice": false,
              "reverse": false,
              "clamp": false
            },
            "axis": {
              "orient": "bottom",
              "grid": true,
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
            "field": "y",
            "type": "quantitative",
            "title": "Y",
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
          },
          "color": {
            "field": "series",
            "type": "nominal",
            "scale": { "type": "ordinal", "scheme": "tableau10" },
            "legend": {
              "title": "Series",
              "orient": "right",
              "direction": "vertical",
              "symbolType": "stroke",
              "symbolSize": 100,
              "symbolStrokeWidth": 1.5,
              "labelFontSize": 10,
              "labelColor": "#000",
              "labelLimit": 160,
              "titleFontSize": 11,
              "titleFontWeight": "bold",
              "titleColor": "#000",
              "titleLimit": 180,
              "titlePadding": 5,
              "rowPadding": 2,
              "columnPadding": 10
            }
          }
        }
      }""")
  )

  import template.{M, spec}

  /** A single line through the points (xs, ys), drawn in `mark.color`. */
  def apply[S: Label](xs: Tensor1[S, Float32], ys: Tensor1[S, Float32], mods: (M => SpecMod)*): UnitSpec =
    apply(xs.toArray, Seq("" -> ys.toArray), Seq[M => SpecMod](_.encoding.color := Json.Null) ++ mods)

  /** One line per index of the L axis, all sharing the same xs. */
  @targetName("applySeries")
  def apply[L: Label, S: Label](xs: Tensor1[S, Float32], ys: Tensor2[L, S, Float32], mods: (M => SpecMod)*): UnitSpec =
    val label = summon[Label[L]].name
    apply(xs, ys, (0 until ys.shape(Axis[L])).map(i => s"${label}_$i"), mods*)

  /** One named line per index of the L axis, all sharing the same xs. */
  @targetName("applyNamedSeries")
  def apply[L: Label, S: Label](
      xs: Tensor1[S, Float32],
      ys: Tensor2[L, S, Float32],
      names: Seq[String],
      mods: (M => SpecMod)*
  ): UnitSpec =
    val numSeries = ys.shape(Axis[L])
    require(names.size == numSeries, s"Expected $numSeries series names, got ${names.size}")
    apply(xs.toArray, names.zip(ys.toArray), mods)

  private def apply(
      xsArray: Array[Float],
      series: Seq[(String, Array[Float])],
      mods: Seq[M => SpecMod]
  ): UnitSpec = UnitSpec:
    val jsonValues = series.flatMap: (name, ysArray) =>
      xsArray.indices.map: i =>
        Json.obj(
          "series" -> Json.fromString(name),
          "x" -> xsArray(i).asJson,
          "y" -> ysArray(i).asJson
        )
    .asJson

    dropNullChannels(spec.build((Seq[M => SpecMod](
      _.data.values := jsonValues,
      _.encoding.x.scale.domain := List(xsArray.min, xsArray.max).asJson
    ) ++ mods)*))
