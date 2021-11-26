package com.techatpark.sjson;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 *
 */
public final class Json {

    public Object read(final Reader reader) throws IOException {
        try (Reader shiftReader = new ShiftReader(reader)) {
            return getValue(shiftReader);
        }
    }

    private boolean getTrue(final Reader reader) throws IOException {
        if ((char) reader.read() == 'r'
                && (char) reader.read() == 'u'
                && (char) reader.read() == 'e') {
            return true;
        } else {
            throw new IllegalArgumentException("Illegal value at ");
        }
    }

    private boolean getFalse(final Reader reader) throws IOException {
        if ((char) reader.read() == 'a'
                && (char) reader.read() == 'l'
                && (char) reader.read() == 's'
                && (char) reader.read() == 'e') {
            return false;
        } else {
            throw new IllegalArgumentException("Illegal value at ");
        }
    }

    private Object getNull(final Reader reader) throws IOException {
        if ((char) reader.read() == 'u'
                && (char) reader.read() == 'l'
                && (char) reader.read() == 'l') {
            return null;
        } else {
            throw new IllegalArgumentException("Illegal value at ");
        }
    }

    private String getString(final Reader reader) throws IOException {
        char c;
        final StringBuilder sb = new StringBuilder();
        for (; ; ) {
            c = (char) reader.read();
            switch (c) {
                case 0, '\n', '\r':
                    throw new IllegalArgumentException("Invalid Token at ");
                case '\\':
                    c = (char) reader.read();
                    switch (c) {
                        case 'b':
                            sb.append('\b');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 'u':
                            sb.append((char) Integer.parseInt(next4(reader), 16));
                            break;
                        case '"', '\'', '\\', '/':
                            sb.append(c);
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid Token at ");
                    }
                    break;
                default:
                    if (c == '"') {
                        return sb.toString();
                    }
                    sb.append(c);
            }
        }
    }

    private Number getNumber(final Reader reader, final char startingChar) throws IOException {

        final StringBuilder builder = new StringBuilder();
        builder.append(startingChar);
        int length = 1;
        char character;

        while (Character.isDigit(character = (char) reader.read())) {
            builder.append(character);
            length++;
        }

        if (character == '.') {
            builder.append('.');
            length++;
            while (Character.isDigit(character = (char) reader.read())) {
                builder.append(character);
                length++;
            }
            ((ShiftReader) reader).reverse(character);
            return Double.parseDouble(builder.toString());
        } else {
            ((ShiftReader) reader).reverse(character);
            return Long.parseLong(builder,0,length,10);
        }
    }

    private Map<String, Object> getObject(final Reader reader) throws IOException {

        char character;
        if ((character = nextClean(reader)) == '}') {
            return Collections.EMPTY_MAP;
        }

        final Map<String, Object> jsonMap = new HashMap<>();
        String theKey;

        while (character == '"') {
            // 1. Get the Key. User String Pool as JSON Keys may be repeating across
            theKey = getString(reader).intern();

            // 2. Move to :
            nextClean(reader);

            // 3. Get the Value
            jsonMap.put(theKey, getValue(reader));

            if ((character = nextClean(reader)) == ',') {
                character = nextClean(reader);
            }
        }

        return Collections.unmodifiableMap(jsonMap);
    }

    private List getArray(final Reader reader) throws IOException {
        final Object value = getValue(reader);
        // If not Empty List
        if (value == reader) {
            return Collections.EMPTY_LIST;
        }
        final List list = new ArrayList();
        list.add(value);
        while (nextClean(reader) == ',') {
            list.add(getValue(reader));
        }
        return Collections.unmodifiableList(list);
    }

    private char nextClean(final Reader reader) throws IOException {
        char character;
        while ((character = (char) reader.read()) == ' '
                || character == '\n'
                || character == '\r'
                || character == '\t') {
        }
        return character;
    }

    private String next4(final Reader reader) throws IOException {
        return new String(new char[]{(char) reader.read(), (char) reader.read(),
                (char) reader.read(), (char) reader.read()});
    }

    private Object getValue(final Reader reader, final char character) throws IOException {
        switch (character) {
            case '"':
                return getString(reader);
            case 'n':
                return getNull(reader);
            case 't':
                return getTrue(reader);
            case 'f':
                return getFalse(reader);
            case '{':
                return getObject(reader);
            case '[':
                return getArray(reader);
            case ']':
                return reader;
            default:
                return getNumber(reader, character);
        }
    }

    private Object getValue(final Reader reader) throws IOException {
        return getValue(reader, nextClean(reader));
    }

    private class ShiftReader extends Reader {

        private final Reader reader;

        private int previous;

        private ShiftReader(final Reader reader) {
            this.reader = reader;
            this.previous = 0;
        }

        private void reverse(final int previous) {
            this.previous = previous;
        }

        @Override
        public int read() throws IOException {
            if (this.previous == 0) {
                return this.reader.read();
            } else {
                int temp = this.previous;
                this.previous = 0;
                return temp;
            }
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) throws IOException {
            return this.reader.read(cbuf, off, len);
        }

        @Override
        public void close() throws IOException {
            this.reader.close();
        }
    }
}