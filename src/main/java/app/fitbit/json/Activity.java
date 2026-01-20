package app.fitbit.json;

import java.time.LocalDate;

import app.fitbit.FitbitImporter;
import app.util.Log;

public class Activity extends Parent{
	
	private String activityName;
	private Double distance;
	private String distanceUnit;
	private String startTime;
	private String originalStartTime;
	private int steps;
	
	public Activity(String originalStartTime, String activityName, String distanceUnit, Double distance, int steps) {
		this.originalStartTime = originalStartTime;
		this.activityName = activityName;
		this.distanceUnit = distanceUnit;
		this.distance = distance;
		this.steps = steps;
	}

	public int getSteps() {
		return steps;
	}
	public void setSteps(int steps) {
		this.steps = steps;
	}
	public String getActivityName() {
		return activityName;
	}
	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}
	public Double getDistance() {
		return distance;
	}
	public void setDistance(Double distance) {
		this.distance = distance;
	}
	public String getDistanceUnit() {
		return distanceUnit;
	}
	public void setDistanceUnit(String distanceUnit) {
		this.distanceUnit = distanceUnit;
		if (distanceUnit != null && distanceUnit.equals("")) {
			Log.error(this, "Ich hätte als distance Kilometer erwartet und nicht " + distanceUnit);
			try {
				Log.error(this, FitbitImporter.MAPPER.writeValueAsString(this));
			} catch (Exception e) {} 
			throw new RuntimeException("Ich hätte als distance Kilometer erwartet und nicht " + distanceUnit);
		}
	}
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getOriginalStartTime() {
		return originalStartTime;
	}
	public void setOriginalStartTime(String originalStartTime) {
		this.originalStartTime = originalStartTime;
	}
	public LocalDate getDate() {
		if (!startTime.equals(originalStartTime)) {
			Log.error(this, "Hurra, ich habe eine Aktivität gefunden wo StartTime <> OrigStartTime ist. Schau Dir mal " + activityName + " am " + startTime + "an");
			throw new RuntimeException("Hurra, ich habe eine Aktivität gefunden wo StartTime <> OrigStartTime ist. Schau Dir mal " + activityName + " am " + startTime + "an");
		}
		return LocalDate.parse(startTime.substring(0, 10));
	}
	
}
