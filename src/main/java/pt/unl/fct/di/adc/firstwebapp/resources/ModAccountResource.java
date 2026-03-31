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
import pt.unl.fct.di.adc.firstwebapp.util.UpdateProfile;

@Path("/modaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ModAccountResource {

    private static final Logger LOG = Logger.getLogger(ModAccountResource.class.getName());
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();
    private final Gson g = new Gson();

    public ModAccountResource() {}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyAccount(UpdateProfile req) {

        // 1. Validar se o input base existe (Erro 9906: INVALID_INPUT)
        if (req == null || req.token == null || req.token.tokenId == null ||
                req.input == null || req.input.username == null) {
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();
        }

        try {
            // 2. Validar o Token na BD
            Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenId);
            Entity storedToken = datastore.get(tokenKey);

            if (storedToken == null) {
                return Response.ok(g.toJson(new ErrorResponse("9903", "INVALID_TOKEN"))).build();
            }
            if (storedToken.getLong("expiresAt") < (System.currentTimeMillis() / 1000)) {
                return Response.ok(g.toJson(new ErrorResponse("9904", "TOKEN_EXPIRED"))).build();
            }

            // 3. Verificar permissões (Erro 9905: UNAUTHORIZED)
            // Apenas o próprio, um ADMIN ou um BOFFICER podem modificar contas
            String requesterUsername = storedToken.getString("username");
            String requesterRole = storedToken.getString("role");

            if (!requesterUsername.equals(req.input.username) &&
                    !"ADMIN".equals(requesterRole) &&
                    !"BOFFICER".equals(requesterRole)) {
                return Response.ok(g.toJson(new ErrorResponse("9905", "UNAUTHORIZED"))).build();
            }

            // 4. Verificar se a conta a alterar existe (Erro 9902: USER_NOT_FOUND)
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(req.input.username);
            Entity user = datastore.get(userKey);

            if (user == null) {
                return Response.ok(g.toJson(new ErrorResponse("9902", "USER_NOT_FOUND"))).build();
            }

            // 5. Atualizar os atributos
            Entity.Builder builder = Entity.newBuilder(user);

            if (req.input.attributes != null) {
                if (req.input.attributes.phone != null) {
                    builder.set("user_phone", req.input.attributes.phone);
                }
                if (req.input.attributes.address != null) {
                    builder.set("user_address", req.input.attributes.address);
                }
            }

            datastore.put(builder.build());

            // 6. Resposta de sucesso
            Map<String, String> responseData = new HashMap<>();
            responseData.put("message", "Updated successfully");

            LOG.info("Atributos da conta atualizados com sucesso: " + req.input.username);
            return Response.ok(g.toJson(new SuccessResponse(responseData))).build();

        } catch (Exception e) {
            LOG.severe("Erro interno ao modificar conta: " + e.getMessage());
            return Response.ok(g.toJson(new ErrorResponse("500", "Internal Server Error"))).build();
        }
    }
}