
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Main {

    static final String URL = "jdbc:mysql://localhost:3306/teamforge";
    static final String USER = "root";
    static final String PASSWORD =
            System.getenv().getOrDefault("TEAMFORGE_DB_PASSWORD", "root");

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0);

        // Health check
        server.createContext("/api/health", ex -> {
            if (handleOptions(ex)) return;

            if (!ex.getRequestMethod().equals("GET")) {
                send(ex, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            send(ex, 200,
                    "{\"status\":\"running\",\"app\":\"TeamForge\"}");
        });

        // Students API
        server.createContext("/api/students", ex -> {
            if (handleOptions(ex)) return;

            try (Connection con = db()) {

                String method = ex.getRequestMethod();

                if (method.equals("GET")) {

                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;

                    try (Statement st = con.createStatement();
                         ResultSet rs = st.executeQuery(
                                 "SELECT id,name,email,skills FROM students ORDER BY id")) {

                        while (rs.next()) {
                            if (!first) json.append(",");
                            first = false;

                            json.append("{")
                                    .append("\"id\":").append(rs.getInt("id")).append(",")
                                    .append("\"name\":").append(quote(rs.getString("name"))).append(",")
                                    .append("\"email\":").append(quote(rs.getString("email"))).append(",")
                                    .append("\"skills\":").append(quote(rs.getString("skills")))
                                    .append("}");
                        }
                    }

                    json.append("]");
                    send(ex, 200, json.toString());

                } else if (method.equals("POST")) {

                    Map<String, String> data = readForm(ex);

                    String name = data.get("name");
                    String email = data.get("email");
                    String skills = data.get("skills");

                    if (empty(name) || empty(email) || empty(skills)) {
                        send(ex, 400,
                                "{\"error\":\"Name, email and skills are required\"}");
                        return;
                    }

                    String sql =
                            "INSERT INTO students(name,email,skills) VALUES(?,?,?)";

                    try (PreparedStatement ps = con.prepareStatement(
                            sql, Statement.RETURN_GENERATED_KEYS)) {

                        ps.setString(1, name.trim());
                        ps.setString(2, email.trim());
                        ps.setString(3, skills.trim());
                        ps.executeUpdate();

                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            keys.next();

                            send(ex, 201,
                                    "{\"message\":\"Student registered\",\"id\":"
                                            + keys.getInt(1) + "}");
                        }
                    }

                } else {
                    send(ex, 405, "{\"error\":\"Method not allowed\"}");
                }

            } catch (SQLIntegrityConstraintViolationException e) {
                send(ex, 409,
                        "{\"error\":\"This email may already be registered\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(ex, 500,
                        "{\"error\":\"Student operation failed\"}");
            }
        });

        // Projects, team generation and saved team membership API
        server.createContext("/api/projects", ex -> {
            if (handleOptions(ex)) return;

            String path = ex.getRequestURI().getPath();

            if (path.matches("/api/projects/\\d+/generate")) {
                if (!ex.getRequestMethod().equals("POST")) {
                    send(ex, 405,
                            "{\"error\":\"Use POST to generate a team\"}");
                    return;
                }

                generateTeam(ex);
                return;
            }

            if (path.matches("/api/projects/\\d+/team")) {
                if (!ex.getRequestMethod().equals("GET")) {
                    send(ex, 405,
                            "{\"error\":\"Use GET to retrieve a team\"}");
                    return;
                }

                getProjectTeam(ex);
                return;
            }

            // Only the projects collection is supported below.
            if (!path.equals("/api/projects")) {
                send(ex, 404, "{\"error\":\"Endpoint not found\"}");
                return;
            }

            try (Connection con = db()) {

                String method = ex.getRequestMethod();

                if (method.equals("GET")) {

                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;

                    try (Statement st = con.createStatement();
                         ResultSet rs = st.executeQuery(
                                 "SELECT id,title,required_skills,team_size " +
                                         "FROM projects ORDER BY id")) {

                        while (rs.next()) {
                            if (!first) json.append(",");
                            first = false;

                            json.append("{")
                                    .append("\"id\":").append(rs.getInt("id")).append(",")
                                    .append("\"title\":").append(quote(rs.getString("title"))).append(",")
                                    .append("\"requiredSkills\":").append(quote(rs.getString("required_skills"))).append(",")
                                    .append("\"teamSize\":").append(rs.getInt("team_size"))
                                    .append("}");
                        }
                    }

                    json.append("]");
                    send(ex, 200, json.toString());

                } else if (method.equals("POST")) {

                    Map<String, String> data = readForm(ex);

                    String title = data.get("title");
                    String skills = data.get("requiredSkills");
                    int size;

                    try {
                        size = Integer.parseInt(data.get("teamSize"));
                    } catch (Exception e) {
                        send(ex, 400,
                                "{\"error\":\"Team size must be a number\"}");
                        return;
                    }

                    if (empty(title) || empty(skills) ||
                            size < 2 || size > 20) {
                        send(ex, 400,
                                "{\"error\":\"Provide title, skills and team size from 2 to 20\"}");
                        return;
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO projects(title,required_skills,team_size) VALUES(?,?,?)",
                            Statement.RETURN_GENERATED_KEYS)) {

                        ps.setString(1, title.trim());
                        ps.setString(2, skills.trim());
                        ps.setInt(3, size);
                        ps.executeUpdate();

                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            keys.next();

                            send(ex, 201,
                                    "{\"message\":\"Project created\",\"id\":"
                                            + keys.getInt(1) + "}");
                        }
                    }

                } else {
                    send(ex, 405, "{\"error\":\"Method not allowed\"}");
                }

            } catch (Exception e) {
                e.printStackTrace();
                send(ex, 500,
                        "{\"error\":\"Project operation failed\"}");
            }
        });

        server.setExecutor(null);
        server.start();

        System.out.println("TeamForge backend running at http://localhost:8080");
        System.out.println("Health:   http://localhost:8080/api/health");
        System.out.println("Students: http://localhost:8080/api/students");
        System.out.println("Projects: http://localhost:8080/api/projects");
    }

    // Generate a team and save the selected members in MySQL.
    static void generateTeam(HttpExchange ex) throws IOException {

        int projectId = extractProjectId(ex);

        if (projectId <= 0) {
            send(ex, 400, "{\"error\":\"Invalid project ID\"}");
            return;
        }

        try (Connection con = db()) {
            con.setAutoCommit(false);

            try {
                String title;
                String requiredSkills;
                int teamSize;

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT title,required_skills,team_size FROM projects WHERE id=?")) {

                    ps.setInt(1, projectId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            send(ex, 404,
                                    "{\"error\":\"Project not found\"}");
                            return;
                        }

                        title = rs.getString("title");
                        requiredSkills = rs.getString("required_skills");
                        teamSize = rs.getInt("team_size");
                    }
                }

                Set<String> required = new LinkedHashSet<>();

                for (String skill : requiredSkills.split(",")) {
                    if (!skill.trim().isEmpty()) {
                        required.add(skill.trim().toLowerCase(Locale.ROOT));
                    }
                }

                if (required.isEmpty()) {
                    send(ex, 400,
                            "{\"error\":\"No required skills configured\"}");
                    return;
                }

                class Candidate {
                    int id;
                    String name;
                    String email;
                    String skills;
                    int score;

                    Candidate(int id, String name, String email,
                              String skills, int score) {
                        this.id = id;
                        this.name = name;
                        this.email = email;
                        this.skills = skills;
                        this.score = score;
                    }
                }

                List<Candidate> candidates = new ArrayList<>();

                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT id,name,email,skills FROM students")) {

                    while (rs.next()) {
                        String skills = rs.getString("skills");
                        Set<String> studentSkills = new HashSet<>();

                        for (String skill : skills.split(",")) {
                            if (!skill.trim().isEmpty()) {
                                studentSkills.add(
                                        skill.trim().toLowerCase(Locale.ROOT));
                            }
                        }

                        int score = 0;

                        for (String skill : required) {
                            if (studentSkills.contains(skill)) {
                                score++;
                            }
                        }

                        if (score > 0) {
                            candidates.add(new Candidate(
                                    rs.getInt("id"),
                                    rs.getString("name"),
                                    rs.getString("email"),
                                    skills,
                                    score
                            ));
                        }
                    }
                }

                candidates.sort((a, b) -> {
                    int result = Integer.compare(b.score, a.score);
                    return result != 0
                            ? result : Integer.compare(a.id, b.id);
                });

                int count = Math.min(teamSize, candidates.size());

                if (count == 0) {
                    send(ex, 400,
                            "{\"error\":\"No students match the project's required skills\"}");
                    return;
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM teams WHERE project_id=?")) {
                    ps.setInt(1, projectId);
                    ps.executeUpdate();
                }

                List<Candidate> selected =
                        new ArrayList<>(candidates.subList(0, count));

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO teams(project_id,student_id) VALUES(?,?)")) {

                    for (Candidate c : selected) {
                        ps.setInt(1, projectId);
                        ps.setInt(2, c.id);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

                con.commit();

                StringBuilder json = new StringBuilder();

                json.append("{")
                        .append("\"projectId\":").append(projectId).append(",")
                        .append("\"projectTitle\":").append(quote(title)).append(",")
                        .append("\"requestedTeamSize\":").append(teamSize).append(",")
                        .append("\"actualTeamSize\":").append(count).append(",")
                        .append("\"members\":[");

                for (int i = 0; i < selected.size(); i++) {
                    Candidate c = selected.get(i);

                    if (i > 0) json.append(",");

                    json.append("{")
                            .append("\"id\":").append(c.id).append(",")
                            .append("\"name\":").append(quote(c.name)).append(",")
                            .append("\"email\":").append(quote(c.email)).append(",")
                            .append("\"skills\":").append(quote(c.skills)).append(",")
                            .append("\"matchedSkills\":").append(c.score).append(",")
                            .append("\"matchPercentage\":")
                            .append(Math.round(
                                    100.0 * c.score / required.size()))
                            .append("}");
                }

                json.append("],")
                        .append("\"message\":")
                        .append(quote(count < teamSize
                                ? "Partial team generated: not enough matching students."
                                : "Team generated successfully."))
                        .append("}");

                send(ex, 200, json.toString());

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500,
                    "{\"error\":\"Team generation failed. Check backend terminal.\"}");
        }
    }

    // Retrieve saved team members for one project.
    static void getProjectTeam(HttpExchange ex) throws IOException {

        int projectId = extractProjectId(ex);

        if (projectId <= 0) {
            send(ex, 400, "{\"error\":\"Invalid project ID\"}");
            return;
        }

        try (Connection con = db()) {

            String sql =
                    "SELECT p.id AS project_id, p.title, p.team_size, " +
                    "s.id AS student_id, s.name, s.email, s.skills " +
                    "FROM projects p " +
                    "LEFT JOIN teams t ON p.id = t.project_id " +
                    "LEFT JOIN students s ON t.student_id = s.id " +
                    "WHERE p.id = ? ORDER BY s.id";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, projectId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        send(ex, 404,
                                "{\"error\":\"Project not found\"}");
                        return;
                    }

                    String title = rs.getString("title");
                    int teamSize = rs.getInt("team_size");
                    StringBuilder json = new StringBuilder();

                    json.append("{")
                            .append("\"projectId\":").append(projectId).append(",")
                            .append("\"projectTitle\":").append(quote(title)).append(",")
                            .append("\"requestedTeamSize\":").append(teamSize).append(",")
                            .append("\"members\":[");

                    boolean first = true;

                    do {
                        int studentId = rs.getInt("student_id");

                        if (rs.wasNull()) {
                            continue;
                        }

                        if (!first) json.append(",");
                        first = false;

                        json.append("{")
                                .append("\"id\":").append(studentId).append(",")
                                .append("\"name\":").append(quote(rs.getString("name"))).append(",")
                                .append("\"email\":").append(quote(rs.getString("email"))).append(",")
                                .append("\"skills\":").append(quote(rs.getString("skills")))
                                .append("}");
                    } while (rs.next());

                    json.append("]}");
                    send(ex, 200, json.toString());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500,
                    "{\"error\":\"Could not retrieve project team\"}");
        }
    }

    static int extractProjectId(HttpExchange ex) {
        try {
            String[] parts = ex.getRequestURI().getPath().split("/");
            return Integer.parseInt(parts[3]);
        } catch (Exception e) {
            return -1;
        }
    }

    static Connection db() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    static boolean handleOptions(HttpExchange ex) throws IOException {
        cors(ex);

        if (ex.getRequestMethod().equals("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return true;
        }

        return false;
    }

    static void cors(HttpExchange ex) {
        ex.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set(
                "Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type");
        ex.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");
    }

    static void send(HttpExchange ex, int status, String body)
            throws IOException {

        cors(ex);

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    static Map<String, String> readForm(HttpExchange ex)
            throws IOException {

        String body = new String(
                ex.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);

        Map<String, String> data = new HashMap<>();

        for (String part : body.split("&")) {
            String[] pair = part.split("=", 2);

            String key = URLDecoder.decode(
                    pair[0], StandardCharsets.UTF_8);

            String value = pair.length > 1
                    ? URLDecoder.decode(
                            pair[1], StandardCharsets.UTF_8)
                    : "";

            data.put(key, value);
        }

        return data;
    }

    static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    static String quote(String value) {
        if (value == null) return "null";

        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
