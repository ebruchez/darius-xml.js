package org.apache.xerces.util

import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

object DatatypeMessageFormatter {

  private val BASE_NAME = "org.apache.xerces.impl.msg.DatatypeMessages"

  /**
   * Formats a message with the specified arguments using the given
   * locale information.
   *
   * @param _locale   The locale of the message.
   * @param key       The message key.
   * @param arguments The message replacement text arguments. The order
   *                  of the arguments must match that of the placeholders
   *                  in the actual message.
   *
   * @return          the formatted message.
   *
   * @throws MissingResourceException Thrown if the message with the
   *                                  specified key cannot be found.
   */
  def formatMessage(_locale: Locale, key: String, arguments: Array[Any]): String = {
    var locale = _locale
    if (locale eq null) {
      locale = Locale.getDefault
    }
    val resourceBundle = ResourceBundle.getBundle(BASE_NAME, locale)
    var msg: String = null
    try {
      msg = resourceBundle.getString(key)
      if (arguments ne null) {
        try {
          msg = java.text.MessageFormat.format(msg, arguments)
        } catch {
          case e: Exception ⇒
            msg = resourceBundle.getString("FormatFailed")
            msg += " " + resourceBundle.getString(key)
        }
      }
    } catch {
      case e: MissingResourceException ⇒
        msg = resourceBundle.getString("BadMessageKey")
        throw new MissingResourceException(key, msg, key)
    }
    if (msg eq null) {
      msg = key
      if (arguments.length > 0) {
        val str = new StringBuffer(msg)
        str.append('?')
        for (i ← arguments.indices) {
          if (i > 0) {
            str.append('&')
          }
          str.append(String.valueOf(arguments(i)))
        }
      }
    }
    msg
  }
}
