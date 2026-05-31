package app.fitbit.model.json;

import java.time.LocalDate;

public class ActivityDaySummary extends Parent {

	private Summary summary;
	private LocalDate date;

	
	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public Summary getSummary() {
		return summary;
	}

	public void setSummary(Summary summary) {
		this.summary = summary;
	}
	
	
}
