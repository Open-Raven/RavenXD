package xyz.daikirai.script;

import java.util.Random;

public final class ScriptText {
    private static final Random RANDOM = new Random();

    private ScriptText() {
    }

    public static String randomClassName(int n) {
        String string = "Il";
        StringBuilder stringBuilder = new StringBuilder(n);
        for (int i = 0; i < n; ++i) {
            stringBuilder.append(string.charAt(RANDOM.nextInt(string.length())));
        }
        return stringBuilder.toString();
    }

    public static int skipLiteral(String string, int n, char c) {
        int n2 = n + 1;
        boolean escaped = false;
        while (n2 < string.length()) {
            char c2 = string.charAt(n2++);
            if (!escaped) {
                if (c2 != '\\') {
                    if (c2 != c) {
                        continue;
                    }
                    break;
                } else {
                    escaped = true;
                }
            } else {
                escaped = false;
            }
        }
        return n2;
    }

    public static String decodeUnicodeEscapes(String var1) {
        if (var1 != null && var1.indexOf(92) >= 0) {
            StringBuilder var2 = new StringBuilder(var1.length());
            int var3 = var1.length();
            for (int var4 = 0; var4 < var3; ++var4) {
                char var5 = var1.charAt(var4);
                if (var5 == '\\' && var4 + 1 < var3 && var1.charAt(var4 + 1) == 'u') {
                    int var6 = 0;
                    int var7;
                    for (var7 = var4 - 1; var7 >= 0 && var1.charAt(var7) == '\\'; --var7) {
                        ++var6;
                    }
                    if ((var6 & 1) == 1) {
                        var2.append(var5);
                        continue;
                    }
                    for (var7 = var4 + 2; var7 < var3 && var1.charAt(var7) == 'u'; ++var7) {
                    }
                    if (var7 + 4 <= var3) {
                        int var8 = 0;
                        boolean var9 = true;
                        for (int var10 = 0; var10 < 4; ++var10) {
                            int var11 = Character.digit(var1.charAt(var7 + var10), 16);
                            if (var11 < 0) {
                                var9 = false;
                                break;
                            }
                            var8 = var8 << 4 | var11;
                        }
                        if (var9) {
                            var2.append((char) var8);
                            var4 = var7 + 3;
                            continue;
                        }
                    }
                }
                var2.append(var5);
            }
            return var2.toString();
        }
        return var1;
    }

    public static String dropIgnorable(String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }
        StringBuilder stringBuilder = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (Character.isIdentifierIgnorable(c)) continue;
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    public static String stripComments(String var1) {
        int var2 = var1.length();
        char[] var3 = var1.toCharArray();
        StringBuilder var4 = new StringBuilder(var2);
        boolean var5 = false;
        boolean var6 = false;
        boolean var7 = false;
        boolean var8 = false;
        boolean var9 = false;
        try {
            for (int var10 = 0; var10 < var2; ++var10) {
                char var11 = var3[var10];
                if (!var7) {
                    if (!var8) {
                        if (!var5) {
                            if (!var6) {
                                if (var11 != '/' && var11 != '"' && var11 != '\'') {
                                    var4.append(var11);
                                } else if (var11 == '/' && var10 + 1 < var2) {
                                    char var12 = var3[var10 + 1];
                                    if (var12 != '/') {
                                        if (var12 != '*') {
                                            var4.append('/');
                                        } else {
                                            var8 = true;
                                            ++var10;
                                        }
                                    } else {
                                        var7 = true;
                                        ++var10;
                                    }
                                } else if (var11 != '"') {
                                    var6 = true;
                                    var4.append('\'');
                                } else {
                                    var5 = true;
                                    var4.append('"');
                                }
                            } else {
                                var4.append(var11);
                                if (!var9) {
                                    if (var11 != '\\') {
                                        if (var11 == '\'') {
                                            var6 = false;
                                        }
                                    } else {
                                        var9 = true;
                                    }
                                } else {
                                    var9 = false;
                                }
                            }
                        } else {
                            var4.append(var11);
                            if (!var9) {
                                if (var11 != '\\') {
                                    if (var11 == '"') {
                                        var5 = false;
                                    }
                                } else {
                                    var9 = true;
                                }
                            } else {
                                var9 = false;
                            }
                        }
                    } else {
                        if (var11 != '*') {
                            continue;
                        }
                        if (var10 + 1 < var2 && var3[var10 + 1] == '/') {
                            var8 = false;
                            ++var10;
                        }
                    }
                } else if (var11 == '\n' || var11 == '\r') {
                    var7 = false;
                    var4.append(var11);
                    if (var11 == '\r' && var10 + 1 < var2 && var3[var10 + 1] == '\n') {
                        var4.append('\n');
                        ++var10;
                    }
                }
            }
        } catch (Exception var14) {
            return var1;
        }
        return var4.toString();
    }

    public static boolean callsMangledName(String string) {
        int n = string.length();
        int n2 = 0;
        while (n2 < n) {
            int n3;
            char c = string.charAt(n2);
            if (c == '"' || c == '\'') {
                n2 = skipLiteral(string, n2, c);
                continue;
            }
            if (c != 'm') {
                ++n2;
                continue;
            }
            int n4 = n2 + 15;
            if (n4 > n) {
                return false;
            }
            if (n2 > 0 && Character.isJavaIdentifierPart(string.charAt(n2 - 1))) {
                ++n2;
                continue;
            }
            boolean bl = true;
            for (n3 = n2 + 1; n3 < n4; ++n3) {
                char c2 = string.charAt(n3);
                if (c2 >= '0' && c2 <= '9' || c2 >= 'a' && c2 <= 'f') continue;
                bl = false;
                break;
            }
            if (!bl || n4 < n && Character.isJavaIdentifierPart(string.charAt(n4))) {
                ++n2;
                continue;
            }
            for (n3 = n4; n3 < n && string.charAt(n3) <= ' '; ++n3) {
            }
            if (n3 < n && string.charAt(n3) == '(') {
                return true;
            }
            n2 = n4;
        }
        return false;
    }

    public static boolean isSafe(String string, boolean encrypted) {
        string = decodeUnicodeEscapes(string);
        string = dropIgnorable(string);
        string = stripComments(string);
        if (!encrypted && callsMangledName(string)) {
            return false;
        }
        string = string.replaceAll("\\s+", "");
        for (String string2 : ScriptManager.BLOCKED) {
            if (!string.contains(string2)) continue;
            return false;
        }
        return true;
    }
}
