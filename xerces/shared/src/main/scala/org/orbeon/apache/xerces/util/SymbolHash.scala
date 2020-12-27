/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orbeon.apache.xerces.util

class SymbolHash(fTableSize: Int) {

  import SymbolHash._

  /** Buckets. */
  protected var fBuckets = new Array[Entry](fTableSize)
  /** Number of elements. */
  protected var fNum = 0

  def this() =
    this(101)

  /**
   * Adds the key/value mapping to the key table. If the key already exists,
   * the previous value associated with this key is overwritten by the new
   * value.
   *
   * @param key
   * @param value
   */
  def put(key: Any, value: Any): Unit = {
    val bucket = (key.hashCode & 0x7FFFFFFF) % fTableSize
    var entry = search(key, bucket)

    // replace old value
    if (entry != null)
      entry.value = value
    else {
      // create new entry
      entry = Entry(key, value, fBuckets(bucket))
      fBuckets(bucket) = entry
      fNum += 1
    }
  }

  /**
   * Get the value associated with the given key.
   *
   * @param key
   * @return the value associated with the given key.
   */
  def get(key: Any): Any = {
    val bucket = (key.hashCode & 0x7FFFFFFF) % fTableSize
    val entry = search(key, bucket)
    if (entry != null)
      entry.value
    else
      null
  }

  /**
   * Get the number of key/value pairs stored in this table.
   *
   * @return the number of key/value pairs stored in this table.
   */
  def getLength: Int = fNum

  /**
   * Add all values to the given array. The array must have enough entry.
   *
   * @param elements the array to store the elements
   * @param from     where to start store element in the array
   * @return number of elements copied to the array
   */
  def getValues(elements: Array[Any], from: Int): Int = {
    var i = 0
    var j = 0
    while (i < fTableSize && j < fNum) {
      var entry = fBuckets(i)
      while (entry != null) {
        elements(from + j) = entry.value
        j += 1

        entry = entry.next
      }

      i += 1
    }
    fNum
  }

  /**
   * Return key/value pairs of all entries in the map
   */
  def getEntries: Array[Any] = {
    val entries = new Array[Any](fNum << 1)
    var i = 0
    var j = 0
    while (i < fTableSize && j < (fNum << 1)) {
      var entry = fBuckets(i)
      while (entry != null) {
        entries(j) = entry.key
        entries({
          j += 1; j
        }) = entry.value
        j += 1

        entry = entry.next
      }

      i += 1
    }
    entries
  }

  /**
   * Make a clone of this object.
   */
  def makeClone: SymbolHash = {
    val newTable = new SymbolHash(fTableSize)
    newTable.fNum = fNum
    for (i <- 0 until fTableSize)
      if (fBuckets(i) != null)
        newTable.fBuckets(i) = fBuckets(i).makeClone
    newTable
  }

  /**
   * Remove all key/value assocaition. This tries to save a bit of GC'ing
   * by at least keeping the fBuckets array around.
   */
  def clear(): Unit = {
    for (i <- 0 until fTableSize)
      fBuckets(i) = null
    fNum = 0
  }

  protected def search(key: Any, bucket: Int): Entry = {
    // search for identical key
    var entry = fBuckets(bucket)
    while (entry != null) {
      if (key == entry.key)
        return entry
      entry = entry.next
    }
    null
  }
}

object SymbolHash {

  /**
   * This class is a key table entry. Each entry acts as a node
   * in a linked list.
   */
  case class Entry(var key: Any, var value: Any, next: Entry) {
    def makeClone: Entry =
      this.copy(next = if (next ne null) next.makeClone else null)
  }
}