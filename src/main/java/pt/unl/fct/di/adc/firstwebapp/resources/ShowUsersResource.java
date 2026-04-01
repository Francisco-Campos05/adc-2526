package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@Path("/showusers")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUsersResource {
    private static final Logger LOG = Logger.getLogger(ShowUsersResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUsers(GenericToken req) {
        // 1. Validar se enviou o Token (Erro 9906)
        if (req == null || req.token == null || req.token.tokenId == null) {
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();
        }

        try {
            // 2. Validar Token na BD
            Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenId);
            Entity storedToken = datastore.get(tokenKey);

            Response response = VerificationResource.validateToken(g, storedToken);
            if (response != null) return response;

            // 3. Apenas ADMIN ou BOFFICER podem ver todos os utilizadores
            String role = storedToken.getString("role");
            if (!"ADMIN".equals(role) && !"BOFFICER".equals(role)) {
                return Response.ok(g.toJson(new ErrorResponse("9905", "UNAUTHORIZED"))).build();
            }

            // 4. Procurar utilizadores
            Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
            QueryResults<Entity> results = datastore.run(query);

            List<Map<String, String>> userList = new ArrayList<>();

            while (results.hasNext()) {
                Entity u = results.next();
                Map<String, String> userInfo = new HashMap<>();
                userInfo.put("username", u.getKey().getName());
                userInfo.put("role", u.getString("user_role"));


                userList.add(userInfo);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("users", userList);

            return Response.ok(g.toJson(new SuccessResponse(responseData))).build();

        } catch (Exception e) {
            LOG.severe("Erro interno a listar utilizadores: " + e.getMessage());
            return Response.ok(g.toJson(new ErrorResponse("500", "Internal Server Error"))).build();
        }
    }
}