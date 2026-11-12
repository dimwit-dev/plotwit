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

object scatterPlot:

  /** Every property is spelled out with the value Vega-Lite would choose, so that each of them can be changed with a
    * mod. Where Vega-Lite derives a value from the data (an axis domain, say), the plot fills it in.
    */
  lazy val template = new PlotTemplate(
    VegaPlot.fromString("""
      {
        "$schema": "https://vega.github.io/schema/vega-lite/v6.json",
        "description": "Scatter plot",
        "title": "",
        "width": 200,
        "height": 200,
        "data": { "values": [] },
        "mark": {
          "type": "circle",
          "filled": true,
          "color": "#4c78a8",
          "size": 30,
          "opacity": 0.7,
          "strokeWidth": 2,
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
              "domain": [],
              "zero": false,
              "nice": false,
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
          "size": {
            "field": "size",
            "type": "quantitative",
            "scale": {
              "type": "linear",
              "zero": true,
              "range": [0, 361]
            },
            "legend": null
          },
          "color": {
            "field": "series",
            "type": "nominal",
            "scale": { "type": "ordinal", "scheme": "tableau10" },
            "legend": {
              "title": "Series",
              "orient": "right",
              "direction": "vertical",
              "symbolType": "circle",
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

  /** The points (xs, ys), all in the same colour. */
  def apply[S: Label](xs: Tensor1[S, Float32], ys: Tensor1[S, Float32], mods: (M => SpecMod)*): UnitSpec =
    apply(xs, ys, None, None, mods)

  /** The points (xs, ys), all in the same colour, with the size of each point given by `size`. */
  def apply[S: Label](
      xs: Tensor1[S, Float32],
      ys: Tensor1[S, Float32],
      size: Tensor1[S, Float32],
      mods: (M => SpecMod)*
  ): UnitSpec =
    apply(xs, ys, Some(size), None, mods)

  /** The points (xs, ys), coloured by the series each point belongs to. */
  @targetName("applySeries")
  def apply[S: Label](
      xs: Tensor1[S, Float32],
      ys: Tensor1[S, Float32],
      series: Seq[String],
      mods: (M => SpecMod)*
  ): UnitSpec =
    apply(xs, ys, None, Some(series), mods)

  /** The points (xs, ys), sized by `size` and coloured by the series each point belongs to. */
  @targetName("applySizedSeries")
  def apply[S: Label](
      xs: Tensor1[S, Float32],
      ys: Tensor1[S, Float32],
      size: Tensor1[S, Float32],
      series: Seq[String],
      mods: (M => SpecMod)*
  ): UnitSpec =
    apply(xs, ys, Some(size), Some(series), mods)

  private def apply[S: Label](
      xs: Tensor1[S, Float32],
      ys: Tensor1[S, Float32],
      maybeSizes: Option[Tensor1[S, Float32]],
      maybeSeries: Option[Seq[String]],
      mods: Seq[M => SpecMod]
  ): UnitSpec = UnitSpec:
    val numPoints = xs.shape(Axis[S])
    maybeSeries.foreach: series =>
      require(series.size == numPoints, s"Expected $numPoints series names, got ${series.size}")
    val xsArray = xs.toArray
    val ysArray = ys.toArray
    val sizeArray = maybeSizes.map(_.toArray)

    val jsonValues = (0 until numPoints).map: i =>
      val point = Json.obj("x" -> xsArray(i).asJson, "y" -> ysArray(i).asJson)
      val withSize = sizeArray.fold(point)(sizes => point.deepMerge(Json.obj("size" -> sizes(i).asJson)))
      maybeSeries.fold(withSize)(series => withSize.deepMerge(Json.obj("series" -> Json.fromString(series(i)))))
    .asJson

    // Without sizes or series names, the channels are dropped, so that `mark.size` and `mark.color` apply to every point.
    val dropSize: Seq[M => SpecMod] = if maybeSizes.isEmpty then Seq(_.encoding.size := Json.Null) else Seq.empty
    val dropColor: Seq[M => SpecMod] = if maybeSeries.isEmpty then Seq(_.encoding.color := Json.Null) else Seq.empty
    val channelMods = dropSize ++ dropColor

    dropNullChannels(spec.build((channelMods ++ Seq[M => SpecMod](
      _.data.values := jsonValues,
      _.encoding.x.scale.domain := List(xs.min.item - 1, xs.max.item + 1).asJson,
      _.encoding.y.scale.domain := List(ys.min.item - 1, ys.max.item + 1).asJson
    ) ++ mods)*))
