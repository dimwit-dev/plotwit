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

  opaque type Grid = VConcatSpec

  def overlay(plots: VegaLiteSpec*): LayerSpec = LayerSpec(plots)
  def hconcat(plots: Seq[VegaLiteSpec]): HConcatSpec = HConcatSpec(specs = plots)
  def vconcat(plots: Seq[VegaLiteSpec]): VConcatSpec = VConcatSpec(specs = plots)
  def slider(plots: Seq[VegaLiteSpec]): SliderSpec = SliderSpec(plots)

  trait Plot1[M, MData](val template: VegaSpec[M & MData]):

    def encodeData[L: Label](data: Tensor1[L, Float32]): Seq[MData => SpecMod]

    def plot[L: Label](data: Tensor1[L, Float32], mods: (M => SpecMod)*): UnitSpec = UnitSpec:
      template.build((encodeData(data) ++ mods)*)

  trait Plot2[M, MData](val template: VegaSpec[M & MData]):

    def encodeData[L1: Label, L2: Label](data: Tensor2[L1, L2, Float32]): Seq[MData => SpecMod]

    def plot[L1: Label, L2: Label](data: Tensor2[L1, L2, Float32], mods: (M => SpecMod)*): UnitSpec = UnitSpec:
      template.build((encodeData(data) ++ mods)*)

  trait Plot2UInt8[M, MData](val template: VegaSpec[M & MData]):

    def encodeData[L1: Label, L2: Label](data: Tensor2[L1, L2, UInt8]): Seq[MData => SpecMod]

    def plot[L1: Label, L2: Label](data: Tensor2[L1, L2, UInt8], mods: (M => SpecMod)*): UnitSpec = UnitSpec:
      template.build((encodeData(data) ++ mods)*)

  lazy val histogram = new Plot1(
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
  ):
    def encodeData[L: Label](data: Tensor1[L, Float32]) = Seq:
      _.data.values := Json.fromValues(data.toArray.map: v =>
        Json.obj("value" -> Json.fromFloatOrNull(v)))

  lazy val heatmap = new Plot2(
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
  ):
    def encodeData[L1: Label, L2: Label](data: Tensor2[L1, L2, Float32]) =
      val rows = data.shape(Axis[L1])
      val cols = data.shape(Axis[L2])
      val array = data.toArray
      val jsonValues = Json.fromValues(
        for
          r <- 0 until rows
          c <- 0 until cols
        yield Json.obj(
          "x" -> Json.fromInt(c),
          "y" -> Json.fromInt(r),
          "value" -> Json.fromFloatOrNull(array(r)(c))
        )
      )
      Seq(_.data.values := jsonValues)

  lazy val image = new Plot2UInt8(
    VegaPlot.fromString("""{
      "$schema": "https://vega.github.io/schema/vega-lite/v6.json",
      "title": "Image",
      "description": "Image created from data.",
      "data": { "values": [] },
      "mark": { "type": "image", "width": 300, "height": 300 },
      "encoding": {
        "url": {"field": "image_url", "type": "nominal"}
      }
    }""")
  ):
    def encodeData[L1: Label, L2: Label](img: Tensor2[L1, L2, UInt8]) =
      val width = img.shape(Axis[L1])
      val height = img.shape(Axis[L2])

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

      Seq(
        _.data.values := Json.fromValues(
          Iterable(
            Json.obj("image_url" -> Json.fromString(dataUri))
          )
        ),
        _.mark.width := width,
        _.mark.height := height
      )

  trait PlotTree[M, MData](val template: VegaSpec[M & MData]):
    /** Encodes a generic TensorTree structure into Vega-compatible hierarchical data */
    def encodeData[P](data: P)(using tt: TensorTree[P]): Seq[MData => SpecMod]

    def plot[P](data: P, mods: (M => SpecMod)*)(using tt: TensorTree[P]): UnitSpec = UnitSpec:
      template.build((encodeData(data) ++ mods)*)

  lazy val tensorTreePlot = new PlotTree(
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
  ):
    def encodeData[P](data: P)(using tt: TensorTree[P]) =
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

  def grid(rows: List[List[VegaLiteSpec]]): Grid = VConcatSpec(specs = rows.map(row => HConcatSpec(specs = row)))

  def display(spec: VegaLiteSpec)(using ev: LowPriorityPlotTarget): VizReturn =
    toJsonRoot(spec).plot()
