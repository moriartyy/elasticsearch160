package org.elasticsearch.index.fielddata.plain.ConditionalFloat;

public abstract class MultiConditionalFloatValues {

    protected MultiConditionalFloatValues() {
    }

    public abstract void setDocument(int docId);

    public abstract int count();

    public abstract ConditionalFloat valueAt(int i);
}
