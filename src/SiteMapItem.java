import java.net.URL;

public class SiteMapItem {
	public URL location;
	public int occurence;
	public float priority;
	public SiteMapItem(URL location, int occurence) {
		this.location = location;
		this.occurence = occurence;
	}
	
	public void setPriority(int sum) {
		this.priority = (float)occurence/sum;
	}

	@Override
	public String toString() {
		return "SiteMapItem [location=" + location + ", occurence=" + occurence
				+ ", priority=" + priority + "]";
	}


	
}
