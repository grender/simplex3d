/*
 * Simplex3dEngine - LWJGL Module
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

package simplex3d
package engine
package impl.lwjgl

import java.util.logging._
import java.nio._
import scala.annotation._
import scala.collection.mutable.ArrayBuffer
import org.lwjgl.opengl._
import simplex3d.math.types._
import simplex3d.math._
import simplex3d.math.double._
import simplex3d.math.double.functions._
import simplex3d.data._
import simplex3d.data.double._
import simplex3d.engine.scene._
import simplex3d.engine.graphics._
import simplex3d.engine.impl.gl._


private[lwjgl] object RenderManager {
  private final val logger = Logger.getLogger(classOf[RenderManager].getName)
}


private[lwjgl] final class RenderManager(val techniqueManager: TechniqueManager)
extends engine.RenderManager with ImplAccess
{
  import GL11._; import GL12._; import GL13._; import GL14._; import GL15._;
  import GL20._; import GL21._
  import RenderManager.logger._
  
  import PropertyAccess._
  
  
  private val elementRange = new ElementRange()
  
  
  def render(ctx: engine.RenderContext, camera: AbstractCamera, renderArray: InplaceSortBuffer[AbstractMesh]) {
    val context = ctx.asInstanceOf[RenderContext]
    if (context.requiresReset) context.resetState()
    
    // XXX these should come from the path, and get activated via context
    glEnable(GL_DEPTH_TEST)
    glDepthFunc(GL_LESS)
    
    var i = 0; while (i < renderArray.length) {
      val mesh = renderArray(i)
      render(context, camera, mesh)
      
      i += 1
    }
  }

  
  private def render(context: RenderContext, camera: AbstractCamera, mesh: AbstractMesh) {
    
    val program = mesh.technique.defined
    
    // Compiles shaders, links the program, build the program mapping, and calls glUseProgram.
    context.bindProgram(program)
    
    val programMapping = program.mapping
    
    if (mesh.technique.hasRefChanges) {
      context.rebuildMeshMapping(mesh, programMapping)
      mesh.technique.clearRefChanges()
    }
    
    
    val transformation = WorldMatrixAccess.getWorldMatrix(mesh)
    val geometry = mesh.geometry
    val material = mesh.material
    
    mesh.resolveElementRange(elementRange)
    context.setFaceCulling(geometry.faceCulling)
    
    
    val predefinedUniforms = mesh.predefinedUniforms
    predefinedUniforms.se_modelViewMatrix := Mat4(transformation concat camera.view)
    predefinedUniforms.se_modelViewProjectionMatrix := camera.projection*predefinedUniforms.se_modelViewMatrix
    predefinedUniforms.se_normalMatrix := transpose(inverse(Mat3(predefinedUniforms.se_modelViewMatrix)))
    
    // Update bindings using predefined uniforms.
    val properties = mesh.worldEnvironment.properties
    var i = 0; while (i < properties.length) { val property = properties(i)
      if (property.isDefined) property.defined.updateBinding(predefinedUniforms)
      
      i += 1
    }
    
    programMapping.bind(mesh.mapping)
    
//    val directionalLightPos = Vec3(-0.25, 0.5, 1)
//    val ecLightDir = -normalize(camera.view.transformVector(directionalLightPos))
//    program("ecLightDir") = ecLightDir //XXX, figure out how to perform uniform transformations, given various matrices
    
    if (geometry.indices.isDefined) {
      context.init(geometry.indices.defined)
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, geometry.indices.defined.managedFields.id)
      glDrawElements(GL_TRIANGLES, elementRange.count, geometry.indices.defined.src.rawType, elementRange.first.toConst)
    }
    else {
      glDrawArrays(GL_TRIANGLES, elementRange.first, elementRange.count)
    }
  }
  
  // XXX must be updated with new attributes
  private val comparator = new java.util.Comparator[AbstractMesh] with ImplAccess {
    
    def compare(a: AbstractMesh, b: AbstractMesh) :Int = {
      // XXX sort by textures: if (at0id < bt0id) -1 else if (at0id > bt0id)  1 else 0
      // XXX also sort by parent's environment
      
      val apid = a.technique.defined.managedFields.id
      val bpid = b.technique.defined.managedFields.id
      if (apid < bpid) -1 else if (apid > bpid)  1 else 0
    }
  }
  
  def sortRenderArray(pass: Pass, renderArray: InplaceSortBuffer[AbstractMesh]) {
    renderArray.inplaceSort(comparator)
  }
}
