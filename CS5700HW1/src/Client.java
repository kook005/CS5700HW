import java.io.IOException;

public class Client {
	public static final String COURSE = "cs5700fall2014";
	public static String NUID = "001989426";

	public enum Message {
		HELLO, STATUS, SOLUTION, BYE
	}

	public static void main(String[] args) throws IOException {
		String hostName = null;
		String portNum = null;
		boolean ssl = false;
		
		
		// validate input
		try {
			if (args.length < 2 || args.length > 5) {
				throw new RuntimeException("invalid input");
			} else if (args.length == 2) {
				hostName = args[0];
				NUID = args[1];
			} else if (args.length == 3) {
				if (!args[0].equals("-s")) {
					throw new RuntimeException("invalid input");
				} else {
					ssl = true;
					hostName = args[1];
					NUID = args[2];
				}
			} else if (args.length == 4) {
				if (!args[0].equals("-p")) {
					throw new RuntimeException("invalid input");
				} else {
					portNum = args[1];
					hostName = args[2];
					NUID = args[3];
				}
			} else if (args.length == 5) {
				if (!args[0].equals("-p") || !args[2].equals("-s")) {
					throw new RuntimeException("invalid input");
				} else {
					portNum = args[1];
					ssl = true;
					hostName = args[3];
					NUID = args[4];
				}
			}
		} catch (Exception e) {
			System.err.println("Invalid input format. Check your input format!");
			System.err.println("client <-p port> <-s> [hostname] [NEU ID]");
			System.exit(1);
		}

		// start the communication with server
		SocketWrapper socketWrapper = null;
		try {
			socketWrapper = new SocketWrapper(hostName, portNum, ssl);
			socketWrapper.sendMessage(createHelloMessage());
			String[] response = socketWrapper.getResponse();

			
			//validate the information received
			if (!validate(response, socketWrapper)) {
				terminate(socketWrapper);
			}

			String responseStatus = response[1];

			while (responseStatus.equals(Message.STATUS.toString())) {
				int solution = calculateExpression(response[2], response[4],
						response[3]);
				socketWrapper.sendMessage(createSolutionMessage(solution));
				response = socketWrapper.getResponse();

				if (!validate(response, socketWrapper)) {
					terminate(socketWrapper);
				}

				responseStatus = response[1];
			}

			System.out.println(response[1]);
		
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			if (socketWrapper != null)
				socketWrapper.closeSocket();
		}
	}

	
	/** validate the received message is the correct format, otherwise terminate the client
	 * @param message
	 * @param socketWrapper
	 * @return
	 */
	private static boolean validate(String[] response,
			SocketWrapper socketWrapper) {

		int len = response.length;
		if (len != 5 && len != 3) {
			return false;
		}

		if (!response[0].equals(COURSE)) {
			return false;
		}

		// validate status message
		if (len == 5) {
			if (!response[1].equals(Message.STATUS.toString())) {
				return false;
			}

			if (!isValidInteger(response[2]) || !isValidInteger(response[4])) {
				return false;
			}

			String operator = response[3];

			if (!(operator.equals("+") || operator.equals("-")
					|| operator.equals("*") || operator.equals("/"))) {
				return false;
			}

			return true;
		}

		// validate bye message
		if (len == 3) {
			if (response[1].length() != 64) {
				return false;
			}

			if (!response[2].equals(Message.BYE.toString())) {
				return false;
			}

			return true;
			
		}

		return true;
	}
	
	/**
	 * if s is an integer between 0 and 1000
	 * @param s
	 * @return
	 */
	private static boolean isValidInteger(String s) {

		int num = 0;

		try {
			num = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		}
		// only got here if we didn't return false
		return num <= 1000 ? true : false;
	}

	
	/**
	 * function to terminate the client, make sure socket is closed
	 * @param socketWrapper
	 */
	private static void terminate(SocketWrapper socketWrapper) {
		System.err.println("Not valid resposne");
		if (socketWrapper != null) {
			socketWrapper.closeSocket();
		}
		System.exit(1);
	}

	/**
	 * helper function to create hello message
	 * @return
	 */
	private static String createHelloMessage() {
		return String.format("%s %s %s\n", COURSE, Message.HELLO.toString(),
				NUID);
	}

	/**
	 * helper function to create solution message
	 * @param solution
	 * @return
	 */
	private static String createSolutionMessage(int solution) {
		return String.format("%s %d\n", COURSE, solution);
	}

	
	/**
	 * helper function to calculate the result
	 * @param number1
	 * @param number2
	 * @param op
	 * @return
	 */
	private static int calculateExpression(String number1, String number2,
			String op) {
		int num1 = Integer.parseInt(number1);
		int num2 = Integer.parseInt(number2);
		int result;
		switch (op.charAt(0)) {
		case '+':
			result = num1 + num2;
			break;
		case '-':
			result = num1 - num2;
			break;
		case '*':
			result = num1 * num2;
			break;
		case '/':
			result = num1 / num2;
			break;
		default:
			throw new RuntimeException("Operator Error!");
		}
		return result;
	}

}
