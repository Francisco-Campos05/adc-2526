package pt.unl.fct.di.adc.firstwebapp.resources;

import com.google.cloud.datastore.Entity;
import com.google.gson.Gson;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.firstwebapp.util.ErrorResponse;

public class VerificationResource {

    public VerificationResource() {}

    public static Response validateToken(Gson g, Entity token) {
        if (token == null)
            return Response.ok(g.toJson(new ErrorResponse("9903", "INVALID_TOKEN"))).build();

        if (token.getLong("expiresAt") < (System.currentTimeMillis() / 1000))
            return Response.ok(g.toJson(new ErrorResponse("9904", "TOKEN_EXPIRED"))).build();

        return null;
    }
}
