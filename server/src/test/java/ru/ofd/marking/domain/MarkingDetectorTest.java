package ru.ofd.marking.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MarkingDetectorTest {

    @Test
    void markedBySubjectType31() {
        assertTrue(MarkingDetector.isMarked(31, "{}"));
    }

    @Test
    void markedBySubjectType33() {
        assertTrue(MarkingDetector.isMarked(33, null));
    }

    @Test
    void markedByProductCodeGs1m() {
        assertTrue(MarkingDetector.isMarked(1, "{\"Code_GS_1M\":\"0104660184311215215osnRMP5\"}"));
    }

    @Test
    void emptyProductCodeNotMarked() {
        assertFalse(MarkingDetector.isMarked(1, "{}"));
        assertFalse(MarkingDetector.isMarked(1, null));
        assertFalse(MarkingDetector.isMarked(32, "{}"));
    }

    @Test
    void subjectType30WithoutCodeNotMarked() {
        assertFalse(MarkingDetector.isMarked(30, "{}"));
    }
}
