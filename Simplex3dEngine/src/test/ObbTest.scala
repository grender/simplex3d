/*
 * Simplex3dEngine - Test Package
 * Copyright (C) 2011, Aleksey Nikiforov
 *
 * This file is part of Simplex3dEngine.
 *
 * Simplex3dEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Simplex3dEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package test

import scala.collection.mutable.ArrayBuffer
import simplex3d.math._
import simplex3d.math.double._
import simplex3d.math.double.functions._
import simplex3d.data._
import simplex3d.data.double._
import simplex3d.algorithm.noise._
import simplex3d.algorithm.shapes._
import simplex3d.engine._
import simplex3d.engine.renderer._
import simplex3d.engine.app._
import simplex3d.engine.input._
import simplex3d.engine.input.handler._
import simplex3d.engine.scenegraph._
import simplex3d.engine.impl._
import simplex3d.engine.default._


object ObbTest extends BasicApp with lwjgl.App {
  val title = "Obb Test"
  
  def main(args: Array[String]) {
    val settings = new Settings(
      fullScreen = false,
      verticalSync = true,
      capabilitiesLog = true,
      performanceLog = true,
      dimensions = Vec2i(800, 600)
    )
    
    launch(settings)
  }
  
  
  val texture = Texture2d(Vec2i(128), DataBuffer[Vec3, UByte](128*128));
  {
    val data = texture.write
    for (y <- 0 until texture.dimensions.y; x <- 0 until texture.dimensions.x) {
      data(x + y*texture.dimensions.x) = Vec3(0, 1, 1)
    }
  }
  
  val movingCube = new Mesh
  val cube1 = new Mesh
  val cube2 = new Mesh
  val cubeScale = 10
  val translation = ConstVec3(20, 0, 0)
  
  def init() {
    val (indices, vertices, normals, texCoords) = makeBox()
    val aindices = Attributes(indices)
    val avertices = Attributes(vertices)
    val anormals = Attributes(normals)
    val atexCoords = Attributes(texCoords)
    
    
    movingCube.geometry.indices.defineAs(aindices)
    movingCube.geometry.vertices.defineAs(avertices)
    movingCube.geometry.normals.defineAs(anormals)
    movingCube.geometry.texCoords.defineAs(atexCoords)
    
    movingCube.material.texture.mutable := texture
    movingCube.transformation.mutable.rotation := Quat4 rotateX(radians(20)) rotateY(radians(-30))
    movingCube.transformation.mutable.scale := cubeScale
    
    world.attach(movingCube)
    
    
    cube1.geometry.indices.defineAs(aindices)
    cube1.geometry.vertices.defineAs(avertices)
    cube1.geometry.normals.defineAs(anormals)
    cube1.geometry.texCoords.defineAs(atexCoords)
    
    cube1.material.texture.mutable := texture
    cube1.transformation.mutable.scale := cubeScale
    cube1.transformation.mutable.translation := -translation
    
    world.attach(cube1)
    
    
    cube2.geometry.indices.defineAs(aindices)
    cube2.geometry.vertices.defineAs(avertices)
    cube2.geometry.normals.defineAs(anormals)
    cube2.geometry.texCoords.defineAs(atexCoords)
    
    cube2.material.texture.mutable := texture
    cube2.transformation.mutable.rotation := Quat4 rotateX(radians(-15)) rotateZ(radians(30))
    cube2.transformation.mutable.scale := cubeScale
    cube2.transformation.mutable.translation := translation
    
    world.attach(cube2)
  }
  
  
  val cubeControls = new InputListener {
    val motionSpeed: Double = 20.0
    val dynamic = movingCube.transformation
  
    override def update(input: Input, time: TimeStamp) {
      val keyDown = input.keyboard.isKeyDown(_); import KeyCode._
      
      val transformation = movingCube.transformation.mutable
      val position = movingCube.transformation.mutable.translation
      
      if (keyDown(K_w)) position.y += time.interval*motionSpeed
      if (keyDown(K_s)) position.y -= time.interval*motionSpeed
      if (keyDown(K_a)) position.x -= time.interval*motionSpeed
      if (keyDown(K_d)) position.x += time.interval*motionSpeed
    }
  }
  addInputListener(cubeControls)
  
  val camControls = new FirstPersonHandler(world.camera.transformation)
  world.camera.transformation.mutable.translation := Vec3(0, 0, 60)
  camControls.isEnabled = false
  addInputListener(camControls)
  
  addInputListener(new InputListener {
    override val keyboardListener = new KeyboardListener {
      override def keyTyped(input: Input, e: KeyEvent) {
        if (KeyCode.Num_Enter == e.keyCode || KeyCode.K_Enter == e.keyCode) {
          input.mouse.isGrabbed = !input.mouse.isGrabbed
          
          camControls.isEnabled = input.mouse.isGrabbed
          cubeControls.isEnabled = !input.mouse.isGrabbed
        }
      }
    }
  })
  
  
  def update(time: TimeStamp) {
    //import simplex3d.algorithm.intersection._
    
    val min = Vec3(-0.5)
    val max = Vec3(0.5)

    val intersection1 = Obb.intersectAabb(min, max, movingCube.worldTransformation.matrix)(min*cubeScale - translation, max*cubeScale - translation)
    if (intersection1 != Vec3.Zero) println("Aabb " + time.total + ": " + intersection1)
    
    val intersection2 = Obb.intersectObb(min, max, movingCube.worldTransformation.matrix)(min, max, cube2.worldTransformation.matrix)
    if (intersection2 != Vec3.Zero) println("Obb " + time.total + ": " + intersection2)
  }

  
  def reshape(position: inVec2i, dimensions: inVec2i) {
    val aspect = dimensions.x.toDouble/dimensions.y
    world.camera.projection := perspectiveProj(radians(60), aspect, 5, 200)
    world.camera.transformation.mutable.translation := Vec3(0, 0, 60)
  }
}


import simplex3d.algorithm.intersection._
object Obb {
  
  /** Intersect dynamic Obb (dmin, dmax, dtransformation) with a static Aabb (smin, smax).
   * The result is a vector by which the dynamic box has to be displaced to resolve the collision.
   */
  def intersectAabb(dmin: inVec3, dmax: inVec3, dtransformation: inMat3x4)(smin: inVec3, smax: inVec3) :Vec3 = {
    intersectObb(dmin, dmax, dtransformation)(smin, smax, Mat3x4.Identity)
  }
  
  /** Intersect dynamic Obb (dmin, dmax, dtransformation) with a static Obb (smin, smax, stransformation).
   * The result is a vector by which the dynamic box has to be displaced to resolve the collision.
   */
  def intersectObb
    (dmin: inVec3, dmax: inVec3, dtransformation: inMat3x4)
    (smin: inVec3, smax: inVec3, stransformation: inMat3x4)
  :Vec3 = {
    Vec3(0)
  }
}