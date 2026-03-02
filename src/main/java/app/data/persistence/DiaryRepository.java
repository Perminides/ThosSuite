package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DiaryRepository {

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	public void saveEntry(LocalDateTime createdAt, LocalDate entryDate, String text, List<String> tags) {
		Connection conn = DB.getConnection();
		try {
			conn.setAutoCommit(false);

			try (PreparedStatement ps = conn.prepareStatement("INSERT INTO diary_entry (created_at, entry_date, text) VALUES (?, ?, ?)")) {
				ps.setString(1, createdAt.withNano(0).format(TIMESTAMP_FORMAT));
				ps.setString(2, entryDate.format(DATE_FORMAT));
				ps.setString(3, text);
				ps.executeUpdate();
			}

			try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO diary_tag (name) VALUES (?)")) {
				for (String tag : tags) {
					ps.setString(1, tag);
					ps.addBatch();
				}
				ps.executeBatch();
			}

			try (PreparedStatement ps = conn.prepareStatement("INSERT INTO diary_entry_tag (entry_created_at, tag_name) VALUES (?, ?)")) {
				String timestamp = createdAt.withNano(0).format(TIMESTAMP_FORMAT);
				for (String tag : tags) {
					ps.setString(1, timestamp);
					ps.setString(2, tag);
					ps.addBatch();
				}
				ps.executeBatch();
			}

			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (Exception ignored) {
			}
			throw new RuntimeException("Failed to save diary entry", e);
		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (Exception ignored) {
			}
		}
	}

	public List<String> loadAllTags() {
		List<String> tags = new ArrayList<>();
		try (PreparedStatement ps = DB.getConnection().prepareStatement("select tag_name from diary_entry_tag group by tag_name order by count(*) desc"); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				tags.add(rs.getString("tag_name"));
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to load diary tags", e);
		}
		return tags;
	}

	public LocalDateTime findLastEntryTimestamp() {
		try (PreparedStatement ps = DB.getConnection().prepareStatement("SELECT created_at FROM diary_entry ORDER BY created_at DESC LIMIT 1");
				ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				return LocalDateTime.parse(rs.getString("created_at"), TIMESTAMP_FORMAT);
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Failed to load last diary entry timestamp", e);
		}
	}
}