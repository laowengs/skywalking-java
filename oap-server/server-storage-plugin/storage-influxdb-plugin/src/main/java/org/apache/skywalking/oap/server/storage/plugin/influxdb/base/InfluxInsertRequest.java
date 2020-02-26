/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.storage.plugin.influxdb.base;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.skywalking.oap.server.core.analysis.metrics.Metrics;
import org.apache.skywalking.oap.server.core.storage.StorageBuilder;
import org.apache.skywalking.oap.server.core.storage.StorageData;
import org.apache.skywalking.oap.server.core.storage.model.Model;
import org.apache.skywalking.oap.server.core.storage.model.ModelColumn;
import org.apache.skywalking.oap.server.core.storage.type.StorageDataType;
import org.apache.skywalking.oap.server.library.client.request.InsertRequest;
import org.apache.skywalking.oap.server.library.client.request.UpdateRequest;
import org.apache.skywalking.oap.server.storage.plugin.influxdb.InfluxClient;
import org.influxdb.dto.Point;

/**
 * InfluxDB Point wrapper.
 */
public class InfluxInsertRequest implements InsertRequest, UpdateRequest {
    public static final String ID = "id";

    private Point.Builder builder;
    private Map<String, Object> fields = Maps.newHashMap();

    public InfluxInsertRequest(Model model, StorageData storageData, StorageBuilder storageBuilder) {
        Map<String, Object> objectMap = storageBuilder.data2Map(storageData);

        for (ModelColumn column : model.getColumns()) {
            Object value = objectMap.get(column.getColumnName().getName());

            if (value instanceof StorageDataType) {
                fields.put(
                    column.getColumnName().getStorageName(),
                    ((StorageDataType) value).toStorageData()
                );
            } else {
                fields.put(column.getColumnName().getStorageName(), value);
            }
        }
        builder = Point.measurement(model.getName())
                       .addField(ID, storageData.id())
                       .fields(fields)
                       .tag(InfluxClient.TAG_TIME_BUCKET, String.valueOf(fields.get(Metrics.TIME_BUCKET)));
    }

    public InfluxInsertRequest time(long time, TimeUnit unit) {
        builder.time(time, unit);
        return this;
    }

    public InfluxInsertRequest addFieldAsTag(String fieldName, String tagName) {
        if (fields.containsKey(fieldName)) {
            builder.tag(tagName, String.valueOf(fields.get(fieldName)));
        }
        return this;
    }

    public Point getPoint() {
        return builder.build();
    }
}