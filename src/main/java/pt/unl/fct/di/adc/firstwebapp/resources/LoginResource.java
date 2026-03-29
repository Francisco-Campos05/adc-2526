package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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

        // 1. Verificações de segurança
        if (data.username == null || data.password == null || data.username.trim().isEmpty() || data.password.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(g.toJson(new ErrorResponse("400", "Missing username or password."))).build();
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
                    // 5. Sucesso! Gerar a "Pulseira VIP" (Token)
                    AuthToken token = new AuthToken(data.username);

                    // 6. Guardar o Token na base de dados (para sabermos no futuro se ele é válido)
                    Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(token.tokenID);
                    Entity tokenEntity = Entity.newBuilder(tokenKey)
                            .set("username", token.username)
                            .set("creationData", token.creationData)
                            .set("expirationData", token.expirationData)
                            .build();

                    datastore.put(tokenEntity);

                    // 7. Devolver o Token ao utilizador no Postman
                    LOG.info("User '" + data.username + "' logged in successfully.");
                    return Response.ok(g.toJson(token)).build();
                }
            }

            // 8. Se chegou aqui, é porque a password estava errada ou o utilizador não existe
            LOG.warning("Failed login attempt for user: " + data.username);
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson(new ErrorResponse("403", "Incorrect username or password.")))
                    .build();

        } catch (Exception e) {
            LOG.severe("Error during login: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(g.toJson(new ErrorResponse("500", "Internal server error.")))
                    .build();
        }
    }

    @GET
    @Path("/{username}")
    public Response checkUsernameAvailable(@PathParam("username") String username) {
        // Substituir a lógica hardcoded do jleitao por uma verificação real na BD
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
        Entity user = datastore.get(userKey);

        // Se 'user' não for null, o username já está ocupado (false). Se for null, está livre (true).
        boolean isAvailable = (user == null);

        return Response.ok().entity(g.toJson(isAvailable)).build();
    }
}