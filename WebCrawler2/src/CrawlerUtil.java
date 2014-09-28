import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CrawlerUtil {

	public static String regexSingleHelper(String pattern, String response) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(response);
		
		while (m.find()) {
			return m.group(1);
		}
		
		return null;
	}

	public static Set<String> regexMultipleHelper(String pattern, String response) {
		
		Set<String> result = new HashSet<>();
		
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(response);
		
		while (m.find()) {
			result.add(m.group(1));
		}
		
		return result;
	}

	public static String getPathFromUrl(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String path = uri.getRawPath();
		if (path == null || path.length() == 0) {
			path = "/";
		}
		return path;
	}
	
	public static Set<String> extractLinks(String rawPage, String page) {
		int index = 0;
		Set<String> links = new HashSet<>();
		while ((index = page.indexOf("<a ", index)) != -1) {
			if ((index = page.indexOf("href", index)) == -1)
				break;
			if ((index = page.indexOf("=", index)) == -1)
				break;
			String remaining = rawPage.substring(++index);
			StringTokenizer st = new StringTokenizer(remaining, "\t\n\r\"'>#");
			String strLink = st.nextToken();
			if (!links.contains(strLink))
				links.add(strLink);
		}
		return links;
	}
}
