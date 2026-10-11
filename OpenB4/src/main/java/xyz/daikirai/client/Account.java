package xyz.daikirai.client;

public final class Account {
    public static final String UID = "1";
    public static final String VERSION = "premium-20";

    private Account() {
    }

    public static boolean isDev() {
        return UID.equals("1");
    }

    public static String user() {
        return ChatCommands.ravenUser;
    }
}
