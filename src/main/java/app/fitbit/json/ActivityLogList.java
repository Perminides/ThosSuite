package app.fitbit.json;

import java.util.List;

// !Sofort: Ich denke, die wollten wir nciht mehr sondern alles ins Log. Kann weg?
public class ActivityLogList extends Parent{

	private List<Activity> activities;

	public List<Activity> getActivities() {
		return activities;
	}

	public void setActivities(List<Activity> activities) {
		this.activities = activities;
	}
	
	
}
