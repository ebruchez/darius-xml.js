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

class SymbolHash(protected var fTableSize: Int) {

  import SymbolHash._

  /** Buckets. */
  protected var fBuckets = new Array[Entry](fTableSize)
  /** Number of elements. */
  protected var fNum = 0

  /**
   * Array of randomly selected hash function multipliers or `null`
   * if the default String.hashCode() function should be used.
   */
  protected var fHashMultipliers: Array[Int] = null

  def this() =
    this(SymbolHash.TABLE_SIZE)

  /**
   * Adds the key/value mapping to the key table. If the key already exists,
   * the previous value associated with this key is overwritten by the new
   * value.
   *
   * @param key
   * @param value
   */
  def put(key: Any, value: Any): Unit = {
    // search for identical key
    var collisionCount = 0
    val hash = this.hash(key)
    var bucket = hash % fTableSize
    var entry = fBuckets(bucket)
    while (entry ne null) {
      if (key == entry.key) {
        // replace old value
        entry.value = value
        return
      }
      collisionCount += 1
      entry = entry.next
    }

    if (fNum >= fTableSize) {
      // Rehash the table if the number of entries
      // would exceed the number of buckets.
      rehash()
      bucket = hash % fTableSize
    } else if (collisionCount >= MAX_HASH_COLLISIONS && key.isInstanceOf[String]) {
      // Select a new hash function and rehash the table if
      // MAX_HASH_COLLISIONS is exceeded.
      rebalance()
      bucket = this.hash(key) % fTableSize
    }

    // create new entry
    entry = Entry(key, value, fBuckets(bucket))
    fBuckets(bucket) = entry
    fNum += 1
  }

  /**
   * Get the value associated with the given key.
   *
   * @param key
   * @return the value associated with the given key.
   */
  def get(key: Any): Any = {
    val bucket = hash(key) % fTableSize
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
    newTable.fHashMultipliers = if (fHashMultipliers ne null) fHashMultipliers.clone() else null
    for (i <- 0 until fTableSize)
      if (fBuckets(i) != null)
        newTable.fBuckets(i) = fBuckets(i).makeClone
    newTable
  }

  /**
   * Remove all key/value association. This tries to save a bit of GC'ing
   * by at least keeping the fBuckets array around.
   */
  def clear(): Unit = {
    for (i <- 0 until fTableSize)
      fBuckets(i) = null
    fNum = 0
    fHashMultipliers = null
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

  /**
   * Returns a hashcode value for the specified key.
   *
   * @param key The key to hash.
   */
  protected def hash(key: Any): Int =
    if ((fHashMultipliers eq null) || !key.isInstanceOf[String])
      key.hashCode & 0x7FFFFFFF
    else
      hash0(key.asInstanceOf[String])

  private def hash0(symbol: String): Int = {
    var code = 0
    val length = symbol.length
    val multipliers = fHashMultipliers
    var i = 0
    while (i < length) {
      code = code * multipliers(i & MULTIPLIERS_MASK) + symbol.charAt(i)
      i += 1
    }
    code & 0x7FFFFFFF
  }

  /**
   * Increases the capacity of and internally reorganizes this
   * SymbolHash, in order to accommodate and access its entries more
   * efficiently. This method is called automatically when the
   * number of keys in the SymbolHash exceeds its number of buckets.
   */
  protected def rehash(): Unit =
    rehashCommon((fBuckets.length << 1) + 1)

  /**
   * Randomly selects a new hash function and reorganizes this SymbolHash
   * in order to more evenly distribute its entries across the table. This
   * method is called automatically when the number keys in one of the
   * SymbolHash's buckets exceeds MAX_HASH_COLLISIONS.
   */
  protected def rebalance(): Unit = {
    if (fHashMultipliers eq null)
      fHashMultipliers = new Array[Int](MULTIPLIERS_SIZE)
    PrimeNumberSequenceGenerator.generateSequence(fHashMultipliers)
    rehashCommon(fBuckets.length)
  }

  private def rehashCommon(newCapacity: Int): Unit = {
    val oldCapacity = fBuckets.length
    val oldTable = fBuckets

    val newTable = new Array[Entry](newCapacity)

    fBuckets = newTable
    fTableSize = fBuckets.length

    var i = oldCapacity
    while (i > 0) {
      i -= 1
      var old = oldTable(i)
      while (old ne null) {
        val e = old
        old = old.next

        val index = hash(e.key) % newCapacity
        e.next = newTable(index)
        newTable(index) = e
      }
    }
  }
}

object SymbolHash {

  /** Default table size. */
  protected val TABLE_SIZE = 101

  /** Maximum hash collisions per bucket. */
  protected val MAX_HASH_COLLISIONS = 40

  protected val MULTIPLIERS_SIZE = 1 << 5
  protected val MULTIPLIERS_MASK = MULTIPLIERS_SIZE - 1

  /**
   * This class is a key table entry. Each entry acts as a node
   * in a linked list.
   */
  case class Entry(var key: Any, var value: Any, var next: Entry) {
    def makeClone: Entry =
      this.copy(next = if (next ne null) next.makeClone else null)
  }
}