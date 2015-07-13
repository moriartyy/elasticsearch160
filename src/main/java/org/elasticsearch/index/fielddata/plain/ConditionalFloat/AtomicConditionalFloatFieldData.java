package org.elasticsearch.index.fielddata.plain.ConditionalFloat;

import org.elasticsearch.index.fielddata.AtomicFieldData;

public interface AtomicConditionalFloatFieldData extends AtomicFieldData {

    MultiConditionalFloatValues getConditionalFloatValues();
}
