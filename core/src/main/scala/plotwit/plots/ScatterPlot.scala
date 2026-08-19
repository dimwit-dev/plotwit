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

  /** The colour every point gets unless the plot is split into series. Matches the Vega-Lite default mark colour. */
  private val singleColor = "#4c78a8"

  lazy val template = new PlotTemplate(
    VegaPlot.fromString("""
      {
        "$schema": "https://vega.github.io/schema/vega-lite/v6.json",
        "description": "Scatter plot",
        "title": "",
        "data": { "values": [] },
        "mark": { "type": "circle", "filled": true },
        "encoding": {
          "x": { 
            "field": "x", 
            "type": "quantitative", 
            "title": "X" ,
            "scale": { "domain": [] }
          },
          "y": { 
            "field": "y", 
            "type": "quantitative", 
            "title": "Y",
            "scale": { "domain": [] }
          },
          "size": { 
            "field": "size", 
            "type": "quantitative", 
            "legend": null 
          },
          "color": { 
            "field": "series", 
            "type": "nominal", 
            "scale": { "scheme": "tableau10" },
            "legend": { "title": "Series" } 
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
      val point = Json.obj(
        "x" -> xsArray(i).asJson,
        "y" -> ysArray(i).asJson,
        "size" -> sizeArray.map(_(i)).getOrElse(30.0f).asJson
      )
      maybeSeries.fold(point)(series => Json.obj("series" -> Json.fromString(series(i))).deepMerge(point))
    .asJson

    // Without series names every point shares one colour, like matplotlib's default scatter.
    val colorMods: Seq[M => SpecMod] =
      if maybeSeries.isDefined then Seq.empty
      else Seq(_.encoding.color := Json.obj("value" -> Json.fromString(singleColor)))

    spec.build((colorMods ++ Seq[M => SpecMod](
      _.data.values := jsonValues,
      _.encoding.x.scale.domain := List(xs.min.item - 1, xs.max.item + 1).asJson,
      _.encoding.y.scale.domain := List(ys.min.item - 1, ys.max.item + 1).asJson
    ) ++ mods)*)
