/*
 * Copyright 1996-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

import java.io.*;
import java.util.*;

/**
 * This is the parser used to parse and convert into appropropiately typed data
 * the parts of a squawk assembly file.
 */
class SquawkFileParser implements SquawkTags {

    private final static int MAX_KEYS       = 64;

    private static final SymbolTable tagAttributes = new SymbolTable(MAX_KEYS);
    static {
        tagAttributes.put("xmlns",        A_XMLNS);
        tagAttributes.put("line",         A_LINE);
    }
    /**
     * This table maps tag IDs to a 64 bit flags value that describes
     * which attributes are valid for a given tag.
     */
    private static final long legalAttributes[] = new long[NUM_OF_TAGS];

    /**
     * The table of Squawk element tags.
     */
    private static final SymbolTable tags = new SymbolTable(MAX_KEYS);
    static {
        addSymbol("arrayof",            T_ARRAYOF,         null);
        addSymbol("abstract/",          T_ABSTRACT,        null);
        addSymbol("byte_array",         T_BYTE_ARRAY,      null);
        addSymbol("char_array",         T_CHAR_ARRAY,      null);
        addSymbol("class",              T_CLASS,           null);
        addSymbol("constants",          T_CONSTANTS,       null);
        addSymbol("double_array",       T_DOUBLE_ARRAY,    null);
        addSymbol("extends",            T_EXTENDS,         null);
        addSymbol("float_array",        T_FLOAT_ARRAY,     null);
        addSymbol("from",               T_FROM,            null);
        addSymbol("i",                  T_I,               new byte[] { A_LINE });
        addSymbol("implements",         T_IMPLEMENTS,      null);
        addSymbol("instance_variables", T_INSTANCE_VARS,   null);
        addSymbol("instructions",       T_INSTRUCTIONS,    null);
        addSymbol("int_array",          T_INT_ARRAY,       null);
        addSymbol("interface_map",      T_INTERFACE_MAP,   null);
        addSymbol("interface/",         T_INTERFACE,       null);
        addSymbol("line_number_map",    T_LINE_NUMBER_MAP, null);
        addSymbol("linkage_error/",     T_LINKAGE_ERROR,   null);
        addSymbol("local_variables",    T_LOCAL_VARS,      null);
        addSymbol("long_array",         T_LONG_ARRAY,      null);
        addSymbol("method",             T_METHOD,          null);
        addSymbol("virtual_methods",    T_METHODS_V,       null);
        addSymbol("non_virtual_methods",T_METHODS_NON_V,   null);
        addSymbol("name",               T_NAME,            null);
        addSymbol("native/",            T_NATIVE,          null);
        addSymbol("number",             T_NUMBER,          null);
        addSymbol("parameter_map",      T_PARAMETER_MAP,   null);
        addSymbol("short_array",        T_SHORT_ARRAY,     null);
        addSymbol("slot",               T_SLOT,            null);
        addSymbol("sourcefile",         T_SOURCEFILE,      null);
        addSymbol("squawk",             T_SQUAWK,          new byte[] { A_XMLNS });
        addSymbol("static/",            T_STATIC,          null);
        addSymbol("static_variables",   T_STATIC_VARS,     null);
        addSymbol("string",             T_STRING,          null);
        addSymbol("super",              T_SUPER,           null);
        addSymbol("to",                 T_TO,              null);
        addSymbol("dword",              T_DWORD,           null);
        addSymbol("dword/",             T_DWORD_EMPTY,     null);
        addSymbol("word",               T_WORD,            null);
        addSymbol("word/",              T_WORD_EMPTY,      null);
        addSymbol("half",               T_HALF,            null);
        addSymbol("half/",              T_HALF_EMPTY,      null);
        addSymbol("byte",               T_BYTE,            null);
        addSymbol("byte/",              T_BYTE_EMPTY,      null);
        addSymbol("ref/",               T_REF_EMPTY,       null);
    }

    /**
     * Helper to add a Squawk tag to the tags table. This also takes care of
     * adding the closing varsion of the tag to the table for 'atomic' elements
     * (i.e. those that never have any content) such as <byte/>, <word/> etc.
     */
    private static void addSymbol(String name, int value,byte[] attributes) {
        if (attributes != null)
            for (int i = 0; i != attributes.length; i++) {
                if (attributes[i] >= 64)
                    throw new RuntimeException("Attribute namespace exhausted");
                legalAttributes[value] |= (1 << (attributes[i] - 1));
            }
        if (!name.endsWith("/")) {
            tags.put(name, value);
            tags.put("/"+name, 0-value);
        }
        else {
            // Set the high bit for an empty tag
            tags.put(name,value);
        }
    }


    /**
     * General result buffer.
     */
    private char[] buffer = new char[1000];

    /**
     * Safe way to append to buffer.
     */
    private void append(int i, char ch) {
        if (buffer.length <= i) {
            Object old = buffer;
            buffer = new char[buffer.length*2];
            System.arraycopy(old,0,buffer,0,i);
        }
        buffer[i] = ch;
    }

    /**
     * This is the class being parsed.
     */
//    final ClassBase clazz;

    /**
     * Input data reader.
     */
//    private Reader is;
    private InputStream is;

    /**
     * Keep track of whether or not the last tag decoded was an empty tag (i.e.
     * ended with '/>'). This is used in skipTag.
     */
    private boolean lastTagWasEmpty = false;

    /**
     * The last character read.
     */
    private int last = ' ';

    /**
     * One 'ungot' character
     */
    private int ungetch = -1;

    /**
     * Constructor.
     */
    public SquawkFileParser(InputStream is) throws IOException {
//        this.is = new InputStreamReader(is);
        this.is = is;
    }

    /**
     * Assert facility.
     */
    public static void check(boolean b, String msg) {
        if (!b) {
            throw new SquawkClassFormatError(msg);
        }
    }

    /**
     * Read a character from the input stream.
     */
    private char xgetc() {
        check (last != -1,"Premature EOF");
        try {
            do {
                last = is.read();
                if (SquawkClassLoader.Tracer.getInstance().echoInput) {
                    System.err.print((char)last);
                }
            } while(last == '\r' || last == '\n');
        } catch (IOException ex) {

        }
        return (char)last;
    }

    /**
     * Put back one character in the input stream.
     */
    public void ungetc() {
        check(ungetch == -1,"Cannot ungetc more than once");
        ungetch = last;
    }

    /**
     * Parse and ignore all content until the given closing tag is found.
     * @param tag The closing tag to find.
     */
    public void skipTag(int tag) {
        // Return now if the last tag parsed was an empty tag
        if (lastTagWasEmpty)
            return;
        char ch;
        for (;;) {
            ch = eatWhiteSpace(true);
            if (ch != '<')
                continue;
            if (getTag(true,null) == tag)
                return;
        }
    }

    /**
     * Parse and ignore whitespace (i.e. spaces and tabs).
     * @param comments If true, parse and ignore and XML style comments
     * (i.e. delimited * by "<!--" and "-->") as well.
     * @return the first non-white character read
     */
    public char eatWhiteSpace(boolean comments) {
        boolean inComment = false;
        char ch;
        for (;;) {
            ch = getc();
            if (ch == ' ' || ch == '\t')
                continue;
            if (!comments) {
                return (char)last;
            }
            if (inComment) {
                if (ch == '-' && getc() == '-' && getc() == '>')
                    inComment = false;
            } else {
                if (ch == '<') {
                    if (getc() == '!') {
                        inComment = getc() == '-' && getc() == '-';
                        check(inComment,"Bad comment tag start");
                    }
                    else {
                        ungetc();
                        return '<';
                    }
                }
                else
                    return ch;
            }
        }
    }

    /**
     * Test for whether or not a given character is a valid tag name element.
     * That is, does it match the charactet set [A-Za-z_].
     */
    public boolean isTagNameChar(char ch) {
        return (ch >= 'A' && ch <= 'Z') ||
               (ch >= 'a' && ch <= 'z') ||
                ch == '_';
    }

    /**
     * Return the next available character either from the 'unget' buffer or
     * the underlying input stream.
     */
    public char getc() {
        if (ungetch != -1) {
            char res = (char)ungetch;
            ungetch = -1;
            return res;
        } else {
            return xgetc();
        }
    }

    /**
     * Parse a single tag (skipping any leading spaces) between the opening '<' and
     * the closing '>'. A symbol table lookup is performed and the resulting
     * numeric ID of the tag is returned. Any attributes parsed are ignored.
     */
    public int getTag() {
        return getTag(false,null);
//        return getTag(new Vector());
    }

    /**
     * Parse a single tag (skipping any leading spaces) between the opening '<' and
     * the closing '>'. A symbol table lookup is performed and the resulting
     * numeric ID of the tag is returned.
     * @param openingParsed The opening '<' has already been parsed if true.
     * @param attributes If non-null, then this table is cleared and any
     * attributes parsed are added to this table.
     */
    public int getTag(boolean openingParsed, NoSyncHashtable attributes) {
        char ch;
        if (!openingParsed) {
            ch = eatWhiteSpace(true);
            check(ch == '<', "Bad tag start: "+ch);
        }
        int i = 0;

        // Parse the element name first
        for (;;) {
            ch = getc();
            if (isTagNameChar(ch) || (i == 0 && ch == '/'))
                append(i++,ch);
            else {
                ungetc();
                break;
            }
        }
        check(i != 0, "Bad tag start");
        int nameEnd = i;
        // Parse attributes. Each attribute must match the following regular
        // expression:
        //
        //     [A-Za-z_][A-Za-z_]*="[^"]*
        if (attributes != null) {
            attributes.clear();
        }
        for (;;) {
            int start = nameEnd + 2;
            ch = eatWhiteSpace(false);
            check(ch != -1,"Bad tag");
            // Parse tag close
            if (ch == '/') {
                check(getc() == '>',"Bad tag end");
                append(nameEnd++,'/');
                break;
            }
            if (ch == '>')
                break;

            // Parse name
            i = start;
            while (isTagNameChar(ch)) {
                append(i++,ch);
                ch = getc();
            }
            check(i != 0 && ch == '=' && getc() == '"',"Bad attribute");
            append(i,(char)0);
            int attr = tagAttributes.get(buffer,start);
            check(attr != 0,"Invalid attribute: " + new String(buffer,start,i-start));

            // Parse value
            i = start;
            ch = getc();
            boolean escaped = false;
            for (;;) {
                if (ch == '"' && !escaped)
                    break;
                escaped = (ch == '\\') && !escaped;
                append(i++,ch);
                ch = getc();
            }
            if (attributes != null) {
                attributes.put(new Integer(attr),new String(buffer,start,i-start));
            }
        }

        // Lookup the tag ID
        append(nameEnd,(char)0);
        int tag = tags.get(buffer);
        check(tag != 0,"Invalid Tag "+new String(buffer,0,nameEnd));
        lastTagWasEmpty = buffer[nameEnd - 1] == '/';

        // Check legality of attributes
        if (attributes != null) {
            for (Enumeration e = attributes.keys(); e.hasMoreElements();) {
                int attr = ((Integer)e.nextElement()).intValue();
                check((legalAttributes[tag] & (1 << (attr-1))) != 0,
                    "Attribute " + tagAttributes.getKey(attr) + " not defined for " +
                    tags.getKey(tag));
            }
        }
        return tag;
    }


    /**
     * Read in the next tag and ensure that it matches the given tag.
     */
    public void getTag(int expected) {
        int tag = getTag();
        if (tag != expected)
            unexpectedTag(expected,tag);
    }

    /**
     * Called when an unexpected tag is parsed.
     * @param expected The expected tag or 0 if there were multiple choices.
     * @param tag The tag that was parsed.
     * @exception LinkageError
     */
    public static void unexpectedTag(int expected, int tag) {
        String expectedMsg = (expected == 0 ? "" :
            "(expected " + new String(tags.getKey(expected)) + ")");
        check(false,"Unexpected tag " + expectedMsg+": " +
            new String(tags.getKey(tag)));
    }

    /**
     * Return true if the input stream is at the end of file.
     */
    public boolean isEOF() {
        if (last != -1) {
            getc();
            if (last != -1) {
                ungetc();
                return false;
            }
        }
        return true;
    }

    /**
     * Read everything up to (but not including) the next '<'
     * and return it as a String.
     * @return the sequence of characters read.
     */
    public String getString() {
        int i;
        for (i = 0 ;; i++) {
            char ch = getc();
            if (ch == '&') {
                // An encoded char
                check(getc() == '#',"Bad encoded string");
                int code = 0;
                ch = getc();
                while (ch >= '0' && ch <= '9') {
                    code = (code * 10) + (ch - '0');
                    ch = getc();
                }
                check(ch == ';',"Bad encoded string");
                append(i,(char)code);
            }
            else
            if (ch != '<') {
                append(i,ch);
            }
            else {
                ungetc();
                return new String(buffer,0,i);
            }
        }
    }

    /**
     * Read chars up to (but not including) the next '<' or ' '.
     */
    public char[] getNonSpaceChars() {
        int i;
        for (i = 0 ;; i++) {
            char ch = getc();
            if (ch == ' ' || ch == '<') {
                append(i,(char)0);
                if (ch == '<')
                    ungetc();
                break;
            }
            append(i,ch);
        }
        return buffer;
    }

    /**
     * Read a sequence of digits and return the integer represented by them.
     * This method skips any leading spaces. The number parsed may
     * (optionally) contain a decimal point indicating that this is a float
     * number.
     */
    public int getNumber() {
        return getNumber(true);
    }

    /**
     * Read a sequence of digits and return the integer represented by them.
     * This method skips any leading spaces.
     * @param allowDecimalPoint Specifies whether or not the number parsed may
     * (optionally) contain a single decimal point.
     */
    public int getNumber(boolean allowDecimalPoint) {
        return (int)getLongNumber(false,allowDecimalPoint);
    }

    /**
     * Read a sequence of digits including one optional '.' and return the
     * value represented as a long. This method skips any leading spaces.
     * The number parsed may (optionally) contain a decimal point indicating
     * that this is a double number.
     */
    public long getLongNumber() {
        return getLongNumber(true,true);
    }

    /**
     * Read a sequence of digits including one optional '.' and return the
     * value represented as a long. This method skips any leading spaces.
     * @param dwordResult Specifies how to interpret the encoding of a
     * floating point number: as a double (64 bit) or a float (32 bit).
     * @param allowDecimalPoint Specifies whether or not the number parsed may
     * (optionally) contain a single decimal point.
     */
    public long getLongNumber(boolean dwordResult, boolean allowDecimalPoint) {
        int digits = 0;
        // Eat any leading spaces
        int ch = eatWhiteSpace(false);
        // Look for leading '-'
        int sign = 1;
        if (ch == '-') {
            sign = -1;
            ch = getc();
        }
        long acc = 0;
        // Read digits
        while (ch >= '0' && ch <= '9') {
            digits++;
            acc = (acc * 10) + (ch - '0');
            ch = getc();
            if (ch == '.') {
                check(allowDecimalPoint,"Float or double not expected");
                return sign * (dwordResult ? getDouble(acc) : getFloat(acc));
            }
        }
        ungetc();
        check(digits > 0, "Null number");
        acc *= sign;
        check(dwordResult || (acc <= Integer.MAX_VALUE && acc >= Integer.MIN_VALUE),
            "Number overflow: " + acc);
        return acc;
    }

    /**
     * Parse the fractional part of a double or float number, add its value to
     * a given whole number value and return the bit representation of the
     * complete number encoded in a long.
     */
    private double getDouble(double d) {
        int ch = getc();
        int divisor = 10;
        while (ch >= '0' && ch <= '9') {
            d += (double)(ch - '0') / divisor;
            divisor *= 10;
            ch = getc();
        }
        ungetc();
        return d;

    }

    /**
     * Parse the fractional part of a double or float number, add its value to
     * a given whole number value and return the bit representation of the
     * complete number encoded in a long.
     */
    private long getDouble(long wholePart) {
        double d = getDouble((double)wholePart);
        return Double.doubleToLongBits(d);

    }

    /**
     * Parse the fractional part of a float number, add its value to
     * a given whole number value and return the bit representation of the
     * complete number encoded in a long.
     */
    private long getFloat(long wholePart) {
        double d = getDouble((double)wholePart);
        return Float.floatToIntBits((float)d);
    }

    /**
     * Close the input stream.
     */
    public void close() {
        try {
            if (is != null) {
                is.close();
                is = null;
            }
        } catch (IOException ex) {
        }
    }

    /**
     * Parse the content of a number based element (e.g. T_NUMBER, T_WORD etc)
     * as well as the closing tag.
     * @param closing The closing tag. If 0, then don't try and parse this
     * tag.
     * @return the value of the parsed number as an integer.
     */
    public long parseNumber(int closing,
        boolean dwordResult, boolean allowDecimalPoint) {
        long result = getLongNumber(dwordResult,allowDecimalPoint);
        // Eat any trailing spaces
        while (getc() == ' ')
            ;
        ungetc();
        if (closing != 0)
            getTag(closing);
        return result;
    }

    /**
     * Parse the content of a number list based element (e.g. T_IMPLEMENTS)
     * as well as the closing tag.
     * @param closing The closing tag. If 0, then don't try and parse this
     * tag.
     * @return the value of the parsed number list as a Vector.
     */
    public Vector parseNumberList(int closing) {
        Vector result = new Vector(10);
        while (true) {
            int num = (int)parseNumber(0,false,false);
            result.addElement(new Integer(num));
            char next = getc();
            ungetc();
            if (next == '<') {
                break;
            }
        }
        if (closing != 0)
            getTag(closing);
        return result;
    }

    /**
     * Parse the content of a String based element (e.g. T_NAME or T_STRING)
     * as well as the appropriate closing tag.
     * @param closing The closing tag. If 0, then don't try and parse this
     * tag.
     * @return the value of the parsed name as a String.
     */
    public String parseString(int closing) {
        String result = getString();
        if (closing != 0)
            getTag(closing);
        return result;
    }

    /**
     * Parse the children of a T_STATIC_VARS or T_INSTANCE_VARS element.
     * @param closing The closing tag to be parsed.
     * @return a Vector containing serialised version of a map where
     * the type of variable i is in element i*2 and the initial value (or null) of
     * variable i is in element (i*2)+1.
     */
    public Vector parseVariables(int closing) {
        Vector vars = new Vector(10);
        int token;
        while ((token = getTag()) != closing) {
            int type = 0;
            Object value = null;
            switch (token) {
                case T_DWORD:       value = new Long(parseNumber(-T_DWORD,true,true));
                                    // fall through
                case T_DWORD_EMPTY: type  = T_DWORD;
                                    break;
                case T_WORD:        value = new Integer((int)parseNumber(-T_WORD,false,true));
                                    // fall through
                case T_WORD_EMPTY:  type = T_WORD;
                                    break;
                case T_HALF:        value = new Short((short)parseNumber(-T_HALF,false,true));
                                    // fall through
                case T_HALF_EMPTY:  type = T_HALF;
                                    break;
                case T_BYTE:        value = new Byte((byte)parseNumber(-T_BYTE,false,true));
                                    // fall through
                case T_BYTE_EMPTY:  type = T_BYTE;
                                    break;
                case T_REF_EMPTY:   type = T_REF;
                                    break;
                case T_STRING:      type = T_STRING; value = parseString(-T_STRING);
                                    break;
                default: unexpectedTag(0,token); break;
            }
            vars.addElement(new Integer(type));
            vars.addElement(value);
        }
        if (vars.size() == 0) {
            return null;
        }
        else {
            return vars;
        }
    }

    /**
     * Parse the children of a map in the form <to>..</to><from>..</from>.
     * @param closing The closing tag to be parsed.
     * @return a Vector containing serialised version of the map where
     * entry i is a <from> value and entry i+1 is the corresponding <to> value.
     */
    public Vector parseFromToMap(int closing) {
        Vector map = new Vector(10);
        int token;
        while ((token = getTag()) != closing) {
            if (token != T_FROM)
                unexpectedTag(T_FROM,token);
            map.addElement(new Integer((int)parseNumber(-T_FROM, false, false)));
            getTag(T_TO);
            map.addElement(new Integer((int)parseNumber(-T_TO, false, false)));
        }
        return map;
    }
}

