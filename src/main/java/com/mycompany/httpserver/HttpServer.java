package com.mycompany.httpserver;

import com.mycompany.microspingboot.anotations.GetMapping;
import com.mycompany.microspingboot.anotations.RequestParam;
import com.mycompany.microspingboot.anotations.RestController;
import java.net.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServer {

    public static Map<String, Method> services = new HashMap();
    //public static Map<String, Handler> services = new HashMap<>();
    private static String staticRoot = null;
    private static final Path PUBLIC_DIR = Paths.get("src", "main", "resources", "webroot").toAbsolutePath();    
    private static final Map<String, Object> instances = new HashMap<>();
    private static volatile Path publicDir = null;
    private static final int DEFAULT_PORT = 35000;
    private static final int DEFAULT_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static ServerSocket serverSocket;
    private static ExecutorService pool;
 
    public static void staticfiles(String classpathDir) {
        try {
            var root = Paths.get(HttpServer.class.getResource(classpathDir).toURI());
            publicDir = root;
        } catch (Exception e) {
            System.err.println("No static dir found: " + e.getMessage());
        }
    }

    public static void registerController(String fqcn) throws ReflectiveOperationException {
        Class<?> cls = Class.forName(fqcn);
        if (!cls.isAnnotationPresent(RestController.class)) {
            return;
        }
        Object inst = cls.getDeclaredConstructor().newInstance();
        for (Method m : cls.getDeclaredMethods()) {
            GetMapping gm = m.getDeclaredAnnotation(GetMapping.class);
            if (gm == null) {
                continue;
            }
            services.put(gm.value(), m);
            instances.put(gm.value(), inst);
        }
    }

    private static void loadServices(String[] args) {
        try {
            registerController("com.mycompany.microspingboot.examples.GreetingController");
        } catch (Exception ignore) {
        }
    }

    public static void runServer(String[] args) throws IOException {
        loadServices(args);

        int port = DEFAULT_PORT;
        try {
            if (args != null && args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
        } catch (Exception ignore) {
        }

        int threads = DEFAULT_THREADS;
        pool = Executors.newFixedThreadPool(threads);

        Runtime.getRuntime().addShutdownHook(new Thread(HttpServer::gracefulStop));

        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        System.out.println("Escuchando por http://localhost:" + port + " con " + threads + " threads");

        while (running.get()) {
            try {
                final Socket client = serverSocket.accept();
                pool.execute(() -> handleClient(client));
            } catch (SocketException se) {
                if (running.get()) {
                    System.err.println("SocketException: " + se.getMessage());
                }
            }
        }
    }

    public static void gracefulStop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        if (pool != null) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Server stopped gracefully.");
    }

    private static void handleClient(Socket clientSocket) {
        try (clientSocket;
                OutputStream rawOut = clientSocket.getOutputStream(); PrintWriter out = new PrintWriter(rawOut, true); BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String inputLine;
            boolean firstline = true;
            URI requri = null;

            while ((inputLine = in.readLine()) != null) {
                if (firstline) {
                    String[] parts = inputLine.split(" ");
                    if (parts.length > 1) {
                        requri = new URI(parts[1]);
                    }
                    firstline = false;
                }
                if (!in.ready()) {
                    break;
                }
            }

            if (requri == null) {
                sendText(rawOut, 400, "Bad Request");
                return;
            }

            // endpoint de cierre
            if ("/__shutdown".equals(requri.getPath())) {
                sendText(rawOut, 200, "Bye");
                new Thread(HttpServer::gracefulStop).start();
                return;
            }

            String outputLine;
            if (requri.getPath().startsWith("/app")) {
                outputLine = invokeService(requri);
            } else {
                boolean served = publicDir != null && serveStatic(publicDir, requri.getPath(), rawOut);
                if (served) {
                    return;
                }
                outputLine = defaultIndex();
            }

            out.println(outputLine);
            out.flush();
        } catch (Exception e) {
            System.err.println("Error handling client: " + e);
        }
    }

    public static String invokeService(URI requri) {
        String path = requri.getPath().replace("/app", "");
        Method m = services.get(path);
        Object inst = instances.get(path);
        //String header = "HTTP/1.1 200 OK\\r\\nContent-Type: text/html\\r\\n\\r\\n";
        String header = "HTTP/1.1 200 OK\r\ncontent-type: text/plain; charset=utf-8\r\n\r\n";
        if (m == null) {
            return header + "Service not found";
        }

        try {
            if (m.getParameterCount() == 0) {
                return header + m.invoke(inst).toString();
            } else {
                var params = m.getParameters();
                Object[] values = new Object[params.length];
                HttpRequest req = new HttpRequest(requri);
                for (int i = 0; i < params.length; i++) {
                    RequestParam rp = params[i].getAnnotation(RequestParam.class);
                    String v = rp != null ? req.getValue(rp.value()) : null;
                    if (v == null || v.isEmpty()) {
                        v = rp != null ? rp.defaultValue() : "";
                    }
                    values[i] = v;
                }
                Object ret = m.invoke(inst, values);
                return header + (ret == null ? "" : ret.toString());
            }
        } catch (Exception e) {
            Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, e);
            return header + "Error";
        }
    }

    private static boolean serveStatic(Path publicDir, String path, OutputStream out) {
        try {
            Path p = publicDir.resolve("." + path).normalize();
            if (!p.startsWith(publicDir) || !Files.exists(p) || Files.isDirectory(p)) {
                return false;
            }
            byte[] bytes = Files.readAllBytes(p);
            String ct = guessCT(p.getFileName().toString());

            var pw = new PrintWriter(new OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8), false);
            pw.print("HTTP/1.1 200 OK\\r\\n");
            pw.printf("Content-Type: %s\\r\\n", ct);
            pw.printf("Content-Length: %d\\r\\n", bytes.length);
            pw.print("\\r\\n");
            pw.flush();

            out.write(bytes);
            out.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String guessCT(String f) {
        String n = f.toLowerCase();
        if (n.endsWith(".html") || n.endsWith(".htm")) {
            return "text/html; charset=utf-8";
        }
        if (n.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (n.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (n.endsWith(".png")) {
            return "image/png";
        }
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (n.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "text/plain; charset=utf-8";
    }

    private static void sendText(OutputStream out, int code, String body) throws IOException {
        var pw = new PrintWriter(new OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8), false);
        pw.printf("HTTP/1.1 %d OK\\r\\n", code);
        pw.print("Content-Type: text/plain; charset=utf-8\\r\\n\\r\\n");
        pw.print(body);
        pw.flush();
    }

    private static String defaultIndex() {
        return "HTTP/1.1 200 OK\\r\\nContent-Type: text/html\\r\\n\\r\\n"
                + "<html><body><h1>It works</h1>"
                + "<p>Try <code>/app/greeting?name=Juan</code></p>"
                + "<p>Shutdown with <code>/__shutdown</code></p>"
                + "</body></html>";
    }
}
