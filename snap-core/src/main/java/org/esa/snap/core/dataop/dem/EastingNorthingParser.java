package org.esa.snap.core.dataop.dem;

import java.text.ParseException;

public class EastingNorthingParser {

    private static final int ILLEGAL_DIRECTION_VALUE = -999;

    private static final int directionWest = 0;
    private static final int directionEast = 1;
    private static final int directionNorth = 2;
    private static final int directionSouth = 3;

    private static final int indexEasting = 0;
    private static final int indexNorthing = 1;

    private String text;
    private int pos;
    private final String allowedSuffix;
    private static final char EOF = (char) -1;

    public EastingNorthingParser(final String text, final String allowedSuffix) {
        this.text = text;
        this.pos = -1;
        this.allowedSuffix = allowedSuffix;
    }

    public int[] parse() throws ParseException {
        return parseImpl();
    }

    private int[] parseImpl() throws ParseException {
        final int[] eastingNorthing = new int[]{ILLEGAL_DIRECTION_VALUE, ILLEGAL_DIRECTION_VALUE};
        parseDirectionValueAndAssign(eastingNorthing); // one per direction
        parseDirectionValueAndAssign(eastingNorthing); // one per direction
        validateThatValuesAreAssigned(eastingNorthing);
        validateCorrectSuffix();

        return eastingNorthing;
    }

    private void validateThatValuesAreAssigned(final int[] eastingNorthing) throws ParseException {
        if (eastingNorthing[indexEasting] == ILLEGAL_DIRECTION_VALUE) {
            throw new ParseException("Easting value not available.", -1);
        }
        if (eastingNorthing[indexNorthing] == ILLEGAL_DIRECTION_VALUE) {
            throw new ParseException("Northing value not available.", -1);
        }
    }

    private void validateCorrectSuffix() throws ParseException {
        final String suffix = text.substring(++pos);
        if (!suffix.matches(allowedSuffix)) {
            throw new ParseException("Illegal string format.", pos);
        }
    }

    protected void parseDirectionValueAndAssign(final int[] eastingNorthing) throws ParseException {
        int value = readNumber();
        final int direction = getDirection();
        value = correctValueByDirection(value, direction);
        assignValueByDirection(eastingNorthing, value, direction);
    }

    protected void assignValueByDirection(final int[] eastingNorthing, final int value, final int direction) {
        if (isWest(direction) || isEast(direction)) {
            eastingNorthing[indexEasting] = value;
        } else {
            eastingNorthing[indexNorthing] = value;
        }
    }

    protected int correctValueByDirection(int value, final int direction) throws ParseException {
        value *= (isWest(direction) || isSouth(direction)) ? -1 : +1;
        if (isWest(direction) && (value > 0 || value < -180)) {
            throw new ParseException(
                    "The value '" + value + "' for west direction is out of the range -180 ... 0.", pos);
        }
        if (isEast(direction) && (value < 0 || value > 180)) {
            throw new ParseException("The value '" + value + "' for east direction is out of the range 0 ... 180.",
                    pos);
        }
        if (isSouth(direction) && (value > 0 || value < -90)) {
            throw new ParseException(
                    "The value '" + value + "' for south direction is out of the range -90 ... 0.", pos);
        }
        if (isNorth(direction) && (value < 0 || value > 90)) {
            throw new ParseException("The value '" + value + "' for north direction is out of the range 0 ... 90.",
                    pos);
        }
        return value;
    }

    private boolean isNorth(final int direction) {
        return compareDirection(directionNorth, direction);
    }

    private boolean isEast(final int direction) {
        return compareDirection(directionEast, direction);
    }

    private boolean isSouth(final int direction) {
        return compareDirection(directionSouth, direction);
    }

    private boolean isWest(final int direction) {
        return compareDirection(directionWest, direction);
    }

    private boolean compareDirection(final int expected, final int direction) {
        return expected == direction;
    }

    protected int getDirection() throws ParseException {
        final char c = nextChar();
        if (c == 'w' || c == 'W') {
            return directionWest;
        }
        if (c == 'e' || c == 'E') {
            return directionEast;
        }
        if (c == 'n' || c == 'N') {
            return directionNorth;
        }
        if (c == 's' || c == 'S') {
            return directionSouth;
        }
        throw new ParseException("Illegal direction character.", pos);
    }

    protected int readNumber() throws ParseException {
        char c = nextChar();
        if (!Character.isDigit(c)) {
            throw new ParseException("Digit character expected.", pos);
        }
        int value = 0;
        while (Character.isDigit(c)) {
            value *= 10;
            value += (c - '0');
            c = nextChar();
        }
        pos--;
        return value;
    }

    private char nextChar() {
        pos++;
        return pos < text.length() ? text.charAt(pos) : EOF;
    }
}
