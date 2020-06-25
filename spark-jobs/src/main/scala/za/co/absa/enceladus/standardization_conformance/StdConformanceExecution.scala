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

package za.co.absa.enceladus.standardization_conformance

import za.co.absa.enceladus.common.config.PathConfig
import za.co.absa.enceladus.conformance.ConformanceExecution
import za.co.absa.enceladus.model.Dataset
import za.co.absa.enceladus.standardization.StandardizationExecution
import za.co.absa.enceladus.standardization_conformance.config.StdConformanceConfigInstance

trait StdConformanceExecution extends StandardizationExecution with ConformanceExecution {

  def getFullPathCfg[T](cmd: StdConformanceConfigInstance, dataset: Dataset, reportVersion: Int): PathConfig = {
    val standardization = getStandardizationPath(cmd, reportVersion)

    PathConfig(
      inputPath = buildRawPath(cmd, dataset, reportVersion),
      outputPath = buildPublishPath(cmd, dataset, reportVersion),
      standardizationPath = Some(standardization)
    )
  }

}
