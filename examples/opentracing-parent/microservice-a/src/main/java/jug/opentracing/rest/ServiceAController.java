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
package jug.opentracing.rest;

import io.opentracing.Span;
import io.opentracing.Tracer;
import org.eclipse.microprofile.opentracing.Traced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/serviceA")
public class ServiceAController {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServiceAController.class);

    @Inject
    Tracer tracer;

    @GET
    @Path("/actionA")
    @Traced(operationName="operationActionA")
    public String actionA(){

        System.out.println("Current span = " + tracer.activeSpan());

        // this span will be ChildOf of span representing server request processing
        Span childSpan = tracer.buildSpan("businessOperation")
                .start();

        childSpan.log("Business annotation");

        // business logic
        childSpan.finish();

        LOGGER.info("Log at " + ServiceAController.class.getName());

        return "Action A";
    }
}
