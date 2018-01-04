package org.towerhawk.monitor.check.execution.network;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.Status;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.serde.resolver.CheckExecutorType;
import org.towerhawk.spring.config.Configuration;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@CheckExecutorType("port")
public class PortCheck implements CheckExecutor {

	private int port;
	private String host;
	private int localPort = -1;
	private String localHost;

	private String send = null;
	private String outputCharset = "UTF8";
	private String delimiter = null;

	public Socket getSocket() throws IOException {
		Socket socket = null;
		if (localPort <= 0 || Configuration.DEFAULT_LOCAL_HOST.equals(localHost)) {
			log.info("Running port check on {}:{}", host, port);
			socket = new Socket(host, port);
		} else {
			log.info("Running port check on {}:{} from {}:{}", host, port, localHost, localPort);
			InetAddress address = InetAddress.getByName(localHost);
			socket = new Socket(host, port, address, localPort);
		}
		return socket;
	}

	private void closeSocket(Socket socket) {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			log.error("Unable to close connection to {}:{}", socket.getInetAddress().getHostName(), socket.getPort(), e);
		}
	}

	@Override
	public ExecutionResult execute(CheckRun.Builder builder, RunContext context) throws Exception {
		Socket socket = null;
		try {
			ExecutionResult result = ExecutionResult.startTimer();
			socket = getSocket();
			if (send != null) {
				long msRemaining = builder.getCheck().getMsRemaining(true);
				if (msRemaining > Integer.MAX_VALUE) {
					msRemaining = Integer.MAX_VALUE;
				}
				socket.setSoTimeout((int) msRemaining);
				OutputStream os = socket.getOutputStream();
				os.write(send.getBytes(outputCharset));
				os.flush();
				String input = transformInputStream(socket.getInputStream());
				result.setResult(input);
			}
			result.complete();
			return result;
		} finally {
			closeSocket(socket);
		}
	}

	@Override
	public void init(CheckExecutor checkExecutor, Check check, Configuration configuration) throws Exception {
		if (host == null) {
			host = configuration.getDefaultHost();
		}
		if (localHost == null) {
			localHost = configuration.getDefaultLocalHost();
		}
		if (send == null || send.isEmpty()) {
			throw new IllegalStateException("send must not be empty");
		}
		if (delimiter == null) {
			delimiter = configuration.getLineDelimiter();
		}
	}

	protected String transformInputStream(InputStream inputStream) {
		return new BufferedReader(new InputStreamReader(inputStream))
				.lines().collect(Collectors.joining());
	}

}
