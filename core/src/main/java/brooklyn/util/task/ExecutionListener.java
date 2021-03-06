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
package brooklyn.util.task;

import brooklyn.management.Task;

public interface ExecutionListener {

    /** invoked when a task completes: 
     * {@link Task#getEndTimeUtc()} and {@link Task#isDone()} are guaranteed to be set,
     * and {@link Task#get()} should return immediately for most Task implementations
     * (care has been taken to avoid potential deadlocks here, waiting for a result!)  */
    public void onTaskDone(Task<?> task);
    
}
