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

  lazy val template = new PlotTemplate(
    VegaPlot.fromString("""
      {
        "$schema": "https://vega.github.io/schema/vega-lite/v6.json",
        "description": "Line plot",
        "title": "",
        "data": { "values": [] },
        "mark": { "type": "line", "point": false, "interpolate": "linear" },
        "encoding": {
          "x": { 
            "field": "x", 
            "type": "quantitative", 
            "title": "X",
            "scale": { "domain": [] }
          },
          "y": { 
            "field": "y", 
            "type": "quantitative", 
            "title": "Y" 
          },
          "color": { 
            "field": "series", 
            "type": "nominal", 
            "legend": { "title": "Series" } 
          }
        }
      }""")
  )

  import template.{M, spec}

  /** A single line through the points (xs, ys). */
  def apply[S: Label](xs: Tensor1[S, Float32], ys: Tensor1[S, Float32], mods: (M => SpecMod)*): UnitSpec =
    apply(xs.toArray, Seq("" -> ys.toArray), Seq[M => SpecMod](_.encoding.color.legend := Json.Null) ++ mods)

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

    spec.build((Seq[M => SpecMod](
      _.data.values := jsonValues,
      _.encoding.x.scale.domain := List(xsArray.min, xsArray.max).asJson
    ) ++ mods)*)
