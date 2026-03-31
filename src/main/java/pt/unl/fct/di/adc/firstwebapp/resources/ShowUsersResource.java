package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;

@Path("/showusers")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUsersResource {
    private static final Logger LOG = Logger.getLogger(ShowUsersResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUsers(AuthToken token) {
        if (token == null || token.tokenID == null)
            return Response.status(Response.Status.BAD_REQUEST).entity(g.toJson(new ErrorResponse("400", "Missing token."))).build();

        try {
            // 1. Validar Token e se é ADMIN
            Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(token.tokenID);
            Entity storedToken = datastore.get(tokenKey);
            if (storedToken == null || storedToken.getLong("expirationData") < System.currentTimeMillis())
                return Response.status(Response.Status.FORBIDDEN).build();

            Entity user = datastore.get(datastore.newKeyFactory().setKind("User").newKey(storedToken.getString("username")));
            if (!"ADMIN".equals(user.getString("user_role")))
                return Response.status(Response.Status.FORBIDDEN).entity(g.toJson(new ErrorResponse("403", "Only admins can see all users."))).build();

            // 2. Query para listar todos
            Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
            QueryResults<Entity> results = datastore.run(query);
            List<String> userList = new ArrayList<>();
            while (results.hasNext()) {
                userList.add(results.next().getKey().getName());
            }
            return Response.ok(g.toJson(userList)).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }
}
