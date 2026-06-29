package scripts.learn;

import static java.util.Map.entry;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import app.learn.model.Deck;
import app.learn.region.model.Mode;
import app.shared.Config;
import app.shared.DB;

public class CopyLandkreiseStats {
	
	private static final String statsFile = "C:/Users/Markgraf/OneDrive/Geographie Suite/Spielstand/regionsStats.csv";
	private static final String logFile = "C:/Users/Markgraf/OneDrive/Geographie Suite/Spielstand/played.log";
	private static final Map<String, Deck> lkNameMap = Map.ofEntries(
		    entry("Region Hannover", Deck.HANNOVER_REGION)
		);
	private static final Map<String, Mode> modeMap = Map.ofEntries(
			entry("recognise_circle", Mode.WRITE_REGION),
			//entry("recognise_capital", RegionMode.WRITE_CAPITAL),
			//entry("recognise_both", RegionMode.WRITE_BOTH),
			entry("elimination_circle", Mode.ELIMINATION_REGION),
			//entry("elimination_capital", RegionMode.ELIMINATION_CITY),
			//entry("elimination_both", RegionMode.ELIMINATION_BOTH),
			entry("colour_circle", Mode.CLICK_REGION_COLORED),
			//entry("colour_capital", RegionMode.CLICK_CITY_COLORED),
			entry("no_colour_circle", Mode.CLICK_REGION_BLANK)
			//entry("no_colour_capital", RegionMode.CLICK_CITY_BLANK)
			);
	private static final String insertStatSql = "INSERT INTO region_learn_stat (deck,mode,first_played,last_played,level,wrong_count) VALUES (?, ?, ?, ?, ?, ?)";
	private static final String insertLogSql = "INSERT INTO region_log (played_timestamp,deck,mode,correct_flag,wrong_region_id) VALUES (?, ?, ?, ?, ?)";
			
	public static void main(String[] args) throws Exception {
	    Config.init("C:/Users/Markgraf/OneDrive/ThosSuite/");
	    Connection conn = DB.getConnection();

	    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(statsFile), Charset.forName("Cp1252")));
	         PreparedStatement psStat = conn.prepareStatement(insertStatSql)) {
	        String line;
	        while ((line = br.readLine()) != null) {
	            String[] tokens = line.split(";");
	            Mode mode = modeMap.get(tokens[2]);
	            Deck type = lkNameMap.get(tokens[1]);
	            if (mode == null || type == null)
	                continue;

	            psStat.setString(1, type.getId());
	            psStat.setString(2, mode.name());
	            psStat.setString(3, "2000-01-01");
	            psStat.setString(4, tokens[4]);
	            psStat.setString(5, tokens[3]);
	            psStat.setString(6, "0");
	            psStat.execute();
	            System.out.println("executed");
	        }
	    }

	    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), Charset.forName("UTF-8")));
	         PreparedStatement psLog = conn.prepareStatement(insertLogSql)) {
	        String line;
	        DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
	        while ((line = br.readLine()) != null) {
	            String[] tokens = line.split(";");
	            Mode mode = modeMap.get(tokens[2]);
	            Deck type = lkNameMap.get(tokens[1]);
	            if (mode == null || type == null)
	                continue;

	            LocalDateTime dt = LocalDateTime.parse(tokens[3], FMT);
	            psLog.setString(1, dt.toString());
	            psLog.setString(2, type.getId());
	            psLog.setString(3, mode.name());
	            psLog.setInt(4, tokens[5].equals("0") ? 1 : 0);
	            psLog.setString(5, null);
	            System.out.println(psLog.getParameterMetaData());
	            System.out.println(dt.toString());
	            psLog.execute();
	            System.out.println("executed");
	        }
	    }
	}
}
