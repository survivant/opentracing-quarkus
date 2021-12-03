package it.hilling.training.opentracing;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {
    private static final Logger LOG = LoggerFactory.getLogger(HelloResource.class);

    @RestClient
    WorldResource worldResource;

    @Path("/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("name") @NotNull String name) {
        LOG.info("called hello");
        if(name.equals("world")) {
            name = worldResource.world();
        }
        return "hello " + name;
    }

    @Path("/{name}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String helloPost(@PathParam("name") @NotNull String name, String body) {
        LOG.info("called hello POST");
        if(name.equals("world")) {
            name = worldResource.world();
        }
        return "hello POST " + name;
    }

}