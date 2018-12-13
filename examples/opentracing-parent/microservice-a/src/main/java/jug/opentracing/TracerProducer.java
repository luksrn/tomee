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
 */
package jug.opentracing;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import io.opentracing.Tracer;

import javax.enterprise.inject.Produces;

public class TracerProducer {

    @Produces
    public Tracer buildTracer(){
        // Now, create a Brave tracing component with the service name you want to see in Zipkin.
        //   (the dependency is io.zipkin.brave:brave)
        Tracing braveTracing = Tracing.newBuilder()
                .localServiceName("service-a")
                //.spanReporter(spanReporter)
                .build();
        // use this to create an OpenTracing Tracer
        Tracer tracer = BraveTracer.create(braveTracing);
        return tracer;
    }
}
