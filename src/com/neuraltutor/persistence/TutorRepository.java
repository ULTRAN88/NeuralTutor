package com.neuraltutor.persistence;

import com.neuraltutor.model.*;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class TutorRepository {
    private final String url;

    public TutorRepository() {
        this("jdbc:sqlite:neuraltutor_pro.db");
    }

    public TutorRepository(String url) {
        this.url = url;
        initialize();
    }

    private Connection connection() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=5000");
        }
        return c;
    }

    private void initialize() {
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS learner_profiles(
                  name TEXT PRIMARY KEY,
                  goal TEXT NOT NULL,
                  password_hash TEXT NOT NULL DEFAULT '',
                  xp INTEGER NOT NULL DEFAULT 0,
                  streak INTEGER NOT NULL DEFAULT 0,
                  last_active INTEGER NOT NULL DEFAULT 0
                )
            """);
            try {
                s.execute("ALTER TABLE learner_profiles ADD COLUMN password_hash TEXT NOT NULL DEFAULT ''");
            } catch (SQLException ignored) {
                // Existing databases already have the column.
            }
            s.execute("""
                CREATE TABLE IF NOT EXISTS attempts(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  learner TEXT NOT NULL,
                  ts INTEGER NOT NULL,
                  question_id INTEGER NOT NULL,
                  skill TEXT NOT NULL,
                  difficulty INTEGER NOT NULL,
                  correct INTEGER NOT NULL,
                  latency_ms INTEGER NOT NULL,
                  corrections INTEGER NOT NULL,
                  load_score REAL NOT NULL,
                  cognitive_state TEXT NOT NULL,
                  intervention TEXT NOT NULL
                )
            """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS sessions(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  learner TEXT NOT NULL,
                  started_at INTEGER NOT NULL,
                  attempts INTEGER NOT NULL,
                  correct INTEGER NOT NULL
                )
            """);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize NeuralTutor database.", e);
        }
    }

    public Optional<LearnerProfile> loadProfile(String name) {
        String sql = "SELECT goal FROM learner_profiles WHERE name=?";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                LearnerProfile p = new LearnerProfile(name, rs.getString("goal"));
                loadAttemptsInto(p);
                return Optional.of(p);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean register(String name, String password, String goal) {
        String sql = """
            INSERT INTO learner_profiles(name, goal, password_hash, xp, streak, last_active)
            VALUES(?,?,?,?,?,?)
        """;
        try (Connection c = connection()) {
            try (PreparedStatement existing = c.prepareStatement(
                    "SELECT password_hash FROM learner_profiles WHERE name=?")) {
                existing.setString(1, name);
                try (ResultSet rs = existing.executeQuery()) {
                    if (rs.next()) {
                        if (!rs.getString("password_hash").isBlank()) return false;
                        try (PreparedStatement upgrade = c.prepareStatement(
                                "UPDATE learner_profiles SET goal=?, password_hash=? WHERE name=?")) {
                            upgrade.setString(1, goal);
                            upgrade.setString(2, hashPassword(password));
                            upgrade.setString(3, name);
                            upgrade.executeUpdate();
                            return true;
                        }
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, goal);
            ps.setString(3, hashPassword(password));
            ps.setInt(4, 0);
            ps.setInt(5, 0);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
            return true;
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()) || e.getMessage().contains("UNIQUE constraint failed")) {
                return false;
            }
            throw new IllegalStateException(e);
        }
    }

    public boolean authenticate(String name, String password) {
        String sql = "SELECT password_hash FROM learner_profiles WHERE name=?";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && !rs.getString("password_hash").isBlank()
                        && MessageDigest.isEqual(
                        rs.getString("password_hash").getBytes(StandardCharsets.UTF_8),
                        hashPassword(password).getBytes(StandardCharsets.UTF_8));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private String hashPassword(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Password hashing is unavailable.", e);
        }
    }

    private void loadAttemptsInto(LearnerProfile p) throws SQLException {
        String sql = """
            SELECT ts, question_id, skill, difficulty, correct, latency_ms,
                   corrections, load_score, cognitive_state, intervention
            FROM attempts WHERE learner=? ORDER BY ts
        """;
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attempt a = new Attempt(
                            rs.getLong("ts"),
                            p.getName(),
                            rs.getInt("question_id"),
                            rs.getString("skill"),
                            rs.getInt("difficulty"),
                            rs.getInt("correct") == 1,
                            rs.getLong("latency_ms"),
                            rs.getInt("corrections"),
                            rs.getDouble("load_score"),
                            CognitiveState.valueOf(rs.getString("cognitive_state")),
                            AdaptiveMode.valueOf(rs.getString("intervention")));
                    p.applyAttempt(a);
                }
            }
        }
    }

    public void saveProfile(LearnerProfile p) {
        String sql = """
            INSERT INTO learner_profiles(name, goal, xp, streak, last_active)
            VALUES(?,?,?,?,?)
            ON CONFLICT(name) DO UPDATE SET
              goal=excluded.goal,
              xp=excluded.xp,
              streak=excluded.streak,
              last_active=excluded.last_active
        """;
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getGoal());
            ps.setInt(3, p.getXp());
            ps.setInt(4, p.getStreak());
            ps.setLong(5, p.getLastActiveAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void recordAttempt(Attempt a) {
        String sql = """
            INSERT INTO attempts(
              learner,ts,question_id,skill,difficulty,correct,latency_ms,
              corrections,load_score,cognitive_state,intervention)
            VALUES(?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.learner());
            ps.setLong(2, a.timestamp());
            ps.setInt(3, a.questionId());
            ps.setString(4, a.skill());
            ps.setInt(5, a.difficulty());
            ps.setInt(6, a.correct() ? 1 : 0);
            ps.setLong(7, a.latencyMs());
            ps.setInt(8, a.corrections());
            ps.setDouble(9, a.loadScore());
            ps.setString(10, a.cognitiveState().name());
            ps.setString(11, a.intervention().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void recordSession(String learner, int attempts, int correct) {
        String sql = "INSERT INTO sessions(learner,started_at,attempts,correct) VALUES(?,?,?,?)";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, learner);
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, attempts);
            ps.setInt(4, correct);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Attempt> recentAttempts(String learner, int limit) {
        List<Attempt> list = new ArrayList<>();
        String sql = """
            SELECT ts, question_id, skill, difficulty, correct, latency_ms,
                   corrections, load_score, cognitive_state, intervention
            FROM attempts WHERE learner=? ORDER BY ts DESC LIMIT ?
        """;
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, learner);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Attempt(
                            rs.getLong("ts"), learner, rs.getInt("question_id"),
                            rs.getString("skill"), rs.getInt("difficulty"),
                            rs.getInt("correct") == 1, rs.getLong("latency_ms"),
                            rs.getInt("corrections"), rs.getDouble("load_score"),
                            CognitiveState.valueOf(rs.getString("cognitive_state")),
                            AdaptiveMode.valueOf(rs.getString("intervention"))));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return list;
    }

    public Map<String, Integer> interventionCounts(String learner) {
        Map<String,Integer> out = new LinkedHashMap<>();
        String sql = "SELECT intervention, COUNT(*) c FROM attempts WHERE learner=? GROUP BY intervention ORDER BY c DESC";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, learner);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString("intervention"), rs.getInt("c"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    public void exportAttemptsCsv(String learner, java.nio.file.Path path) throws java.io.IOException {
        List<Attempt> attempts = recentAttempts(learner, Integer.MAX_VALUE);
        try (var w = java.nio.file.Files.newBufferedWriter(path)) {
            w.write("timestamp,learner,question_id,skill,difficulty,correct,latency_ms,corrections,load_score,cognitive_state,intervention\n");
            for (Attempt a : attempts) {
                w.write(String.format(Locale.US,
                        "%d,%s,%d,\"%s\",%d,%s,%d,%d,%.4f,%s,%s%n",
                        a.timestamp(), a.learner(), a.questionId(),
                        a.skill().replace("\"","\"\""), a.difficulty(), a.correct(),
                        a.latencyMs(), a.corrections(), a.loadScore(),
                        a.cognitiveState().name(), a.intervention().name()));
            }
        }
    }
}