package org.orbeon.darius.util

import org.orbeon.darius.util.XMLStringBuffer._
import org.orbeon.darius.xni.XMLString

object XMLStringBuffer {

  /**
   Default buffer size (32).
   */
  val DEFAULT_SIZE = 32
}

/**
 * XMLString is a structure used to pass character arrays. However,
 * XMLStringBuffer is a buffer in which characters can be appended
 * and extends XMLString so that it can be passed to methods
 * expecting an XMLString object. This is a safe operation because
 * it is assumed that any callee will *not* modify
 * the contents of the XMLString structure.
 * 
 * The contents of the string are managed by the string buffer. As
 * characters are appended, the string buffer will grow as needed.
 * 
 * *Note:* Never set the `ch`,
 * `offset`, and `length` fields directly.
 * These fields are managed by the string buffer. In order to reset
 * the buffer, call `clear()`.
 */
class XMLStringBuffer(size: Int) extends XMLString {

  ch = new Array[Char](size)

  def this() {
    this(DEFAULT_SIZE)
  }

  /**
   Constructs a string buffer from a char.
   */
  def this(c: Char) {
    this(1)
    append(c)
  }

  /**
   Constructs a string buffer from a String.
   */
  def this(s: String) {
    this(s.length)
    append(s)
  }

  /**
   Constructs a string buffer from the specified character array.
   */
  def this(ch: Array[Char], offset: Int, length: Int) {
    this(length)
    append(ch, offset, length)
  }

  /**
   Constructs a string buffer from the specified XMLString.
   */
  def this(s: XMLString) {
    this(s.length)
    append(s)
  }

  override def clear(): Unit = {
    offset = 0
    length = 0
  }

  def append(c: Char): Unit = {
    if (this.length + 1 > this.ch.length) {
      var newLength = this.ch.length * 2
      if (newLength < this.ch.length + DEFAULT_SIZE) newLength = this.ch.length + DEFAULT_SIZE
      val newch = new Array[Char](newLength)
      System.arraycopy(this.ch, 0, newch, 0, this.length)
      this.ch = newch
    }
    this.ch(this.length) = c
    this.length += 1
  }

  def append(s: String): Unit = {
    val length = s.length
    if (this.length + length > this.ch.length) {
      var newLength = this.ch.length * 2
      if (newLength < this.length + length + DEFAULT_SIZE) newLength = this.ch.length + length + DEFAULT_SIZE
      val newch = new Array[Char](newLength)
      System.arraycopy(this.ch, 0, newch, 0, this.length)
      this.ch = newch
    }
    s.getChars(0, length, this.ch, this.length)
    this.length += length
  }

  def append(ch: Array[Char], offset: Int, length: Int): Unit = {
    if (this.length + length > this.ch.length) {
      val newch = new Array[Char](this.ch.length + length + DEFAULT_SIZE)
      System.arraycopy(this.ch, 0, newch, 0, this.length)
      this.ch = newch
    }
    System.arraycopy(ch, offset, this.ch, this.length, length)
    this.length += length
  }

  def append(s: XMLString): Unit = {
    append(s.ch, s.offset, s.length)
  }
}
