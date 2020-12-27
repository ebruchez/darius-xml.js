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

import org.orbeon.apache.xerces.xni.{XMLLocator, XNIException}
import org.orbeon.apache.xerces.xni.parser.{XMLErrorHandler, XMLParseException}
import org.xml.sax.{ErrorHandler, SAXException, SAXParseException}


/**
 * This class wraps a SAX error handler in an XNI error handler.
 *
 * @see ErrorHandler
 * @author Andy Clark, IBM
 * @version $Id: ErrorHandlerWrapper.java 447241 2006-09-18 05:12:57Z mrglavas $
 */
object ErrorHandlerWrapper {

  /** Creates a SAXParseException from an XMLParseException. */
  protected def createSAXParseException(exception: XMLParseException) = new SAXParseException(
    exception.getMessage, exception.getPublicId, exception.getExpandedSystemId, exception.getLineNumber,
    exception.getColumnNumber, exception.getException
  )

  /** Creates an XMLParseException from a SAXParseException. */
  protected def createXMLParseException(exception: SAXParseException): XMLParseException = {
    val fPublicId         = exception.getPublicId
    val fExpandedSystemId = exception.getSystemId
    val fLineNumber       = exception.getLineNumber
    val fColumnNumber     = exception.getColumnNumber
    val location: XMLLocator = new XMLLocator {
      def getPublicId: String = fPublicId
      def getExpandedSystemId: String = fExpandedSystemId
      def getBaseSystemId: String = null
      def getLiteralSystemId: String = null
      def getColumnNumber: Int = fColumnNumber
      def getLineNumber: Int = fLineNumber
      def getCharacterOffset: Int = -1
      def getEncoding: String = null
      def getXMLVersion: String = null
    }
    new XMLParseException(location, exception.getMessage, exception)
  }
  /** Creates an XNIException from a SAXException.
   * NOTE:  care should be taken *not* to call this with a SAXParseException; this will
   * lose information!!! */
  protected def createXNIException(exception: SAXException) = new XNIException(exception.getMessage, exception)
}

class ErrorHandlerWrapper extends XMLErrorHandler {

  /** The SAX error handler. */
  protected var fErrorHandler: ErrorHandler = null

  /** Wraps the specified SAX error handler. */
  def this(errorHandler: ErrorHandler) {
    this()
    setErrorHandler(errorHandler)
  }

  /** Sets the SAX error handler. */
  def setErrorHandler(errorHandler: ErrorHandler): Unit = fErrorHandler = errorHandler

  /** Returns the SAX error handler. */
  def getErrorHandler: ErrorHandler = fErrorHandler

  /**
   * Reports a warning. Warnings are non-fatal and can be safely ignored
   * by most applications.
   *
   * @param domain    The domain of the warning. The domain can be any
   *                  string but is suggested to be a valid URI. The
   *                  domain can be used to conveniently specify a web
   *                  site location of the relevent specification or
   *                  document pertaining to this warning.
   * @param key       The warning key. This key can be any string and
   *                  is implementation dependent.
   * @param exception Exception.
   * @throws XNIException Thrown to signal that the parser should stop
   *                      parsing the document.
   */
  def warning(
    domain   : String,
    key      : String,
    exception: XMLParseException
  ): Unit =
    if (fErrorHandler != null) {
      val saxException = ErrorHandlerWrapper.createSAXParseException(exception)
      try
        fErrorHandler.warning(saxException)
      catch {
        case e: SAXParseException =>
          throw ErrorHandlerWrapper.createXMLParseException(e)
        case e: SAXException      =>
          throw ErrorHandlerWrapper.createXNIException(e)
      }
    }

  /**
   * Reports an error. Errors are non-fatal and usually signify that the
   * document is invalid with respect to its grammar(s).
   *
   * @param domain    The domain of the error. The domain can be any
   *                  string but is suggested to be a valid URI. The
   *                  domain can be used to conveniently specify a web
   *                  site location of the relevent specification or
   *                  document pertaining to this error.
   * @param key       The error key. This key can be any string and
   *                  is implementation dependent.
   * @param exception Exception.
   * @throws XNIException Thrown to signal that the parser should stop
   *                      parsing the document.
   */
  def error(
    domain   : String,
    key      : String,
    exception: XMLParseException
  ): Unit =
    if (fErrorHandler != null) {
      val saxException = ErrorHandlerWrapper.createSAXParseException(exception)
      try
        fErrorHandler.error(saxException)
      catch {
        case e: SAXParseException =>
          throw ErrorHandlerWrapper.createXMLParseException(e)
        case e: SAXException      =>
          throw ErrorHandlerWrapper.createXNIException(e)
      }
    }

  /**
   * Report a fatal error. Fatal errors usually occur when the document
   * is not well-formed and signifies that the parser cannot continue
   * normal operation.
   * <p>
   * <strong>Note:</strong> The error handler should <em>always</em>
   * throw an <code>XNIException</code> from this method. This exception
   * can either be the same exception that is passed as a parameter to
   * the method or a new XNI exception object. If the registered error
   * handler fails to throw an exception, the continuing operation of
   * the parser is undetermined.
   *
   * @param domain    The domain of the fatal error. The domain can be
   *                  any string but is suggested to be a valid URI. The
   *                  domain can be used to conveniently specify a web
   *                  site location of the relevent specification or
   *                  document pertaining to this fatal error.
   * @param key       The fatal error key. This key can be any string
   *                  and is implementation dependent.
   * @param exception Exception.
   * @throws XNIException Thrown to signal that the parser should stop
   *                      parsing the document.
   */
  def fatalError(
    domain   : String,
    key      : String,
    exception: XMLParseException
  ): Unit =
    if (fErrorHandler != null) {
      val saxException = ErrorHandlerWrapper.createSAXParseException(exception)
      try
        fErrorHandler.fatalError(saxException)
      catch {
        case e: SAXParseException =>
          throw ErrorHandlerWrapper.createXMLParseException(e)
        case e: SAXException      =>
          throw ErrorHandlerWrapper.createXNIException(e)
      }
    }
}