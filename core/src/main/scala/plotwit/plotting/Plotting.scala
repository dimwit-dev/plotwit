package plotwit.plotting

import viz.vega.plots.SpecUrl
import io.github.quafadas.plots.SetupVega.{*, given}
import dimwit.*
import io.circe.syntax.*
import io.circe.literal.*
import viz.ChartLibrary.Vega

import viz.extensions.*
import viz.Utils
import viz.VizReturn
import dimwit.tensor.Tensor4
import viz.LowPriorityPlotTarget

import viz.macros.SpecMod
import viz.macros.ObjField
import viz.macros.ArrField

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

import io.circe.Json

import java.io.ByteArrayInputStream
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import scala.util.Using

object Plotting:

  type VegaJson = Json

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

  class PlotTemplate[M0](val template: VegaSpec[M0]):
    type M = M0

  lazy val histogramPlotTemplate = new PlotTemplate(
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
  object histogramPlot:
    import histogramPlotTemplate.{M, template}
    def apply[L: Label](data: Tensor1[L, Float32], mods: (M => SpecMod)*): UnitSpec = UnitSpec:
      template.build((
        Seq[M => SpecMod](
          _.data.values := data.toArray.map: v =>
            Map("value" -> v)
          .asJson
        ) ++ mods
      )*)

  private lazy val heatmapPlotTemplate = new PlotTemplate(
    VegaPlot.fromString("""
      {
        "$schema": "https://vega.github.io/schema/vega-lite/v6.json",
        "description": "Heatmap created from Tensor2 data.",
        "title": "",
        "data": { "values": [] },
        "mark": "rect",
        "encoding": {
          "x": { 
            "field": "x", 
            "type": "ordinal", 
            "title": "",
            "axis": { "labelAngle": 0 }
          },
          "y": { 
            "field": "y", 
            "type": "ordinal", 
            "title": "" 
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

  object heatmapPlot:
    import heatmapPlotTemplate.{M, template}
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
      template.build((Seq[M => SpecMod](_.data.values := jsonValues) ++ mods)*)

  private lazy val imagePlotTemplate = new PlotTemplate(
    VegaPlot.fromString("""{
      "$schema": "https://vega.github.io/schema/vega-lite/v6.json",
      "title": "Image",
      "description": "Image created from data.",
      "data": { "values": [] },
      "mark": { "type": "image", "width": 300, "height": 300, "smooth": false },
      "encoding": {
        "url": {"field": "image_url", "type": "nominal"}
      }
    }""")
  )

  object imagePlot:
    import imagePlotTemplate.{M, template}

    def apply[Width: Label, Height: Label](img: Tensor2[Width, Height, UInt8], mods: (M => SpecMod)*): UnitSpec = UnitSpec:

      val width = img.shape(Axis[Width])
      val height = img.shape(Axis[Height])

      val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
      val data = img.asInt32.toArray
      for x <- 0 until width; y <- 0 until height do
        val pixelValue = data(x)(y).toInt
        val rgb = (pixelValue << 16) | (pixelValue << 8) | pixelValue
        bufferedImage.setRGB(x, y, rgb)

      val baos = new ByteArrayOutputStream()
      ImageIO.write(bufferedImage, "png", baos)

      val base64Str = Base64.getEncoder.encodeToString(baos.toByteArray)
      val dataUri = s"data:image/png;base64,$base64Str"

      template.build((Seq[M => SpecMod](
        _.data.values := Json.fromValues(
          Iterable(
            Json.obj("image_url" -> Json.fromString(dataUri))
          )
        ),
        _.mark.width := width,
        _.mark.height := height
      ) ++ mods)*)

  lazy val treePlotTemplate = new PlotTemplate(
    VegaPlot.fromString("""
      {
        "$schema": "https://vega.github.io/schema/vega/v5.json",
        "title": "TensorTree Hierarchy",
        "description": "Tree layout for DimWit TensorTree structures",
        "width": 800,
        "height": 600,
        "padding": 20,
        "data": [
          {
            "name": "tree",
            "values": [],
            "transform": [
              {
                "type": "stratify",
                "key": "id",
                "parentKey": "parent"
              },
              {
                "type": "tree",
                "method": "tidy",
                "size": [{"signal": "height"}, {"signal": "width - 150"}],
                "separation": false,
                "as": ["y", "x", "depth", "children"]
              }
            ]
          },
          {
            "name": "links",
            "source": "tree",
            "transform": [
              { "type": "treelinks" },
              { "type": "linkpath", "orient": "horizontal", "shape": "diagonal" }
            ]
          }
        ],
        "scales": [
          {
            "name": "color",
            "type": "linear",
            "range": {"scheme": "tealblues"},
            "domain": {"data": "tree", "field": "depth"},
            "zero": true
          }
        ],
        "marks": [
          {
            "type": "path",
            "from": {"data": "links"},
            "encode": {
              "update": {
                "path": {"field": "path"},
                "stroke": {"value": "#ccc"}
              }
            }
          },
          {
            "type": "symbol",
            "from": {"data": "tree"},
            "encode": {
              "enter": {
                "size": {"value": 100},
                "stroke": {"value": "#fff"}
              },
              "update": {
                "x": {"field": "x"},
                "y": {"field": "y"},
                "fill": {"scale": "color", "field": "depth"}
              }
            }
          },
          {
            "type": "text",
            "from": {"data": "tree"},
            "encode": {
              "enter": {
                "text": {"field": "name"},
                "fontSize": {"value": 11},
                "baseline": {"value": "middle"}
              },
              "update": {
                "x": {"field": "x"},
                "y": {"field": "y"},
                "dx": {"signal": "datum.children ? -8 : 8"},
                "align": {"signal": "datum.children ? 'right' : 'left'"}
              }
            }
          }
        ]
      }""")
  )

  object treePlot:
    import treePlotTemplate.{M, template}
    def apply[P](data: P, mods: (M => SpecMod)*)(using tt: TensorTree[P]): UnitSpec = UnitSpec:
      template.build((encodeData(data) ++ mods)*)

    def encodeData[P](data: P)(using tt: TensorTree[P]): Seq[M => SpecMod] =
      // 1. Gather all leaves and their label info
      val leaves = scala.collection.mutable.ListBuffer[(String, String)]()
      tt.foreachWithName(
        data,
        [T <: Tuple, V] =>
          (l: Labels[T]) ?=>
            (path: String, t: Tensor[T, V]) =>
              val shapeStr = t.shape.toString
              leaves += ((if path.isEmpty then "root" else path) -> s"Tensor$shapeStr")
      )

      // 2. Build the hierarchical graph nodes
      val nodeMap = scala.collection.mutable.Map[String, Json]()

      // Initialize the root node
      nodeMap("root") = Json.obj(
        "id" -> Json.fromString("root"),
        "parent" -> Json.Null,
        "name" -> Json.fromString("root")
      )

      for (path, details) <- leaves do
        // Normalize paths (e.g. replace list indices "list[0]" -> "list.0")
        val normPath = path.replace("[", ".").replace("]", "").stripPrefix(".")
        val parts = normPath.split("\\.").filter(_.nonEmpty)

        var currentPath = "root"
        for i <- parts.indices do
          val part = parts(i)
          val nextPath = if currentPath == "root" then part else s"$currentPath.$part"
          val isLeaf = (i == parts.length - 1)

          if !nodeMap.contains(nextPath) then
            nodeMap(nextPath) = Json.obj(
              "id" -> Json.fromString(nextPath),
              "parent" -> Json.fromString(currentPath),
              "name" -> Json.fromString(if isLeaf then s"$part: $details" else part)
            )
          else if isLeaf then
            // If the node already exists but is a leaf, append the tensor details
            nodeMap(nextPath) = Json.obj(
              "id" -> Json.fromString(nextPath),
              "parent" -> Json.fromString(currentPath),
              "name" -> Json.fromString(s"$part: $details")
            )

          currentPath = nextPath

      val jsonValues = Json.fromValues(nodeMap.values)

      // 3. Inject into the "tree" data array (Vega v5 format)
      // Note: Since Vega v5 expects `data` to be an array, we target `.head.values`.
      // Adjust this to `_.data(0).values` depending on your specific Vega wrapper's generated code.
      Seq(_.data.head.values := jsonValues)

  lazy val scatterPlotTemplate = new PlotTemplate(
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
  object scatterPlot:
    import scatterPlotTemplate.{M, template}
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

      template.build((Seq[M => SpecMod](
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

  private def stripSchema(json: VegaJson): VegaJson = json.mapObject(_.remove("$schema"))

  private def toJsonUnit(spec: VegaLiteSpec): VegaJson = spec match
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

  private def toJsonRoot(spec: VegaLiteSpec): VegaJson = spec match
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
