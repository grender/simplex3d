package example.simplex3d.procedural.texture

import simplex3d.math._
import simplex3d.math.double._
import simplex3d.math.double.functions._
import simplex3d.data._
import simplex3d.data.double._
import simplex3d.console.extension.ImageUtils._


/**
 * @author Aleksey Nikiforov (lex)
 */
object AnimationLoop extends App {

  val turbulence = new TiledTurbulence(
    tile = Vec4(3, 3, 1, 3),
    frequency = 1,
    octaves = 3, lacunarity = 2.0, persistence = 0.5,
    roundness = 0.3
  )
  val noise = (p: inVec2) => turbulence(p)

  animateFunction("Animation Loop (5 Seconds)") { (dims, time, pixel) =>
    val p = Vec3(pixel/100, time*0.2)
    val tiled = turbulence(p)*0.6
    Vec3(tiled)
  }

}
