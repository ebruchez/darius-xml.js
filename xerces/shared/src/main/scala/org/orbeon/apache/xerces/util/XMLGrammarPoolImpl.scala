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

import org.orbeon.apache.xerces.util.XMLGrammarPoolImpl._
import org.orbeon.apache.xerces.xni.grammars.Grammar
import org.orbeon.apache.xerces.xni.grammars.XMLGrammarDescription
import org.orbeon.apache.xerces.xni.grammars.XMLGrammarPool

object XMLGrammarPoolImpl {

  /**
   Default size.
   */
  protected val TABLE_SIZE = 11

  private val DEBUG = false

  /**
   * This class is a grammar pool entry. Each entry acts as a node
   * in a linked list.
   */
  protected class Entry protected[XMLGrammarPoolImpl] (
    var hash    : Int,
    var desc    : XMLGrammarDescription,
    var grammar : Grammar,
    var next    : Entry
  ) {

    protected[XMLGrammarPoolImpl] def clear(): Unit = {
      desc = null
      grammar = null
      if (next ne null) {
        next.clear()
        next = null
      }
    }
  }
}

/**
 * Stores grammars in a pool associated to a specific key. This grammar pool
 * implementation stores two types of grammars: those keyed by the root element
 * name, and those keyed by the grammar's target namespace.
 *
 * This is the default implementation of the GrammarPool interface.
 * As we move forward, this will become more function-rich and robust.
 */
class XMLGrammarPoolImpl extends XMLGrammarPool {

  /**
   Grammars.
   */
  protected var fGrammars: Array[Entry] = new Array[Entry](TABLE_SIZE)

  protected var fPoolIsLocked: Boolean = false

  protected var fGrammarCount: Int = 0

  /**
   Constructs a grammar pool with a specified number of buckets.
   */
  def this(initialCapacity: Int) = {
    this()
    fGrammars = new Array[Entry](initialCapacity)
    fPoolIsLocked = false
  }

  def retrieveInitialGrammarSet(grammarType: String): Array[Grammar] = {
    fGrammars.synchronized {
      val grammarSize = fGrammars.length
      val tempGrammars = new Array[Grammar](fGrammarCount)
      var pos = 0
      for (i <- 0 until grammarSize) {
        var e = fGrammars(i)
        while (e ne null) {
          if (e.desc.getGrammarType == grammarType) {
            tempGrammars(pos) = e.grammar
            pos += 1
          }
          e = e.next
          e = e.next
        }
      }
      val toReturn = new Array[Grammar](pos)
      System.arraycopy(tempGrammars, 0, toReturn, 0, pos)
      toReturn
    }
  }

  def cacheGrammars(grammarType: String, grammars: Array[Grammar]): Unit = {
    if (!fPoolIsLocked) {
      for (i <- grammars.indices) {
        if (DEBUG) {
          println("CACHED GRAMMAR " + (i + 1))
          val temp = grammars(i)
        }
        putGrammar(grammars(i))
      }
    }
  }

  def retrieveGrammar(desc: XMLGrammarDescription): Grammar = {
    if (DEBUG) {
      println("RETRIEVING GRAMMAR FROM THE APPLICATION WITH FOLLOWING DESCRIPTION :")
    }
    getGrammar(desc)
  }

  /**
   * Puts the specified grammar into the grammar pool and associates it to
   * its root element name or its target namespace.
   *
   * @param grammar The Grammar.
   */
  def putGrammar(grammar: Grammar): Unit = {
    if (!fPoolIsLocked) {
      fGrammars.synchronized {
        val desc = grammar.getGrammarDescription
        val hash = hashCode(desc)
        val index = (hash & 0x7FFFFFFF) % fGrammars.length
        locally {
          var entry = fGrammars(index)
          while (entry ne null) {
            if (entry.hash == hash && ==(entry.desc, desc)) {
              entry.grammar = grammar
              return
            }
            entry = entry.next
          }
        }
        val entry = new Entry(hash, desc, grammar, fGrammars(index))
        fGrammars(index) = entry
        fGrammarCount += 1
      }
    }
  }

  /**
   * Returns the grammar associated to the specified grammar description.
   * Currently, the root element name is used as the key for DTD grammars
   * and the target namespace  is used as the key for Schema grammars.
   *
   * @param desc The Grammar Description.
   */
  def getGrammar(desc: XMLGrammarDescription): Grammar = {
    fGrammars.synchronized {
      val hash = hashCode(desc)
      val index = (hash & 0x7FFFFFFF) % fGrammars.length
      var entry = fGrammars(index)
      while (entry ne null) {
        if ((entry.hash == hash) && ==(entry.desc, desc)) {
          return entry.grammar
        }
        entry = entry.next
        entry = entry.next
      }
      null
    }
  }

  /**
   * Removes the grammar associated to the specified grammar description from the
   * grammar pool and returns the removed grammar. Currently, the root element name
   * is used as the key for DTD grammars and the target namespace  is used
   * as the key for Schema grammars.
   *
   * @param desc The Grammar Description.
   * @return     The removed grammar.
   */
  def removeGrammar(desc: XMLGrammarDescription): Grammar = {
    fGrammars.synchronized {
      val hash = hashCode(desc)
      val index = (hash & 0x7FFFFFFF) % fGrammars.length
      var entry = fGrammars(index)
      var prev: Entry = null
      while (entry ne null) {
        if ((entry.hash == hash) && ==(entry.desc, desc)) {
          if (prev ne null) {
            prev.next = entry.next
          } else {
            fGrammars(index) = entry.next
          }
          val tempGrammar = entry.grammar
          entry.grammar = null
          fGrammarCount -= 1
          return tempGrammar
        }
        prev = entry
        entry = entry.next
        prev = entry
        entry = entry.next
      }
      null
    }
  }

  /**
   * Returns true if the grammar pool contains a grammar associated
   * to the specified grammar description. Currently, the root element name
   * is used as the key for DTD grammars and the target namespace  is used
   * as the key for Schema grammars.
   *
   * @param desc The Grammar Description.
   */
  def containsGrammar(desc: XMLGrammarDescription): Boolean = {
    fGrammars.synchronized {
      val hash = hashCode(desc)
      val index = (hash & 0x7FFFFFFF) % fGrammars.length
      var entry = fGrammars(index)
      while (entry ne null) {
        if ((entry.hash == hash) && ==(entry.desc, desc)) {
          return true
        }
        entry = entry.next
        entry = entry.next
      }
      false
    }
  }

  def lockPool(): Unit = {
    fPoolIsLocked = true
  }

  def unlockPool(): Unit = {
    fPoolIsLocked = false
  }

  def clear(): Unit = {
    for (i <- fGrammars.indices if fGrammars(i) ne null) {
      fGrammars(i).clear()
      fGrammars(i) = null
    }
    fGrammarCount = 0
  }

  /**
   * This method checks whether two grammars are the same. Currently, we compare
   * the root element names for DTD grammars and the target namespaces for Schema grammars.
   * The application can override this behaviour and add its own logic.
   *
   * @param desc1 The grammar description
   * @param desc2 The grammar description of the grammar to be compared to
   * @return      True if the grammars are equal, otherwise false
   */
  def equals(desc1: XMLGrammarDescription, desc2: XMLGrammarDescription): Boolean = desc1 == desc2

  /**
   * Returns the hash code value for the given grammar description.
   *
   * @param desc The grammar description
   * @return     The hash code value
   */
  def hashCode(desc: XMLGrammarDescription): Int = desc.hashCode
}
