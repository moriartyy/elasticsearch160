package org.elasticsearch.index.fielddata.plain.ConditionalFloat;

import org.apache.lucene.util.Bits;

public class SingletonConditionalFloatValues extends MultiConditionalFloatValues {
	
	private ConditionalFloatValues in;
	private Bits docsWithField;
    private ConditionalFloat value;
    private int count;

	SingletonConditionalFloatValues(ConditionalFloatValues in, Bits docsWithField) {
        this.in = in;
        this.docsWithField = docsWithField;
	}

	@Override
	public void setDocument(int docId) {
        value = in.get(docId);
        if (value.isEmpty() && docsWithField != null && !docsWithField.get(docId)) {
            count = 0;
        } else {
            count = 1;
        }
	}

	@Override
	public int count() {
		return count;
	}

	@Override
	public ConditionalFloat valueAt(int index) {
        assert index == 0;
        return value;
	}
	
	public ConditionalFloatValues getConditionalFloatValues() {
		return in;
	}
	
    public Bits getDocsWithField() {
        return docsWithField;
    }

}
