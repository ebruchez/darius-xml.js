package org.orbeon.darius.xni.parser

/**
 * The component manager manages a parser configuration and the components
 * that make up that configuration. The manager notifies each component
 * before parsing to allow the components to initialize their state; and
 * also any time that a parser feature or property changes.
 * 
 * The methods of the component manager allow components to query features
 * and properties that affect the operation of the component.
 *
 * @see XMLComponent
 */
trait XMLComponentManager {

  /**
   * Returns the state of a feature.
   *
   * @param featureId The feature identifier.
   *
   * @throws XMLConfigurationException Thrown on configuration error.
   */
  def getFeature(featureId: String): Boolean

  /**
   * Returns the value of a property.
   *
   * @param propertyId The property identifier.
   *
   * @throws XMLConfigurationException Thrown on configuration error.
   */
  def getProperty(propertyId: String): Any
}
