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

package za.co.absa.enceladus.model

import org.scalatest.FunSuite

class MappingTableTest extends FunSuite {
  private val defaultValue1 = DefaultValue("someColumn", "alfa")
  private val defaultValue2 = DefaultValue("otherColumn", "beta")

  private val mappingTable = MappingTable(
    name = "Testing Mapping Table",
    version = 1,
    hdfsPath = "/path/to/hdfs",
    schemaName = "Some Schema",
    schemaVersion = 1,
    userCreated = "user",
    defaultMappingValue = List(defaultValue1, defaultValue2)
  )

  private val modelVersion = ModelVersion

  private val expectedMappingTable =
    s"""{"metadata":{"exportVersion":$modelVersion},"item":{"name":"Testing Mapping Table","hdfsPath":"/path/to/hdfs",
      |"schemaName":"Some Schema","schemaVersion":1,"defaultMappingValue":[{"columnName":"someColumn","value":"alfa"},
      |{"columnName":"otherColumn","value":"beta"}]}}""".stripMargin.replaceAll("[\\r\\n]", "")

  test("export Mapping Table") {
    assert(expectedMappingTable == mappingTable.exportItem())
  }
}
