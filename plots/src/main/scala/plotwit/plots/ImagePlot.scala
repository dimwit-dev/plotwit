package plotwit.plots

import dimwit._
import io.circe.Json
import io.github.quafadas.plots.SetupVega._
import plotwit.Core._
import viz.macros.ArrField
import viz.macros.ObjField
import viz.macros.SpecMod

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

import scala.annotation.targetName

object imagePlot:

  private lazy val template = new PlotTemplate(
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

  import template.{M, spec}

  /** A greyscale image, given as pixel intensities. */
  def apply[Width: Label, Height: Label](img: Tensor2[Width, Height, UInt8], mods: (M => SpecMod)*): UnitSpec = UnitSpec:

    val width = img.shape(Axis[Width])
    val height = img.shape(Axis[Height])

    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
    val data = img.asInt32.toArray
    for x <- 0 until width; y <- 0 until height do
      val pixelValue = data(x)(y).toInt
      val rgb = (pixelValue << 16) | (pixelValue << 8) | pixelValue
      bufferedImage.setRGB(x, y, rgb)

    build(bufferedImage, width, height, mods)

  /** A colour image, given as pixel intensities per channel. The channel axis holds either three (red, green, blue) or
    * four (red, green, blue, alpha) values.
    */
  @targetName("applyColor")
  def apply[Width: Label, Height: Label, Channel: Label](
      img: Tensor3[Width, Height, Channel, UInt8],
      mods: (M => SpecMod)*
  ): UnitSpec = UnitSpec:

    val width = img.shape(Axis[Width])
    val height = img.shape(Axis[Height])
    val channels = img.shape(Axis[Channel])
    require(
      channels == 3 || channels == 4,
      s"A colour image needs 3 (RGB) or 4 (RGBA) channels, but the ${summon[Label[Channel]].name} axis has $channels."
    )

    val imageType = if channels == 4 then BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
    val bufferedImage = new BufferedImage(width, height, imageType)
    val data = img.asInt32.toArray
    for x <- 0 until width; y <- 0 until height do
      val pixel = data(x)(y)
      val alpha = if channels == 4 then pixel(3) else 255
      val argb = (alpha << 24) | (pixel(0) << 16) | (pixel(1) << 8) | pixel(2)
      bufferedImage.setRGB(x, y, argb)

    build(bufferedImage, width, height, mods)

  /** Embeds the image in the spec as a base64 encoded PNG data URI. */
  private def build(bufferedImage: BufferedImage, width: Int, height: Int, mods: Seq[M => SpecMod]) =
    val baos = new ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "png", baos)

    val base64Str = Base64.getEncoder.encodeToString(baos.toByteArray)
    val dataUri = s"data:image/png;base64,$base64Str"

    spec.build((Seq[M => SpecMod](
      _.data.values := Json.fromValues(
        Iterable(
          Json.obj("image_url" -> Json.fromString(dataUri))
        )
      ),
      _.mark.width := width,
      _.mark.height := height
    ) ++ mods)*)
