/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.rest.domain;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.google.common.collect.ImmutableMap;

public class LocationConfigSummary extends ConfigSummary {

    @JsonSerialize(include = Inclusion.NON_NULL)
    private final Map<String, URI> links;

    public LocationConfigSummary(
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
            @JsonProperty("description") String description,
            @JsonProperty("defaultValue") Object defaultValue,
            @JsonProperty("reconfigurable") boolean reconfigurable,
            @JsonProperty("label") String label,
            @JsonProperty("priority") Double priority,
            @JsonProperty("possibleValues") List<Map<String, String>> possibleValues,
            @JsonProperty("links") Map<String, URI> links) {
        super(name, type, description, defaultValue, reconfigurable, label, priority, possibleValues);
        this.links = (links == null) ? ImmutableMap.<String, URI>of() : ImmutableMap.copyOf(links);
    }

    @Override
    public Map<String, URI> getLinks() {
        return links;
    }

    @Override
    public String toString() {
        return "LocationConfigSummary{"
                + "name='" + getName() + '\''
                + ", type='" + getType() + '\''
                + '}';
  }
}
