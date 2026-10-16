package openraven.ravenxd.client;

public final class Account {
    public static final String UID = "1";
    public static final String VERSION = "3.1";
    public static final String NAME = "Raven XD " + VERSION;
    public static final String DEVELOPER = "qloha";

    private Account() {
    }

    public static boolean isDev() {
        return UID.equals("1");
    }

    public static String user() {
        return ChatCommands.ravenUser;
    }
}
