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

import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;
import pt.unl.fct.di.adc.firstwebapp.util.GenericToken;
import pt.unl.fct.di.adc.firstwebapp.util.SuccessResponse;

@Path("/showauthsessions")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowAuthSessionsResource {

    private static final Logger LOG = Logger.getLogger(ShowAuthSessionsResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();

    public ShowAuthSessionsResource() {}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showSessions(GenericToken req) {

        if (req == null || req.token == null || req.token.tokenId == null) {
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();
        }

        try {
            Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenId);
            Entity storedToken = datastore.get(tokenKey);

            if (storedToken == null) return Response.ok(g.toJson(new ErrorResponse("9903", "INVALID_TOKEN"))).build();
            if (storedToken.getLong("expiresAt") < (System.currentTimeMillis() / 1000))
                return Response.ok(g.toJson(new ErrorResponse("9904", "TOKEN_EXPIRED"))).build();

            if (!"ADMIN".equals(storedToken.getString("role"))) {
                return Response.ok(g.toJson(new ErrorResponse("9905", "UNAUTHORIZED"))).build();
            }

            Query<Entity> query = Query.newEntityQueryBuilder().setKind("UserToken").build();
            QueryResults<Entity> results = datastore.run(query);

            List<Map<String, Object>> sessionList = new ArrayList<>();
            long currentTime = System.currentTimeMillis() / 1000;

            while (results.hasNext()) {
                Entity t = results.next();

                // Extrair o tempo (lidando com os antigos também)
                long expiresAt = t.contains("expiresAt") ? t.getLong("expiresAt") : 0;

                //Se a sessão já expirou, apaga-mo-la da Base de Dados e saltamos à frente!
                if (expiresAt < currentTime) {
                    datastore.delete(t.getKey());
                    continue; // Ignora o resto e passa para o próximo Token
                }

                Map<String, Object> sessionInfo = new HashMap<>();
                sessionInfo.put("tokenId", t.getKey().getName());
                sessionInfo.put("username", t.contains("username") ? t.getString("username") : "Desconhecido");
                sessionInfo.put("role", t.contains("role") ? t.getString("role") : "USER");
                sessionInfo.put("expiresAt", expiresAt);

                sessionList.add(sessionInfo);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("sessions", sessionList);

            return Response.ok(g.toJson(new SuccessResponse(responseData))).build();

        } catch (Exception e) {
            LOG.severe("Erro interno ao listar sessões: " + e.getMessage());
            return Response.ok(g.toJson(new ErrorResponse("500", "Internal Server Error"))).build();
        }
    }
}