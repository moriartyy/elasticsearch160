package org.elasticsearch.index.fielddata.plain.ConditionalFloat;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.util.Bits;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.index.fielddata.SortedBinaryDocValues;
import org.elasticsearch.index.fielddata.SortingBinaryDocValues;

public abstract class AbstractAtomicConditionalFloatFieldData implements AtomicConditionalFloatFieldData {

    @Override
    public final SortedBinaryDocValues getBytesValues() {
    	final MultiConditionalFloatValues cfValues = getConditionalFloatValues();
        return new SortingBinaryDocValues() {

            final List<CharSequence> list = new ArrayList<>();

            @Override
            public void setDocument(int docID) {
                list.clear();
                cfValues.setDocument(docID);
                for (int i = 0, count = cfValues.count(); i < count; ++i) {
                    list.add(cfValues.valueAt(i).toString());
                }
                count = list.size();
                grow();
                for (int i = 0; i < count; ++i) {
                    final CharSequence s = list.get(i);
                    values[i].copyChars(s);
                }
                sort();
            }

        };
    }

    @Override
    public final ScriptDocValues.GeoPoints getScriptValues() {
        throw new UnsupportedOperationException();
    }

	public static AtomicConditionalFloatFieldData empty(final int maxDoc) {
		return new AbstractAtomicConditionalFloatFieldData() {
			
			@Override
			public void close() {
				
			}
			
			@Override
			public long ramBytesUsed() {
				return 0;
			}
			
			
			@Override
			public MultiConditionalFloatValues getConditionalFloatValues() {
				return new SingletonConditionalFloatValues(emptyConditionFloat(), new Bits.MatchNoBits(maxDoc));
			}
		};
	}
	
	static ConditionalFloatValues emptyConditionFloat() {
		final ConditionalFloat conditionalFloat = new ConditionalFloat();
		return new ConditionalFloatValues() {
			
			@Override
			public ConditionalFloat get(int docID) {
				return conditionalFloat;
			}
		};
	}

}
