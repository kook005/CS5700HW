import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class WebCrawler {

	public String host;
	public int port;

	public String username;
	public String passWord;

	public String csrfToken;
	public String authSessionid;
	public String sessionId;

	public String loginUrl = "http://cs5700f14.ccs.neu.edu/accounts/login/?next=/fakebook/";
	public String homePath = "/fakebook/";
	public String rootPath = "/";

	public LinkedList<String> pathQueue;
	public Set<String> pathVisited;
	public Set<String> secretFlags;

	public static final String CSRF_TOKEN_PATTERN = "csrftoken=(\\w+).*";
	public static final String SESSION_ID_PATTERN = "sessionid=(\\w+).*";
	public static final String URL_PATH_PATTERN = "<a href=\"(.+?)\">";
	public static final String REDIRECT_LOCATION_PATTERN = "Location: (.*)";
	public static final String SECRET_FLAG_PATTERN = "<h2 class='secret_flag' style=\"color:red\">(.*?)</h2>";

	public WebCrawler(String username, String passwd) {
		this.username = username;
		this.passWord = passwd;
	}

	/***
	 * Initial WebCrawler to set host, port and rootpath
	 * 
	 * @throws URISyntaxException
	 */
	void init() throws URISyntaxException {
		URI uri = new URI(loginUrl);
		host = uri.getHost();
		homePath = uri.getRawPath();
		if (homePath == null || homePath.length() == 0) {
			homePath = "/";
		}
		String protocol = uri.getScheme();
		port = uri.getPort();
		if (port == -1) {
			if (protocol.equals("http")) {
				port = 80;
			} else if (protocol.equals("https")) {
				port = 443;
			}
		}

		homePath = uri.getRawPath();

		pathQueue = new LinkedList<>();
		pathVisited = new HashSet<>();
		secretFlags = new HashSet<>();

	}

	public void process() {

		// step1. use the rootUrl to get the authsessionId and csrftoken;
		System.out.println("start auth");
		auth();

		// step2. use the username and password to get the visit sessionId
		System.out.println("start login");
		login();

		// step2. process the url in queue until queue is empty || 5 secret
		// flags are found

		System.out.println("start process");
		pathQueue.add(rootPath);
		pathVisited.add(rootPath);

		// while (!shouldTerminate()) {
		//
		// String currentPath = pathQueue.pollFirst();
		// processPath(currentPath);
		// }

		processPath2();
		
		
		
		// step3. print the flags
		printFlags();
		printMsg("finish", "finish");
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

		CrawlerSocket authSocket = new CrawlerSocket(host, port, homePath);

		try {
			authSocket.init();
			authSocket.sendHttpGetRequest(null, null);
			String authResponse = authSocket.getHttpResponse();

			this.csrfToken = CrawlerUtil.regexSingleHelper(CSRF_TOKEN_PATTERN,
					authResponse);
			this.authSessionid = CrawlerUtil.regexSingleHelper(
					SESSION_ID_PATTERN, authResponse);
			// System.out.println(this.csrftoken);
			// System.out.println(this.authSessionid);

			// printMsg("auth", authResponse);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			authSocket.close();
		}
	}

	private void login() {
		CrawlerSocket loginSocket = new CrawlerSocket(host, port, homePath);

		try {
			loginSocket.init();
			String loginContent = "username=" + urlEncode(username) + "&"
					+ "password=" + urlEncode(passWord)
					+ "&csrfmiddlewaretoken=" + urlEncode(csrfToken) + "&next="
					+ urlEncode("/fakebook/");

			loginSocket.sendHttpPostRequest(csrfToken, authSessionid,
					loginContent);

			String loginResponse = loginSocket.getHttpResponse();

			// System.out.println("loginResponse" + loginResponse);

			this.sessionId = CrawlerUtil.regexSingleHelper(SESSION_ID_PATTERN,
					loginResponse);

			// printMsg("login", loginResponse);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			loginSocket.close();
		}
	}

	void processPath(String path) {

		System.out.println("--------------");
		System.out.println("Get " + path);
		CrawlerSocket crawlerSocket = new CrawlerSocket(host, port, path);

		try {

			long t = System.currentTimeMillis();
			crawlerSocket.init();
			long t1 = System.currentTimeMillis();

			crawlerSocket.sendHttpGetRequest(csrfToken, sessionId);
			String response = crawlerSocket.getHttpResponse();

			long t2 = System.currentTimeMillis();

			System.out.println("response:" + response.substring(0, 16));
			analyzeResponse(response, path);
			long t3 = System.currentTimeMillis();

			System.out.println("Time to create socket " + (t1 - t));
			System.out.println("Time to get response " + (t2 - t1));
			System.out.println("Time to analyze " + (t3 - t2));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			crawlerSocket.close();
		}

	}

	public String buildMessage(String path) {
		StringBuilder sb = new StringBuilder();
		sb.append("GET " + path + " HTTP/1.1\r\n");
		sb.append("HOST: " + host + "\r\n");

		if (sessionId != null) {
			sb.append("Cookie:csrftoken=" + csrfToken + "; sessionid="
					+ sessionId + "\r\n");
		}

		sb.append("Connection: keep-alive\r\n\r\n");
		return sb.toString();
	}

	void processPath2() {

		String path = pathQueue.pollFirst();
		Socket crawlerSocket = null;
		BufferedReader bf = null;
		PrintWriter wr = null;
		try {
			crawlerSocket = new Socket(host, port);
			bf = new BufferedReader(new InputStreamReader(
					crawlerSocket.getInputStream()));
			wr = new PrintWriter(crawlerSocket.getOutputStream());

			String requestMsg = buildMessage(path);
			System.out.println("--------------");
			System.out.println("Get " + path);

			wr.write(requestMsg);
			wr.flush();

			String line = null;

			while (true) {

				line = readAndPrint(bf);

				// enter the request body
				if (line.contains("HTTP/1.1")) {

					String status = null;
					String encodeMode = null;
					String connection = null;
					// if 500, internal error, continuein
					if (line.contains("HTTP/1.1 500")) {
						pathQueue.addFirst(path);
						System.out.println("");
						status = "500";
					}

					if (line.contains("HTTP/1.1 404")) {
						status = "404";
					}

					if (line.contains("HTTP/1.1 200")) {
						status = "200";
					}

					int length = 0;

					// read the head info until a /r/n
					while (!"".equals(line)) {
						line = readAndPrint(bf);

						if (line.contains("Content-Length")) {
							String lenStr = line.split(":\\s")[1];
							length = Integer.parseInt(lenStr);
						}

						if (line.contains("Transfer-Encoding")) {
							encodeMode = line.split(":\\s")[1];
						}

						if (line.contains("Connection")) {
							connection = line.split(":\\s")[1];
						}

					}

					// this is chunked response
					if ("chunked".equals(encodeMode)) {

						line = readAndPrint(bf);
						int size = 0;
						StringBuilder sb = new StringBuilder();

						// read all chuncks from the body
						while (!"0".equals(line)) {
							// read size of this chuck
							String sizeStr = line;

							size = Integer.parseInt(sizeStr, 16);
							char[] buffer = new char[size];

							int remainingLen = size;
							int idx = 0;
							int readSize = remainingLen < 500 ? remainingLen
									: 500;

							while (remainingLen > 0) {
								int readLen = bf.read(buffer, idx, readSize);
								remainingLen -= readLen;
								idx += readLen;

								if (remainingLen < readSize) {
									readSize = remainingLen;
								}
							}

							String body = new String(buffer);
							sb.append(body);

							// System.out.println(body);

							line = readAndPrint(bf);

							while ("".equals(line)) {
								line = readAndPrint(bf);
							}
						}

						String contentBody = sb.toString();
						analyzeResponse(contentBody, path);
						path = sendNextPath(wr);

					}

					// this is normal response
					if (length != 0) {

						// System.out.println("length used ------------" +
						// length);

						// get body and analyze
						char[] buffer = new char[length];

						// prevent the length is too big
						int remainingLen = length;
						int idx = 0;
						int readSize = remainingLen < 500 ? remainingLen : 500;

						while (remainingLen > 0) {
							int readLen = bf.read(buffer, idx, readSize);
							remainingLen -= readLen;
							idx += readLen;

							if (remainingLen < readSize) {
								readSize = remainingLen;
							}
						}

						String body = new String(buffer);
						// System.out.println(body);

						if ("200".equals(status)) {
							analyzeResponse(body, path);
						}

						// if it should be terminated
						if (shouldTerminate()) {
							System.out.println("terminated");
							break;
						}

						// send next path
						path = sendNextPath(wr);
					}

					if ("close".equals(connection)) {
						bf.close();
						wr.close();

						crawlerSocket.close();
						crawlerSocket = new Socket(host, port);

						bf = new BufferedReader(new InputStreamReader(
								crawlerSocket.getInputStream()));
						wr = new PrintWriter(crawlerSocket.getOutputStream());

						sendNextPath(wr);
					}

					if ("404".equals(status)) {
						sendNextPath(wr);
					}

				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			wr.close();
			try {
				bf.close();
				crawlerSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private String readAndPrint(BufferedReader bf) throws IOException {
		String line;
		line = bf.readLine();
//		System.out.println(line);
		return line;
	}

	private String sendNextPath(PrintWriter wr) {
		String path;
		String requestMsg;
		path = pathQueue.pollFirst();
		requestMsg = buildMessage(path);
		System.out.println("--------------");
		System.out.println("Get" + path);
		wr.write(requestMsg);
		wr.flush();
		return path;
	}

	private void printMsg(String msgHead, String msgContent) {
		System.out.println();
		System.out.println(msgHead + "----------------------");
		System.out.println(msgContent);
		System.out.println("\n---------------------------");
	}

	void analyzeResponse(String response, String currentPath)
			throws URISyntaxException {

		Set<String> paths = CrawlerUtil.extractLinks(response, response);

		for (String p : paths) {
			if (validate(p)) {
//				System.out.println("current path:" + currentPath
//						+ "------------------->" + p);
			}
		}

		Set<String> flags = CrawlerUtil.regexMultipleHelper(
				SECRET_FLAG_PATTERN, response);
		for (String s : flags) {
			secretFlags.add(s);
			System.out.println("Oh, yeah! find one flag : " + s);
		}
	}

	boolean validate(String path) {
		if (!path.startsWith("/") || pathVisited.contains(path)
				|| ".".equals(path)) {
			return false;
		}

		pathQueue.add(path);
		pathVisited.add(path);

		return true;

	}

	private static String urlEncode(String content)
			throws UnsupportedEncodingException {
		return URLEncoder.encode(content, "UTF-8");
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getCsrftoken() {
		return csrfToken;
	}

	public void setCsrftoken(String csrftoken) {
		this.csrfToken = csrftoken;
	}

	public String getAuthSessionid() {
		return authSessionid;
	}

	public void setAuthSessionid(String authSessionid) {
		this.authSessionid = authSessionid;
	}

	public String getSessionid() {
		return sessionId;
	}

	public void setSessionid(String sessionid) {
		this.sessionId = sessionid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswd() {
		return passWord;
	}

	public void setPasswd(String passwd) {
		this.passWord = passwd;
	}

	public String getLoginUrl() {
		return loginUrl;
	}

	public void setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
	}

	public String getHomePath() {
		return homePath;
	}

	public void setHomePath(String homePath) {
		this.homePath = homePath;
	}
}