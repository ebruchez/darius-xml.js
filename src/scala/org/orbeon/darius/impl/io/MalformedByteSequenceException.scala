package org.orbeon.darius.impl.io

import java.util.Locale

import org.orbeon.darius.util.MessageFormatter

/**
 * Signals that a malformed byte sequence was detected
 * by a `java.io.Reader` that decodes bytes
 * of a given encoding into characters.
 */
class MalformedByteSequenceException(
  var fFormatter : MessageFormatter, 
  var fLocale    : Locale,
  var fDomain    : String, 
  var fKey       : String, 
  var fArguments : Array[Any]
) extends Exception {

  /**
   message text for this message, initially null *
   */
  private var fMessage: String = _

  /**
   * Returns the error domain of the error message.
   *
   * @return the error domain
   */
  def getDomain: String = fDomain

  /**
   * Returns the key of the error message.
   *
   * @return the error key of the error message
   */
  def getKey: String = fKey

  /**
   * Returns the replacement arguments for the error
   * message or `null` if none exist.
   *
   * @return the replacement arguments for the error message
   * or `null` if none exist
   */
  def getArguments: Array[Any] = fArguments

  /**
   * Returns the localized message for this exception.
   *
   * @return the localized message for this exception.
   */
  override def getMessage: String = {
    synchronized {
      if (fMessage eq null) {
        fMessage = fFormatter.formatMessage(fLocale, fKey, fArguments)
        fFormatter = null
        fLocale = null
      }
      fMessage
    }
  }
}
