import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class WebCrawler {

	private static final String LOGIN_URL = "http://cs5700f14.ccs.neu.edu/accounts/login/?next=/fakebook/";
	private static final String LOGIN_PATH = "/accounts/login/";
	private static final String ROOT_PATH = "/";
	private String host;
	private int port;
	private String username;
	private String passWord;
	private String csrfToken;
	private String authSessionid;
	private String sessionId;

	private LinkedList<String> pathQueue;
	private Set<String> pathVisited;
	private Set<String> secretFlags;

	public WebCrawler(String username, String passwd) {
		this.username = username;
		this.passWord = passwd;
		this.port = 80;
	}

	/***
	 * Initial WebCrawler to set host, port and rootpath
	 * 
	 */
	void init() {
		try {
			URI uri = new URI(LOGIN_URL);
			host = uri.getHost();
			pathQueue = new LinkedList<String>();
			pathVisited = new HashSet<String>();
			secretFlags = new HashSet<String>();
		} catch (URISyntaxException e) {
			System.err.println("invalid login url");
			System.exit(1);
		}
	}

	public void process() {

		// step1. use the rootUrl to get the authsessionId and csrftoken;
		auth();

		// step2. use the username and password to get the visit sessionId
		login();

		// step2. process the url in queue until queue is empty || 5 secret
		// flags are found
		pathQueue.add(ROOT_PATH);
		pathVisited.add(ROOT_PATH);

		processPath();

		// step3. print the flags
		printFlags();
	}

	private void printFlags() {
		for (String flag : secretFlags) {
			System.out.println(flag);
		}
	}

	private boolean shouldTerminate() {
		return pathQueue.isEmpty() || secretFlags.size() == 5;
	}

	private void auth() {
		CrawlerSocket authSocket = new CrawlerSocket(host, port);
		authSocket.sendHttpGetRequest(LOGIN_PATH, null, null);
		String authResponse = authSocket.getHttpResponse();
		this.csrfToken = CrawlerUtil.regexSingleHelper(
				CrawlerUtil.CSRF_TOKEN_PATTERN, authResponse);
		this.authSessionid = CrawlerUtil.regexSingleHelper(
				CrawlerUtil.SESSION_ID_PATTERN, authResponse);
		authSocket.close();
	}

	private void login() {
		CrawlerSocket loginSocket = new CrawlerSocket(host, port);

		try {
			String content = "username=" + urlEncode(username) + "&"
					+ "password=" + urlEncode(passWord)
					+ "&csrfmiddlewaretoken=" + urlEncode(csrfToken) + "&next="
					+ urlEncode("/fakebook/");

			loginSocket.sendHttpPostRequest(LOGIN_PATH, csrfToken,
					authSessionid, content);

			String loginResponse = loginSocket.getHttpResponse();

			String error = CrawlerUtil.regexSingleHelper(CrawlerUtil.ERROR_LOGIN_PATTERN, loginResponse);
			if("errorlist".equals(error)) {
				System.err.println("invalid username and password!");
				System.exit(1);
			}
				
			this.sessionId = CrawlerUtil.regexSingleHelper(
					CrawlerUtil.SESSION_ID_PATTERN, loginResponse);

		} catch (UnsupportedEncodingException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} finally {
			loginSocket.close();
		}
	}

	public void processPath() {

		String path = pathQueue.pollFirst();
		CrawlerSocket crawlerSocket = new CrawlerSocket(host, port);
		BufferedReader bf = crawlerSocket.getReader();
		crawlerSocket.sendHttpGetRequest(path, this.csrfToken, this.sessionId);
		
		try {
			while (true) {
				String line = bf.readLine();

				// enter the response header
				if (line.contains("HTTP/1.1")) {
					String status = null;
					String encodeMode = null;
					String connection = null;
					String location = null;

					if (line.contains("HTTP/1.1 500")) {
						pathQueue.addFirst(path);
						status = "500";
					} else if (line.contains("HTTP/1.1 404")) {
						status = "404";
					} else if (line.contains("HTTP/1.1 403")) {
						status = "403";
					} else if (line.contains("HTTP/1.1 301")) {
						status = "301";
					} else if (line.contains("HTTP/1.1 200")) {
						status = "200";
					}

					int length = -1;

					// read the head info until a /r/n
					while (!"".equals(line)) {
						line = bf.readLine();

						if (line.contains("Content-Length")) {
							String lenStr = line.split(":\\s")[1];
							length = Integer.parseInt(lenStr);
						} else if (line.contains("Transfer-Encoding")) {
							encodeMode = line.split(":\\s")[1];
						} else if (line.contains("Connection")) {
							connection = line.split(":\\s")[1];
						} else if (status.equals("301") && line.contains("Location:")) {
							location = line.split(" ")[1];
							addPath(location);
						}
					}
					
					//enter response body
					if ("chunked".equals(encodeMode)) {
						// this is chunked response
						line = bf.readLine();
						int size = 0;
						StringBuilder sb = new StringBuilder();

						// read all chuncks from the body
						while (!"0".equals(line)) {
							// read size of this chuck
							String sizeStr = line;
							size = Integer.parseInt(sizeStr, 16);
							char[] buffer = new char[size];
							readContent(bf, size, buffer);
							String body = new String(buffer);
							sb.append(body);
							line = bf.readLine();
							while ("".equals(line)) {
								line = bf.readLine();
							}
						}
						String contentBody = sb.toString();
						analyzeResponse(contentBody, path);

					} else if (length != -1) {
						// this is normal response
						// get body and analyze
						char[] buffer = new char[length];

						readContent(bf, length, buffer);

						String body = new String(buffer);

						if ("200".equals(status)) {
							analyzeResponse(body, path);
						}

						// if it should be terminated
						if (shouldTerminate()) {
							break;
						}
					}

					if ("close".equals(connection)) {
						crawlerSocket.close();
						crawlerSocket = new CrawlerSocket(host, port);
						bf = crawlerSocket.getReader();
					}
					path = pathQueue.pollFirst();
					crawlerSocket.sendHttpGetRequest(path, csrfToken, sessionId);
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	private void readContent(BufferedReader bf, int size, char[] buffer)
			throws IOException {
		int remainingLen = size;
		int curPostion = 0;
		int readSize = remainingLen < 500 ? remainingLen : 500;

		while (remainingLen > 0) {
			int readLen = bf.read(buffer, curPostion, readSize);
			remainingLen -= readLen;
			curPostion += readLen;

			if (remainingLen < readSize) {
				readSize = remainingLen;
			}
		}
	}

	void analyzeResponse(String response, String currentPath)
			throws URISyntaxException {

		Set<String> paths = CrawlerUtil.extractLinks(response, response);

		addPath(paths);

		Set<String> flags = CrawlerUtil.regexMultipleHelper(
				CrawlerUtil.SECRET_FLAG_PATTERN, response);
		for (String s : flags) {
			secretFlags.add(s);
		}
	}

	private void addPath(Set<String> paths) {
		for (String path : paths) {
			if (!path.startsWith("/") || pathVisited.contains(path)
					|| ".".equals(path)) {
				continue;
			}
			pathQueue.add(path);
			pathVisited.add(path);
		}
	}

	private void addPath(String path) {
		URI uri = null;
		try {
			uri = new URI(path);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if (!uri.getHost().equals(host)) 
			return;
		path = uri.getRawPath();
		if(path == null || pathVisited.contains(path))
			return;
		pathQueue.add(path);
		pathVisited.add(path);
	}

	private static String urlEncode(String content)
			throws UnsupportedEncodingException {
		return URLEncoder.encode(content, "UTF-8");
	}
}