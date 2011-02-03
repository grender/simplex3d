/*
 * Simplex3d, MathTest package
 * Copyright (C) 2010-2011, Aleksey Nikiforov
 *
 * This file is part of Simplex3dMathTest.
 *
 * Simplex3dMathTest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Simplex3dMathTest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package visual.math

import visual.math.draw._

import simplex3d.math.double._
import simplex3d.math.doublex.functions._


/**
 * @author Aleksey Nikiforov (lex)
 */
object NoiseDegradation {

  // Shows gradual noise degradation at large offsets.
  def main(args: Array[String]) {
    val scale = 1.0/50
    val noiseSpeed = 1.0/3
    val scrollSpeed = 10

    val minOffset = 5e13
    val maxOffset = 1e15
    val incrementFactor = 3.0
    val timePerSlice = 3
    val numSlices = (maxOffset/minOffset/incrementFactor).toInt

    Launcher.launch(new Function {
    final def apply(pixel: ReadVec2, time: Double) = {
      def largeOffset :Double = {
        val localTime = time % (numSlices * timePerSlice)
        val slice = (localTime/timePerSlice).toInt
        minOffset*pow(incrementFactor, slice)
      }

      val p = pixel + time*scrollSpeed
      Vec3((noise1(Vec3(p*scale + largeOffset, time*noiseSpeed)) + 1)/2)
    }})
  }
}