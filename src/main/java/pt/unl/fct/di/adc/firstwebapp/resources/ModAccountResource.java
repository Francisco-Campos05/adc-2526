package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/modaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ModAccountResource {
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyAccount(UpdateProfile req) {
        try {
            // Validar Token
            Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenID);
            Entity storedToken = datastore.get(tokenKey);
            if(storedToken == null || storedToken.getLong("expirationData") < System.currentTimeMillis())
                return Response.status(Response.Status.FORBIDDEN).build();

            // Buscar Utilizador
            String username = storedToken.getString("username");
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
            Entity user = datastore.get(userKey);

            // Atualizar apenas atributos (NÃO password)
            Entity.Builder builder = Entity.newBuilder(user);
            if(req.input.email != null) builder.set("user_email", req.input.email);
            if(req.input.phone != null) builder.set("user_phone", req.input.phone);
            if(req.input.address != null) builder.set("user_address", req.input.address);

            datastore.put(builder.build());
            return Response.ok(g.toJson(new SuccessResponse("Attributes updated."))).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }
}
