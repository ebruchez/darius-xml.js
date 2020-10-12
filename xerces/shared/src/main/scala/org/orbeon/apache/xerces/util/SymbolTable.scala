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

import org.orbeon.apache.xerces.util.SymbolTable._

import scala.util.control.Breaks

private object SymbolTable {

  /**
   Default table size.
   */
  val TABLE_SIZE = 101

  /**
   * This class is a symbol table entry. Each entry acts as a node
   * in a linked list.
   */
  class Entry(val symbol: String, val characters: Array[Char], var next: Entry)

  object Entry {
    def apply(symbol: String, next: Entry): Entry = {

      val characters = new Array[Char](symbol.length)
      symbol.getChars(0, characters.length, characters, 0)

      new Entry(
        symbol.intern(),
        characters,
        next
      )
    }

    /**
     * Constructs a new entry from the specified symbol information and
     * next entry reference.
     */
    def apply(
      ch     : Array[Char],
      offset : Int,
      length : Int,
      next   : Entry
    ): Entry = {

      val characters = new Array[Char](length)
      System.arraycopy(ch, offset, characters, 0, length)

      new Entry(
        new String(characters).intern(),
        characters,
        next
      )
    }
  }
}

/**
 * This class is a symbol table implementation that guarantees that
 * strings used as identifiers are unique references. Multiple calls
 * to `addSymbol` will always return the same string
 * reference.
 *
 * The symbol table performs the same task as `String.intern()`
 * with the following differences:
 *
 *  -
 *   A new string object does not need to be created in order to
 *   retrieve a unique reference. Symbols can be added by using
 *   a series of characters in a character array.
 *
 *  -
 *   Users of the symbol table can provide their own symbol hashing
 *   implementation. For example, a simple string hashing algorithm
 *   may fail to produce a balanced set of hashcodes for symbols
 *   that are *mostly* unique. Strings with similar leading
 *   characters are especially prone to this poor hashing behavior.
 *
 *
 *
 * An instance of `SymbolTable` has two parameters that affect its
 * performance: *initial capacity* and *load factor*.  The
 * *capacity* is the number of *buckets* in the SymbolTable, and the
 * *initial capacity* is simply the capacity at the time the SymbolTable
 * is created.  Note that the SymbolTable is *open*: in the case of a "hash
 * collision", a single bucket stores multiple entries, which must be searched
 * sequentially.  The *load factor* is a measure of how full the SymbolTable
 * is allowed to get before its capacity is automatically increased.
 * When the number of entries in the SymbolTable exceeds the product of the load
 * factor and the current capacity, the capacity is increased by calling the
 * `rehash` method.
 *
 * Generally, the default load factor (.75) offers a good tradeoff between
 * time and space costs.  Higher values decrease the space overhead but
 * increase the time cost to look up an entry (which is reflected in most
 * `SymbolTable` operations, including `addSymbol` and `containsSymbol`).
 *
 * The initial capacity controls a trade-off between wasted space and the
 * need for `rehash` operations, which are time-consuming.
 * No `rehash` operations will *ever* occur if the initial
 * capacity is greater than the maximum number of entries the
 * hash table will contain divided by its load factor.  However,
 * setting the initial capacity too high can waste space.
 *
 * If many entries are to be made into a `SymbolTable`,
 * creating it with a sufficiently large capacity may allow the
 * entries to be inserted more efficiently than letting it perform
 * automatic rehashing as needed to grow the table.
 */
class SymbolTable(protected var fTableSize: Int, protected var fLoadFactor: Float) {

  private var fBuckets: Array[Entry] = new Array[Entry](fTableSize)

  /**
   The total number of entries in the hash table.
   */
  private var fCount: Int = 0

  /**
   The table is rehashed when its size exceeds this threshold.  (The
   * value of this field is (int)(capacity * loadFactor).)
   */
  private var fThreshold: Int = (fTableSize * fLoadFactor).toInt

  locally {
    if (fTableSize < 0) {
      throw new IllegalArgumentException("Illegal Capacity: " + fTableSize)
    }

    if (fLoadFactor <= 0 || fLoadFactor.isNaN) {
      throw new IllegalArgumentException("Illegal Load: " + fLoadFactor)
    }

    if (fTableSize == 0) {
      fTableSize = 1
    }
  }

  /**
   * Constructs a new, empty SymbolTable with the specified initial capacity
   * and default load factor, which is `0.75`.
   *
   * @param     initialCapacity   the initial capacity of the hash table.
   * @throws    IllegalArgumentException if the initial capacity is less
   *            than zero.
   */
  def this(initialCapacity: Int) =
    this(initialCapacity, 0.75f)

  /**
   * Constructs a new, empty SymbolTable with a default initial capacity (101)
   * and load factor, which is `0.75`.
   */
  def this() =
    this(TABLE_SIZE, 0.75f)

  /**
   * Adds the specified symbol to the symbol table and returns a
   * reference to the unique symbol. If the symbol already exists,
   * the previous symbol reference is returned instead, in order
   * guarantee that symbol references remain unique.
   *
   * @param symbol The new symbol.
   */
  def addSymbol(symbol: String): String = {
    var bucket = hash(symbol) % fTableSize
    locally {
      var entry = fBuckets(bucket)
      while (entry ne null) {
        if (entry.symbol == symbol) {
          return entry.symbol
        }
        entry = entry.next
      }
    }
    if (fCount >= fThreshold) {
      rehash()
      bucket = hash(symbol) % fTableSize
    }
    val entry = Entry(symbol, fBuckets(bucket))
    fBuckets(bucket) = entry
    fCount += 1
    entry.symbol
  }

  /**
   * Adds the specified symbol to the symbol table and returns a
   * reference to the unique symbol. If the symbol already exists,
   * the previous symbol reference is returned instead, in order
   * guarantee that symbol references remain unique.
   *
   * @param buffer The buffer containing the new symbol.
   * @param offset The offset into the buffer of the new symbol.
   * @param length The length of the new symbol in the buffer.
   */
  def addSymbol(buffer: Array[Char], offset: Int, length: Int): String = {
    var bucket = hash(buffer, offset, length) % fTableSize
    val whileBreaks = new Breaks
    whileBreaks.breakable {
      var entry = fBuckets(bucket)
      while (entry ne null) {
        if (length == entry.characters.length) {
          for (i <- 0 until length if buffer(offset + i) != entry.characters(i)) {
            whileBreaks.break()
          }
          return entry.symbol
        }
        entry = entry.next
      }
    }
    if (fCount >= fThreshold) {
      rehash()
      bucket = hash(buffer, offset, length) % fTableSize
    }
    val entry = Entry(buffer, offset, length, fBuckets(bucket))
    fBuckets(bucket) = entry
    fCount += 1
    entry.symbol
  }

  /**
   * Returns a hashcode value for the specified symbol. The value
   * returned by this method must be identical to the value returned
   * by the `hash(char[],int,int)` method when called
   * with the character array that comprises the symbol string.
   *
   * @param symbol The symbol to hash.
   */
  def hash(symbol: String): Int = symbol.hashCode & 0x7FFFFFFF

  /**
   * Returns a hashcode value for the specified symbol information.
   * The value returned by this method must be identical to the value
   * returned by the `hash(String)` method when called
   * with the string object created from the symbol information.
   *
   * @param buffer The character buffer containing the symbol.
   * @param offset The offset into the character buffer of the start
   *               of the symbol.
   * @param length The length of the symbol.
   */
  def hash(buffer: Array[Char], offset: Int, length: Int): Int = {
    var code = 0
    for (i <- 0 until length) {
      code = code * 31 + buffer(offset + i)
    }
    code & 0x7FFFFFFF
  }

  /**
   * Increases the capacity of and internally reorganizes this
   * SymbolTable, in order to accommodate and access its entries more
   * efficiently.  This method is called automatically when the
   * number of keys in the SymbolTable exceeds this hash table's capacity
   * and load factor.
   */
  protected def rehash(): Unit = {
    val oldCapacity = fBuckets.length
    val oldTable = fBuckets
    val newCapacity = oldCapacity * 2 + 1
    val newTable = new Array[Entry](newCapacity)
    fThreshold = (newCapacity * fLoadFactor).toInt
    fBuckets = newTable
    fTableSize = fBuckets.length
    var i = oldCapacity
    while (i  > 0) {
      var old = oldTable(i - 1)
      while (old ne null) {
        val e = old
        old = old.next
        val index = hash(e.characters, 0, e.characters.length) % newCapacity
        e.next = newTable(index)
        newTable(index) = e
      }
      i -= 1
    }
  }

  /**
   * Returns true if the symbol table already contains the specified
   * symbol.
   *
   * @param symbol The symbol to look for.
   */
  def containsSymbol(symbol: String): Boolean = {
    val bucket = hash(symbol) % fTableSize
    val length = symbol.length
    val whileBreaks = new Breaks
    whileBreaks.breakable {
      var entry = fBuckets(bucket)
      while (entry ne null) {
        if (length == entry.characters.length) {
          for (i <- 0 until length if symbol.charAt(i) != entry.characters(i)) {
            whileBreaks.break()
          }
          return true
        }
        entry = entry.next
      }
    }
    false
  }

  /**
   * Returns true if the symbol table already contains the specified
   * symbol.
   *
   * @param buffer The buffer containing the symbol to look for.
   * @param offset The offset into the buffer.
   * @param length The length of the symbol in the buffer.
   */
  def containsSymbol(buffer: Array[Char], offset: Int, length: Int): Boolean = {
    val bucket = hash(buffer, offset, length) % fTableSize
    val whileBreaks = new Breaks
    whileBreaks.breakable {
      var entry = fBuckets(bucket)
      while (entry ne null) {
        if (length == entry.characters.length) {
          for (i <- 0 until length if buffer(offset + i) != entry.characters(i)) {
            whileBreaks.break()
          }
          return true
        }
        entry = entry.next
      }
    }
    false
  }

  // @ebruchez
  def dumpEntries(): Unit = {
    println("Symbol table contents:")
    for (topLevelEntry <- fBuckets if topLevelEntry ne null) {
      Iterator.iterate(topLevelEntry)(_.next) takeWhile (_ ne null) foreach { e =>
        println(s"""  ${e.symbol}""")
      }
    }
  }
}
