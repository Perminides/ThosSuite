package app.data.persistence.region;

import app.data.LearnStat;
import app.data.RegionSessionSpec;

/**
 * Ein sehr schlanker Wrapper um die db-Source
 */
public class RegionDeckRepository {
	private final DbRegionDeckProgressSource db;
    
    public RegionDeckRepository() {
    	db = new DbRegionDeckProgressSource();
    }
	
	public LearnStat getLearnStat(RegionSessionSpec spec) {
		return db.load(spec);
	}
	
	public void saveRegionSession (RegionSessionSpec spec, LearnStat stats, boolean correct, String wrongId) {
		db.save(spec, stats, correct, wrongId);
	}
}
