import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CrawlerUtil {
	public static final String CSRF_TOKEN_PATTERN = "csrftoken=(\\w+).*";
	public static final String SESSION_ID_PATTERN = "sessionid=(\\w+).*";
	public static final String URL_PATH_PATTERN = "<a href=\"(.+?)\">";
	public static final String REDIRECT_LOCATION_PATTERN = "Location: (.*)";
	public static final String SECRET_FLAG_PATTERN = "<h2 class='secret_flag' style=\"color:red\">FLAG: (.*?)</h2>";
	public static final String ERROR_LOGIN_PATTERN = "class=\"(errorlist)\"";
	public static String regexSingleHelper(String pattern, String response) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(response);
		
		while (m.find()) {
			return m.group(1);
		}
		
		return null;
	}

	public static Set<String> regexMultipleHelper(String pattern, String response) {
		
		Set<String> result = new HashSet<String>();
		
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(response);
		
		while (m.find()) {
			result.add(m.group(1));
		}
		
		return result;
	}

	public static Set<String> extractLinks(String rawPage, String page) {
		int index = 0;
		Set<String> links = new HashSet<String>();
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
