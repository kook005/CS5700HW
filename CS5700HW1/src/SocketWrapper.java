import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class SocketWrapper {
	private Socket socket;
	private BufferedWriter socketWriter;
	private BufferedReader socketReader;
	private String hostName = "cs5700f14.ccs.neu.edu";
	private int port = 27993;
	private int sslPort = 27994;
	private String trustStorePath = "public.jks";
	private char[] trustStorePassword = "kook005".toCharArray();

	public SocketWrapper(String hostName, String portNum, boolean ssl) {
		if (hostName != null)
			this.setHostName(hostName);
		if (ssl) {
			if (portNum != null)
				this.setSslPort(Integer.parseInt(portNum));
			createSSLSocket();
		} else {
			if (portNum != null)
				this.setPort(Integer.parseInt(portNum));
			createSocket();
		}
	}

	private void createSSLSocket() {
		KeyStore tks;
		try {
			tks = KeyStore.getInstance(KeyStore.getDefaultType());
			tks.load(new FileInputStream(trustStorePath), trustStorePassword);
			
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(tks);
			
			SSLContext ctx = SSLContext.getInstance("SSL");
			ctx.init(null, tmf.getTrustManagers(), null);
			
			socket = ctx.getSocketFactory().createSocket(getHostName(), getSslPort());
			socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public void createSocket() {
		try {
			socket = new Socket(getHostName(), getPort());
			socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public void closeSocket() {
		if (this.socket != null && !socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("closing connection failed!");
			}
		}
	}

	public void sendMessage(String message) throws IOException {
		getSocketWriter().write(message);
		getSocketWriter().flush();
	}

	public String readMessage() throws IOException {
		return getSocketReader().readLine();
	}

	public String[] getResponse() throws IOException {
		String message = readMessage();
		return message.split("\\s+");
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public BufferedWriter getSocketWriter() {
		return socketWriter;
	}

	public void setSocketWriter(BufferedWriter socketWriter) {
		this.socketWriter = socketWriter;
	}

	public BufferedReader getSocketReader() {
		return socketReader;
	}

	public void setSocketReader(BufferedReader socketReader) {
		this.socketReader = socketReader;
	}
	
	public int getSslPort() {
		return sslPort;
	}

	public void setSslPort(int sslPort) {
		this.sslPort = sslPort;
	}

}
