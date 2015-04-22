package org.orbeon.darius.xni

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
  def this(exception: Exception) {
    this(exception.getMessage)
    fException = exception
  }

  /**
   * Constructs an XNI exception with a message and wrapped exception.
   *
   * @param message The exception message.
   * @param exception The wrapped exception.
   */
  def this(message: String, exception: Exception) {
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
