package org.apache.xerces.impl.msg

import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

import org.apache.xerces.util.MessageFormatter

object XMLMessageFormatter {

  /**
   * The domain of messages concerning the XML 1.0 specification.
   */
  val XML_DOMAIN = "http://www.w3.org/TR/1998/REC-xml-19980210"

  val XMLNS_DOMAIN = "http://www.w3.org/TR/1999/REC-xml-names-19990114"
}

/**
 * XMLMessageFormatter provides error messages for the XML 1.0 Recommendation and for
 * the Namespaces Recommendation
 */
class XMLMessageFormatter extends MessageFormatter {

  private var fLocale: Locale = null

  private var fResourceBundle: ResourceBundle = null

  /**
   * Formats a message with the specified arguments using the given
   * locale information.
   *
   * @param _locale    The locale of the message.
   * @param key       The message key.
   * @param arguments The message replacement text arguments. The order
   *                  of the arguments must match that of the placeholders
   *                  in the actual message.
   *
   * @return Returns the formatted message.
   *
   * @throws MissingResourceException Thrown if the message with the
   *                                  specified key cannot be found.
   */
  def formatMessage(_locale: Locale, key: String, arguments: Array[Any]): String = {
    var locale = _locale
    if (locale eq null) {
      locale = Locale.getDefault
    }
    if (locale != fLocale) {
      fResourceBundle = ResourceBundle.getBundle("org.apache.xerces.impl.msg.XMLMessages", locale)
      fLocale = locale
    }
    var msg: String = null
    try {
      msg = fResourceBundle.getString(key)
      if (arguments ne null) {
        try {
          msg = java.text.MessageFormat.format(msg, arguments)
        } catch {
          case e: Exception ⇒
            msg = fResourceBundle.getString("FormatFailed")
            msg += " " + fResourceBundle.getString(key)
        }
      }
    } catch {
      case e: MissingResourceException ⇒
        msg = fResourceBundle.getString("BadMessageKey")
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
