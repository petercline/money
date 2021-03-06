/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
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

package com.comcast.money.core.handlers

import com.comcast.money.api.{ Note, SpanId }
import com.comcast.money.core.CoreSpanInfo
import com.typesafe.config.ConfigFactory
import org.scalatest.{ Matchers, WordSpec }

import scala.collection.JavaConversions._
import scala.collection._

class SpanLogFormatterSpec extends WordSpec with Matchers {

  val emitterConf = ConfigFactory.parseString(
    """
          {
            log-level="INFO"
            emitter="com.comcast.money.emitters.LogRecorder"
          }
    """
  )
  val spanLogFormatter = SpanLogFormatter(emitterConf)

  val sampleData = CoreSpanInfo(
    id = SpanId.fromString("SpanId~1~1~1"),
    startTimeMillis = 1L,
    startTimeMicros = 1L,
    endTimeMillis = 26L,
    endTimeMicros = 36L,
    durationMicros = 35000L,
    name = "key",
    appName = "unknown",
    host = "host",
    notes = Map("bob" -> Note.of("bob", "craig"), "what" -> Note.of("what", 1L), "when" -> Note.of("when", 2L)),
    success = true
  )

  val withNull = CoreSpanInfo(
    id = SpanId.fromString("SpanId~1~1~1"),
    startTimeMillis = 1L,
    startTimeMicros = 1L,
    endTimeMillis = 26L,
    endTimeMicros = 36L,
    durationMicros = 35000L,
    name = "key",
    appName = "unknown",
    host = "host",
    notes = Map("empty" -> Note.of("empty", null)),
    success = true
  )

  "A LogEmitter must" must {
    "have a correctly formatted message" in {
      val actualMessage = spanLogFormatter.buildMessage(sampleData)

      assert(
        actualMessage === ("Span: [ span-id=1 ][ trace-id=1 ][ parent-id=1 ][ span-name=key ][ app-name=unknown ][ " +
          "start-time=1 ][ span-duration=35000 ][ span-success=true ][ bob=craig ][ what=1 ][ when=2 ]")
      )
    }
    "honor key names from the config" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  keys {
                    span-id = "spanId"
                    trace-id = "traceId"
                    parent-id = "parentId"
                    span-name = "spanName"
                    app-name = "appName"
                    start-time = "startTime"
                    span-duration = "spanDuration"
                    span-success = "spanSuccess"
                  }
                }
              }
        """
      )
      val spanLogFormatter = SpanLogFormatter(conf)

      val actualMessage = spanLogFormatter.buildMessage(sampleData)
      assert(
        actualMessage === ("Span: [ spanId=1 ][ traceId=1 ][ parentId=1 ][ spanName=key ][ appName=unknown ][ " +
          "startTime=1 ][ spanDuration=35000 ][ spanSuccess=true ][ bob=craig ][ what=1 ][ when=2 ]")
      )
    }
    "honor the span-start from the config" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  span-start = "Start :|: "
                }
              }
        """
      )
      val spanLogFormatter = SpanLogFormatter(conf)
      val actualMessage = spanLogFormatter.buildMessage(sampleData)
      assert(
        actualMessage === ("Start :|: [ span-id=1 ][ trace-id=1 ][ parent-id=1 ][ span-name=key ][ app-name=unknown ][ " +
          "start-time=1 ][ span-duration=35000 ][ span-success=true ][ bob=craig ][ what=1 ][ when=2 ]")
      )
    }
    "honor the log-template from the config" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  log-template = "%s=\"%s\" "
                }
              }
        """
      )
      val spanLogFormatter = SpanLogFormatter(conf)
      val actualMessage = spanLogFormatter.buildMessage(sampleData)
      assert(
        actualMessage === ("""Span: span-id="1" trace-id="1" parent-id="1" span-name="key" """ +
          """app-name="unknown" start-time="1" span-duration="35000" span-success="true" """ +
          """bob="craig" what="1" when="2" """)
      )
    }
    "honor the span-duration-ms settings in the config" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  span-duration-ms-enabled = "true"
                  keys {
                    span-duration-ms = "spanDurationMs"
                  }
                }
              }
        """
      )
      val spanLogFormatter = SpanLogFormatter(conf)
      val actualMessage = spanLogFormatter.buildMessage(sampleData)

      actualMessage should include("[ span-duration=35000 ]")
      actualMessage should include("[ spanDurationMs=35 ]")
    }
    "log NULL when the note value is None" in {
      val expectedLogMessage = spanLogFormatter.buildMessage(withNull)

      expectedLogMessage should include("[ empty=NULL ]")
    }
    "honor the null value to log from the config" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  null-value = "null_value"
                }
              }
        """
      )
      val spanLogFormatter = SpanLogFormatter(conf)
      val expectedLogMessage = spanLogFormatter.buildMessage(withNull)

      expectedLogMessage should include("[ empty=null_value ]")
    }
  }
}
