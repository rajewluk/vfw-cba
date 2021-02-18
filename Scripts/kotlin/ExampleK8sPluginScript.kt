/*
 * Copyright © 2019 Orange
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

package org.onap.ccsdk.cds.blueprintsprocessor.services.execution.scripts

import com.fasterxml.jackson.databind.node.ObjectNode
import org.onap.ccsdk.cds.blueprintsprocessor.core.BlueprintPropertiesService
import org.onap.ccsdk.cds.blueprintsprocessor.core.api.data.ExecutionServiceInput
import org.onap.ccsdk.cds.blueprintsprocessor.functions.k8s.K8sConnectionPluginConfiguration
import org.onap.ccsdk.cds.blueprintsprocessor.functions.k8s.definition.K8sPluginDefinitionApi
import org.onap.ccsdk.cds.blueprintsprocessor.functions.k8s.instance.K8sPluginInstanceApi
import org.onap.ccsdk.cds.blueprintsprocessor.functions.k8s.instance.K8sRbInstance
import org.onap.ccsdk.cds.blueprintsprocessor.functions.k8s.instance.K8sRbInstanceStatus
import org.onap.ccsdk.cds.blueprintsprocessor.rest.BasicAuthRestClientProperties
import org.onap.ccsdk.cds.blueprintsprocessor.rest.RestLibConstants
import org.onap.ccsdk.cds.blueprintsprocessor.rest.SSLBasicAuthRestClientProperties
import org.onap.ccsdk.cds.blueprintsprocessor.rest.service.BlueprintWebClientService
import org.onap.ccsdk.cds.blueprintsprocessor.rest.service.SSLRestClientService
import org.onap.ccsdk.cds.blueprintsprocessor.services.execution.AbstractScriptComponentFunction
import org.onap.ccsdk.cds.controllerblueprints.core.utils.JacksonUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod

open class ExampleK8sPluginScript : AbstractScriptComponentFunction() {

    private val log = LoggerFactory.getLogger(ExampleK8sPluginScript::class.java)!!

    override fun getName(): String {
        return "ExampleK8sPluginScript"
    }

    override suspend fun processNB(executionRequest: ExecutionServiceInput) {
        log.info("EXEC CUSTOM SCRIPT - START")

        val bluePrintContext = bluePrintRuntimeService.bluePrintContext()

        val bluePrintPropertiesService: BlueprintPropertiesService =
                this.functionDependencyInstanceAsType("blueprintPropertiesService")

        val k8sConfiguration = K8sConnectionPluginConfiguration(bluePrintPropertiesService)

        val aaiHttpClientProperties = bluePrintPropertiesService.propertyBeanType(
                RestLibConstants.PROPERTY_REST_CLIENT_PREFIX + "aai-data",
                AaiHttpClientProperties::class.java)

        // queryAai(aaiHttpClientProperties)
        // Creating API connector

        var api = K8sPluginDefinitionApi(k8sConfiguration)

        var instanceApi = K8sPluginInstanceApi(k8sConfiguration)

        val instances: List<K8sRbInstance>? = instanceApi.getInstanceList()
        for (instance in instances!!) {
            log.info(instance.id)
        }
        var instance: K8sRbInstance? = instanceApi.getInstanceById("jovial_khayyam")
        instance = instanceApi.getInstanceById("naughty_morseasdsd")
        var instanceStatus: K8sRbInstanceStatus? = instanceApi.getInstanceStatus("jovial_khayyam")
        instanceStatus = instanceApi.getInstanceStatus("jovial_khayyamm")
        log.info("EXEC CUSTOM SCRIPT - END")
    }

    override suspend fun recoverNB(runtimeException: RuntimeException, executionRequest: ExecutionServiceInput) {
        log.info("Executing Recovery")
        bluePrintRuntimeService.getBlueprintError().addError("${runtimeException.message}")
    }

    fun queryAai(aaiProperties: AaiHttpClientProperties) {
        val vnfID = "51274ece-55ca-4cbc-b7c4-0da0dcc65d38"
        val vnfUrl = aaiProperties.url + "/aai/v19/network/generic-vnfs/generic-vnf/" + vnfID + "/vf-modules";

        val mapOfHeaders = hashMapOf<String, String>()
        mapOfHeaders.put("Accept", "application/json")
        mapOfHeaders.put("Content-Type", "application/json")
        mapOfHeaders.put("x-FromAppId", "CDS")
        mapOfHeaders.put("X-TransactionId", "get_aai_subscr")
        val basicAuthRestClientProperties: BasicAuthRestClientProperties = BasicAuthRestClientProperties()
        basicAuthRestClientProperties.username = aaiProperties.username
        basicAuthRestClientProperties.password = aaiProperties.password
        val sslBasicAuthRestClientProperties: SSLBasicAuthRestClientProperties = SSLBasicAuthRestClientProperties()
        sslBasicAuthRestClientProperties.basicAuth = basicAuthRestClientProperties
        sslBasicAuthRestClientProperties.url = vnfUrl
        sslBasicAuthRestClientProperties.type = RestLibConstants.TYPE_SSL_BASIC_AUTH
        sslBasicAuthRestClientProperties.additionalHeaders = mapOfHeaders
        sslBasicAuthRestClientProperties.sslTrustIgnoreHostname = true
        sslBasicAuthRestClientProperties.keyStoreInstance = "PKCS12"
        sslBasicAuthRestClientProperties.sslTrust = "src/test/resources/keystore.p12"
        sslBasicAuthRestClientProperties.sslTrustPassword = "changeit"

        val sshRestClientService: SSLRestClientService = SSLRestClientService(sslBasicAuthRestClientProperties)
        try {
            val resultOfGet: BlueprintWebClientService.WebClientResponse<String> = sshRestClientService.exchangeResource(HttpMethod.GET.name, "", "")
            val aaiBody = resultOfGet.body
            val aaiPayloadObject = JacksonUtils.jsonNode(aaiBody) as ObjectNode

            for (item in aaiPayloadObject.get("vf-module")) {

            }
        } catch (e: Exception) {
            log.info("Caught exception trying to get the vnf Details!!")
            // throw BlueprintProcessorException("${e.message}")
        }
    }
}

open class AaiHttpClientProperties {
    lateinit var type: String
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
}

//blueprintsprocessor.restclient.aai-data.additionalHeaders.X-TransactionId=cds-transaction-id
//blueprintsprocessor.restclient.aai-data.additionalHeaders.X-FromAppId=cds-app-id
//blueprintsprocessor.restclient.aai-data.additionalHeaders.Accept=application/json

