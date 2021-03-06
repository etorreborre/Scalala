/*
 * Distributed as part of Scalala, a linear algebra library.
 * 
 * Copyright (C) 2008- Daniel Ramage
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110 USA 
 */
package scalala;
package tensor;
package operators;

import collection.{MergeableSet,ProductSet};
import sparse._;
import VectorTypes._;
import MatrixTypes._;

/** Type aliases supporting DenseVector operators. */
object SparseBinaryVectorTypes {
  type ColSparseBinaryVectorOp[V<:SparseBinaryVector] =
    ColVectorOp[V];
  
  type RowSparseBinaryVectorOp[V<:SparseBinaryVector] =
    RowVectorOp[V];
 }

import SparseBinaryVectorTypes._;

/** Implicits supporting DenseVector operations. */
trait SparseBinaryVectorOps {

  implicit def iSparseBinaryVectorToRichColVectorOp(v : SparseBinaryVector) =
    new RichColTensor1Op(ColSparseBinaryVectorAsSparseVectorIdentity(v));
  
  case class ColSparseBinaryVectorAsSparseVectorIdentity(val tensor : SparseBinaryVector)
  extends TensorOp[SparseVector,Tensor1Op.Col] {
    override lazy val value = tensor.toSparseVector;
    override lazy val working = value.copy;
  }
}

/** Singleton instance of DenseVectorOps trait. */
object SparseBinaryVectorOps extends SparseBinaryVectorOps;
