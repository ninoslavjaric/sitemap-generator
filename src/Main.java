
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;
import org.json.simple.parser.ParseException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public class Main {
	private static HashSet<String> visitedLocations = new HashSet<>(), 
			supportedLinks = new HashSet<>();
	private static ArrayList<SiteMapItem> smis = new ArrayList<>();
	private static HashMap<String, Integer> locationOcurrence = new HashMap<>();
	private static int remainingLinks = 0, levelDepth = 2;
	private static URL baseUrl = null, sitemapTarget = null;

	public static void main(String[] args) throws IOException, ParseException {
//		getLaraToken();
//		System.out.println(Arrays.toString(args));
//		System.exit(0);
		URL url = initUrl(args);
		int occMax = 0;
		scan(url, 0);
		filterVisitors();
		System.out.println("-----------------------------");
		Iterator<String> visitees = supportedLinks.iterator();
		
		while(visitees.hasNext()){
			String visiteeUrl = visitees.next();
			int occurence = locationOcurrence.get(visiteeUrl);
			occMax=occMax<occurence?occurence:occMax;
			System.out.println(occurence+"\t"+visiteeUrl);
			smis.add(new SiteMapItem(new URL(visiteeUrl), occurence));
		}
		Iterator<SiteMapItem> smi = smis.iterator();
		while (smi.hasNext()) {
			SiteMapItem siteMapItem = (SiteMapItem) smi.next();
			siteMapItem.setPriority(occMax);
		}
		Collections.sort(smis, new Comparator<SiteMapItem>(){
			@Override
			public int compare(SiteMapItem a1, SiteMapItem a2) {
				return a2.occurence-a1.occurence;
			}
		});
		int responseCode = pushSitemapToTarget(xmlOutput(smis));
		System.out.println("Response code from server : "+responseCode);
	}
	
	private static String xmlOutput(ArrayList<SiteMapItem> al){
		Iterator<SiteMapItem> it = al.iterator();
		SiteMapItem siteMapItem;
		String buffer = "";
		while (it.hasNext()) {
			siteMapItem = (SiteMapItem) it.next();
			buffer+="<url>\n";
			buffer+="\t<loc>"+siteMapItem.location+"</loc>\n";
			buffer+="\t<priority>"+siteMapItem.priority+"</priority>\n";
			buffer+="</url>\n";
		}
		buffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<urlset "
				+ "xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
				+ "xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd\">\n"
				+ buffer
				+ "</urlset>";
		return buffer;
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
		String link = params.length>=1?params[0]:"http://www.plemenito.com";
		String updateLink = params.length>=2?params[1]:"http://www.plemenito.com/sitemap-update";
		URL url = null, updateUrl = null;
		try {
			url = new URL(link);
			updateUrl = new URL(updateLink);
			baseUrl = baseUrl==null ? url : baseUrl;
			sitemapTarget = sitemapTarget==null ? updateUrl : sitemapTarget;
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
//			e.printStackTrace();
			System.out.println("Malformed URL exception occurred!!");
			return false;
		}
		return url.getHost().equals(baseUrl.getHost());
	}
	
	private static void scan(URL base, int level){
		level++;
		if(!updateLocations(base) || level > levelDepth)
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
	
	private static int pushSitemapToTarget(String xml) throws IOException, ParseException{
		HttpURLConnection huc = (HttpURLConnection) sitemapTarget.openConnection();
		
		huc.setRequestProperty("Content-type", "text/xml");
//		huc.setRequestProperty("X-XSRF-TOKEN", getLaraToken());
		huc.setDoOutput(true);
		huc.setRequestMethod("POST");
		DataOutputStream dos = new DataOutputStream(huc.getOutputStream());
		System.out.println(xml);
		dos.writeChars(xml);
		dos.flush();
		dos.close();
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(huc.getInputStream()));
		String bfr;
		while((bfr=in.readLine())!=null)
			System.out.println(bfr);
		return huc.getResponseCode();
	}
//	private static String getLaraToken() throws IOException, ParseException{
//		HttpURLConnection huc = (HttpURLConnection) sitemapTarget.openConnection();
//		huc.setRequestProperty("Magic", "daj token znam da imas");
//		huc.setRequestMethod("GET");
//		
//		BufferedReader in = new BufferedReader(
//		        new InputStreamReader(huc.getInputStream()));
//		String bfr="", holder;
//		while((holder=in.readLine())!=null)
//			bfr+=holder;
//		JSONParser jp = new JSONParser();
//		JSONObject o = (JSONObject) jp.parse(bfr);
//		System.out.println(o.get("token"));
//		return (String) o.get("token");
//	}
}
