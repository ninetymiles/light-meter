package com.rex.lightmeter;

public class MainSpinnerItem {

    private Object mValue;
    private String mText;

    public MainSpinnerItem(Object value, String text) {
        mValue = value;
        mText = text;
    }

    public Object getValue() {
        return mValue;
    }

    @Override
    public boolean equals(Object o) {
        return mValue.equals(o);
    }

    @Override
    public String toString() {
        return mText;
    }
}
