package plotwit.examples.heatequation

import dimwit.*
import dimwit.Conversions.given
import dimwit.stats.Normal
import plotwit.*
import plotwit.plotting.Plotting.*

@main
def main(): Unit =
  dimwit.initialize()

  trait X derives Label
  trait Y derives Label

  def stepFn(laplacianX: Tensor2[X, X, Float32], laplacianY: Tensor2[Y, Y, Float32], diffusivity: Double, dt: Double)(state: State): State =
    val diffusionX = state.dot(Axis[X])(laplacianX).transpose
    val diffusionY = state.dot(Axis[Y])(laplacianY)
    val delta = (diffusionX + diffusionY) *! diffusivity *! dt
    state + delta

  val nx = 128
  val ny = 128
  val diffusivity = 1.0 // often alpha
  val dt = 0.1

  def createLaplacianMatrix(n: Int): Array[Array[Float]] =
    val laplacian = Array.ofDim[Float](n * n)
    for i <- 0 until n do
      laplacian(i * n + i) = -2.0f
      if i > 0 then laplacian(i * n + (i - 1)) = 1.0f
      if i < n - 1 then laplacian(i * n + (i + 1)) = 1.0f
    laplacian.grouped(n).toArray

  val laplacianX = Tensor2(Axis[X], Axis[X]).fromArray(createLaplacianMatrix(nx))
  val laplacianY = Tensor2(Axis[Y], Axis[Y]).fromArray(createLaplacianMatrix(ny))

  val step = stepFn(laplacianX, laplacianY, diffusivity, dt)
  type State = Tensor2[X, Y, Float32]

  def circleOfHeat(nx: Int, ny: Int): Array[Array[Float]] =
    // Creates a circle of heat in the center of the grid
    val result = Array.ofDim[Float](nx, ny)
    val radius = 20
    for i <- 0 until nx do
      for j <- 0 until ny do
        val (dx, dy) = (i - nx / 2, j - ny / 2)
        val distance = Math.sqrt(dx * dx + dy * dy).toFloat
        if distance < radius then
          result(i)(j) = 1.0f - (distance / radius)
        else
          result(i)(j) = 0.0f
    result
  val initialState = Tensor2(Axis[X], Axis[Y]).fromArray(circleOfHeat(nx, ny))

  val (min, max) = (initialState.min, initialState.max)
  val states = LazyList.iterate(initialState)(step)

  val specs = states.zipWithIndex
    .filter((_, c) => c % 10 == 0) // Only plot every 10th state
    .map:
      case (t, c) =>
        val img = (t -! min) /! (max - min)
        image.plot(
          (img *! 255.0f).asInt(VType[UInt8]),
          _.title := f"$c"
        )

  import viz.PlotTargets.desktopBrowser
  display(grid(specs.take(50).grouped(10).toList))
