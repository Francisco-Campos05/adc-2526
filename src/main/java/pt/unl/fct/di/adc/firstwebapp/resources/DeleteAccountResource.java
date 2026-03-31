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
import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.DeleteAccount;
import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;
import pt.unl.fct.di.adc.firstwebapp.util.SuccessResponse;

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
    public Response deleteAccount(DeleteAccount req) {
        LOG.fine("Attempt to delete account.");

        // 1. Validar inputs
        if (req == null || req.token == null || req.token.tokenID == null || req.targetUsername == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(g.toJson(new ErrorResponse("400", "Missing token or target username."))).build();
        }

        try {
            // 2. Validar o Token do Administrador na BD
            Key tokenKey = datastore.newKeyFactory().setKind("UserToken").newKey(req.token.tokenID);
            Entity storedToken = datastore.get(tokenKey);

            if (storedToken == null || storedToken.getLong("expirationData") < System.currentTimeMillis()) {
                return Response.status(Response.Status.FORBIDDEN).entity(g.toJson(new ErrorResponse("403", "Invalid or Expired Token."))).build();
            }

            // 3. Buscar o utilizador que está a fazer o pedido para verificar se é ADMIN
            String requesterUsername = storedToken.getString("username");
            Key requesterKey = datastore.newKeyFactory().setKind("User").newKey(requesterUsername);
            Entity requester = datastore.get(requesterKey);

            if (requester == null || !"ADMIN".equals(requester.getString("user_role"))) {
                LOG.warning("User " + requesterUsername + " attempted to delete an account without ADMIN privileges.");
                return Response.status(Response.Status.FORBIDDEN).entity(g.toJson(new ErrorResponse("403", "Access denied. Admins only."))).build();
            }

            // 4. Verificar se a conta a apagar existe
            Key targetUserKey = datastore.newKeyFactory().setKind("User").newKey(req.targetUsername);
            Entity targetUser = datastore.get(targetUserKey);

            if (targetUser == null) {
                return Response.status(Response.Status.NOT_FOUND).entity(g.toJson(new ErrorResponse("404", "Target user not found."))).build();
            }

            // 5. O Admin pode apagar! (Apaga a conta na base de dados)
            datastore.delete(targetUserKey);

            LOG.info("Admin " + requesterUsername + " deleted account: " + req.targetUsername);
            return Response.ok(g.toJson(new SuccessResponse("Account deleted successfully."))).build();

        } catch (Exception e) {
            LOG.severe("Error deleting account: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(g.toJson(new ErrorResponse("500", "Internal server error."))).build();
        }
    }
}
