package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangeRole {
    public AuthToken token;
    public Input input;

    public static class Input {
        public String username;
        public String newRole;
    }
}