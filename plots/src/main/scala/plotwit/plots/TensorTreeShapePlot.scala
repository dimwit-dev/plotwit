package plotwit.plots

import dimwit._
import io.circe.Json
import io.github.quafadas.plots.SetupVega._
import plotwit.Core._
import viz.macros.ArrField
import viz.macros.ObjField
import viz.macros.SpecMod

object tensorTreeShapePlot:

  lazy val template = new PlotTemplate(
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

  import template.{M, spec}
  def apply[P](data: P, mods: (M => SpecMod)*)(using tt: TensorTree[P]): UnitSpec = UnitSpec:
    spec.build((encodeData(data) ++ mods)*)

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
