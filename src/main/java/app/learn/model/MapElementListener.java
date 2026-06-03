package app.learn.model;

public interface MapElementListener {

	/**
	 * 
	 * @param id: Can be null in case of a wrong click on the image map
	 */
	public void mouseClicked(String id);
}
