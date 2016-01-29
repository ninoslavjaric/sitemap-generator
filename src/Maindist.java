
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public class Maindist {
	private static HashSet<String> visitedLocations = new HashSet<>(), 
			supportedLinks = new HashSet<>();
	private static HashMap<String, Integer> locationOcurrence = new HashMap<>();
	private static int remainingLinks = 0;
	private static URL baseUrl = null;

	public static void main(String[] args) {
//		System.setProperty("file.encoding", "UTF-8");
		URL url = initUrl(args);
		scan(url, 0);
		filterVisitors();
		System.out.println("-----------------------------");
		Iterator<String> visitees = supportedLinks.iterator();
		while(visitees.hasNext()){
			String visiteeUrl = visitees.next();
			System.out.println(locationOcurrence.get(visiteeUrl)+"\t"+visiteeUrl);
		}
		System.out.println(locationOcurrence);
	}
	
	private static void filterVisitors(){
		Iterator<String> it = visitedLocations.iterator();
			String string, contentType=null;
			URL checker = null;
			HttpURLConnection huc;
		while (it.hasNext()) {
			string = (String) it.next();
			System.out.println("Checking : \""+string+"\"");
			try {
				checker = new URL(string);
				huc = (HttpURLConnection) checker.openConnection();
				huc.setRequestMethod("GET");
				contentType = huc.getHeaderField("Content-type");
				if(contentType.contains("text/html") && contentType != null)
					supportedLinks.add(string);
			} catch (IOException e) {
				Logger.getLogger("content-type").warning("Ne valja nesto");
				e.printStackTrace();
			}
		}
	}

	private static URL initUrl(String[] params){
		String link = params.length>0?params[0]:"http://bitlab.rs";
		URL url = null;
		try {
			url = new URL(link);
			baseUrl = baseUrl==null ? url : baseUrl;
		} catch(MalformedURLException e){
			e.printStackTrace();
		}
		return url;
	}
	
	private static URL initUrl(String param){
		URL url = null;
		try {
			url = new URL(param);
		} catch(MalformedURLException e){
			e.printStackTrace();
			System.out.println("\t\t\t"+param);
		}
		return url;
	}
	
	private static Element getBody(URL resource){
		Element body = null;
		try {
			body = Jsoup.parse(resource,10_000).select("body").first();
		}catch (HttpStatusException e) {
//			e.printStackTrace();
		}catch (UnsupportedMimeTypeException e) {
//			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return body;
	}
	
	private static boolean updateLocations(URL url){
		if (locationOcurrence.containsKey(url.toString()))
			locationOcurrence.put(url.toString(), locationOcurrence.get(url.toString())+1);
		else
			locationOcurrence.put(url.toString(), 1);
		if (visitedLocations.contains(url.toString()))
			return false;
		else
			visitedLocations.add(url.toString());
		return true;
	}

	private static boolean isInternal(String string){
		URL url;
		try {
			url = new URL(string);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		return url.getHost().equals(baseUrl.getHost());
	}
	
	private static void scan(URL base, int level){
		level++;
		if(!updateLocations(base) || level >4)
			return;
		Element body = getBody(base);
		if(body==null)
			return;
		System.out.println("Listing links at : \t"+base);
		System.out.println("Remaining links :\t"+remainingLinks);
		Elements bodyChildren = body.select("a");
		remainingLinks+=bodyChildren.size();
		Iterator<Element> bc = bodyChildren.iterator();
		while (bc.hasNext()) {
			--remainingLinks;
			Element element = (Element) bc.next();		
			if(element.hasAttr("href") && isInternal(element.absUrl("href"))){		
			String href = element.absUrl("href");
			URL hrefUrl = initUrl(href);
			if(!hrefUrl.toString().contains("#")){	
//				System.out.println(base+"\t"+hrefUrl);
//				System.out.println("Remaining links :\t"+remainingLinks);
				scan(initUrl(href), level);
			}
			}
		}
	}
}
