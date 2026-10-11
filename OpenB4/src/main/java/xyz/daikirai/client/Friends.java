package xyz.daikirai.client;

import java.util.HashSet;

public final class Friends {
    public static HashSet<String> friends = new HashSet<String>();
    public static HashSet<String> enemies = new HashSet<String>();

    private Friends() {
    }

    public static boolean isFriend(String s) {
        return !friends.isEmpty() && friends.contains(s.toLowerCase());
    }

    public static boolean isEnemy(String s) {
        return !enemies.isEmpty() && enemies.contains(s.toLowerCase());
    }

    public static boolean addFriend(String s) {
        if (friends.add(s.toLowerCase())) {
            Chat.print("&7Added friend: &b" + s);
            ClientConfig.save();
            return true;
        }
        return false;
    }

    public static boolean addEnemy(String s) {
        if (enemies.add(s.toLowerCase())) {
            Chat.print("&7Added enemy: &b" + s);
            ClientConfig.save();
            return true;
        }
        return false;
    }

    public static boolean removeEnemy(String s) {
        if (enemies.remove(s.toLowerCase())) {
            Chat.print("&7Removed enemy: &b" + s);
            ClientConfig.save();
            return true;
        }
        return false;
    }

    public static boolean removeFriend(String s) {
        if (friends.remove(s.toLowerCase())) {
            Chat.print("&7Removed friend: &b" + s);
            ClientConfig.save();
            return true;
        }
        return false;
    }
}
