package pt.unl.fct.di.adc.firstwebapp.util;

public class UpdateProfile {
    public AuthToken token;
    public Input input;

    public static class Input {
        public String username;
        public Attributes attributes;
    }

    public static class Attributes {
        public String phone;
        public String address;
    }
}