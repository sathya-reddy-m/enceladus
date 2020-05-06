/*
 * Copyright 2018 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package za.co.absa.enceladus.standardization

import org.scalatest.FunSuite
import za.co.absa.atum.model.{Checkpoint, Measurement}
import za.co.absa.enceladus.utils.validation.ValidationException
import scala.util.Success

class ControlInfoValidationSuite extends FunSuite {

  import za.co.absa.atum.core.Constants._

  private val checkpoints1 = List(
    Checkpoint("raw", None, None, "", "", "", 0, List(
      Measurement("", controlTypeAbsAggregatedTotal, "", 0),
      Measurement("", controlTypeRecordCount, "", 11)
    )
    ),
    Checkpoint("source", None, None, "", "", "", 1, List(
      Measurement("", controlTypeRecordCount, "", 3)
    )
    )
  )

  private val checkpoints2 = List(
    Checkpoint("source", None, None, "", "", "", 1, List(
      Measurement("", controlTypeDistinctCount, "", 1)
    )
    )
  )

  private val checkpoints3 = List(
    Checkpoint("raw", None, None, "", "", "", 0, List(
      Measurement("", controlTypeRecordCount, "", -3)
    )
    ),
    Checkpoint("source", None, None, "", "", "", 1, List(
      Measurement("", controlTypeRecordCount, "", "")
    )
    )
  )

  test("Correct values") {
    val rawResult = ControlInfoValidation.getCountFromGivenCheckpoint("raw", checkpoints1)
    val sourceResult = ControlInfoValidation.getCountFromGivenCheckpoint("source", checkpoints1)
    val validation = ControlInfoValidation.validateFields(rawResult, sourceResult)

    assert(rawResult == Success(11))
    assert(sourceResult == Success(3))
  }

  test("Errors in parsing") {
    val rawResult = ControlInfoValidation.getCountFromGivenCheckpoint("raw", checkpoints2)
    val sourceResult = ControlInfoValidation.getCountFromGivenCheckpoint("source", checkpoints2)

    val rawError = "Missing raw checkpoint"
    val sourceError = s"source checkpoint does not have a $controlTypeRecordCount control"

    assert(rawResult.failed.get.getMessage == rawError)
    assert(sourceResult.failed.get.getMessage == sourceError)

    val exception = intercept[ValidationException](ControlInfoValidation.validateFields(rawResult,sourceResult))
    assert(exception.msg contains rawError)
    assert(exception.msg contains sourceError)
  }

  test("Wrong controlValue values") {
    val rawResult = ControlInfoValidation.getCountFromGivenCheckpoint("raw", checkpoints3)
    val sourceResult = ControlInfoValidation.getCountFromGivenCheckpoint("source", checkpoints3)

    val rawError = s"Wrong raw $controlTypeRecordCount value: Negative value"
    val sourceError = s"""Wrong source $controlTypeRecordCount value: For input string: \"\""""

    assert(rawResult.failed.get.getMessage == rawError)
    assert(sourceResult.failed.get.getMessage == sourceError)

    val exception = intercept[ValidationException](ControlInfoValidation.validateFields(rawResult, sourceResult))
    assert(exception.msg contains rawError)
    assert(exception.msg contains sourceError)
  }

}
