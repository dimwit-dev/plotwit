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

  def apply[Width: Label, Height: Label](img: Tensor2[Width, Height, UInt8], mods: (M => SpecMod)*): UnitSpec = UnitSpec:

    val width = img.shape(Axis[Width])
    val height = img.shape(Axis[Height])

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

    spec.build((Seq[M => SpecMod](
      _.data.values := Json.fromValues(
        Iterable(
          Json.obj("image_url" -> Json.fromString(dataUri))
        )
      ),
      _.mark.width := width,
      _.mark.height := height
    ) ++ mods)*)
