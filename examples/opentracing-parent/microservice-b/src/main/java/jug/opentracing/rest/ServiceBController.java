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

@Path("/serviceB")
public class ServiceBController {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServiceBController.class);

    @Inject
    private Tracer tracer;

    @GET
    @Path("/actionB")
    @Traced(operationName="operationActionB")
    public String actionA(){

        // this span will be ChildOf of span representing server request processing
        Span childSpan = tracer.buildSpan("businessOperation")
                .start();

        LOGGER.info("Log at " + ServiceBController.class.getName());

        childSpan.log("Business annotation B");

        // business logic
        childSpan.finish();

        return "Action B";
    }
}
