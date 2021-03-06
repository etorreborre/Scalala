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
package collection;

/**
 * An extension of a PartialMap that supports value updates.
 * 
 * @author dramage
 */
trait MutablePartialMap[A,B] extends PartialMap[A,B] {
  /** Update the default value. */
  def default_=(d : B) : Unit;
  
  /**
   * Update an individual value.  The given key must be in the
   * map's domain, but need not be in its activeDomain.
   */
  def update(key : A, value : B) : Unit;
  
  /** Batch update of keys and values. */
  def update(keys : Seq[A], values : Seq[B]) : Unit =
    update(keys.iterator, values.iterator);
  
  /** Batch update of keys and values. */
  def update(keys : Iterator[A], values : Iterator[B]) : Unit = {
    for ((key,value) <- (keys zip values)) update(key,value);
    if (keys.hasNext || values.hasNext) {
      throw new MutablePartialMap.UpdateException(
        "Keys and values had different numbers of iterator");
    }
  }
  
  /** Batch update of keys with single value. */
  def update(keys : Iterator[A], value : B) : Unit =
    for (key <- keys) update(key,value);
  
  /** Batch update of keys with single value. */
  def update(keys : Iterable[A], value : B) : Unit =
    update(keys.iterator, value);
  
  /** A set is both an Iterable and a (A=>Boolean), so it is special-cased. */
  def update(keys : scala.collection.Set[A], value : B) : Unit =
    update(keys.iterator, value);
  
  /** Batch update of keys for which f returns true. */
  def update(s : Function1[A,Boolean], value : B) : Unit =
    for (key <- this.keysIterator) if (s(key)) update(key,value);
  
  /** Batch update of keys based on a function applied to the value. */
  def update(keys : Iterator[A], f : Function1[B,B]) : Unit =
    for (key <- keys) update(key,f(apply(key)));
  
  /** Batch update of keys based on a function applied to the value. */
  def update(keys : Iterable[A], f : Function1[B,B]) : Unit =
    update(keys.iterator, f);
  
  /** A set is both an Iterable and a (A=>Boolean), so it is special-cased. */
  def update(keys : scala.collection.Set[A], f : Function1[B,B]) : Unit =
    update(keys.iterator, f);
  
  /** Batch update of keys for which f returns true. */
  def update(s : Function1[A,Boolean], f : Function1[B,B]) : Unit =
    for ((key,value) <- iterator) if (s(key)) update(key,f(value));
  
  /** Batch update of keys based on a function applied to the key and value. */
  def update(keys : Iterator[A], f : Function2[A,B,B]) : Unit =
    for (key <- keys) update(key,f(key,apply(key)));
  
  /** Batch update of keys based on a function applied to the key and value. */
  def update(keys : Iterable[A], f : Function2[A,B,B]) : Unit =
    update(keys.iterator, f);
  
  /** A set is both an Iterable and a (A=>Boolean), so it is special-cased. */
  def update(keys : scala.collection.Set[A], f : Function2[A,B,B]) : Unit =
    update(keys.iterator, f);
    
  /** Batch update of keys for which f returns true. */
  def update(s : Function1[A,Boolean], f : Function2[A,B,B]) : Unit =
    for ((key,value) <- iterator) if (s(key)) update(key,f(key,value));

  def transform(f : B=>B) {
    default = f(default);
    update(activeDomain,f);
  }
}

object MutablePartialMap {
  class UpdateException(msg : String) extends RuntimeException(msg);
}
