package dev.doughbay.fabric.hosted;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DashboardConfigTest {
    @TempDir Path tempDir;
    
    @BeforeEach void beforeEach() { DashboardConfig.reset(); }
    
    @AfterEach void afterEach() { DashboardConfig.reset(); }
    
    @Test void defaultsToPublicEndpoints() {
        assertEquals("gonuts.cyou", DashboardProtocol.DEFAULT_PAGE.getHost());
        assertEquals("gonuts.cyou", DashboardProtocol.DEFAULT_REDEEM.getHost());
        assertEquals("gonuts.cyou", DashboardProtocol.DEFAULT_CHECK.getHost());
        assertEquals("gonuts.cyou", DashboardProtocol.DEFAULT_SOCKET.getHost());
    }
    
    @Test void returnsDefaultsWhenNoConfigExists() {
        DashboardConfig.load(tempDir);
        assertEquals(DashboardProtocol.DEFAULT_PAGE, DashboardConfig.page());
        assertEquals(DashboardProtocol.DEFAULT_REDEEM, DashboardConfig.redeem());
        assertEquals(DashboardProtocol.DEFAULT_CHECK, DashboardConfig.check());
        assertEquals(DashboardProtocol.DEFAULT_SOCKET, DashboardConfig.socket());
    }
    
    @Test void loadsLocalDashboardConfig() throws Exception {
        Path dir = tempDir.resolve("gonuts-dashboard");
        dir.toFile().mkdirs();
        var json = """
            {
                "baseUrl": "http://192.168.2.8:8080",
                "page": "/dashboard/",
                "redeem": "/dashboard/api/pair/redeem",
                "check": "/dashboard/api/client/check",
                "socket": "/dashboard/ws/client"
            }
            """;
        java.nio.file.Files.writeString(dir.resolve("config.json"), json);
        DashboardConfig.load(tempDir);
        assertEquals("192.168.2.8", DashboardConfig.page().getHost());
        assertEquals(8080, DashboardConfig.page().getPort());
        assertEquals("192.168.2.8", DashboardConfig.redeem().getHost());
        assertEquals("192.168.2.8", DashboardConfig.check().getHost());
        assertEquals("192.168.2.8", DashboardConfig.socket().getHost());
    }
    
    @Test void supportsHttpsAndWssScheme() throws Exception {
        Path dir = tempDir.resolve("gonuts-dashboard");
        dir.toFile().mkdirs();
        var json = """
            {
                "baseUrl": "https://dashboard.local",
                "page": "/dashboard/",
                "socket": "/dashboard/ws/client"
            }
            """;
        java.nio.file.Files.writeString(dir.resolve("config.json"), json);
        DashboardConfig.load(tempDir);
        assertEquals("https", DashboardConfig.page().getScheme());
        assertEquals("wss", DashboardConfig.socket().getScheme());
    }
    
    @Test void acceptsFullUrlsInsteadOfRelative() throws Exception {
        Path dir = tempDir.resolve("gonuts-dashboard");
        dir.toFile().mkdirs();
        var json = """
            {
                "baseUrl": "http://example.com",
                "page": "http://other.com/app/",
                "socket": "wss://other.com/socket"
            }
            """;
        java.nio.file.Files.writeString(dir.resolve("config.json"), json);
        DashboardConfig.load(tempDir);
        assertEquals("other.com", DashboardConfig.page().getHost());
        assertEquals("other.com", DashboardConfig.socket().getHost());
    }
    
    @Test void ignoresMalformedConfig() throws Exception {
        Path dir = tempDir.resolve("gonuts-dashboard");
        dir.toFile().mkdirs();
        java.nio.file.Files.writeString(dir.resolve("config.json"), "not json");
        DashboardConfig.load(tempDir);
        assertEquals(DashboardProtocol.DEFAULT_PAGE, DashboardConfig.page());
    }
    
    @Test void resetReturnsToDefaults() throws Exception {
        Path dir = tempDir.resolve("gonuts-dashboard");
        dir.toFile().mkdirs();
        var json = "{\"baseUrl\": \"http://local\"}";
        java.nio.file.Files.writeString(dir.resolve("config.json"), json);
        DashboardConfig.load(tempDir);
        assertEquals("local", DashboardConfig.page().getHost());
        DashboardConfig.reset();
        assertEquals(DashboardProtocol.DEFAULT_PAGE, DashboardConfig.page());
    }
}