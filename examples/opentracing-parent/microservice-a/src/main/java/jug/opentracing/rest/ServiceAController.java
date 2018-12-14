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

import io.opentracing.Tracer;
import org.eclipse.microprofile.opentracing.Traced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

@Path("/serviceA")
public class ServiceAController {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServiceAController.class);

    @Inject
    private Tracer tracer;

    private Client client = ClientBuilder.newBuilder()
            .build();
    @GET
    @Path("/actionA")
    @Traced(operationName="operationActionA")
    public String actionA(){

        Response response = client.target("http://localhost:8080/microservice_b_war/serviceB/actionB")
                .request()
                .get();

        LOGGER.info("Log at " + ServiceAController.class.getName());

        return "Action A [" + response.readEntity(String.class) + "]";
    }
}
