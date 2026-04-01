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
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;
import pt.unl.fct.di.adc.firstwebapp.util.SuccessResponse;
import pt.unl.fct.di.adc.firstwebapp.util.UsernameTarget;

@Path("/logout")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LogoutResource {

    private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();

    public LogoutResource() {}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doLogout(UsernameTarget req) {

        // 1. Validar estrutura do JSON
        if (req == null || req.token == null || req.token.tokenId == null ||
                req.input == null || req.input.username == null) {
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();
        }

        try {
            // 2. Validar o Token de quem está a fazer o pedido
            Key requesterTokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenId);
            Entity storedToken = datastore.get(requesterTokenKey);

            if (storedToken == null)
                return Response.ok(g.toJson(new ErrorResponse("9903", "INVALID_TOKEN"))).build();

            if (storedToken.getLong("expiresAt") < (System.currentTimeMillis() / 1000)) {
                datastore.delete(requesterTokenKey);
                return Response.ok(g.toJson(new ErrorResponse("9904", "TOKEN_EXPIRED"))).build();
            }

            // 3. Verificar Autorização
            String requesterUsername = storedToken.getString("username");
            String requesterRole = storedToken.getString("role");
            String targetUsername = req.input.username;

            if (!requesterUsername.equals(targetUsername) && !"ADMIN".equals(requesterRole)) {
                LOG.warning("Tentativa de logout não autorizada de " + requesterUsername + " sobre " + targetUsername);
                return Response.ok(g.toJson(new ErrorResponse("9905", "UNAUTHORIZED"))).build();
            }

            // 4. Procurar e apagar todos os tokens do utilizador alvo na BD,
            // isto garante que o ADMIN expulsa o utilizador de todos os dispositivos.
            Query<Entity> tokenQuery = Query.newEntityQueryBuilder().setKind("UserToken")
                    .setFilter(PropertyFilter.eq("username", targetUsername)).build();

            QueryResults<Entity> tokensToDelete = datastore.run(tokenQuery);
            int count = 0;
            while (tokensToDelete.hasNext()) {
                datastore.delete(tokensToDelete.next().getKey());
                count++;
            }

            Map<String, String> responseData = new HashMap<>();
            responseData.put("message", "LogoutResource successful");

            LOG.info("LogoutResource realizado. Foram invalidadas " + count + " sessões para o utilizador: " + targetUsername);
            return Response.ok(g.toJson(new SuccessResponse(responseData))).build();

        } catch (Exception e) {
            LOG.severe("Erro interno no logout: " + e.getMessage());
            return Response.ok(g.toJson(new ErrorResponse("500", "Internal Server Error"))).build();
        }
    }
}