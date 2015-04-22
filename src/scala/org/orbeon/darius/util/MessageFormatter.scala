package org.orbeon.darius.util

import java.util.Locale
import java.util.MissingResourceException

/**
 * This interface provides a generic message formatting mechanism and
 * is useful for producing messages that must be localized and/or formatted
 * with replacement text.
 *
 * @see org.orbeon.darius.impl.XMLErrorReporter
 */
trait MessageFormatter {

  /**
   * Formats a message with the specified arguments using the given
   * locale information.
   *
   * @param locale    The locale of the message.
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
  def formatMessage(locale: Locale, key: String, arguments: Array[Any]): String
}
