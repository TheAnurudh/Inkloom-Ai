package com.inkloom.compliance.service;

import com.inkloom.compliance.model.Finding;
import com.inkloom.compliance.model.Flag;
import java.util.List;

/** Turns numbers into a sentence a person would actually say. */
final class HumanSummary {

    private HumanSummary() {}

    static String write(Flag flag, int score, int checksRun, List<Finding> findings) {
        long red = findings.stream().filter(f -> f.flag() == Flag.RED).count();
        long orange = findings.stream().filter(f -> f.flag() == Flag.ORANGE).count();
        String top = findings.stream()
                .sorted((a, b) -> b.severity().penalty() - a.severity().penalty())
                .map(Finding::title).findFirst().orElse("");

        return switch (flag) {
            case GREEN -> "Nice work \u2014 this script passes all " + checksRun
                    + " checks and sits at " + score + "% compliant. It's good to go.";
            case ORANGE -> "You're close. The script is about " + score + "% in line with our guidelines, with "
                    + orange + " thing" + (orange == 1 ? "" : "s") + " worth tidying up before it goes out. "
                    + "Start with \u201C" + top + "\u201D and recheck.";
            case RED -> "I wouldn't publish this just yet. It breaks " + red + " important rule"
                    + (red == 1 ? "" : "s") + " (starting with \u201C" + top + "\u201D) and scores " + score
                    + "%. Fix the red items first, then run it again.";
        };
    }
}
