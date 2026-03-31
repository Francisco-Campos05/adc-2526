package pt.unl.fct.di.adc.firstwebapp.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.*;

import java.util.HashMap;
import java.util.Map;

@Path("/changeuserpwd")
@Produces(MediaType.APPLICATION_JSON)
public class ChangeUserPwdResource {
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePwd(ChangePwd req) {
        if (req == null || req.token == null || req.token.tokenId == null || req.input == null || req.input.username == null) {
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();
        }

        try {
            Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenId);
            Entity storedToken = datastore.get(tokenKey);

            if (storedToken == null) return Response.ok(g.toJson(new ErrorResponse("9903", "INVALID_TOKEN"))).build();
            if (storedToken.getLong("expiresAt") < (System.currentTimeMillis() / 1000))
                return Response.ok(g.toJson(new ErrorResponse("9904", "TOKEN_EXPIRED"))).build();

            if (!storedToken.getString("username").equals(req.input.username))
                return Response.ok(g.toJson(new ErrorResponse("9905", "UNAUTHORIZED"))).build();

            Key userKey = datastore.newKeyFactory().setKind("User").newKey(req.input.username);
            Entity user = datastore.get(userKey);

            if (user == null) return Response.ok(g.toJson(new ErrorResponse("9902", "USER_NOT_FOUND"))).build();

            // Erro 9900: Password antiga errada
            if (!user.getString("user_pwd").equals(req.input.oldPassword))
                return Response.ok(g.toJson(new ErrorResponse("9900", "INVALID_CREDENTIALS"))).build();

            // Atualiza a password
            datastore.put(Entity.newBuilder(user).set("user_pwd", req.input.newPassword).build());

            // Cria a resposta
            Map<String, String> responseData = new HashMap<>();
            responseData.put("message", "Password changed successfully");

            return Response.ok(g.toJson(new SuccessResponse(responseData))).build();
        } catch (Exception e) {
            return Response.ok(g.toJson(new ErrorResponse("500", "Internal server error"))).build();
        }
    }
}