package org.towerhawk.monitor.check.type.network;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.app.App;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.monitor.check.threshold.Threshold;
import org.towerhawk.serde.resolver.CheckType;
import org.towerhawk.spring.config.Configuration;

import java.io.OutputStream;
import java.net.Socket;

@Slf4j
@Getter
@Setter
@CheckType("portResponse")
public class PortResponseCheck extends PortCheck {

	private String send = null;
	private String outputCharset = "UTF8";
	private Threshold responseThreshold;

	@Override
	protected void extension(CheckRun.Builder builder, RunContext context) {
		try {
			Socket socket = (Socket) context.get(SOCKET);
			socket.setSoTimeout(getMsRemaining(true));
			OutputStream os = socket.getOutputStream();
			os.write(send.getBytes(outputCharset));
			os.flush();
			String result = transformInputStream(socket.getInputStream());
			responseThreshold.evaluate(builder, result);
		} catch (Exception e) {
			builder.critical().error(new RuntimeException("Exception caught when trying to send or read from socket", e));
			log.warn("Exception caught on check {} when trying to send or read from socket", getFullName(), e);
		}
	}

	@Override
	public void init(Check check, Configuration configuration, App app, String id) {
		super.init(check, configuration, app, id);
		if (send == null || send.isEmpty()) {
			throw new IllegalStateException("send must not be empty");
		}
	}
}
