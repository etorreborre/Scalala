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
package sparse;

import collection.{MergeableSet,IntSpanSet,DomainException};
import Tensor.CreateException;
import dense.DenseVector;

/**
 * A SparseBinaryVector is a sparse vector data structure that holds a single
 * sorted array of ints as its backing data structure -- the value of the vector
 * at an index in the array is 1.0 and is 0.0 elsewhere.  This vector cannot
 * have its default value changed from 0.0.  Use SparseVector for a more general
 * (but more memory intensive) representation able to handle arbitrary values
 * associated with each key.  Note that this class is not threadsafe.
 * 
 * @author dramage
 */
class SparseBinaryVector(domainSize : Int, initialNonzeros : Int) extends Vector {
  if (domainSize < 0)
    throw new IllegalArgumentException("Invalid domain size: "+domainSize);

  /** Index will be reassigned as the sparse vector grows. */
  var index : Array[Int] = new Array[Int](initialNonzeros);
  
  /** How many iterator of data,index are used. */
  var used : Int = 0;
  
  /** The previous index and offset found by apply or update. */
  var lastOffset = -1;
  var lastIndex = -1;
  
  /** Constructs a new SparseVector with initially 0 allocation. */
  def this(size : Int) =
    this(size, 0);
  
  /**
   * Constructs a new SparseVector of the given size, initially using
   * the given (sorted) array of integers as the set of non-zeros in the
   * vector.
   */
  def this(size : Int, indices : Array[Int]) = {
    this(size, 0);
    use(indices, indices.length);
  }

  /** Use the given index and data arrays, of which the first inUsed are valid. */
  def use(inIndex : Array[Int], inUsed : Int) = {
    if (inIndex.contains((x:Int) => x < 0 || x > size))
      throw new IllegalArgumentException("Index array contains out-of-range index");
    if (inIndex == null)
      throw new IllegalArgumentException("Index must be non-null");
    if (inIndex.size < inUsed)
      throw new IllegalArgumentException("Used is greater than provided array");
    for (i <- 1 until used; if (inIndex(i-1) > inIndex(i))) {
      throw new IllegalArgumentException("Input index is not sorted at "+i);
    }
    
    index = inIndex;
    used = inUsed;
    lastOffset = -1;
    lastIndex = -1;
  }
  
  override def default_=(value : Double) = {
    if (value != 0.0) {
      throw new UnsupportedOperationException("SparseBinaryVector can only have default = 0.0.  Try SparseVector instead.");
    }
  }
  
  override def default = 0.0;
  
  override def size = domainSize;
  
  override def activeDomain = new MergeableSet[Int] {
    override def size = used;
    override def contains(i : Int) = findOffset(i) >= 0;
    override def iterator = {
      if (used == index.length) index.iterator;
      else index.iterator.take(used);
    }
  }

  override def activeElements = new Iterator[(Int,Double)] {
    var offset = 0;
    override def hasNext = offset < used;
    override def next = {
      val rv = (index(offset),1.0);
      offset += 1;
      rv;
    }
  }
  
  override def activeKeys = index.take(used).iterator;
  
  override def activeValues = index.take(used).map(_ => 1.0).iterator;

  /** Zeros this vector, return */
  override def zero() =
    use(new Array[Int](initialNonzeros), 0);
  
  /** Records that the given index was found at this.index(offset). */
  protected final def found(index : Int, offset : Int) : Int = {
    lastOffset = offset;
    lastIndex = index;
    return offset;
  }
  
  /**
   * Returns the offset into index and data for the requested vector
   * index.  If the requested index is not found, the return value is
   * negative and can be converted into an insertion point with -(rv+1).
   */
  def findOffset(i : Int) : Int = {
    if (i < 0)
      throw new IndexOutOfBoundsException("index is negative (" + index + ")");
    if (i >= size)
      throw new IndexOutOfBoundsException("index >= size (" + index + " >= " + size + ")");
    
    if (i == lastIndex) {
      // previous element; don't need to update lastOffset
      return lastOffset;
    } else if (used == 0) {
      // empty list; do nothing
      return -1;
    } else {
      // regular binary search
      var begin = 0;
      var end = used - 1;
      
      // narrow the search if we have a previous reference
      if (lastIndex >= 0 && lastOffset >= 0) {
        if (i < lastIndex) {
          // in range preceding last request
          end = lastOffset;
        } else {
          // in range following last request
          begin = lastOffset;
          
          if (begin + 1 <= end && index(begin + 1) == i) {
            // special case: successor of last request
            return found(i, begin + 1);
          }
        }
      }

      // this assert is for debug only
      //assert(begin >= 0 && end >= begin,
      //       "Invalid range: "+begin+" to "+end);
      
      var mid = (end + begin) >> 1;
      
      while (begin <= end) {
        mid = (end + begin) >> 1;
        if (index(mid) < i)
          begin = mid + 1;
        else if (index(mid) > i)
          end = mid - 1;
        else
          return found(i, mid);
      }
      
      // no match found, return insertion point
      if (i <= index(mid))
        return -(mid)-1;     // Insert here (before mid) 
      else 
        return -(mid + 1)-1; // Insert after mid
    }
  }
  
  override def apply(i : Int) : Double = {
    val offset = findOffset(i);
    if (offset >= 0) 1.0 else 0.0;
  }
  
  /**
   * Sets the given value at the given index if the value is not
   * equal to the current default.  The data and
   * index arrays will be grown to support the insertion if
   * necessary.  The growth schedule doubles the amount
   * of allocated memory at each allocation request up until
   * the sparse vector contains 1024 iterator, at which point
   * the growth is additive: an additional n * 1024 spaces will
   * be allocated for n in 1,2,4,8,16.
   */
  override def update(i : Int, value : Double) = {
    if (value != 1.0 && value != 0.0) {
      throw new IllegalArgumentException("SparseBinaryVector can only set values of 1.0 or 0.0");
    }
    
    val offset = findOffset(i);
    if (offset >= 0 && value == 0.0) {
      // remove value
      System.arraycopy(index, offset+1, index, offset, used - offset - 1);
      found(-1,-1);
      used -= 1;
    } else if (value == 1.0) {
      // need to insert at position -(offset+1)
      val insertPos = -(offset+1);
      
      used += 1;
      
      var newIndex = index;
      
      if (used > index.length) {
        val newLength = {
          if (index.length < 8) { 8 }
          else if (index.length > 16*1024) { index.length + 16*1024 }
          else if (index.length > 8*1024)  { index.length +  8*1024 }
          else if (index.length > 4*1024)  { index.length +  4*1024 }
          else if (index.length > 2*1024)  { index.length +  2*1024 }
          else if (index.length > 1*1024)  { index.length +  1*1024 }
          else { index.length * 2 }
        }
        
        // copy existing data into new arrays
        newIndex = new Array[Int](newLength);
        System.arraycopy(index, 0, newIndex, 0, insertPos);
      }
    
      // make room for insertion
      System.arraycopy(index, insertPos, newIndex, insertPos + 1, used - insertPos - 1);
      
      // assign new value
      newIndex(insertPos) = i;
      
      // record the insertion point
      found(i,insertPos);
      
      // update pointers
      index = newIndex;
    }
  }
  
  /** Compacts the vector by removing all stored default values. */
  def compact() {
    val newIndex = new Array[Int](used);
    System.arraycopy(index, 0, newIndex, 0, used);
    use(newIndex, used);
  }
  
  override def copy = {
    val rv = new SparseBinaryVector(size, 0);
    rv.use(java.util.Arrays.copyOf(index, index.length), used);
    rv;
  }

  /**
  * Creates a vector "like" this one, but with zeros everywhere.
  */
  def like = new SparseBinaryVector(size);

  /**
  * Creates a vector "like" this one, but with zeros everywhere.
  */
  def vectorLike(size:Int) = new SparseBinaryVector(size);

  /**
  * Creates a vector "like" this one, but with zeros everywhere.
  */
  def matrixLike(rows:Int, cols: Int) = throw new UnsupportedOperationException("Soon we will have SparseBinaryMatrix");

  /** Uses optimized implementations. */
  override def dot(other : Tensor1[Int]) : Double = other match {
    case singleton : SingletonBinaryVector => this.apply(singleton.singleIndex);
    case binary : SparseBinaryVector => dot(binary);
    case sparse : SparseVector => dot(sparse);
    case _ => dotTensor(other);
  }

  def dot(that : SparseBinaryVector) : Double = {
    ensure(that);
    
    var o1 = 0;    // offset into this.index
    var o2 = 0;    // offset into that.data, that.index
    var sum = 0;   // the dot product
    
    while (o1 < this.used && o2 < that.used) {
      val i1 = this.index(o1);
      val i2 = that.index(o2);
      if (i1 == i2) {
        sum += 1;
        o1 += 1;
        o2 += 1;
      } else if (i1 < i2) {
        o1 += 1;
      } else { // i2 > i1
        o2 += 1;
      }
    }
    
    sum;
  }

  /** Optimized implementation for SparseVector dot Tensor1. */
  def dotTensor(that : Tensor1[Int]) : Double = {
    ensure(that);

    var sum = 0.0;
    var o = 0;
    while (o < this.used) {
      sum += that(this.index(o));
      o += 1;
    }
    return sum;
  }
  
  /** Optimized implementation for SpraseVector dot SparseVector. */
  def dot(that : SparseVector) : Double = {
    ensure(that);
    
    var o1 = 0;    // offset into this.index
    var o2 = 0;    // offset into that.data, that.index
    var sum = 0.0; // the dot product
    
    val thatDefault = that.default;
    
    if (thatDefault == 0) {
      while (o1 < this.used && o2 < that.used) {
        val i1 = this.index(o1);
        val i2 = that.index(o2);
        if (i1 == i2) {
          sum += that.data(o2);
          o1 += 1;
          o2 += 1;
        } else if (i1 < i2) {
          o1 += 1;
        } else { // i2 > i1
          o2 += 1;
        }
      }
    } else { // thatDefault != 0
      while (o1 < this.used && o2 < that.used) {
        val i1 = this.index(o1);
        val i2 = that.index(o2);
        if (i1 == i2) {
          sum += that.data(o2);
          o1 += 1;
          o2 += 1;
        } else if (i1 < i2) {
          sum += thatDefault;
          o1 += 1;
        } else { // i2 > i1
          o2 += 1;
        }
      }
      // consume remainder of this
      sum += ((this.used - o1) * thatDefault);
    }
    
    sum;
  }
  
  /** Returns a SparseVector representation of this vector. */
  def toSparseVector() : SparseVector = {
    val data = new Array[Double](index.length);
    var i = 0;
    while (i < used) {
      data(i) = 1.0;
      i += 1;
    }
    val rv = new SparseVector(size);
    rv.use(index,data,used);
    rv;
  }
}
