package app.learn.region.repository;

import app.learn.model.LearnStat;
import app.learn.region.model.SessionSpec;

/**
 * Ein sehr schlanker Wrapper um die db-Source
 */
public class RegionDeckRepository {
	private final DbRegionDeckProgressSource db;
    
    public RegionDeckRepository() {
    	db = new DbRegionDeckProgressSource();
    }
	
	public LearnStat getLearnStat(SessionSpec spec) {
		return db.load(spec);
	}
	
	public void saveRegionSession (SessionSpec spec, LearnStat stats, boolean correct, String wrongId) {
		db.save(spec, stats, correct, wrongId);
	}
}
