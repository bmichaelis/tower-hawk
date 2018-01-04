package org.towerhawk.monitor.check.execution.jmx;

import org.towerhawk.serde.resolver.CheckExecutorType;

@CheckExecutorType("javaheap")
public class JavaHeapCheck extends JmxCheck {

	public JavaHeapCheck() {
		setMbean("java.lang:type=Memory");
		setAttribute("HeapMemoryUsage");
		setPath("used");
		setBasePath("max");
	}
}
