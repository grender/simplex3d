/*
 * Simplex3d, CoreMath module
 * Copyright (C) 2011, Aleksey Nikiforov
 *
 * This file is part of Simplex3dMath.
 *
 * Simplex3dMath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Simplex3dMath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package simplex3d.math


/** This is an implementation of Classical Gradient Noise.
 *
 * @author Aleksey Nikiforov (lex)
 */
@SerialVersionUID(8104346712419693669L)
final class ClassicalGradientNoise(seed: Long)
extends TiledNoiseSource(seed) with Serializable
{

  import ClassicalGradientNoise._
  

  private[this] final val permArray: Array[Byte] = {

    val array = Array[Byte](
      -85, 6, -43, -11, -118, -116, 16, 71, -123, 27, 21, -46, 43, 78, 91, 116,
      12, 8, 60, -4, -80, -21, -60, 123, 83, -117, 98, -19, -29, -72, 73, -58,
      -22, -5, 104, -34, 70, -57, 114, 102, -40, -41, 57, 127, -24, 74, 122, -94,
      -39, -10, -79, 109, 25, -55, 56, 58, 53, -42, 54, -47, -53, 19, -128, -49, 87,
      -105, -102, -109, -45, 20, 85, -38, 108, -66, 117, 111, -2, 115, -115, 101,
      15, -14, 5, -68, 65, 63, -108, -70, 39, -78, -54, -27, -26, 97, 36, -52, 110,
      -50, -112, -8, 50, -81, -17, -89, -75, -69, 55, 124, -36, 47, 52, -90, -88,
      -114, 31, 79, -100, 119, -15, 72, 28, 23, 126, -119, -64, -104, -98, 0, -13,
      -16, 100, 113, -20, 66, 18, 96, -35, 45, 84, 118, 17, 90, -125, 32, -6, -73,
      92, -63, 49, -23, 62, 94, -121, -48, 75, 13, 38, 40, 107, 125, -65, -93, 7, 77,
      1, -124, -28, 44, -86, 26, -9, -103, -12, 59, -31, 10, 64, -33, 99, 9, -77, -71,
      80, -95, -1, 103, -84, 95, -44, -67, 41, -91, 35, -106, 51, -61, -122, 121, 89,
      -59, -62, 105, -74, -18, -32, -7, -82, -120, -110, 11, -30, -3, 34, 76, -87, -126,
      42, 3, 46, -96, 30, -51, 82, -76, 68, 106, 112, 14, 81, 67, 48, -111, 120, 37,
      -56, 33, 22, -92, -97, 29, -83, 88, 24, -25, 86, 69, 61, 2, -99, 93, -101, -113,
      -107, 4, -127, -37
    )

    val random = new java.util.Random(seed)
    var i = 0; while (i < array.length) {
      val randomIndex = random.nextInt(array.length)

      val tmp = array(i)
      array(i) = array(randomIndex)
      array(randomIndex) = tmp

      i += 1
    }

    array
  }


  private final def perm(i: Int) = {
    val xx = i ^ (i >> 16)
    permArray((xx ^ (xx >> 8)) & 0xFF)
  }

  private final def ifloor(x: Double) :Long = {
    val i = x.toLong
    if (x > 0 || x == i) i else i - 1
  }

  private final def fade(t: Double) :Double = {
    t*t*t*(t*(t*6 - 15) + 10)
  }


  final def apply(x: Double) :Double = {
    val lx = ifloor(x)
    val fx = x - lx
    val ix = lx.toInt

    val n0 = {
      val px = perm(ix)
      // Gradient function, produces ints in [-8, 8] excluding 0 from perm.
      val grad = if ((px & 0x8) == 0) ((px & 0x7) + 1) else (px | 0xFFFFFFF8)
      grad*fx
    }
    
    val n1 = {
      val px = perm(ix + 1)
      // Gradient function, produces ints in [-8, 8] excluding 0 from perm.
      val grad = if ((px & 0x8) == 0) ((px & 0x7) + 1) else (px | 0xFFFFFFF8)
      grad*(fx - 1)
    }

    val xfade = fade(fx)
    (n0*(1 - xfade) + n1*xfade)*0.25
  }

  final def apply(x: Double, y: Double) :Double = {
    val lx = ifloor(x)
    val ly = ifloor(y)

    val fx = x - lx
    val fy = y - ly

    val ix = lx.toInt
    val iy = ly.toInt

    val px0 = perm(ix)
    val px1 = perm(ix + 1)

    val n00 = {
      val py = perm(px0 + iy)
      val grad = grad3(py & 0x0F)
      grad(0)*fx + grad(1)*fy
    }

    val n10 = {
      val py = perm(px1 + iy)
      val grad = grad3(py & 0x0F)
      grad(0)*(fx - 1) + grad(1)*fy
    }

    val n01 = {
      val py = perm(px0 + iy + 1)
      val grad = grad3(py & 0x0F)
      grad(0)*fx + grad(1)*(fy - 1)
    }

    val n11 = {
      val py = perm(px1 + iy + 1)
      val grad = grad3(py & 0x0F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1)
    }

    val xfade = fade(fx)
    val mx0 = n00*(1 - xfade) + n10*xfade
    val mx1 = n01*(1 - xfade) + n11*xfade

    val yfade = fade(fy)
    (mx0*(1 - yfade) + mx1*yfade)*1.5// 1.5 is a guess
  }

  final def apply(x: Double, y: Double, z:Double) :Double = {
    val lx = ifloor(x)
    val ly = ifloor(y)
    val lz = ifloor(z)

    val fx = x - lx
    val fy = y - ly
    val fz = z - lz

    val ix = lx.toInt
    val iy = ly.toInt
    val iz = lz.toInt

    val px0 = perm(ix)
    val px1 = perm(ix + 1)
    val py00 = perm(px0 + iy)
    val py10 = perm(px1 + iy)
    val py01 = perm(px0 + iy + 1)
    val py11 = perm(px1 + iy + 1)

    val n000 = {
      val pz = perm(py00 + iz)
      val grad = grad3(pz & 0x0F)
      grad(0)*fx + grad(1)*fy + grad(2)*fz
    }

    val n100 = {
      val pz = perm(py10 + iz)
      val grad = grad3(pz & 0x0F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*fz
    }

    val n010 = {
      val pz = perm(py01 + iz)
      val grad = grad3(pz & 0x0F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*fz
    }

    val n110 = {
      val pz = perm(py11 + iz)
      val grad = grad3(pz & 0x0F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*fz
    }

    val n001 = {
      val pz = perm(py00 + iz + 1)
      val grad = grad3(pz & 0x0F)
      grad(0)*fx + grad(1)*fy + grad(2)*(fz - 1)
    }

    val n101 = {
      val pz = perm(py10 + iz + 1)
      val grad = grad3(pz & 0x0F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*(fz - 1)
    }

    val n011 = {
      val pz = perm(py01 + iz + 1)
      val grad = grad3(pz & 0x0F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*(fz - 1)
    }

    val n111 = {
      val pz = perm(py11 + iz + 1)
      val grad = grad3(pz & 0x0F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*(fz - 1)
    }

    val xfade = fade(fx)
    val mx00 = n000*(1 - xfade) + n100*xfade
    val mx10 = n010*(1 - xfade) + n110*xfade
    val mx01 = n001*(1 - xfade) + n101*xfade
    val mx11 = n011*(1 - xfade) + n111*xfade

    val yfade = fade(fy)
    val my0 = mx00*(1 - yfade) + mx10*yfade
    val my1 = mx01*(1 - yfade) + mx11*yfade

    val zfade = fade(fz)
    (my0*(1 - zfade) + my1*zfade)*1.3// 1.3 is a guess
  }

  final def apply(x: Double, y: Double, z:Double, w:Double) :Double = {
    val lx = ifloor(x)
    val ly = ifloor(y)
    val lz = ifloor(z)
    val lw = ifloor(w)

    val fx = x - lx
    val fy = y - ly
    val fz = z - lz
    val fw = w - lw

    val ix = lx.toInt
    val iy = ly.toInt
    val iz = lz.toInt
    val iw = lw.toInt

    val px0 = perm(ix)
    val px1 = perm(ix + 1)
    val py00 = perm(px0 + iy)
    val py10 = perm(px1 + iy)
    val py01 = perm(px0 + iy + 1)
    val py11 = perm(px1 + iy + 1)
    val pz000 = perm(py00 + iz)
    val pz100 = perm(py10 + iz)
    val pz010 = perm(py01 + iz)
    val pz110 = perm(py11 + iz)
    val pz001 = perm(py00 + iz + 1)
    val pz101 = perm(py10 + iz + 1)
    val pz011 = perm(py01 + iz + 1)
    val pz111 = perm(py11 + iz + 1)

    val n0000 = {
      val pw = perm(pz000 + iw)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*fy + grad(2)*fz + grad(3)*fw
    }

    val n1000 = {
      val pw = perm(pz100 + iw)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*fz + grad(3)*fw
    }

    val n0100 = {
      val pw = perm(pz010 + iw)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*fz + grad(3)*fw
    }

    val n1100 = {
      val pw = perm(pz110 + iw)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*fz + grad(3)*fw
    }

    val n0010 = {
      val pw = perm(pz001 + iw)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*fy + grad(2)*(fz - 1) + grad(3)*fw
    }

    val n1010 = {
      val pw = perm(pz101 + iw)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*(fz - 1) + grad(3)*fw
    }

    val n0110 = {
      val pw = perm(pz011 + iw)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*(fz - 1) + grad(3)*fw
    }

    val n1110 = {
      val pw = perm(pz111 + iw)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*(fz - 1) + grad(3)*fw
    }

    val n0001 = {
      val pw = perm(pz000 + iw + 1)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*fy + grad(2)*fz + grad(3)*(fw - 1)
    }

    val n1001 = {
      val pw = perm(pz100 + iw + 1)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*fz + grad(3)*(fw - 1)
    }

    val n0101 = {
      val pw = perm(pz010 + iw + 1)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*fz + grad(3)*(fw - 1)
    }

    val n1101 = {
      val pw = perm(pz110 + iw + 1)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*fz + grad(3)*(fw - 1)
    }

    val n0011 = {
      val pw = perm(pz001 + iw + 1)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*fy + grad(2)*(fz - 1) + grad(3)*(fw - 1)
    }

    val n1011 = {
      val pw = perm(pz101 + iw + 1)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*(fz - 1) + grad(3)*(fw - 1)
    }

    val n0111 = {
      val pw = perm(pz011 + iw + 1)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*(fz - 1) + grad(3)*(fw - 1)
    }

    val n1111 = {
      val pw = perm(pz111 + iw + 1)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*(fz - 1) + grad(3)*(fw - 1)
    }


    val xfade = fade(fx)
    val mx000 = n0000*(1 - xfade) + n1000*xfade
    val mx100 = n0100*(1 - xfade) + n1100*xfade
    val mx010 = n0010*(1 - xfade) + n1010*xfade
    val mx110 = n0110*(1 - xfade) + n1110*xfade
    val mx001 = n0001*(1 - xfade) + n1001*xfade
    val mx101 = n0101*(1 - xfade) + n1101*xfade
    val mx011 = n0011*(1 - xfade) + n1011*xfade
    val mx111 = n0111*(1 - xfade) + n1111*xfade

    val yfade = fade(fy)
    val my00 = mx000*(1 - yfade) + mx100*yfade
    val my10 = mx010*(1 - yfade) + mx110*yfade
    val my01 = mx001*(1 - yfade) + mx101*yfade
    val my11 = mx011*(1 - yfade) + mx111*yfade

    val zfade = fade(fz)
    val mz0 = my00*(1 - zfade) + my10*zfade
    val mz1 = my01*(1 - zfade) + my11*zfade

    val wfade = fade(fw)
    (mz0*(1 - wfade) + mz1*wfade)*1.2// 1.2 is a guess
  }

  // Tiled noise
  final val tileSizeX :Double = 1.0
  final val tileSizeY :Double = 1.0
  final val tileSizeZ :Double = 1.0
  final val tileSizeW :Double = 1.0

  final def apply(
    tile: Int,
    x: Double
  ) :Double = {
    val lx = ifloor(x)
    val fx = x - lx
    val ix = lx.toInt & 0x7FFFFFFF

    val n0 = {
      val px = perm(ix % tile)
      // Gradient function, produces ints in [-8, 8] excluding 0 from perm.
      val grad = if ((px & 0x8) == 0) ((px & 0x7) + 1) else (px | 0xFFFFFFF8)
      grad*fx
    }

    val n1 = {
      val px = perm((ix + 1) % tile)
      // Gradient function, produces ints in [-8, 8] excluding 0 from perm.
      val grad = if ((px & 0x8) == 0) ((px & 0x7) + 1) else (px | 0xFFFFFFF8)
      grad*(fx - 1)
    }

    val xfade = fade(fx)
    (n0*(1 - xfade) + n1*xfade)*0.25
  }

  final def apply(
    tilex: Int, tiley: Int,
    x: Double, y: Double
  ) :Double = {
    val lx = ifloor(x)
    val ly = ifloor(y)

    val fx = x - lx
    val fy = y - ly

    val ix = lx.toInt & 0x7FFFFFFF
    val iy = ly.toInt & 0x7FFFFFFF

    val px0 = perm(ix % tilex)
    val px1 = perm((ix + 1) % tilex)
    val ty = iy % tiley
    val ty1 = (iy + 1) % tiley

    val n00 = {
      val py = perm(px0 + ty)
      val grad = grad3(py & 0x0F)
      grad(0)*fx + grad(1)*fy
    }

    val n10 = {
      val py = perm(px1 + ty)
      val grad = grad3(py & 0x0F)
      grad(0)*(fx - 1) + grad(1)*fy
    }

    val n01 = {
      val py = perm(px0 + ty1)
      val grad = grad3(py & 0x0F)
      grad(0)*fx + grad(1)*(fy - 1)
    }

    val n11 = {
      val py = perm(px1 + ty1)
      val grad = grad3(py & 0x0F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1)
    }

    val xfade = fade(fx)
    val mx0 = n00*(1 - xfade) + n10*xfade
    val mx1 = n01*(1 - xfade) + n11*xfade

    val yfade = fade(fy)
    (mx0*(1 - yfade) + mx1*yfade)*1.5// 1.5 is a guess
  }

  final def apply(
    tilex: Int, tiley: Int, tilez: Int,
    x: Double, y: Double, z:Double
  ) :Double = {
    val lx = ifloor(x)
    val ly = ifloor(y)
    val lz = ifloor(z)

    val fx = x - lx
    val fy = y - ly
    val fz = z - lz

    val ix = lx.toInt & 0x7FFFFFFF
    val iy = ly.toInt & 0x7FFFFFFF
    val iz = lz.toInt & 0x7FFFFFFF

    val px0 = perm(ix % tilex)
    val px1 = perm((ix + 1) % tilex)
    val ty = iy % tiley
    val ty1 = (iy + 1) % tiley
    val py00 = perm(px0 + ty)
    val py10 = perm(px1 + ty)
    val py01 = perm(px0 + ty1)
    val py11 = perm(px1 + ty1)
    val tz = iz % tilez
    val tz1 = (iz + 1) % tilez

    val n000 = {
      val pz = perm(py00 + tz)
      val grad = grad3(pz & 0x0F)
      grad(0)*fx + grad(1)*fy + grad(2)*fz
    }

    val n100 = {
      val pz = perm(py10 + tz)
      val grad = grad3(pz & 0x0F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*fz
    }

    val n010 = {
      val pz = perm(py01 + tz)
      val grad = grad3(pz & 0x0F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*fz
    }

    val n110 = {
      val pz = perm(py11 + tz)
      val grad = grad3(pz & 0x0F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*fz
    }

    val n001 = {
      val pz = perm(py00 + tz1)
      val grad = grad3(pz & 0x0F)
      grad(0)*fx + grad(1)*fy + grad(2)*(fz - 1)
    }

    val n101 = {
      val pz = perm(py10 + tz1)
      val grad = grad3(pz & 0x0F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*(fz - 1)
    }

    val n011 = {
      val pz = perm(py01 + tz1)
      val grad = grad3(pz & 0x0F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*(fz - 1)
    }

    val n111 = {
      val pz = perm(py11 + tz1)
      val grad = grad3(pz & 0x0F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*(fz - 1)
    }

    val xfade = fade(fx)
    val mx00 = n000*(1 - xfade) + n100*xfade
    val mx10 = n010*(1 - xfade) + n110*xfade
    val mx01 = n001*(1 - xfade) + n101*xfade
    val mx11 = n011*(1 - xfade) + n111*xfade

    val yfade = fade(fy)
    val my0 = mx00*(1 - yfade) + mx10*yfade
    val my1 = mx01*(1 - yfade) + mx11*yfade

    val zfade = fade(fz)
    (my0*(1 - zfade) + my1*zfade)*1.3// 1.3 is a guess
  }

  final def apply(
    tilex: Int, tiley: Int, tilez: Int, tilew: Int,
    x: Double, y: Double, z:Double, w:Double
  ) :Double = {
    val lx = ifloor(x)
    val ly = ifloor(y)
    val lz = ifloor(z)
    val lw = ifloor(w)

    val fx = x - lx
    val fy = y - ly
    val fz = z - lz
    val fw = w - lw

    val ix = lx.toInt & 0x7FFFFFFF
    val iy = ly.toInt & 0x7FFFFFFF
    val iz = lz.toInt & 0x7FFFFFFF
    val iw = lw.toInt & 0x7FFFFFFF

    val px0 = perm(ix % tilex)
    val px1 = perm((ix + 1) % tilex)
    val ty = iy % tiley
    val ty1 = (iy + 1) % tiley
    val py00 = perm(px0 + ty)
    val py10 = perm(px1 + ty)
    val py01 = perm(px0 + ty1)
    val py11 = perm(px1 + ty1)
    val tz = iz % tilez
    val tz1 = (iz + 1) % tilez
    val pz000 = perm(py00 + tz)
    val pz100 = perm(py10 + tz)
    val pz010 = perm(py01 + tz)
    val pz110 = perm(py11 + tz)
    val pz001 = perm(py00 + tz1)
    val pz101 = perm(py10 + tz1)
    val pz011 = perm(py01 + tz1)
    val pz111 = perm(py11 + tz1)
    val tw = iw % tilew
    val tw1 = (iw + 1) % tilew

    val n0000 = {
      val pw = perm(pz000 + tw)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*fy + grad(2)*fz + grad(3)*fw
    }

    val n1000 = {
      val pw = perm(pz100 + tw)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*fz + grad(3)*fw
    }

    val n0100 = {
      val pw = perm(pz010 + tw)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*fz + grad(3)*fw
    }

    val n1100 = {
      val pw = perm(pz110 + tw)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*fz + grad(3)*fw
    }

    val n0010 = {
      val pw = perm(pz001 + tw)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*fy + grad(2)*(fz - 1) + grad(3)*fw
    }

    val n1010 = {
      val pw = perm(pz101 + tw)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*(fz - 1) + grad(3)*fw
    }

    val n0110 = {
      val pw = perm(pz011 + tw)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*(fz - 1) + grad(3)*fw
    }

    val n1110 = {
      val pw = perm(pz111 + tw)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*(fz - 1) + grad(3)*fw
    }

    val n0001 = {
      val pw = perm(pz000 + tw1)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*fy + grad(2)*fz + grad(3)*(fw - 1)
    }

    val n1001 = {
      val pw = perm(pz100 + tw1)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*fz + grad(3)*(fw - 1)
    }

    val n0101 = {
      val pw = perm(pz010 + tw1)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*fz + grad(3)*(fw - 1)
    }

    val n1101 = {
      val pw = perm(pz110 + tw1)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*fz + grad(3)*(fw - 1)
    }

    val n0011 = {
      val pw = perm(pz001 + tw1)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*fy + grad(2)*(fz - 1) + grad(3)*(fw - 1)
    }

    val n1011 = {
      val pw = perm(pz101 + tw1)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*fy + grad(2)*(fz - 1) + grad(3)*(fw - 1)
    }

    val n0111 = {
      val pw = perm(pz011 + tw1)
      val grad = grad4(pw & 0x1F)
      grad(0)*fx + grad(1)*(fy - 1) + grad(2)*(fz - 1) + grad(3)*(fw - 1)
    }

    val n1111 = {
      val pw = perm(pz111 + tw1)
      val grad = grad4(pw & 0x1F)
      grad(0)*(fx - 1) + grad(1)*(fy - 1) + grad(2)*(fz - 1) + grad(3)*(fw - 1)
    }


    val xfade = fade(fx)
    val mx000 = n0000*(1 - xfade) + n1000*xfade
    val mx100 = n0100*(1 - xfade) + n1100*xfade
    val mx010 = n0010*(1 - xfade) + n1010*xfade
    val mx110 = n0110*(1 - xfade) + n1110*xfade
    val mx001 = n0001*(1 - xfade) + n1001*xfade
    val mx101 = n0101*(1 - xfade) + n1101*xfade
    val mx011 = n0011*(1 - xfade) + n1011*xfade
    val mx111 = n0111*(1 - xfade) + n1111*xfade

    val yfade = fade(fy)
    val my00 = mx000*(1 - yfade) + mx100*yfade
    val my10 = mx010*(1 - yfade) + mx110*yfade
    val my01 = mx001*(1 - yfade) + mx101*yfade
    val my11 = mx011*(1 - yfade) + mx111*yfade

    val zfade = fade(fz)
    val mz0 = my00*(1 - zfade) + my10*zfade
    val mz1 = my01*(1 - zfade) + my11*zfade

    val wfade = fade(fw)
    (mz0*(1 - wfade) + mz1*wfade)*1.2// 1.2 is a guess
  }
}


@SerialVersionUID(8104346712419693669L)
object ClassicalGradientNoise extends TiledNoiseFactory with Serializable {

  def apply(seed: Long) = new ClassicalGradientNoise(seed)

  
  private final val grad3: Array[Array[Byte]] = Array(
    Array(0,1,1), Array(0,1,-1), Array(0,-1,1), Array(0,-1,-1),
    Array(1,0,1), Array(1,0,-1), Array(-1,0,1), Array(-1,0,-1),
    Array(1,1,0), Array(1,-1,0), Array(-1,1,0), Array(-1,-1,0),
    Array(1,0,-1), Array(-1,0,-1), Array(0,-1,1), Array(0,1,1)
  )

  private final val grad4: Array[Array[Byte]] = Array(
    Array(0,1,1,1), Array(0,1,1,-1), Array(0,1,-1,1), Array(0,1,-1,-1),
    Array(0,-1,1,1), Array(0,-1,1,-1), Array(0,-1,-1,1), Array(0,-1,-1,-1),
    Array(1,0,1,1), Array(1,0,1,-1), Array(1,0,-1,1), Array(1,0,-1,-1),
    Array(-1,0,1,1), Array(-1,0,1,-1), Array(-1,0,-1,1), Array(-1,0,-1,-1),
    Array(1,1,0,1), Array(1,1,0,-1), Array(1,-1,0,1), Array(1,-1,0,-1),
    Array(-1,1,0,1), Array(-1,1,0,-1), Array(-1,-1,0,1), Array(-1,-1,0,-1),
    Array(1,1,1,0), Array(1,1,-1,0), Array(1,-1,1,0), Array(1,-1,-1,0),
    Array(-1,1,1,0), Array(-1,1,-1,0), Array(-1,-1,1,0), Array(-1,-1,-1,0)
  )
}