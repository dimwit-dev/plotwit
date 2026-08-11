package plotwit.examples.nbody

import dimwit.Conversions.given
import dimwit._
import dimwit.stats.Uniform
import io.circe.syntax._
import plotwit._

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import scala.util.Using

object NBodyProblem:

  dimwit.initialize()

  trait Body derives Label
  trait Spatial derives Label

  val numBodies = 5
  val numSpatialDimensions = 2

  val G = 1.0
  val dt = 0.01f
  val eps = 1e-9f

  case class BodyState(mass: Tensor0[Float32], position: Tensor1[Spatial, Float32], velocity: Tensor1[Spatial, Float32]):
    def next(acceleration: Tensor1[Spatial, Float32]): BodyState =
      BodyState(
        mass,
        position + (velocity *! dt),
        velocity + (acceleration *! dt)
      )

  def stepFor(state: BodyState, masses: Tensor1[Body, Float32], positions: Tensor2[Body, Spatial, Float32]): BodyState =
    // Calculate the acceleration based on the other bodies' positions and masses
    val relativeOffsets = positions -! state.position
    val relativeDirections = relativeOffsets /! (relativeOffsets.norm + eps)
    val relativeSquaredDistances = relativeOffsets.pow(2).sum(Axis[Spatial]) +! eps
    val accelerationMagnitudes = masses / relativeSquaredDistances
    val acceleration = (relativeDirections *! accelerationMagnitudes).sum(Axis[Body]) *! G
    // Apply the acceleration to update the body's state
    state.next(acceleration)

  def step(masses: Tensor1[Body, Float32], positions: Tensor2[Body, Spatial, Float32], velocities: Tensor2[Body, Spatial, Float32]): (Tensor1[Body, Float32], Tensor2[Body, Spatial, Float32], Tensor2[Body, Spatial, Float32]) =
    zipvmap(Axis[Body])(masses, positions, velocities): (m, p, v) =>
      val state = BodyState(m, p, v)
      val newState = stepFor(state, masses, positions)
      (newState.mass, newState.position, newState.velocity)
  val jitStep = jitDonatingUnsafe(step)

  // --- Initialization (Boilerplate) ---
  val key = Random.Key(42)
  val (posKey, velKey, massKey) = key.splitToTuple(3)
  val shape = Shape(Axis[Body] -> numBodies, Axis[Spatial] -> numSpatialDimensions)
  val initialPositions = Uniform(
    Tensor(shape).fill(-2.0f),
    Tensor(shape).fill(2.0f)
  ).sample(posKey)
  val initialVelocities = Uniform(
    Tensor(shape).fill(-0.5f),
    Tensor(shape).fill(0.5f)
  ).sample(velKey)
  val masses = Uniform(
    Tensor(Shape(Axis[Body] -> numBodies)).fill(1.0f),
    Tensor(Shape(Axis[Body] -> numBodies)).fill(5.0f)
  ).sample(massKey)

  // Run a few steps
  val states: Iterator[(
      masses: Tensor1[Body, Float32],
      positions: Tensor2[Body, Spatial, Float32],
      velocities: Tensor2[Body, Spatial, Float32]
  )] =
    Iterator.iterate((masses, initialPositions, initialVelocities))(jitStep(_, _, _))

  @main
  def speed(): Unit =
    dimwit.initialize()

    // --- Initialization (Boilerplate) ---
    val key = Random.Key(42)
    val (posKey, velKey, massKey) = key.splitToTuple(3)
    val shape = Shape(Axis[Body] -> numBodies, Axis[Spatial] -> numSpatialDimensions)
    val initialPositions = Uniform(
      Tensor(shape).fill(-2.0f),
      Tensor(shape).fill(2.0f)
    ).sample(posKey)
    val initialVelocities = Uniform(
      Tensor(shape).fill(-0.5f),
      Tensor(shape).fill(0.5f)
    ).sample(velKey)
    val masses = Uniform(
      Tensor(Shape(Axis[Body] -> numBodies)).fill(1.0f),
      Tensor(Shape(Axis[Body] -> numBodies)).fill(5.0f)
    ).sample(massKey)

    // Speed-check
    val startTime = System.currentTimeMillis()
    states.take(10_000).foreach(_ => ())
    println(f"Time taken to compute 10,000 steps: ${System.currentTimeMillis() - startTime} ms")

  @main
  def plot(): Unit =
    dimwit.initialize()

    val specs = states.zipWithIndex
      .map:
        case (state, i) =>
          val xs = state.positions.slice(Axis[Spatial].at(0))
          val ys = state.positions.slice(Axis[Spatial].at(1))
          plots.scatterPlot(
            xs,
            ys,
            state.masses,
            _.title := f"Step $i",
            _.encoding.x.scale.domain := List(-3, 3).asJson,
            _.encoding.y.scale.domain := List(-3, 3).asJson
          )

    import plotwit.PlotTargets.desktopBrowser
    display(slider(LazyList.from(specs).take(1200)))

    // Export to Animated PNG
    // val images = specs.map(displayAsImage(_)).take(600).toList
    // createAnimation(images, os.pwd / "nbody_animation.gif", fps = 30)

def createAnimation(images: Seq[BufferedImage], outputPath: os.Path, fps: Int): Boolean =
  val cmdString = s"ffmpeg -y -f image2pipe -vcodec png -r $fps -i - -vf split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse -loop 0"
  val ffmpegCmd = cmdString.split(" ").toSeq :+ outputPath.toString
  val subProcess = os.proc(ffmpegCmd).spawn()
  Using(subProcess.stdin): stdin =>
    images.foreach(ImageIO.write(_, "png", stdin))
  subProcess.join()
