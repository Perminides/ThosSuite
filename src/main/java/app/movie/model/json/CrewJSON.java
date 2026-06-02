package app.movie.model.json;

import java.util.ArrayList;

public class CrewJSON{
    public Boolean adult;
    public Integer gender;
    public Integer id;
    public String known_for_department;
    public String name;
    public String original_name;
    public Double popularity;
    public String profile_path;
    private String credit_id;
    public String department;
    private String job;
    public ArrayList<JobJSON> jobs = new ArrayList<JobJSON>();
    
	public String getCredit_id() {
		return credit_id;
	}
	public void setCredit_id(String credit_id) {
		this.credit_id = credit_id;
		if (jobs.isEmpty())
			jobs.add(new JobJSON());
		this.jobs.get(0).credit_id = credit_id;
	}
	public String getJob() {
		return job;
	}
	public void setJob(String job) {
		this.job = job;
		if (jobs.isEmpty())
			jobs.add(new JobJSON());
		this.jobs.get(0).job = job;
	}
}

