package org.onap.ccsdk.cds.blueprintsprocessor.services.execution.scripts

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.processor.ResourceAssignmentProcessor
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.utils.ResourceAssignmentUtils
import org.onap.ccsdk.cds.controllerblueprints.core.BlueprintProcessorException
import org.onap.ccsdk.cds.controllerblueprints.resource.dict.ResourceAssignment
import org.slf4j.LoggerFactory

open class ConfigValuesSetupKt() : ResourceAssignmentProcessor() {

    private val log = LoggerFactory.getLogger(ConfigValuesSetupKt::class.java)!!

    override fun getName(): String {
        return "ConfigValuesSetupKt"
    }

    override suspend fun processNB(resourceAssignment: ResourceAssignment) {

        var retValue: ObjectNode? = null

        try {
            if(resourceAssignment.name == "config-value-setup") {
                val modules = raRuntimeService.getResolutionStore("vf-modules-list")["vf-modules"]
                val objectMapper = jacksonObjectMapper()
                val result: ObjectNode = objectMapper.createObjectNode()
                val moduleData: ObjectNode = objectMapper.createObjectNode()
                result.put("helm_vpkg", moduleData)
                moduleData.put("template-name", "some name")
                moduleData.put("source-name", "some name")
                for (module in modules) {
                    if (module["vf-module-name"].asText().contains("helm_vpkg")) {
                        moduleData.put("instance-name", module["heat-stack-id"].asText())
                        retValue = result
                    }
                    log.info(module.asText())
                }
            }
            ResourceAssignmentUtils.setResourceDataValue(resourceAssignment, raRuntimeService, retValue)

        } catch (e: Exception) {
            log.error(e.message, e)
            ResourceAssignmentUtils.setResourceDataValue(resourceAssignment, raRuntimeService, "ERROR")

            throw BlueprintProcessorException("Failed in template key ($resourceAssignment) assignments, cause: ${e.message}", e)
        }
    }

    override suspend fun recoverNB(runtimeException: RuntimeException, resourceAssignment: ResourceAssignment) {
        raRuntimeService.getBlueprintError().addError("Failed in ResolvPropertiesKt-ResourceAssignmentProcessor : ${runtimeException.message}")
    }
}