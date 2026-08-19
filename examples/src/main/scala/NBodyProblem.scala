package plotwit.examples.nbody

import dimwit.Conversions.given
import dimwit.*
import dimwit.stats.Uniform
import io.circe.syntax.*
import plotwit.*

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import scala.util.Using

trait Body derives Label
trait Position derives Label
trait Velocity derives Label
trait Acceleration derives Label

case class SystemState(
    val masses: Tensor1[Body, Float32],
    val positions: Tensor2[Body, Position, Float32],
    val velocities: Tensor2[Body, Velocity, Float32]
)

case class BodyState(
    val mass: Tensor0[Float32],
    val position: Tensor1[Position, Float32],
    val velocity: Tensor1[Velocity, Float32]
)

@main
def plot(): Unit =

  dimwit.initialize()

  // -- CONFIGURATION PARAMETERS --

  val numBodies = 5
  val numSpatialDimensions = 2
  val G = 1.0
  val dt = 0.01f
  val eps = 1e-9f

  // -- SIMULATION FUNCTIONS --

  def bodyStepFn(masses: Tensor1[Body, Float32], positions: Tensor2[Body, Position, Float32])(bodyState: BodyState): BodyState =
    import bodyState.{mass, position, velocity}
    val acceleration =
      val relativeOffsets = positions -! position
      val accelerationDirections = ((relativeOffsets /! (relativeOffsets.norm + eps)))
        .relabel(Axis[Position] -> Axis[Acceleration])
      val relativeSquaredDistances = relativeOffsets.pow(2).sum(Axis[Position]) +! eps
      val accelerationMagnitudes = masses / relativeSquaredDistances
      (accelerationDirections *! accelerationMagnitudes).sum(Axis[Body]) *! G
    val deltaPosition = (velocity *! dt).relabelTo(Axis[Position])
    val deltaVelocity = (acceleration *! dt).relabelTo(Axis[Velocity])
    BodyState(
      mass,
      position + deltaPosition,
      velocity + deltaVelocity
    )

  def systemStep(systemState: SystemState): SystemState =
    val bodyStep = bodyStepFn(systemState.masses, systemState.positions)
    val (newMasses, newPositions, newVelocities) = zipvmap(Axis[Body])((systemState.masses, systemState.positions, systemState.velocities)): (m, p, v) =>
      val newBodyState = bodyStep(BodyState(m, p, v))
      (newBodyState.mass, newBodyState.position, newBodyState.velocity)
    SystemState(newMasses, newPositions, newVelocities)
  val jitSystemStep = jitDonatingUnsafe(systemStep)

  def simulate(initialState: SystemState): Iterator[SystemState] =
    Iterator.iterate(initialState)(jitSystemStep)

  // -- RUN SIMULATION AND PLOT RESULTS --

  // Define initial state of the system
  val initialState =
    val key = Random.Key(42)
    val (posKey, velKey, massKey) = key.splitToTuple(3)
    val initialPositions =
      val positionShape = Shape(Axis[Body] -> numBodies, Axis[Position] -> numSpatialDimensions)
      Uniform(
        Tensor(positionShape).fill(-2.0f),
        Tensor(positionShape).fill(2.0f)
      ).sample(posKey)
    val initialVelocities =
      val velocityShape = Shape(Axis[Body] -> numBodies, Axis[Velocity] -> numSpatialDimensions)
      Uniform(
        Tensor(velocityShape).fill(-0.5f),
        Tensor(velocityShape).fill(0.5f)
      ).sample(velKey)
    val masses =
      val massShape = Shape(Axis[Body] -> numBodies)
      Uniform(
        Tensor(massShape).fill(1.0f),
        Tensor(massShape).fill(5.0f)
      ).sample(massKey)
    SystemState(masses, initialPositions, initialVelocities)

  // Run the simulation and collect the state trajectory to plot
  val stateTrajectory = simulate(initialState)
  val specTrajectory = stateTrajectory.zipWithIndex
    .map:
      case (state, i) =>
        plots.scatterPlot(
          xs = state.positions.slice(Axis[Position].at(0)),
          ys = state.positions.slice(Axis[Position].at(1)),
          size = state.masses,
          _.title := f"Step $i",
          _.encoding.x.scale.domain := List(-3, 3).asJson,
          _.encoding.y.scale.domain := List(-3, 3).asJson
        )

  import plotwit.PlotTargets.desktopBrowser
  display(slider(specTrajectory.take(1200).toSeq))
