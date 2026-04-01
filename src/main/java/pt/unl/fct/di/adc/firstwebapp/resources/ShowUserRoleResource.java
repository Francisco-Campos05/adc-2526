package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.HashMap;
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

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;
import pt.unl.fct.di.adc.firstwebapp.util.SuccessResponse;
import pt.unl.fct.di.adc.firstwebapp.util.UsernameTarget;

@Path("/showuserrole")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUserRoleResource {

    private static final Logger LOG = Logger.getLogger(ShowUserRoleResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();

    public ShowUserRoleResource() {}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showRole(UsernameTarget req) {

        // 1. Validar estrutura do JSON
        if (req == null || req.token == null || req.token.tokenId == null || req.input == null || req.input.username == null)
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();

        try {
            // 2. Validar Token de quem faz o pedido
            Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenId);
            Entity storedToken = datastore.get(tokenKey);

            Response response = VerificationResource.validateToken(g, storedToken);
            if (response != null) return response;

            // 3. Permissões: Apenas ADMIN e BOFFICER
            String requesterRole = storedToken.getString("role");
            if (!"ADMIN".equals(requesterRole) && !"BOFFICER".equals(requesterRole))
                return Response.ok(g.toJson(new ErrorResponse("9905", "UNAUTHORIZED"))).build();

            // 4. Buscar o utilizador alvo
            Key targetUserKey = datastore.newKeyFactory().setKind("User").newKey(req.input.username);
            Entity targetUser = datastore.get(targetUserKey);

            if (targetUser == null)
                return Response.ok(g.toJson(new ErrorResponse("9902", "USER_NOT_FOUND"))).build();

            Map<String, String> responseData = new HashMap<>();
            responseData.put("username", targetUser.getKey().getName());
            responseData.put("role", targetUser.getString("user_role"));

            return Response.ok(g.toJson(new SuccessResponse(responseData))).build();

        } catch (Exception e) {
            LOG.severe("Erro interno ao mostrar role: " + e.getMessage());
            return Response.ok(g.toJson(new ErrorResponse("500", "Internal Server Error"))).build();
        }
    }
}
