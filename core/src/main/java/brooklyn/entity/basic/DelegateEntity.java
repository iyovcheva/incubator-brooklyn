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
package brooklyn.entity.basic;

import java.util.concurrent.atomic.AtomicBoolean;

import brooklyn.config.render.RendererHints;
import brooklyn.entity.Entity;
import brooklyn.entity.Group;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.AttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;

import com.google.common.base.Function;

/**
 * A delegate entity for use as a {@link Group} child proxy for members.
 */
@ImplementedBy(DelegateEntityImpl.class)
public interface DelegateEntity extends Entity {

    AttributeSensorAndConfigKey<Entity, Entity> DELEGATE_ENTITY = ConfigKeys.newSensorAndConfigKey(Entity.class, "delegate.entity", "The delegate entity");

    AttributeSensor<String> DELEGATE_ENTITY_LINK = Sensors.newStringSensor("webapp.url", "The delegate entity link");

    /** Hints for rendering the delegate entity as a link in the Brooklyn console UI. */
    public static class EntityUrl {

        private static final AtomicBoolean initialized = new AtomicBoolean(false);
        private static final Function<Entity, String> entityUrlFunction = new Function<Entity, String>() {
            @Override
            public String apply(Entity input) {
                if (input==null) return null;
                Entity entity = (Entity) input;
                String url = String.format("#/v1/applications/%s/entities/%s", entity.getApplicationId(), entity.getId());
                return url;
            }
        };

        public static Function<Entity, String> entityUrl() { return entityUrlFunction; }

        /** Setup renderer hints. */
        public static void init() {
            if (initialized.getAndSet(true)) return;

            RendererHints.register(DELEGATE_ENTITY, RendererHints.namedActionWithUrl(entityUrl()));
            RendererHints.register(DELEGATE_ENTITY_LINK, RendererHints.namedActionWithUrl());
        }

        static {
            init();
        }
    }

}
