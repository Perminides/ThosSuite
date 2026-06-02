package app.movie.model.json;

import java.util.ArrayList;

public class CastJSON{
    public Boolean adult;
    public Integer gender;
    public Integer id;
    public String known_for_department;
    public String name;
    public String original_name;
    public Double popularity;
    public String profile_path;
    public Integer cast_id;
    private String character;
    private String credit_id;
    private ArrayList<RoleJSON> roles = new ArrayList<RoleJSON>();
    public Integer order;
    
	public ArrayList<RoleJSON> getRoles() {
		return roles;
	}
	public void setRoles(ArrayList<RoleJSON> roles) {
		this.roles = roles;
	}
	public String getCharacter() {
		return character;
	}
	public void setCharacter(String character) {
		if (roles.size() == 0)
			roles.add(new RoleJSON());
		roles.get(0).character = character;
		this.character = character;
	}
	public String getCredit_id() {
		return credit_id;
	}
	public void setCredit_id(String credit_id) {
		if (roles.size() == 0)
			roles.add(new RoleJSON());
		roles.get(0).credit_id = credit_id;
		this.credit_id = credit_id;
	}
    
    
    
}

