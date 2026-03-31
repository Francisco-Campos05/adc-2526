package pt.unl.fct.di.adc.firstwebapp.resources;

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
        if (req == null || req.token == null || req.token.tokenId == null || req.input == null || req.input.username == null) {
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();
        }

        try {
            Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenId);
            Entity storedToken = datastore.get(tokenKey);

            if (storedToken == null) return Response.ok(g.toJson(new ErrorResponse("9903", "INVALID_TOKEN"))).build();
            if (storedToken.getLong("expiresAt") < (System.currentTimeMillis() / 1000))
                return Response.ok(g.toJson(new ErrorResponse("9904", "TOKEN_EXPIRED"))).build();

            if (!storedToken.getString("username").equals(req.input.username) && !"ADMIN".equals(storedToken.getString("role"))) {
                return Response.ok(g.toJson(new ErrorResponse("9905", "UNAUTHORIZED"))).build();
            }

            Key userKey = datastore.newKeyFactory().setKind("User").newKey(req.input.username);
            Entity user = datastore.get(userKey);

            if (user == null) return Response.ok(g.toJson(new ErrorResponse("9902", "USER_NOT_FOUND"))).build();

            Entity.Builder builder = Entity.newBuilder(user);
            if (req.input.email != null) builder.set("user_email", req.input.email);
            if (req.input.phone != null) builder.set("user_phone", req.input.phone);
            if (req.input.address != null) builder.set("user_address", req.input.address);

            datastore.put(builder.build());
            return Response.ok(g.toJson(new SuccessResponse("Updated successfully"))).build();
        } catch (Exception e) {
            return Response.ok(g.toJson(new ErrorResponse("500", "Internal Server Error"))).build();
        }
    }
}