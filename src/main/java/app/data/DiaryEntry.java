package app.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DiaryEntry(
	    LocalDateTime createdAt,
	    LocalDate entryDate,
	    String text,
	    List<String> tags,
	    List<String> attachmentPaths
	) {}