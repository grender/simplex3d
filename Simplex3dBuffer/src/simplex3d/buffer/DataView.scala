/*
 * Simplex3d, BaseBuffer module
 * Copyright (C) 2010, Simplex3d Team
 *
 * This file is part of Simplex3dBuffer.
 *
 * Simplex3dBuffer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Simplex3dBuffer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package simplex3d.buffer

import java.nio._


/**
 * @author Aleksey Nikiforov (lex)
 */
trait ReadDataView[E <: MetaElement, +R <: RawData]
extends ReadDataSeq[E, R] {

  assert(buffer.position == 0)
  assert(buffer.limit == buffer.capacity)

  if (!buffer.isDirect)
    throw new IllegalArgumentException(
      "The buffer must be direct."
    )

  if (sharedBuffer.order != ByteOrder.nativeOrder)
    throw new IllegalArgumentException(
      "The buffer must have native order."
    )

  
  def backingSeq: ReadDataBuffer[E#Component, R]
  def asReadOnlySeq() :ReadDataView[E, R]

  final def sharesMemory(seq: inDataSeq[_ <: MetaElement, _ <: RawData]) = {
    seq match {
      case v: ReadDataView[_, _] =>
        sharedBuffer eq v.sharedBuffer
      case _ =>
        false
    }
  }
}

trait DataView[E <: MetaElement, +R <: RawData]
extends DataSeq[E, R] with ReadDataView[E, R] {
  def backingSeq: DataBuffer[E#Component, R]
}

object DataView {
  def apply[E <: MetaElement, R <: ReadableData](
    buffer: ByteBuffer, offset: Int, stride: Int
  )(implicit ref: FactoryRef[E, R]) :DataView[E, R] = {
    ref.factory.mkDataView(buffer, offset, stride)
  }

  def apply[E <: MetaElement, R <: ReadableData](
    db: DataBuffer[_, _], offset: Int, stride: Int
  )(implicit ref: FactoryRef[E, R]) :DataView[E, R] = {
    val res = ref.factory.mkDataView(db.sharedBuffer, offset, stride)
    if (db.isReadOnly) res.asReadOnlySeq.asInstanceOf[DataView[E, R]] else res
  }

  def apply[E <: MetaElement, R <: ReadableData](
    db: inDataBuffer[_, _], offset: Int, stride: Int
  )(implicit ref: FactoryRef[E, R]) :ReadDataView[E, R] = {
    val res = ref.factory.mkDataView(db.sharedBuffer, offset, stride)
    if (db.isReadOnly) res.asReadOnlySeq() else res
  }
}
