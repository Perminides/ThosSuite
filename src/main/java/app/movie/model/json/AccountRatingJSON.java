package app.movie.model.json;

import java.time.LocalDateTime;

public class AccountRatingJSON{
	// Wenn die Sachen im JSON so heißen, sind mir zumindest bei der Variablen oder Methodenbenennung die Hände gebunden...
    private LocalDateTime created_at;
    public Integer value;
    
	public LocalDateTime getCreated_at() {
		return created_at;
	}
	public void setCreated_at(LocalDateTime created_at) {
		this.created_at = created_at;
	}
}