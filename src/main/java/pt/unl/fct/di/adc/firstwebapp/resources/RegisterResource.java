package pt.unl.fct.di.adc.firstwebapp.resources;

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
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;
import pt.unl.fct.di.adc.firstwebapp.util.Register;
import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;
import pt.unl.fct.di.adc.firstwebapp.util.SuccessResponse;

@Path("/createaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {

    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("adc-projeto-francisco").build().getService();

    public RegisterResource() {}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doRegistration(Register req) {

        // Se o JSON não trouxer o objeto "input", dá erro
        if (req == null || req.input == null) {
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();
        }

        RegisterData data = req.input;
        LOG.fine("Tentativa de registo do utilizador: " + data.username);

        // Validações básicas
        if (data.username == null || data.password == null || !data.password.equals(data.confirmation)) {
            return Response.ok(g.toJson(new ErrorResponse("9906", "INVALID_INPUT"))).build();
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Transaction txn = datastore.newTransaction();

        try {
            Entity user = txn.get(userKey);

            if (user != null) {
                // Utilizador já existe! Devolvemos 200 OK, mas com a framework de erro exigida
                txn.rollback();
                return Response.ok(g.toJson(new ErrorResponse("9901", "USER_ALREADY_EXISTS"))).build();
            } else {
                // Criar a entidade para guardar na Base de Dados
                user = Entity.newBuilder(userKey)
                        .set("user_pwd", data.password) // (Mais tarde deve aplicar Hash à password aqui)
                        .set("user_email", data.email != null ? data.email : "")
                        .set("user_phone", data.phone != null ? data.phone : "")
                        .set("user_address", data.address != null ? data.address : "")
                        .set("user_role", data.role != null ? data.role : "USER")
                        .build();

                txn.put(user);
                txn.commit();

                LOG.info("Utilizador registado com sucesso: " + data.username);
                return Response.ok(g.toJson(new SuccessResponse("Conta criada com sucesso"))).build();
            }
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}
