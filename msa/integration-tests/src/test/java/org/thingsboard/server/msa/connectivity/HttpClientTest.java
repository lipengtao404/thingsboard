/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.msa.connectivity;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.WsTelemetryResponse;

import java.util.concurrent.TimeUnit;

public class HttpClientTest extends AbstractContainerTest {

    @Test
    public void telemetryUpdate() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");

        Device device = createDevice("http_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        WsClient mWs = subscribeToTelemetryWebSocket(device.getId());
        ResponseEntity deviceTelemetryResponse = restClient.getRestTemplate()
                .postForEntity(httpUrl + "/api/v1/{credentialsId}/telemetry",
                        mapper.readTree(createPayload().toString()),
                        ResponseEntity.class,
                        deviceCredentials.getCredentialsId());
        Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());
        TimeUnit.SECONDS.sleep(1);
        WsTelemetryResponse actualLatestTelemetry = mapper.readValue(mWs.getLastMessage(), WsTelemetryResponse.class);

        Assert.assertEquals(getExpectedLatestValues(123456789L).keySet(), actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, "booleanKey", Boolean.TRUE.toString()));
        Assert.assertTrue(verify(actualLatestTelemetry, "stringKey", "value1"));
        Assert.assertTrue(verify(actualLatestTelemetry, "doubleKey", Double.toString(42.0)));
        Assert.assertTrue(verify(actualLatestTelemetry, "longKey", Long.toString(73)));

        restClient.getRestTemplate().delete(httpUrl + "/api/device/" + device.getId());
    }
}
