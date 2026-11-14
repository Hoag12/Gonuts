package dev.doughbay.fabric.hosted;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;

/**
 * Dashboard endpoint configuration. Reads from the mod's game directory
 * (gonuts-dashboard/config.json) and falls back to the gonuts.cyou
 * endpoints when no local override is set.
 *
 * <p>This lets a local development dashboard replace the public one
 * without changing any code paths — all callers use the static
 * accessors here instead of hard-coded URIs.
 */
public final class DashboardConfig {
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final URI DEFAULT_PAGE = DashboardProtocol.DEFAULT_PAGE;
    private static final URI DEFAULT_REDEEM = DashboardProtocol.DEFAULT_REDEEM;
    private static final URI DEFAULT_CHECK = DashboardProtocol.DEFAULT_CHECK;
    private static final URI DEFAULT_SOCKET = DashboardProtocol.DEFAULT_SOCKET;

    private static volatile URI page = DEFAULT_PAGE;
    private static volatile URI redeem = DEFAULT_REDEEM;
    private static volatile URI check = DEFAULT_CHECK;
    private static volatile URI socket = DEFAULT_SOCKET;

    private DashboardConfig() {}

    /** Called once at mod init to load any local dashboard config. */
    public static synchronized void load(Path gameDirectory) {
        Path file = gameDirectory.resolve("gonuts-dashboard").resolve("config.json");
        if (!Files.isRegularFile(file)) return;
        try {
            JsonNode n = JSON.readTree(Files.readString(file));
            String base = n.path("baseUrl").asText("").strip();
            if (base.isEmpty()) return;
            URI uri = URI.create(base);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https") && !scheme.equals("ws") && !scheme.equals("wss"))
                return;
            String path = uri.getPath();
            if (path == null || path.isEmpty()) path = "/";

            if (n.has("page")) {
                page = resolve(uri, n.path("page").asText("/dashboard/"));
            } else {
                page = uri;
            }
            redeem = resolve(uri, n.path("redeem").asText("/dashboard/api/pair/redeem"));
            check = resolve(uri, n.path("check").asText("/dashboard/api/client/check"));
            socket = resolve(uri, n.path("socket").asText("/dashboard/ws/client"));
            // Ensure socket uses WS/WSS scheme
            if (socket.getScheme() != null) {
                String s = socket.getScheme().toLowerCase();
                if (!s.equals("ws") && !s.equals("wss")) {
                    String wsScheme = scheme.equals("https") ? "wss" : "ws";
                    socket = URI.create(wsScheme + "://" + socket.getHost()
                            + (socket.getPort() >= 0 ? ":" + socket.getPort() : "")
                            + socket.getPath());
                }
            } else {
                String wsScheme = scheme.equals("https") ? "wss" : "ws";
                socket = URI.create(wsScheme + "://" + uri.getHost()
                        + (uri.getPort() >= 0 ? ":" + uri.getPort() : "")
                        + n.path("socket").asText("/dashboard/ws/client"));
            }
        } catch (Exception ignored) {
            // A malformed config file leaves the defaults in place.
        }
    }

    private static URI resolve(URI base, String relative) {
        if (relative.startsWith("http://") || relative.startsWith("https://")
                || relative.startsWith("ws://") || relative.startsWith("wss://"))
            return URI.create(relative);
        return base.resolve(relative);
    }

    public static URI page() { return page; }
    public static URI redeem() { return redeem; }
    public static URI check() { return check; }
    public static URI socket() { return socket; }

    /** Write a local config file pointing at a dashboard base URL. */
    public static void save(Path gameDirectory, String baseUrl) throws Exception {
        Path dir = gameDirectory.resolve("gonuts-dashboard");
        Files.createDirectories(dir);
        Path file = dir.resolve("config.json");
        ObjectNode n = JSON.createObjectNode();
        n.put("baseUrl", baseUrl);
        n.put("page", "/dashboard/");
        n.put("redeem", "/dashboard/api/pair/redeem");
        n.put("check", "/dashboard/api/client/check");
        n.put("socket", "/dashboard/ws/client");
        Path tmp = Files.createTempFile(dir, ".config-", ".tmp");
        try {
            Files.writeString(tmp, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(n));
            Files.move(tmp, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
        load(gameDirectory);
    }

    /** Reset to public endpoints (used for tests). */
    public static synchronized void reset() {
        page = DEFAULT_PAGE;
        redeem = DEFAULT_REDEEM;
        check = DEFAULT_CHECK;
        socket = DEFAULT_SOCKET;
    }
}