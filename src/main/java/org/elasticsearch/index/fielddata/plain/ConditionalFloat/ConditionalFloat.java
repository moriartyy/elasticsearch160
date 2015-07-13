package org.elasticsearch.index.fielddata.plain.ConditionalFloat;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class ConditionalFloat {
	
	private Map<Integer, Float> values = new HashMap<>();

	public boolean isEmpty() {
		return values.size() == 0;
	}

	public void add(int id, float v) {
		values.put(id, v);
	}

	public float value(BitSet flags) {

		for (Integer key : values.keySet()) {
			if (flags.get(key)) {
				return values.get(key);
			}
		}
		return values.get(0);
	}

}
