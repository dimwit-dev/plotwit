package plotwit.plots

import dimwit._
import io.circe.Json
import io.circe.syntax._
import io.github.quafadas.plots.SetupVega._
import plotwit.Core._
import viz.macros.ArrField
import viz.macros.ObjField
import viz.macros.SpecMod

object scatterPlot:

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
            "field": "particle_id", 
            "type": "nominal", 
            "legend": null 
          }
        }
      }""")
  )

  import template.{M, spec}

  def apply[S: Label](xs: Tensor1[S, Float32], ys: Tensor1[S, Float32], mods: (M => SpecMod)*): UnitSpec =
    apply(xs, ys, None, mods)

  def apply[S: Label](xs: Tensor1[S, Float32], ys: Tensor1[S, Float32], size: Tensor1[S, Float32], mods: (M => SpecMod)*): UnitSpec =
    apply(xs, ys, Some(size), mods)

  private def apply[S: Label](
      xs: Tensor1[S, Float32],
      ys: Tensor1[S, Float32],
      maybeSizes: Option[Tensor1[S, Float32]],
      mods: Seq[M => SpecMod]
  ): UnitSpec = UnitSpec:
    val numParticles = xs.shape(Axis[S])
    val xsArray = xs.toArray
    val ysArray = ys.toArray
    val sizeArray = maybeSizes.map(_.toArray)

    spec.build((Seq[M => SpecMod](
      _.data.values := (0 until numParticles).map: i =>
        Json.obj(
          "particle_id" -> Json.fromString(s"P_$i"),
          "x" -> xsArray(i).asJson,
          "y" -> ysArray(i).asJson,
          "size" -> sizeArray.map(_(i)).getOrElse(30.0f).asJson
        )
      .asJson,
      _.encoding.x.scale.domain := List(xs.min.item - 1, xs.max.item + 1).asJson,
      _.encoding.y.scale.domain := List(ys.min.item - 1, ys.max.item + 1).asJson
    ) ++ mods)*)
