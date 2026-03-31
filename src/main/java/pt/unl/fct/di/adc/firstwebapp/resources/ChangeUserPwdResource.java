package pt.unl.fct.di.adc.firstwebapp.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/changeuserpwd")
@Produces(MediaType.APPLICATION_JSON)
public class ChangeUserPwdResource {
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/")
    public Response changePwd(ChangePwd req) {
        // 1. Validar Token
        Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenID);
        Entity storedToken = datastore.get(tokenKey);
        if(storedToken == null) return Response.status(Response.Status.FORBIDDEN).build();

        // 2. Buscar User e validar password antiga
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(storedToken.getString("username"));
        Entity user = datastore.get(userKey);

        if(!user.getString("user_pwd").equals(req.oldPassword))
            return Response.status(Response.Status.FORBIDDEN).entity(g.toJson(new ErrorResponse("403", "Old password incorrect."))).build();

        // 3. Validar nova password
        if(!req.newPassword.equals(req.confirmation))
            return Response.status(Response.Status.BAD_REQUEST).build();

        // 4. Guardar
        datastore.put(Entity.newBuilder(user).set("user_pwd", req.newPassword).build());
        return Response.ok(g.toJson(new SuccessResponse("Password changed."))).build();
    }
}
