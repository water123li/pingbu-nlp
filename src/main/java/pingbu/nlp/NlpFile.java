package pingbu.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import pingbu.common.FileStorage;
import pingbu.common.Logger;
import pingbu.common.Storage;

/**
 * Class for loading lexicon or grammar from stream such as file
 * 
 * @author pingbu
 */
public abstract class NlpFile {
    private static final String TAG = NlpFile.class.getSimpleName();
    private static final boolean LOG = true;

    private static final void log(final String fmt, final Object... args) {
        if (LOG)
            Logger.d(TAG, String.format(fmt, args));
    }

    public static Lexicon loadLexicon(final String name, final boolean fuzzy,
            final String path) {
        try (final FileInputStream f = new FileInputStream(path)) {
            return loadLexicon(name, fuzzy, f);
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Lexicon loadLexicon(final String name, final boolean fuzzy,
            final Storage storage, final String fileName) {
        try (final InputStream in = storage.open(fileName)) {
            return loadLexicon(name, fuzzy, in);
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Lexicon loadLexicon(final String name, final boolean fuzzy,
            final InputStream in) {
        try {
            final BufferedReader r = new BufferedReader(new InputStreamReader(
                    in, "UTF-8"));
            final LexiconSimple1 lexicon = new LexiconSimple1(name, fuzzy);
            for (;;) {
                final String l = r.readLine();
                if (l == null)
                    break;
                lexicon.addItem(l);
            }
            return lexicon;
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final class GrammarAnalyzeException extends Exception {
        private static final long serialVersionUID = 1L;

        public GrammarAnalyzeException(final String msg) {
            super(msg);
        }
    }

    private interface GrammarAnalyzeState {
        boolean analyze(char c) throws GrammarAnalyzeException;
    }

    private static boolean __isTagHeadChar(final char c) {
        return Character.isAlphabetic(c) || c == '_' || c == '$';
    }

    private static boolean __isTagChar(final char c) {
        return __isTagHeadChar(c) || Character.isDigit(c) || c == '-'
                || c == '.';
    }

    private static final class GrammarAnalyzer {
        private final Parser mParser = new Parser();
        private final Storage mStorage;

        private final class AnalyzeOneGrammar {
            private GrammarAnalyzeState mState = new StateBeforeTag(
                    new StateAfterTag1());
            private String mLexicon = null, mFunction = null;
            private final List<Object> mArgs = new ArrayList<Object>();

            private final class StateComment implements GrammarAnalyzeState {
                private final GrammarAnalyzeState mPrev;

                public StateComment(final GrammarAnalyzeState prev) {
                    mPrev = prev;
                }

                @Override
                public boolean analyze(final char c)
                        throws GrammarAnalyzeException {
                    if (c == '\n')
                        mState = mPrev;
                    return true;
                }
            }

            private final class StateBeforeTag implements GrammarAnalyzeState {
                private final StateAfterTag mNext;

                public StateBeforeTag(final StateAfterTag next) {
                    mNext = next;
                }

                @Override
                public boolean analyze(final char c)
                        throws GrammarAnalyzeException {
                    if (Character.isWhitespace(c))
                        return true;
                    if (__isTagHeadChar(c)) {
                        mState = new StateTag(mNext);
                        return false;
                    }
                    if (c == '#') {
                        mState = new StateComment(this);
                        return true;
                    }
                    throw new GrammarAnalyzeException("expect tag here");
                }
            }

            private abstract class StateAfterTag implements GrammarAnalyzeState {
                protected String mTag;
            }

            private final class StateTag implements GrammarAnalyzeState {
                private final StateAfterTag mNext;
                private final StringBuilder mTag = new StringBuilder();

                public StateTag(final StateAfterTag next) {
                    mNext = next;
                }

                @Override
                public boolean analyze(final char c)
                        throws GrammarAnalyzeException {
                    if (__isTagChar(c)) {
                        mTag.append(c);
                        return true;
                    } else {
                        mNext.mTag = mTag.toString();
                        mState = mNext;
                        return false;
                    }
                }
            }

            private final class StateAfterTag1 extends StateAfterFunction {
                @Override
                public boolean analyze(final char c)
                        throws GrammarAnalyzeException {
                    if (c == '=') {
                        mLexicon = mTag;
                        mState = new StateBeforeTag(new StateAfterFunction());
                        return true;
                    }
                    return super.analyze(c);
                }
            }

            private class StateAfterFunction extends StateAfterTag {
                @Override
                public boolean analyze(final char c)
                        throws GrammarAnalyzeException {
                    if (Character.isWhitespace(c))
                        return true;
                    if (c == '(') {
                        mFunction = mTag.toString();
                        mState = new StateBeforeArgument();
                        return true;
                    }
                    if (c == '#') {
                        mState = new StateComment(this);
                        return true;
                    }
                    throw new GrammarAnalyzeException("expect '(' here");
                }
            }

            private class StateBeforeArgument extends StateAfterTag {
                @Override
                public boolean analyze(final char c)
                        throws GrammarAnalyzeException {
                    if (Character.isWhitespace(c))
                        return true;
                    if (c == '"') {
                        mState = new StateString(new StateAfterArgument(
                                String.class));
                        return true;
                    }
                    if (__isTagHeadChar(c)) {
                        mState = new StateTag(new StateAfterArgument(null));
                        return false;
                    }
                    if (c == '#') {
                        mState = new StateComment(this);
                        return true;
                    }
                    throw new GrammarAnalyzeException("expect parameter here");
                }
            }

            private final class StateString implements GrammarAnalyzeState {
                private final StateAfterTag mNext;
                private final StringBuilder mTag = new StringBuilder();

                public StateString(final StateAfterTag next) {
                    mNext = next;
                }

                @Override
                public boolean analyze(final char c)
                        throws GrammarAnalyzeException {
                    if (c == '"') {
                        mNext.mTag = mTag.toString();
                        mState = mNext;
                    } else {
                        mTag.append(c);
                    }
                    return true;
                }
            }

            private final class StateAfterArgument extends StateAfterTag {
                private final Class<?> mType;

                public StateAfterArgument(final Class<?> type) {
                    mType = type;
                }

                private void __pushArg() throws GrammarAnalyzeException {
                    if (String.class.equals(mType))
                        mArgs.add(mTag);
                    else if ("true".equals(mTag))
                        mArgs.add(true);
                    else if ("false".equals(mTag))
                        mArgs.add(false);
                    else if ("null".equals(mTag))
                        mArgs.add(null);
                    else
                        throw new GrammarAnalyzeException("invalid parameter '"
                                + mTag + "'");
                }

                @Override
                public boolean analyze(final char c)
                        throws GrammarAnalyzeException {
                    if (Character.isWhitespace(c))
                        return true;
                    if (c == ',') {
                        __pushArg();
                        mState = new StateBeforeArgument();
                        return true;
                    }
                    if (c == ')') {
                        __pushArg();
                        mState = new StateAfterLine();
                        return true;
                    }
                    if (c == '#') {
                        mState = new StateComment(this);
                        return true;
                    }
                    throw new GrammarAnalyzeException(
                            "expect ',' or ')' after a parameter");
                }
            }

            private class StateAfterLine extends StateAfterTag {
                @Override
                public boolean analyze(final char c)
                        throws GrammarAnalyzeException {
                    if (Character.isWhitespace(c))
                        return true;
                    if (c == ';') {
                        if (mLexicon != null) {
                            if (mFunction.equals("define")) {
                                if (mArgs.size() == 1 || mArgs.size() == 2) {
                                    final String desc = mArgs.get(0).toString();
                                    if (mArgs.size() == 2) {
                                        Object p2 = mArgs.get(1);
                                        if (!p2.getClass().equals(String.class))
                                            throw new GrammarAnalyzeException(
                                                    "function 'define' parameter 2 require string value");
                                        final String params = p2.toString();
                                        log("%s = define(\"%s\", \"%s\")",
                                                mLexicon, desc, params);
                                        mParser.addSlot(mLexicon, desc, params);
                                    } else {
                                        log("%s = define(\"%s\")", mLexicon,
                                                desc);
                                        mParser.addSlot(mLexicon, desc);
                                    }
                                } else {
                                    throw new GrammarAnalyzeException(
                                            "function 'define' support only 1 or 2 parameters");
                                }
                            } else if (mFunction.equals("compile")) {
                                if (mArgs.size() == 1) {
                                    final String desc = mArgs.get(0).toString();
                                    log("%s = compile(\"%s\")", mLexicon, desc);
                                    mParser.addCompiledSlot(mLexicon, desc);
                                } else {
                                    throw new GrammarAnalyzeException(
                                            "function 'compile' support only one parameter");
                                }
                            } else if (mFunction.equals("load")) {
                                if (mArgs.size() == 1 || mArgs.size() == 2) {
                                    final String fileName = mArgs.get(0)
                                            .toString();
                                    boolean fuzzy = false;
                                    if (mArgs.size() == 2) {
                                        Object p2 = mArgs.get(1);
                                        if (!p2.getClass()
                                                .equals(Boolean.class))
                                            throw new GrammarAnalyzeException(
                                                    "function 'load' parameter 2 require boolean value");
                                        fuzzy = (boolean) p2;
                                        log("%s = load(\"%s\", %s)", mLexicon,
                                                fileName,
                                                Boolean.toString(fuzzy));
                                    } else {
                                        log("%s = load(\"%s\")", mLexicon,
                                                fileName);
                                    }
                                    final Lexicon lexicon = loadLexicon(
                                            mLexicon, fuzzy, mStorage, fileName);
                                    if (lexicon != null)
                                        mParser.addSlot(mLexicon, lexicon);
                                    else
                                        Logger.e(TAG,
                                                "failed loading lexicon '"
                                                        + mLexicon + "'");
                                } else {
                                    throw new GrammarAnalyzeException(
                                            "function 'define' support only 1 or 2 parameters");
                                }
                            } else {
                                throw new GrammarAnalyzeException(
                                        "invalid function '" + mFunction + "'");
                            }
                        } else {
                            if (mFunction.equals("define")) {
                                if (mArgs.size() == 1 || mArgs.size() == 2) {
                                    final String desc = mArgs.get(0).toString();
                                    if (mArgs.size() == 2) {
                                        Object p2 = mArgs.get(1);
                                        if (!p2.getClass().equals(String.class))
                                            throw new GrammarAnalyzeException(
                                                    "function 'define' parameter 2 require string value");
                                        final String params = p2.toString();
                                        log("define(\"%s\", \"%s\")", desc,
                                                params);
                                        mParser.addCommand(desc, params);
                                    } else {
                                        log("define(\"%s\")", desc);
                                        mParser.addCommand(desc);
                                    }
                                } else {
                                    throw new GrammarAnalyzeException(
                                            "function 'define' support only 1 or 2 parameters");
                                }
                            } else if (mFunction.equals("include")) {
                                if (mArgs.size() == 1) {
                                    final String fileName = mArgs.get(0)
                                            .toString();
                                    log("include(\"%s\")", fileName);
                                    try (final InputStream f = mStorage
                                            .open(fileName)) {
                                        new AnalyzeOneGrammar().load(f);
                                    } catch (IOException e) {
                                    }
                                } else {
                                    throw new GrammarAnalyzeException(
                                            "function 'include' support only one parameter");
                                }
                            } else {
                                throw new GrammarAnalyzeException(
                                        "invalid function '" + mFunction + "'");
                            }
                        }
                        mLexicon = null;
                        mFunction = null;
                        mArgs.clear();
                        mState = new StateBeforeTag(new StateAfterTag1());
                        return true;
                    }
                    if (c == '#') {
                        mState = new StateComment(this);
                        return true;
                    }
                    throw new GrammarAnalyzeException("expect ';' here");
                }
            }

            public void load(final InputStream s) throws IOException {
                int line = 0, pos = 0;
                try {
                    final BufferedReader in = new BufferedReader(
                            new InputStreamReader(s, "UTF-8"));
                    for (;; ++line) {
                        final String l = in.readLine();
                        if (l == null)
                            break;
                        for (pos = 0; pos < l.length(); ++pos) {
                            char c = l.charAt(pos);
                            while (!mState.analyze(c))
                                ;
                        }
                        while (!mState.analyze('\n'))
                            ;
                    }
                } catch (final GrammarAnalyzeException e) {
                    Logger.e(TAG, String.format("Grammar error at (%d,%d): %s",
                            line + 1, pos + 1, e.getMessage()));
                }
            }
        }

        public GrammarAnalyzer(final Storage storage) {
            mStorage = storage;
        }

        public void load(final InputStream s) throws IOException {
            new AnalyzeOneGrammar().load(s);
        }

        public Grammar compile() {
            return mParser.compileGrammar();
        }
    }

    /**
     * Load a new grammar from description file.
     * @param path Path of the grammar description file to be loaded.
     * @return New grammar loaded from description file.
     */
    public static Grammar loadGrammar(final String path) {
        final String fullPath = new File(path).getAbsolutePath();
        final int p = fullPath.lastIndexOf(File.separatorChar) + 1;
        final Storage storage = new FileStorage(fullPath.substring(0, p));
        return loadGrammar(storage, fullPath.substring(p));
    }

    public static Grammar loadGrammar(final Storage storage,
            final String fileName) {
        try (final InputStream s = storage.open(fileName)) {
            final GrammarAnalyzer grammarAnalyzer = new GrammarAnalyzer(storage);
            grammarAnalyzer.load(s);
            return grammarAnalyzer.compile();
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
