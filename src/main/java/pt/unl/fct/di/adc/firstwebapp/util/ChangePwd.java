package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangePwd {
    public AuthToken token;
    public Input input;

    public static class Input {
        public String username;
        public String oldPassword;
        public String newPassword;
    }
}