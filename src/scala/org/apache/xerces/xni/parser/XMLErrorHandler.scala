package org.apache.xerces.xni.parser

import org.apache.xerces.xni.XNIException

/**
 * An interface for handling errors. If the application is interested
 * in error notifications, then it can register an error handler object
 * that implements this interface with the parser configuration.
 *
 * @see XMLParserConfiguration
 */
trait XMLErrorHandler {

  /**
   * Reports a warning. Warnings are non-fatal and can be safely ignored
   * by most applications.
   *
   * @param domain    The domain of the warning. The domain can be any
   *                  string but is suggested to be a valid URI. The
   *                  domain can be used to conveniently specify a web
   *                  site location of the relevant specification or
   *                  document pertaining to this warning.
   * @param key       The warning key. This key can be any string and
   *                  is implementation dependent.
   * @param exception Exception.
   *
   * @throws XNIException Thrown to signal that the parser should stop
   *                      parsing the document.
   */
  def warning(domain: String, key: String, exception: XMLParseException): Unit

  /**
   * Reports an error. Errors are non-fatal and usually signify that the
   * document is invalid with respect to its grammar(s).
   *
   * @param domain    The domain of the error. The domain can be any
   *                  string but is suggested to be a valid URI. The
   *                  domain can be used to conveniently specify a web
   *                  site location of the relevant specification or
   *                  document pertaining to this error.
   * @param key       The error key. This key can be any string and
   *                  is implementation dependent.
   * @param exception Exception.
   *
   * @throws XNIException Thrown to signal that the parser should stop
   *                      parsing the document.
   */
  def error(domain: String, key: String, exception: XMLParseException): Unit

  /**
   * Report a fatal error. Fatal errors usually occur when the document
   * is not well-formed and signifies that the parser cannot continue
   * normal operation.
   * 
   * *Note:* The error handler should *always*
   * throw an `XNIException` from this method. This exception
   * can either be the same exception that is passed as a parameter to
   * the method or a new XNI exception object. If the registered error
   * handler fails to throw an exception, the continuing operation of
   * the parser is undetermined.
   *
   * @param domain    The domain of the fatal error. The domain can be
   *                  any string but is suggested to be a valid URI. The
   *                  domain can be used to conveniently specify a web
   *                  site location of the relevant specification or
   *                  document pertaining to this fatal error.
   * @param key       The fatal error key. This key can be any string
   *                  and is implementation dependent.
   * @param exception Exception.
   *
   * @throws XNIException Thrown to signal that the parser should stop
   *                      parsing the document.
   */
  def fatalError(domain: String, key: String, exception: XMLParseException): Unit
}
