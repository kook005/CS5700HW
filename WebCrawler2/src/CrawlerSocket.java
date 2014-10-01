import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.xml.ws.Response;

public class CrawlerSocket {
	private String host;
	private int port;
	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	private static final int BUFFER_SIZE = 65536;

	public CrawlerSocket(String host, int port) {
		this.host = host;
		this.port = port;
		connect();
	}

	private void connect() {
		try {
			socket = new Socket(host, port);
			writer = new PrintWriter(socket.getOutputStream());
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("unknown host");
			System.exit(1);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public void sendHttpGetRequest(String path, String csrfToken,
			String sessionId) {
		StringBuilder sb = new StringBuilder();
		sb.append("GET " + path + " HTTP/1.1\r\n");
		sb.append("HOST: " + host + "\r\n");

		if (csrfToken != null && sessionId != null) {
			sb.append("Cookie:csrftoken=" + csrfToken + "; sessionid="
					+ sessionId + "\r\n");
		}

		sb.append("Connection: keep-alive\r\n\r\n");
		writer.print(sb.toString());
		writer.flush();
	}

	public void sendHttpPostRequest(String path, String csrfToken,
			String sessionId, String content) {
		StringBuilder sb = new StringBuilder();

		sb.append("POST  " + path + " HTTP/1.1\r\n");
		sb.append("HOST: " + host + "\r\n");
		sb.append("Cookie:csrftoken=" + csrfToken + "; sessionid=" + sessionId
				+ "\r\n");
		sb.append("Connection: keep-alive\r\n");
		sb.append("Content-Type: application/x-www-form-urlencoded\r\n");
		sb.append("Content-Length:" + content.length() + "\r\n\r\n");

		sb.append(content);
		writer.print(sb.toString());
		writer.flush();
	}

	public String getHttpResponse() {
		String response = null;
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			int toalRead = 0;
			int read;
			while ((read = socket.getInputStream().read(buffer, toalRead, 100)) != -1) {
				toalRead += read;
			}
			response = new String(buffer);

		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}

		return response;
	}

	public void close() {
		try {
			writer.close();
			reader.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BufferedReader getReader() {
		return reader;
	}
}
