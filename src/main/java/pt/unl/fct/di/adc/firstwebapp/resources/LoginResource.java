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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;
import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;
import pt.unl.fct.di.adc.firstwebapp.util.SuccessResponse;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private final Gson g = new Gson();
    // Forçar o ID do projeto para o Datastore funcionar localmente
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();

    public LoginResource() {}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginData data) {
        LOG.fine("Attempt to login user: " + data.username);

        // Input inválido (9906)
        if (data.username == null || data.password == null || data.username.trim().isEmpty() || data.password.trim().isEmpty()) {
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();
        }

        // 2. Criar a chave para procurar o utilizador na base de dados
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);

        try {
            // 3. Ir buscar o utilizador à base de dados
            Entity user = datastore.get(userKey);

            if (user != null) {
                // 4. Se o utilizador existir, sacar a password gravada e comparar com a que foi enviada
                String pwd = user.getString("user_pwd");

                if (pwd.equals(data.password)) {
                    // Buscar o role à BD para colocar no token
                    String role = user.getString("user_role");
                    AuthToken token = new AuthToken(data.username, role);

                    // Guardar o token na BD
                    Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(token.tokenId);
                    Entity tokenEntity = Entity.newBuilder(tokenKey)
                            .set("username", token.username)
                            .set("role", token.role)
                            .set("issuedAt", token.issuedAt)
                            .set("expiresAt", token.expiresAt)
                            .build();

                    datastore.put(tokenEntity);

                    Map<String, Object> tokenData = new HashMap<>();
                    tokenData.put("token", token);

                    LOG.info("User '" + data.username + "' logged in successfully.");
                    return Response.ok(g.toJson(new SuccessResponse(tokenData))).build();
                }
            }

            // Credenciais inválidas (Erro 9900) - Devolvemos 200 OK na mesma!
            LOG.warning("Failed login attempt for user: " + data.username);
            return Response.ok(g.toJson(new ErrorResponse("9900", "INVALID_CREDENTIALS"))).build();

        } catch (Exception e) {
            LOG.severe("Error during login: " + e.getMessage());
            return Response.ok(g.toJson(new ErrorResponse("500", "Internal server error."))).build();
        }
    }
}