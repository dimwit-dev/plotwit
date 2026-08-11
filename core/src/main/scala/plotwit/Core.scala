package plotwit

import io.circe.Json
import io.github.quafadas.plots.SetupVega.{_, given}
import viz.LowPriorityPlotTarget
import viz.VizReturn

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import scala.util.Using

object Core:

  type VegaJson = io.circe.Json

  sealed trait VegaLiteSpec

  case class UnitSpec(config: VegaJson) extends VegaLiteSpec

  case class LayerSpec(
      layer: Seq[VegaLiteSpec],
      sharedConfig: Option[VegaJson] = None
  ) extends VegaLiteSpec

  case class HConcatSpec(
      specs: Seq[VegaLiteSpec] = Seq.empty,
      sharedConfig: Option[VegaJson] = None
  ) extends VegaLiteSpec

  case class VConcatSpec(
      specs: Seq[VegaLiteSpec] = Seq.empty,
      sharedConfig: Option[VegaJson] = None
  ) extends VegaLiteSpec

  case class SliderSpec(
      plots: Seq[VegaLiteSpec],
      sharedConfig: Option[VegaJson] = None
  ) extends VegaLiteSpec

  case class VegaLiteRoot(
      spec: VegaLiteSpec,
      config: Option[VegaJson] = None,
      schema: String = "https://vega.github.io/schema/vega-lite/v6.json"
  )

  opaque type Grid <: VegaLiteSpec = VConcatSpec

  def overlay(plots: VegaLiteSpec*): LayerSpec = LayerSpec(plots)
  def hconcat(plots: Seq[VegaLiteSpec]): HConcatSpec = HConcatSpec(specs = plots)
  def vconcat(plots: Seq[VegaLiteSpec]): VConcatSpec = VConcatSpec(specs = plots)
  def slider(plots: Seq[VegaLiteSpec]): SliderSpec = SliderSpec(plots)

  class PlotTemplate[M0](val spec: VegaSpec[M0]):
    type M = M0

  private[plotwit] def stripSchema(json: VegaJson): VegaJson = json.mapObject(_.remove("$schema"))

  private[plotwit] def toJsonUnit(spec: VegaLiteSpec): VegaJson = spec match
    case UnitSpec(config)                => stripSchema(config)
    case LayerSpec(layers, sharedConfig) =>
      val base = Json.obj(
        "layer" -> Json.fromValues(layers.map(toJsonUnit))
      )
      sharedConfig.fold(base)(sc => sc.deepMerge(base))
    case HConcatSpec(specs, sharedConfig) =>
      val base = Json.obj("hconcat" -> Json.fromValues(specs.map(toJsonUnit)))
      sharedConfig.fold(base)(sc => sc.deepMerge(base))
    case VConcatSpec(specs, sharedConfig) =>
      val base = Json.obj("vconcat" -> Json.fromValues(specs.map(toJsonUnit)))
      sharedConfig.fold(base)(sc => sc.deepMerge(base))
    case SliderSpec(plots, sharedConfig) =>
      val maxIdx = Math.max(0, plots.size - 1)

      // 1. Create the global slider parameter
      val sliderParam = Json.obj(
        "name" -> Json.fromString("spec_slider"),
        "value" -> Json.fromInt(0),
        "bind" -> Json.obj(
          "input" -> Json.fromString("range"),
          "min" -> Json.fromInt(0),
          "max" -> Json.fromInt(maxIdx),
          "step" -> Json.fromInt(1),
          "name" -> Json.fromString("Plot Index: ")
        )
      )

      // 2. Map over the plots and inject a conditional opacity
      val layeredPlots = plots.zipWithIndex.map: (p, i) =>
        val pJson = toJsonUnit(p)
        val opacityInjection = Json.obj(
          "encoding" -> Json.obj(
            "opacity" -> Json.obj(
              "condition" -> Json.obj(
                "test" -> Json.fromString(s"spec_slider == $i"),
                "value" -> Json.fromInt(1)
              ),
              "value" -> Json.fromInt(0)
            )
          )
        )
        pJson.deepMerge(opacityInjection)

      // 3. Wrap them in a layer, fix the axes, and FORCE legend position
      val base = Json.obj(
        "params" -> Json.arr(sliderParam),
        "layer" -> Json.fromValues(layeredPlots),
        "resolve" -> Json.obj(
          "scale" -> Json.obj(
            "x" -> Json.fromString("independent"),
            "y" -> Json.fromString("independent")
            // Note: color is omitted here so they share ONE legend
          )
        ),
        // NEW: Force the legend outside the chart area
        "config" -> Json.obj(
          "legend" -> Json.obj(
            "orient" -> Json.fromString("right"),
            "offset" -> Json.fromInt(20) // Adjust this value if it needs more space
          )
        )
      )

      sharedConfig.fold(base)(sc => sc.deepMerge(base))

  private[plotwit] def toJsonRoot(spec: VegaLiteSpec): VegaJson = spec match
    case UnitSpec(config) => config
    case _                =>
      toJsonUnit(spec).mapObject(
        _.add(
          "$schema",
          Json.fromString(
            "https://vega.github.io/schema/vega-lite/v6.json"
          )
        )
      )

  def grid(rows: Seq[Seq[VegaLiteSpec]]): Grid = VConcatSpec(specs = rows.map(row => HConcatSpec(specs = row)))

  def display(spec: VegaLiteSpec)(using ev: LowPriorityPlotTarget): VizReturn =
    toJsonRoot(spec).plot()

  def displayAsImage(spec: VegaLiteSpec, scale: Double = 1.0): BufferedImage =
    val jsonString = toJsonRoot(spec).noSpaces
    val vl2pngCommand = f"vl2png -s $scale"
    val result = os.proc(vl2pngCommand.split(" ")).call(stdin = jsonString, check = false)
    Using(new ByteArrayInputStream(result.out.bytes)): inputStream =>
      ImageIO.read(inputStream)
    .get
