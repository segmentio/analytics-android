package io.segment.android.stats;

import java.util.concurrent.ConcurrentHashMap;

public class Statistics extends ConcurrentHashMap<String, Statistic> {

	private static final long serialVersionUID = -8837006750327885446L;

	public Statistic ensure(String key) {
		if (this.containsKey(key)) return this.get(key);
		
		Statistic statistic = new Statistic();
		this.put(key, statistic);
		return statistic;
	}
	
	public void update(String operation, double val) {
		if (!this.containsKey(operation)) {
			this.putIfAbsent(operation, new Statistic());
		}

		this.get(operation).update(val);
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		builder.append("\n-------- Safe Client Statistics --------\n");

		for (Entry<String, Statistic> entry : entrySet()) {

			String operation = entry.getKey();
			Statistic statistic = entry.getValue();

			builder.append(String.format("%s : %s\n", operation, statistic.toString()));
		}

		builder.append("----------------------------------------\n");

		return builder.toString();
	}


}