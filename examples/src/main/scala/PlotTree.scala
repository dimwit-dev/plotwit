package plotwit.examples.paramtree

import dimwit.*
import plotwit.*

@main
def plotParamTreeExample(): Unit =

  dimwit.initialize()

  trait In derives Label
  trait Hidden derives Label
  trait Out derives Label

  case class DenseLayer(weights: Tensor2[In, Hidden, Float32], bias: Tensor1[Hidden, Float32]) derives TensorTree
  case class ResidualBlock(layer1: DenseLayer, layer2: DenseLayer) derives TensorTree
  case class MyModel(featureExtractor: ResidualBlock, classifier: DenseLayer) derives TensorTree

  val dummyWeights = Tensor2(Axis[In], Axis[Hidden]).fromArray(
    Array(Array(0.1f, 0.2f), Array(0.3f, 0.4f))
  )
  val dummyBias = Tensor1(Axis[Hidden]).fromArray(
    Array(0.01f, 0.02f)
  )

  val layer = DenseLayer(dummyWeights, dummyBias)
  val model = MyModel(
    featureExtractor = ResidualBlock(layer1 = layer, layer2 = layer),
    classifier = layer
  )

  val spec = treePlot(
    model,
    _.title := "MyModel Parameter Hierarchy",
    _.width := 900
  )

  import viz.PlotTargets.desktopBrowser
  display(spec)
