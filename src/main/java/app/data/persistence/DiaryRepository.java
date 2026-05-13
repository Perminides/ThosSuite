package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import app.data.DiaryEntry;

public class DiaryRepository {

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	public void saveEntry(LocalDateTime createdAt, LocalDate entryDate, String text, List<String> tags) {
		Connection conn = DB.getConnection();
		try (PreparedStatement ps = conn.prepareStatement("INSERT INTO diary_entry (created_at, entry_date, text) VALUES (?, ?, ?)")) {
			ps.setString(1, createdAt.withNano(0).format(TIMESTAMP_FORMAT));
			ps.setString(2, entryDate.format(DATE_FORMAT));
			ps.setString(3, text);
			ps.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("Failed to save diary entry", e);
		}

		try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO diary_tag (name) VALUES (?)")) {
			for (String tag : tags) {
				ps.setString(1, tag);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (Exception e) {
			throw new RuntimeException("Failed to save diary tags", e);
		}

		try (PreparedStatement ps = conn.prepareStatement("INSERT INTO diary_entry_tag (entry_created_at, tag_name) VALUES (?, ?)")) {
			String timestamp = createdAt.withNano(0).format(TIMESTAMP_FORMAT);
			for (String tag : tags) {
				ps.setString(1, timestamp);
				ps.setString(2, tag);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (Exception e) {
			throw new RuntimeException("Failed to save diary entry tags", e);
		}
	}

	public List<String> loadAllTags() {
		List<String> tags = new ArrayList<>();
		try (PreparedStatement ps = DB.getConnection().prepareStatement("select tag_name from diary_entry_tag group by tag_name order by count(*) desc");
			 ResultSet rs = ps.executeQuery()) {
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

	public void updateEntry(LocalDateTime createdAt, LocalDate entryDate, String text, List<String> tags) {
		Connection conn = DB.getConnection();
		try (PreparedStatement ps = conn.prepareStatement("UPDATE diary_entry SET entry_date = ?, text = ? WHERE created_at = ?")) {
			ps.setString(1, entryDate.format(DATE_FORMAT));
			ps.setString(2, text);
			ps.setString(3, createdAt.withNano(0).format(TIMESTAMP_FORMAT));
			ps.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("Failed to update diary entry", e);
		}

		try (PreparedStatement ps = conn.prepareStatement("DELETE FROM diary_entry_tag WHERE entry_created_at = ?")) {
			ps.setString(1, createdAt.withNano(0).format(TIMESTAMP_FORMAT));
			ps.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("Failed to delete diary entry tags", e);
		}

		try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO diary_tag (name) VALUES (?)")) {
			for (String tag : tags) {
				ps.setString(1, tag);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (Exception e) {
			throw new RuntimeException("Failed to save diary tags", e);
		}

		try (PreparedStatement ps = conn.prepareStatement("INSERT INTO diary_entry_tag (entry_created_at, tag_name) VALUES (?, ?)")) {
			String timestamp = createdAt.withNano(0).format(TIMESTAMP_FORMAT);
			for (String tag : tags) {
				ps.setString(1, timestamp);
				ps.setString(2, tag);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (Exception e) {
			throw new RuntimeException("Failed to save diary entry tags", e);
		}
	}

	public List<DiaryEntry> search(String whereFragment, LocalDate from, LocalDate to, int limit) {
		String sql = """
				SELECT de.created_at, de.entry_date, de.text,
				       GROUP_CONCAT(DISTINCT det.tag_name ORDER BY det.tag_name) AS tags,
				       GROUP_CONCAT(DISTINCT dea.path)                           AS attachments
				FROM diary_entry de
				LEFT JOIN diary_entry_tag        det ON det.entry_created_at = de.created_at
				LEFT JOIN diary_entry_attachment  dea ON dea.entry_created_at = de.created_at
				WHERE (%s)
				  AND de.entry_date BETWEEN ? AND ?
				GROUP BY de.created_at
				ORDER BY de.entry_date DESC, de.created_at DESC
				LIMIT ?
				""".formatted(whereFragment);

		List<DiaryEntry> results = new ArrayList<>();

		try (PreparedStatement ps = DB.getConnection().prepareStatement(sql)) {
			ps.setString(1, from.format(DATE_FORMAT));
			ps.setString(2, to.format(DATE_FORMAT));
			ps.setInt(3, limit);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					LocalDateTime createdAt  = LocalDateTime.parse(rs.getString("created_at"), TIMESTAMP_FORMAT);
					LocalDate     entryDate  = LocalDate.parse(rs.getString("entry_date"), DATE_FORMAT);
					String        text       = rs.getString("text");

					String tagsConcatenated  = rs.getString("tags");
					List<String> tags        = tagsConcatenated != null
							? List.of(tagsConcatenated.split(","))
							: List.of();

					String attachmentsConcatenated = rs.getString("attachments");
					List<String> attachments       = attachmentsConcatenated != null
							? List.of(attachmentsConcatenated.split(","))
							: List.of();

					results.add(new DiaryEntry(createdAt, entryDate, text, tags, attachments));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to search diary entries", e);
		}

		return results;
	}

	public void deleteEntry(LocalDateTime createdAt) {
		Connection conn = DB.getConnection();
		try (PreparedStatement ps = conn.prepareStatement("DELETE FROM diary_entry_tag WHERE entry_created_at = ?")) {
			ps.setString(1, createdAt.withNano(0).format(TIMESTAMP_FORMAT));
			ps.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("Failed to delete diary entry tags", e);
		}

		try (PreparedStatement ps = conn.prepareStatement("DELETE FROM diary_entry WHERE created_at = ?")) {
			ps.setString(1, createdAt.withNano(0).format(TIMESTAMP_FORMAT));
			ps.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("Failed to delete diary entry", e);
		}
	}

	public List<String> loadAttachments(LocalDateTime createdAt) {
		List<String> paths = new ArrayList<>();
		try (PreparedStatement ps = DB.getConnection().prepareStatement(
				"SELECT path FROM diary_entry_attachment WHERE entry_created_at = ? ORDER BY path")) {
			ps.setString(1, createdAt.withNano(0).format(TIMESTAMP_FORMAT));
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					paths.add(rs.getString("path"));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to load diary attachments", e);
		}
		return paths;
	}

	public void saveAttachment(LocalDateTime createdAt, String relativePath) {
		try (PreparedStatement ps = DB.getConnection().prepareStatement(
				"INSERT OR IGNORE INTO diary_entry_attachment (entry_created_at, path) VALUES (?, ?)")) {
			ps.setString(1, createdAt.withNano(0).format(TIMESTAMP_FORMAT));
			ps.setString(2, relativePath);
			ps.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("Failed to save diary attachment", e);
		}
	}

	public void deleteAttachment(LocalDateTime createdAt, String relativePath) {
		try (PreparedStatement ps = DB.getConnection().prepareStatement(
				"DELETE FROM diary_entry_attachment WHERE entry_created_at = ? AND path = ?")) {
			ps.setString(1, createdAt.withNano(0).format(TIMESTAMP_FORMAT));
			ps.setString(2, relativePath);
			ps.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("Failed to delete diary attachment", e);
		}
	}

	public boolean isPathReferencedElsewhere(String relativePath) {
		try (PreparedStatement ps = DB.getConnection().prepareStatement(
				"SELECT COUNT(*) FROM all_attachments WHERE path = ?")) {
			ps.setString(1, relativePath);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) > 0;
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to check attachment references", e);
		}
	}
}