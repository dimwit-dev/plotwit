package src.main.scala

import dimwit.*
import dimwit.Conversions.given
import plotwit.*
import plotwit.plotting.Plotting
import plotwit.plotting.Plotting.*
import dimwit.stats.Normal

@main
def plotImages(): Unit =

  dimwit.initialize()

  trait Width derives Label
  trait Height derives Label

  val imgShape = Shape(Axis[Width] -> 256, Axis[Height] -> 128)
  val imgDist = Normal.standardNormal(imgShape)

  val keys = (0 until 5).map(i => Key(i))
  val imgs = keys.map: key =>
    val img = imgDist.sample(key)
    val normalizedImg = (img -! img.min) /! (img.max - img.min)
    (normalizedImg *! 255f).asInt(VType[UInt8])

  val specs = imgs.zipWithIndex.map: (img, idx) =>
    Plotting.image.plot(
      img,
      _.title := f"Random Normal Image $idx"
    )

  import viz.PlotTargets.desktopBrowser
  Plotting.display(hconcat(specs))
