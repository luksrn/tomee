package jug.opentracing.rest;

import org.eclipse.microprofile.opentracing.Traced;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

public class FooBean {

    private Client client = ClientBuilder.newBuilder()
            .build();

    @Traced
    public String callBarService(){
        Response response = client.target("http://localhost:8081/service-b/serviceB/actionB")//"http://localhost:8080/microservice_b_war/serviceB/actionB")
                .request()
                .get();

        return response.readEntity(String.class);
    }
}
