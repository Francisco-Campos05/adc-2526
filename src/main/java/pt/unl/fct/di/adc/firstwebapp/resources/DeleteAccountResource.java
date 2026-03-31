package pt.unl.fct.di.adc.firstwebapp.resources;

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
import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/deleteaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class DeleteAccountResource {

    private static final Logger LOG = Logger.getLogger(DeleteAccountResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();

    public DeleteAccountResource() {}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteAccount(UsernameTarget req) {
        if (req == null || req.token == null || req.token.tokenId == null || req.input == null || req.input.username == null) {
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();
        }

        try {
            Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenId);
            Entity storedToken = datastore.get(tokenKey);

            if (storedToken == null)
                return Response.ok(g.toJson(new ErrorResponse("9903", "INVALID_TOKEN"))).build();

            if (storedToken.getLong("expiresAt") < (System.currentTimeMillis() / 1000))
                return Response.ok(g.toJson(new ErrorResponse("9904", "TOKEN_EXPIRED"))).build();

            // 3. Verificar se o utilizador que está a pedir a eliminação é mesmo ADMIN
            if (!"ADMIN".equals(storedToken.getString("role"))) {
                LOG.warning("Tentativa de apagar conta sem privilégios ADMIN.");
                return Response.ok(g.toJson(new ErrorResponse("9905", "UNAUTHORIZED"))).build();
            }

            Key targetUserKey = datastore.newKeyFactory().setKind("User").newKey(req.input.username);
            Entity targetUser = datastore.get(targetUserKey);

            // 4. Verificar se a conta alvo a apagar existe
            if (targetUser == null)
                return Response.ok(g.toJson(new ErrorResponse("9902", "USER_NOT_FOUND"))).build();

            // 5. Apagar o utilizador do Datastore
            datastore.delete(targetUserKey);

            // 6. Apagar TODAS as sessões ativas (Tokens) associadas a este utilizador (Regra do PDF!)
            Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                    .setKind("UserToken")
                    .setFilter(PropertyFilter.eq("username", req.input.username))
                    .build();

            QueryResults<Entity> tokensToDelete = datastore.run(tokenQuery);
            while (tokensToDelete.hasNext()) {
                datastore.delete(tokensToDelete.next().getKey()); // Obliterar o token!
            }

            LOG.info("Conta e sessões apagadas com sucesso: " + req.input.username);
            return Response.ok(g.toJson(new SuccessResponse("Account deleted successfully"))).build();

        } catch (Exception e) {
            LOG.severe("Erro interno a apagar conta: " + e.getMessage());
            return Response.ok(g.toJson(new ErrorResponse("500", "Internal server error"))).build();
        }
    }
}