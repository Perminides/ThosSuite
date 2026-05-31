package app.fitbit.model.json;

import java.util.List;

public class ActivityLogList extends Parent{

	private List<Activity> activities;

	public List<Activity> getActivities() {
		return activities;
	}

	public void setActivities(List<Activity> activities) {
		this.activities = activities;
	}
	
	
}
