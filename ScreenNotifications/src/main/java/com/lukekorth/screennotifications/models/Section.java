package com.lukekorth.screennotifications.models;

public class Section {

    public int startingIndex;
    public String section;

    public Section(int startingIndex, String section) {
        this.startingIndex = startingIndex;
        this.section = section;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Section && this.section.equals(((Section) obj).section));
    }

    public String toString() {
        return section;
    }
}
