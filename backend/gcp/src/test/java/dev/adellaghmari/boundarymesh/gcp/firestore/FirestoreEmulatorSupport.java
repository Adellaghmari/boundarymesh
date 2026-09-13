package dev.adellaghmari.boundarymesh.gcp.firestore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Starts the Firebase/Firestore emulator on the host with firebase-tools. Named database ID is
 * {@code boundarymesh}. Adapter tests use the emulator REST API for that database.
 */
final class FirestoreEmulatorSupport implements AutoCloseable {

    static final String HOST = "127.0.0.1";
    static final String PROJECT_ID = "boundarymesh-dev";
    static final String DATABASE_ID = "boundarymesh";
    private static final String FIREBASE_TOOLS = "firebase-tools@15.28.2";

    private final Process process;
    private final int port;
    private final Path workDir;
    private final List<String> logs;

    private FirestoreEmulatorSupport(Process process, int port, Path workDir, List<String> logs) {
        this.process = process;
        this.port = port;
        this.workDir = workDir;
        this.logs = logs;
    }

    static FirestoreEmulatorSupport start() {
        EmulatorPorts ports = allocatePorts();
        Path workDir = writeConfig(ports);
        List<String> command = new ArrayList<>();
        if (windows()) {
            command.add("cmd.exe");
            command.add("/c");
        }
        command.add(npxCommand());
        command.addAll(
                List.of("--yes", FIREBASE_TOOLS, "emulators:start", "--only", "firestore", "--project", PROJECT_ID));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDir.toFile());
        builder.redirectErrorStream(true);
        List<String> logs = new CopyOnWriteArrayList<>();
        try {
            Process process = builder.start();
            Thread drain = new Thread(() -> drain(process, logs));
            drain.setDaemon(true);
            drain.start();
            waitUntilReady(process, ports.firestore, logs);
            return new FirestoreEmulatorSupport(process, ports.firestore, workDir, logs);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to start Firebase Firestore emulator\n" + String.join("\n", logs), ex);
        }
    }

    String endpoint() {
        return HOST + ":" + port;
    }

    @Override
    public void close() {
        destroyProcessTree(process);
        try {
            Files.deleteIfExists(workDir.resolve("firebase.json"));
            Files.deleteIfExists(workDir);
        } catch (IOException ignored) {
            // test leftover
        }
    }

    private static Path writeConfig(EmulatorPorts ports) {
        try {
            Path workDir = Files.createTempDirectory("boundarymesh-firestore-emulator");
            String json =
                    """
                    {
                      "firestore": {
                        "database": "%s",
                        "edition": "standard"
                      },
                      "emulators": {
                        "firestore": {
                          "host": "%s",
                          "port": %d,
                          "websocketPort": %d,
                          "edition": "standard"
                        },
                        "ui": { "enabled": false, "port": %d },
                        "hub": { "port": %d },
                        "logging": { "port": %d }
                      }
                    }
                    """
                            .formatted(
                                    DATABASE_ID,
                                    HOST,
                                    ports.firestore,
                                    ports.websocket,
                                    ports.ui,
                                    ports.hub,
                                    ports.logging);
            Files.writeString(workDir.resolve("firebase.json"), json, StandardCharsets.UTF_8);
            return workDir;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write firebase emulator config", ex);
        }
    }

    private static String npxCommand() {
        return "npx";
    }

    private static boolean windows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static EmulatorPorts allocatePorts() {
        return new EmulatorPorts(freePort(), freePort(), freePort(), freePort(), freePort());
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(HOST, 0));
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to allocate emulator port", ex);
        }
    }

    private static void waitUntilReady(Process process, int port, List<String> logs) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(90));
        Exception last = null;
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                throw new IllegalStateException("Firebase Firestore emulator exited early\n" + String.join("\n", logs));
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(HOST, port), 400);
                probeNamedDatabase(port);
                return;
            } catch (Exception ex) {
                last = ex;
                sleep(400);
            }
        }
        throw new IllegalStateException(
                "Firestore emulator was not ready on " + HOST + ":" + port + "\n" + String.join("\n", logs), last);
    }

    private static void probeNamedDatabase(int port) {
        FirestoreSettings settings = FirestoreSettings.emulator(PROJECT_ID, HOST + ":" + port);
        try (FirestoreFacade probe = GoogleFirestoreFacade.openEmulator(settings)) {
            FirestoreDocumentPath path = FirestoreDocumentPath.of("emulatorHealth", "probe");
            probe.set(path, Map.of("ok", true, "source", "fake-15fa-emulator-probe"));
            if (probe.get(path).isEmpty()) {
                throw new IllegalStateException("Named database boundarymesh probe read was empty");
            }
        }
    }

    private static void destroyProcessTree(Process process) {
        if (windows()) {
            try {
                new ProcessBuilder("taskkill", "/F", "/T", "/PID", Long.toString(process.pid()))
                        .redirectErrorStream(true)
                        .start()
                        .waitFor(20, TimeUnit.SECONDS);
                return;
            } catch (Exception ignored) {
                // fall through to Process destroy
            }
        }
        process.destroy();
        try {
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private static void drain(Process process, List<String> logs) {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
            }
        } catch (IOException ignored) {
            // process closed
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private record EmulatorPorts(int firestore, int hub, int logging, int ui, int websocket) {}
}
