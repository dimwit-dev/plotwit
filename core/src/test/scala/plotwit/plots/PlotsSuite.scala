package plotwit.plots

import dimwit._
import plotwit._

class HistogramPlotSuite extends DimwitTestSuite:

  describe("plotHistogram"):
    it("should generate a Vega-Lite spec with a custom title and mapped data"):
      val data = Tensor1(Axis[A]).fromArray(Array(1.0f, 2.5f, 2.5f, 4.0f))
      val spec = histogramPlot(
        data,
        _.title := "Histogram of Values",
        _.mark.`type` := "area"
      )

      val specString = spec.toString

      specString should include("Histogram of Values")
      specString should include("1.0")
      specString should include("2.5")
      specString should include("4.0")
      specString should include("area")

  describe("plotHeatmap"):
    it("should generate a Vega-Lite spec with a heatmap grid and custom title"):
      val data = Tensor2(Axis[A], Axis[B]).fromArray(
        Array(Array(1.0f, 2.0f), Array(3.0f, 4.0f))
      )
      val spec = heatmapPlot(
        data,
        _.title := "Heatmap Matrix",
        _.encoding.x.title := "X Axis Title",
        _.encoding.x.axis.labelAngle := 90
      )

      val specString = spec.toString

      specString should include("Heatmap Matrix")
      specString should include("X Axis Title")
      specString should include("rect")
      specString should include("1.0")
      specString should include("4.0")

  describe("plotLine"):
    it("should generate a Vega-Lite spec with one named line per series"):
      val xs = Tensor1(Axis[A]).fromArray(Array(0.0f, 1.0f, 2.0f))
      val ys = Tensor2(Axis[B], Axis[A]).fromArray(
        Array(Array(0.0f, 1.0f, 4.0f), Array(0.0f, -1.0f, -4.0f))
      )
      val spec = linePlot(
        xs,
        ys,
        Seq("up", "down"),
        _.title := "Two Lines",
        _.encoding.y.title := "Y Axis Title"
      )

      val specString = spec.toString

      specString should include("Two Lines")
      specString should include("Y Axis Title")
      specString should include("line")
      specString should include("up")
      specString should include("down")
      specString should include("-4.0")

  describe("plotScatter"):
    it("should colour every point the same by default"):
      val xs = Tensor1(Axis[A]).fromArray(Array(0.0f, 1.0f, 2.0f))
      val ys = Tensor1(Axis[A]).fromArray(Array(0.0f, 1.0f, 4.0f))
      val spec = scatterPlot(xs, ys, _.title := "Scattered")

      val specString = spec.toString

      specString should include("Scattered")
      specString should include("circle")
      specString should include("#4c78a8")
      specString should not include ("series")

    it("should colour points by series when series names are given"):
      val xs = Tensor1(Axis[A]).fromArray(Array(0.0f, 1.0f, 2.0f))
      val ys = Tensor1(Axis[A]).fromArray(Array(0.0f, 1.0f, 4.0f))
      val sizes = Tensor1(Axis[A]).fromArray(Array(10.0f, 20.0f, 30.0f))
      val spec = scatterPlot(xs, ys, sizes, Seq("a", "b", "c"))

      val specString = spec.toString

      specString should include("series")
      specString should include("nominal")
      specString should include("\"a\"")
      specString should include("\"c\"")
      specString should include("30.0")

    it("should reject a series list that does not match the number of points"):
      val xs = Tensor1(Axis[A]).fromArray(Array(0.0f, 1.0f, 2.0f))
      val ys = Tensor1(Axis[A]).fromArray(Array(0.0f, 1.0f, 4.0f))

      an[IllegalArgumentException] should be thrownBy scatterPlot(xs, ys, Seq("a", "b")).toString
