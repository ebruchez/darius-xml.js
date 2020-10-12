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

package org.orbeon.darius.xml.xni

/**
 * This exception is the base exception of all XNI exceptions. It
 * can be constructed with an error message or used to wrap another
 * exception object.
 *
 * *Note:* By extending the Java
 * `RuntimeException`, XNI handlers and components are
 * not required to catch XNI exceptions but may explicitly catch
 * them, if so desired.
 */
class XNIException(message: String) extends RuntimeException(message) {

  /**
   The wrapped exception.
   */
  private var fException: Exception = this

  /**
   * Constructs an XNI exception with a wrapped exception.
   *
   * @param exception The wrapped exception.
   */
  def this(exception: Exception) = {
    this(exception.getMessage)
    fException = exception
  }

  /**
   * Constructs an XNI exception with a message and wrapped exception.
   *
   * @param message The exception message.
   * @param exception The wrapped exception.
   */
  def this(message: String, exception: Exception) = {
    this(message)
    fException = exception
  }

  /**
   Returns the wrapped exception.
   */
  def getException: Exception = {
    if (fException != this) fException else null
  }

  /**
   * Initializes the cause of this `XNIException`.
   * The value must be an instance of `Exception` or
   * `null`.
   *
   * @param throwable the cause
   * @return this exception
   *
   * @throws IllegalStateException if a cause has already been set
   * @throws IllegalArgumentException if the cause is this exception
   * @throws ClassCastException if the cause is not assignable to `Exception`
   */
  override def initCause(throwable: Throwable): Throwable = {
    synchronized {
      if (fException != this) {
        throw new IllegalStateException
      }
      if (throwable == this) {
        throw new IllegalArgumentException
      }
      fException = throwable.asInstanceOf[Exception]
      this
    }
  }

  /**
   Returns the cause of this `XNIException`.
   */
  override def getCause: Throwable = getException
}
