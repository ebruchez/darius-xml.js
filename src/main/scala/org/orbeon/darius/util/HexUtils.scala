package org.orbeon.darius.util

object HexUtils {
  
  def toHexString(_i: Int): String = {
    
    var i = _i
    val buf = new Array[Char](33)
    val negative = i < 0
    var charPos = 32
    if (! negative)
      i = -i
    while (i <= -Radix) {
      buf(charPos) = HexDigits(-(i % Radix))
      charPos -= 1
      i = i / Radix
    }
    buf(charPos) = HexDigits(-i)
    if (negative) {
      charPos -= 1
      buf(charPos) = '-'
    }
    new String(buf, charPos, 33 - charPos)
  }

  private val Radix = 16
  private val HexDigits = Array[Char]('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
}
