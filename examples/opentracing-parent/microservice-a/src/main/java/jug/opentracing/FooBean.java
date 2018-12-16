package jug.opentracing;

import org.eclipse.microprofile.opentracing.ClientTracingRegistrar;
import org.eclipse.microprofile.opentracing.Traced;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class FooBean {

    @Traced
    public String callBarService(){

        Client client = ClientTracingRegistrar.configure(ClientBuilder.newBuilder()).build();

        return client.target("http://localhost:8081/service-b/serviceB/actionB")
                .request()
                .get(String.class);
    }
}
