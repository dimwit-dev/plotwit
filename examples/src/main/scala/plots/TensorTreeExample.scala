package plotwit.examples.plots

import dimwit.*
import plotwit.*

/** The parameters of a small model, as a tree of named tensors. */
object TensorTreeExample:

  trait In derives Label
  trait Hidden derives Label
  trait Out derives Label

  case class DenseLayer(weights: Tensor2[In, Hidden, Float32], bias: Tensor1[Hidden, Float32]) derives TensorTree
  case class ResidualBlock(layer1: DenseLayer, layer2: DenseLayer) derives TensorTree
  case class MyModel(featureExtractor: ResidualBlock, classifier: DenseLayer) derives TensorTree

  def spec: VegaLiteSpec =
    val layer = DenseLayer(
      weights = Tensor2(Axis[In], Axis[Hidden]).fromArray(Array(Array(0.1f, 0.2f), Array(0.3f, 0.4f))),
      bias = Tensor1(Axis[Hidden]).fromArray(Array(0.01f, 0.02f))
    )
    val model = MyModel(
      featureExtractor = ResidualBlock(layer1 = layer, layer2 = layer),
      classifier = layer
    )

    plots.tensorTreeShapePlot(
      model,
      _.title := "MyModel parameter hierarchy",
      _.width := 900,
      _.height := 400
    )
