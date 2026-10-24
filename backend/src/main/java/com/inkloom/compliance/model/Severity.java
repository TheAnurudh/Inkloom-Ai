package com.inkloom.compliance.model;

/** How bad a broken rule is: the points it costs and the flag it raises. */
public enum Severity {
    CRITICAL(30, Flag.RED),
    MAJOR(15, Flag.ORANGE),
    MINOR(5, Flag.ORANGE);

    private final int penalty;
    private final Flag flag;

    Severity(int penalty, Flag flag) { this.penalty = penalty; this.flag = flag; }

    public int penalty() { return penalty; }
    public Flag flag() { return flag; }
}
