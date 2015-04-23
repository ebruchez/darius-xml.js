/**
 * Copyright 2015 Orbeon, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orbeon.darius.demo


import org.orbeon.darius.api.API
import org.scalajs.jquery.JQuery
import org.scalajs.jquery.JQueryEventObject
import org.scalajs.jquery._
import rx._
import rx.ops._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.scalajs.js.annotation.JSExport
import scala.util.Failure
import scala.util.Success
import scala.util.Try

@JSExport
object Demo {
  
  private val DebounceDelay = 200.millis
  
  implicit val scheduler = new DomScheduler
  
  private object UI {

    def xmlInput  = jQuery("#input-textarea")
    def results   = jQuery("#result-list")

    def exprAlert(field: JQuery) = field.parent().find(".alert")

    def toggleAlert(field: JQuery, text: Option[String]) = {
      field.parent().toggleClass("has-error", text.isDefined)
      field.parent().toggleClass("has-success", text.isEmpty)
      UI.exprAlert(field).text(text.getOrElse(""))
      UI.exprAlert(field).toggleClass("hidden", text.isEmpty)
    }

    def keyChange(getValue: ⇒ String, rx: Var[String])(x: JQueryEventObject) = {
      val newValue = getValue
      if (rx() != newValue)
        rx() = newValue
    }
  }
  
  private def parseXML(s: String): Try[List[String]] = Try {
    val collector = new XMLEventCollector
    API.parseString(s, collector)
    collector.events map (_.toString)
  }
  
  @JSExport
  def initialize(): Unit = {
    // Model
    val xmlStringVar          = Var(UI.xmlInput.value.toString)
    val debouncedXmlStringRx  = xmlStringVar.debounce(DebounceDelay)
    val parsedXMLRx           = Rx(parseXML(debouncedXmlStringRx()))

    // Alerts
    parsedXMLRx foreach {
      case Success(_) ⇒ UI.toggleAlert(UI.xmlInput, None)
      case Failure(t) ⇒ UI.toggleAlert(UI.xmlInput, Some(t.getMessage))
    }

    // Result
    parsedXMLRx foreach { result ⇒
      result foreach { items ⇒
        UI.results.children("li").detach()
        for (item ← items) {
          UI.results.append(s"""<li class="list-group-item">$item</li>""")
        }
      }
    }

    // Events
    UI.xmlInput.keyup(UI.keyChange(UI.xmlInput.value.toString, xmlStringVar) _)
  }
}
