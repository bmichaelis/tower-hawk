package org.towerhawk.check.type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.towerhawk.check.AbstractCheck;
import org.towerhawk.check.Check;
import org.towerhawk.check.run.CheckRun;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;

public class ShellCheck extends AbstractCheck {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private String cmd = null;
	private String[] cmdList = null;
	private Map<String, String> env = null;
	private String workDir = null;
	private String[] envArray;
	private File dir;
	private boolean unknownIsCritical = true;

	@Override
	protected void doRun(CheckRun.Builder builder) {
		builder.unknownIsCritical(unknownIsCritical);
		Process process = null;
		try {
			if (cmdList == null) {
				log.info("Running ShellCheck with cmd = '{}'", cmd);
				process = Runtime.getRuntime().exec(cmd, envArray, dir);
			} else {
				log.info("Running ShellCheck with cmdList = {}", (Object) cmdList);
				process = Runtime.getRuntime().exec(cmdList, envArray, dir);
			}
			process.waitFor();
			switch (process.exitValue()) {
				case 0:
					builder.succeeded();
					break;
				case 1:
					builder.warning();
					break;
				case 2:
					builder.critical();
					break;
				default:
					builder.unknown();
			}
			String output = transformInputStream(process.getInputStream());
			String errorOutput = transformInputStream(process.getErrorStream());
			if (!output.isEmpty()) {
				builder.addContext("stdout", output);
			}
			if (!errorOutput.isEmpty()) {
				builder.addContext("stderr", errorOutput);
			}
		} catch (Exception e) {
			builder.critical();
			if (process != null) {
				process.destroy();
				if (process.isAlive()) {
					process.destroyForcibly();
				}
			}
		}

	}

	@Override
	public void init(Check check) {
		super.init(check);
		if (env != null) {
			envArray = env.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
		}
		if (workDir != null) {
			dir = Paths.get(workDir).toFile();
		}
	}

	public String getCmd() {
		return cmd;
	}

	private void setCmd(String cmd) {
		this.cmd = cmd;
	}

	private Map<String, String> getEnv() {
		return env;
	}

	private void setEnv(Map<String, String> env) {
		this.env = env;
	}

	private String[] getCmdList() {
		return cmdList;
	}

	private void setCmdList(String[] cmdList) {
		this.cmdList = cmdList;
	}

	private String getWorkDir() {
		return workDir;
	}

	private void setWorkDir(String workDir) {
		this.workDir = workDir;
	}

	private File getDir() {
		return dir;
	}

	private void setDir(File dir) {
		this.dir = dir;
	}

	private boolean isUnknownIsCritical() {
		return unknownIsCritical;
	}

	private void setUnknownIsCritical(boolean unknownIsCritical) {
		this.unknownIsCritical = unknownIsCritical;
	}
}