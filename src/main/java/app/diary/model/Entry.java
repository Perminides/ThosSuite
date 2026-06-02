package app.diary.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record Entry(
	    LocalDateTime createdAt,
	    LocalDate entryDate,
	    String text,
	    List<String> tags,
	    List<String> attachmentPaths
	) {}