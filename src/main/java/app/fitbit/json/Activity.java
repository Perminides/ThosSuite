package app.fitbit.json;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import app.fitbit.FitbitImporter;
import app.util.Log;

public class Activity extends Parent{
	
	private String activityName;
	private Double distance;
	private String distanceUnit;
	private String startTime;
	private String originalStartTime;
	private int steps;
	
	// Default-Konstruktor für Jackson
	public Activity() {
	}
	
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
		Log.debug(this, "setStartTime aufgerufen mit: " + startTime);
		this.startTime = startTime;
	}
	public String getOriginalStartTime() {
		return originalStartTime;
	}
	public void setOriginalStartTime(String originalStartTime) {
		this.originalStartTime = originalStartTime;
	}
	
	@JsonIgnore
	public LocalDate getDate() {
	    // Sicherheitscheck: Felder müssen gesetzt sein
	    if (startTime == null || originalStartTime == null) {
	        Log.error(this, "getDate() aufgerufen, aber startTime oder originalStartTime ist null für Activity: " + activityName);
	        return null;
	    }
	    
	    // Validierung: startTime und originalStartTime sollten gleich sein
	    if (!startTime.equals(originalStartTime)) {
	        Log.error(this, "Hurra, ich habe eine Aktivität gefunden wo StartTime <> OrigStartTime ist. " +
	                        "Schau Dir mal " + activityName + " am " + startTime + " an");
	        throw new RuntimeException("StartTime != OriginalStartTime für Activity: " + activityName);
	    }
	    
	    // Datum extrahieren (Format: "2026-02-01T12:45:00.000+01:00" → "2026-02-01")
	    return LocalDate.parse(startTime.substring(0, 10));
	}
	
}
