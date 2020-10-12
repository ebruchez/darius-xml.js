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

package org.orbeon.apache.xerces.xni.grammars

/**
 *  This interface specifies how the parser and the application
 * interact with respect to Grammar objects that the application
 * possesses--either by having precompiled them or by having stored them
 * from a previous validation of an instance document.  It makes no
 * assumptions about the kind of Grammar involved, or about how the
 * application's storage mechanism works.
 *
 * The interaction works as follows:
 *
 * - When a validator considers a document, it is expected to request
 * grammars of the type it can handle from this object using the
 * `retrieveInitialGrammarSet` method.
 * - If it requires a grammar
 * not in this set, it will request it from this Object using the
 * `retrieveGrammar` method.
 * -  After successfully validating an
 * instance, the validator should make any new grammars it has compiled
 * available to this object using the `cacheGrammars`
 * method; for ease of implementation it may make other Grammars it holds references to as well (i.e.,
 * it may return some grammars that were retrieved from the GrammarPool in earlier operations).
 */
trait XMLGrammarPool {

  /**
   *  retrieve the initial known set of grammars. this method is
   * called by a validator before the validation starts. the application
   * can provide an initial set of grammars available to the current
   * validation attempt.
   * @param grammarType the type of the grammar, from the
   *  `org.orbeon.apache.xerces.xni.grammars.Grammar` interface.
   * @return the set of grammars the validator may put in its "bucket"
   */
  def retrieveInitialGrammarSet(grammarType: String): Array[Grammar]

  /**
   * return the final set of grammars that the validator ended up
   * with.
   * This method is called after the
   * validation finishes. The application may then choose to cache some
   * of the returned grammars.
   * @param grammarType the type of the grammars being returned;
   * @param grammars an array containing the set of grammars being
   *  returned; order is not significant.
   */
  def cacheGrammars(grammarType: String, grammars: Array[Grammar]): Unit

  /**
   *  This method requests that the application retrieve a grammar
   * corresponding to the given GrammarIdentifier from its cache.
   * If it cannot do so it must return null; the parser will then
   * call the EntityResolver.  *An application must not call its
   * EntityResolver itself from this method; this may result in infinite
   * recursions.*
   * @param desc The description of the Grammar being requested.
   * @return the Grammar corresponding to this description or null if
   *  no such Grammar is known.
   */
  def retrieveGrammar(desc: XMLGrammarDescription): Grammar

  /**
   * Causes the XMLGrammarPool not to store any grammars when
   * the cacheGrammars(String, Grammar[[]) method is called.
   */
  def lockPool(): Unit

  /**
   * Allows the XMLGrammarPool to store grammars when its cacheGrammars(String, Grammar[])
   * method is called.  This is the default state of the object.
   */
  def unlockPool(): Unit

  /**
   * Removes all grammars from the pool.
   */
  def clear(): Unit
}
