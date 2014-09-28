import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;


public class CrawlerSocket {
	private String host;
	private int port;
	private String path;
	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	private String requestMsg;
	private static final int BUFFER_SIZE = 65536;

	
	public CrawlerSocket(String host, int port, String path) {
		this.host = host;
		this.port = port;
		this.path = path;
		
	}
	
	public void init(){
		try {
			socket = new Socket(host, port);
			writer = new PrintWriter(socket.getOutputStream());
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public void close() {
		writer.close();
	
		try {
			reader.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Socket getSocket() {
		return socket;
	}


	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public String getHttpResponse() throws IOException {
		
		long t = System.currentTimeMillis();

//		StringBuilder sb = new StringBuilder();
//		String line = null;
//		
////		while ((line = reader.readLine()) != null) {
////			sb.append(line + "\n");
////		}
		
		byte[] buffer = new byte[BUFFER_SIZE];
		int toalRead = 0;
		int read;
		while ((read = socket.getInputStream().read(buffer, toalRead, 100)) != -1) {
			toalRead += read;
		}
		
		long t1 = System.currentTimeMillis();
		System.out.println("Time to read from sever " + (t1 - t));
		
		String response = new String(buffer);
//		System.out.println(response);
		return response;
//		return sb.toString();
	}
	
	
	public void buildHttpGetMessage(String csrfToken, String sessionId) {
		StringBuilder sb = new StringBuilder();
		sb.append("GET " + path + " HTTP/1.1\r\n");
		sb.append("HOST: " + host + "\r\n");
		
		if (sessionId != null) {
			sb.append("Cookie:csrftoken=" + csrfToken + "; sessionid=" + sessionId + "\r\n");
		}
		
		sb.append("Connection: keep-alive\r\n\r\n");
		requestMsg = sb.toString();
	}

	public void sendHttpGetRequest(String csrfToken, String sessionId) {
		long t = System.currentTimeMillis();
		buildHttpGetMessage(csrfToken, sessionId);
		long t1 = System.currentTimeMillis();
		writer.print(requestMsg);
		writer.flush();
		long t2 = System.currentTimeMillis();
		System.out.println("Time to build request " + (t1 - t));
		System.out.println("Time to write " + (t2 - t1));
//		printMsg();
	}


	private void buildHttpPostMessage(String csrfToken, String sessionId,
			String content) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("POST  " + path + " HTTP/1.1\r\n");
		sb.append("HOST: " + host + "\r\n");
		sb.append("Cookie:csrftoken=" + csrfToken + "; sessionid=" + sessionId + "\r\n");
		sb.append("Connection: keep-alive\r\n");
		sb.append("Content-Type: application/x-www-form-urlencoded\r\n");
		sb.append("Content-Length:" + content.length() + "\r\n\r\n");
		
		sb.append(content);
		requestMsg = sb.toString();
	}
	
	public void sendHttpPostRequest(String csrftoken, String sessionid,
			String content) {
		buildHttpPostMessage(csrftoken, sessionid, content);
//		printMsg();
		writer.print(requestMsg);
		writer.flush();
	}
	
	public String getRuquestMsg(){
		return requestMsg;
	}
	
	public PrintWriter getWriter() {
		return writer;
	}
	
	public void setWriter(PrintWriter writer) {
		this.writer = writer;
	}
	
	public BufferedReader getReader() {
		return reader;
	}
	
	public void setReader(BufferedReader reader) {
		this.reader = reader;
	}
}
