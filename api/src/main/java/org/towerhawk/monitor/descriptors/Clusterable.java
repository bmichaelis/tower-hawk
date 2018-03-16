package org.towerhawk.monitor.descriptors;

import java.util.List;

public interface Clusterable {

	boolean isMaster();

	List<String> getNodes();
}
