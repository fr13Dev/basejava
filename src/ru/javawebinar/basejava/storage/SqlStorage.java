package ru.javawebinar.basejava.storage;

import ru.javawebinar.basejava.exception.NotExistStorageException;
import ru.javawebinar.basejava.model.*;
import ru.javawebinar.basejava.sql.SqlHelper;
import ru.javawebinar.basejava.util.JsonParser;

import java.sql.*;
import java.util.*;

public class SqlStorage implements Storage {
    private final SqlHelper sqlHelper;

    public SqlStorage(String dbUrl, String dbUser, String dbPwd) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        sqlHelper = new SqlHelper(() -> DriverManager.getConnection(dbUrl, dbUser, dbPwd));
    }

    @Override
    public Resume get(String uuid) {
        return sqlHelper.execute(
                "SELECT r.uuid, r.full_name, COALESCE(c.type, '') AS type, c.value, 0 AS splitter\n" +
                        "FROM resume AS r\n" +
                        "   LEFT JOIN contact c ON r.uuid = c.resume_uuid\n" +
                        "WHERE r.uuid = ?\n" +
                        "   UNION ALL\n" +
                        "SELECT r.uuid, r.full_name, COALESCE(s.type, '') AS type, s.value, 1\n" +
                        "FROM resume AS r\n" +
                        "   LEFT JOIN section s ON r.uuid = s.resume_uuid\n" +
                        "WHERE r.uuid = ?",
                ps -> {
                    ps.setString(1, uuid);
                    ps.setString(2, uuid);
                    final ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        throw new NotExistStorageException(uuid);
                    }

                    Resume resume = new Resume(uuid, rs.getString("full_name"));
                    final int isSection = 1;
                    do {
                        if (rs.getInt("splitter") != isSection) {
                            addContact(rs, resume);
                        } else {
                            addSection(rs, resume);
                        }
                    } while (rs.next());
                    return resume;
                });
    }

    @Override
    public void update(Resume resume) {
        sqlHelper.executeTransaction(cn -> {
            try (final PreparedStatement ps = cn.prepareStatement("UPDATE resume SET full_name = ? WHERE uuid = ?")) {
                ps.setString(1, resume.getFullName());
                ps.setString(2, resume.getUuid());
                final int updatedRowCount = ps.executeUpdate();
                if (updatedRowCount == 0) {
                    throw new NotExistStorageException(resume.getUuid());
                }
            }
            deleteChildRecords(cn, resume.getUuid(), "DELETE FROM contact WHERE resume_uuid = ?");
            saveContacts(cn, resume);
            deleteChildRecords(cn, resume.getUuid(), "DELETE FROM section WHERE resume_uuid = ?");
            saveSections(cn, resume);
            return null;
        });
    }

    @Override
    public void save(Resume resume) {
        sqlHelper.executeTransaction(cn -> {
                    try (final PreparedStatement ps = cn.prepareStatement("INSERT INTO resume (uuid, full_name) VALUES (?,?)")) {
                        ps.setString(1, resume.getUuid());
                        ps.setString(2, resume.getFullName());
                        ps.execute();
                    }
                    saveContacts(cn, resume);
                    saveSections(cn, resume);
                    return null;
                }
        );
    }

    @Override
    public void delete(String uuid) {
        sqlHelper.execute(
                "DELETE FROM resume WHERE uuid = ?",
                ps -> {
                    ps.setString(1, uuid);
                    final int updatedRowCount = ps.executeUpdate();
                    if (updatedRowCount == 0) {
                        throw new NotExistStorageException(uuid);
                    }
                    return updatedRowCount;
                }
        );
    }

    @Override
    public int size() {
        return sqlHelper.execute(
                "SELECT COUNT(uuid) FROM resume",
                ps -> {
                    final ResultSet resultSet = ps.executeQuery();
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    } else {
                        return 0;
                    }
                }
        );
    }

    @Override
    public void clear() {
        sqlHelper.execute("DELETE FROM resume", PreparedStatement::execute);
    }

    @Override
    public List<Resume> getAllSorted() {
        return sqlHelper.executeTransaction(cn -> {
            Map<String, Resume> resumes = new LinkedHashMap<>();
            try (final PreparedStatement ps = cn.prepareStatement("SELECT * FROM resume ORDER BY full_name, uuid")) {
                final ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    final String uuid = rs.getString("uuid").trim();
                    Resume resume = new Resume(uuid, rs.getString("full_name"));
                    resumes.put(uuid, resume);
                }
            }
            try (final PreparedStatement ps = cn.prepareStatement(
                    "SELECT resume_uuid, type, value\n" +
                            "FROM contact")) {
                final ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    final Resume resume = resumes.get(rs.getString("resume_uuid").trim());
                    addContact(rs, resume);
                }
            }
            try (final PreparedStatement ps = cn.prepareStatement(
                    "SELECT resume_uuid, type, value\n" +
                            "FROM section")) {
                final ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    final Resume resume = resumes.get(rs.getString("resume_uuid").trim());
                    addSection(rs, resume);
                }
            }
            return new ArrayList<>(resumes.values());
        });
    }

    private void saveContacts(Connection cn, Resume resume) throws SQLException {
        Objects.requireNonNull(resume);
        try (PreparedStatement ps = cn.prepareStatement("INSERT INTO contact (resume_uuid, type, value) VALUES (?,?,?)")) {
            for (Map.Entry<ContactType, String> e : resume.getContacts().entrySet()) {
                ps.setString(1, resume.getUuid());
                ps.setString(2, e.getKey().name());
                ps.setString(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void addContact(ResultSet rs, Resume r) throws SQLException {
        final String ct = rs.getString("type");
        if (!ct.isEmpty()) {
            r.addContact(
                    ContactType.valueOf(ct),
                    rs.getString("value"));
        }
    }

    private void saveSections(Connection cn, Resume r) throws SQLException {
        try (final PreparedStatement ps = cn.prepareStatement("INSERT INTO section (resume_uuid, type, value) VALUES (?,?,?)")) {
            for (Map.Entry<SectionType, Section> e : r.getSections().entrySet()) {
                ps.setString(1, r.getUuid());
                final SectionType type = e.getKey();
                ps.setString(2, type.name());
                String result = JsonParser.write(e.getValue(), Section.class);
                ps.setString(3, result);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void addSection(ResultSet rs, Resume r) throws SQLException {
        final String ct = rs.getString("type");
        if (!ct.isEmpty()) {
            final SectionType type = SectionType.valueOf(ct);
            final Section section = JsonParser.read(rs.getString("value"), Section.class);
            r.addSection(type, section);
        }
    }

    private void deleteChildRecords(Connection cn, String uuid, String query) throws SQLException {
        try (final PreparedStatement ps = cn.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        }
    }
}
