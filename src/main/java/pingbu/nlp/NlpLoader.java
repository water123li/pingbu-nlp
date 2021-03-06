package pingbu.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import pingbu.logger.Logger;
import pingbu.storage.FileStorage;
import pingbu.storage.Storage;

/**
 * NLP词典和语法加载器
 * 
 * @author pingbu
 */
public abstract class NlpLoader {
    private static final String TAG = NlpLoader.class.getSimpleName();
    private static final boolean LOG = true;

    private static void log(final String fmt, final Object... args) {
        if (LOG)
            Logger.d(TAG, fmt, args);
    }

    /**
     * 从文本文件加载词典
     *
     * @param name  词典名
     * @param fuzzy 是否模糊匹配
     * @param path  文本文件路径
     * @return 词典对象
     */
    public static Lexicon loadLexicon(final String name, final boolean fuzzy, final String path) {
        try (final FileInputStream f = new FileInputStream(path)) {
            return loadLexicon(name, fuzzy, f);
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从文本文件加载词典
     *
     * @param name     词典名
     * @param fuzzy    是否模糊匹配
     * @param storage  文本文件存储
     * @param fileName 文本文件名
     * @return 词典对象
     */
    public static Lexicon loadLexicon(final String name, final boolean fuzzy, final Storage storage, final String fileName) {
        log("==> loadLexicon %s: storage=%sn fileName=%s", name, storage.toString(), fileName);
        try (final InputStream in = storage.open(fileName)) {
            return loadLexicon(name, fuzzy, in);
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            log("<== loadLexicon %s", name);
        }
    }

    private static Lexicon loadLexicon(final String name, final boolean fuzzy, final InputStream in) {
        try {
            final BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            final LexiconSimple1 lexicon = new LexiconSimple1(name, fuzzy);
            for (; ; ) {
                final String l = r.readLine();
                if (l == null)
                    break;
                //log(" item '%s'", l);
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

        GrammarAnalyzeException(final String msg) {
            super(msg);
        }
    }

    private interface GrammarAnalyzeState {
        boolean analyze(char c) throws GrammarAnalyzeException;
    }

    private static boolean __isWhitespace(final char c) {
        return c >= '\0' && c <= ' ';
    }

    private static boolean __isTagHeadChar(final char c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '_' || c == '$';
    }

    private static boolean __isTagChar(final char c) {
        return __isTagHeadChar(c) || c >= '0' && c <= '9' || c == '-' || c == '.';
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

                StateComment(final GrammarAnalyzeState prev) {
                    mPrev = prev;
                }

                @Override
                public boolean analyze(final char c) {
                    if (c == '\n')
                        mState = mPrev;
                    return true;
                }
            }

            private final class StateBeforeTag implements GrammarAnalyzeState {
                private final StateAfterTag mNext;

                StateBeforeTag(final StateAfterTag next) {
                    mNext = next;
                }

                @Override
                public boolean analyze(final char c)
                        throws GrammarAnalyzeException {
                    if (__isWhitespace(c))
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
                String mTag;
            }

            private final class StateTag implements GrammarAnalyzeState {
                private final StateAfterTag mNext;
                private final StringBuilder mTag = new StringBuilder();

                StateTag(final StateAfterTag next) {
                    mNext = next;
                }

                @Override
                public boolean analyze(final char c) {
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
                public boolean analyze(final char c) throws GrammarAnalyzeException {
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
                public boolean analyze(final char c) throws GrammarAnalyzeException {
                    if (__isWhitespace(c))
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
                public boolean analyze(final char c) throws GrammarAnalyzeException {
                    if (__isWhitespace(c))
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

                StateString(final StateAfterTag next) {
                    mNext = next;
                }

                @Override
                public boolean analyze(final char c) {
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

                StateAfterArgument(final Class<?> type) {
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
                        throw new GrammarAnalyzeException("invalid parameter '" + mTag + "'");
                }

                @Override
                public boolean analyze(final char c) throws GrammarAnalyzeException {
                    if (__isWhitespace(c))
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
                    throw new GrammarAnalyzeException("expect ',' or ')' after a parameter");
                }
            }

            private class StateAfterLine extends StateAfterTag {
                @Override
                public boolean analyze(final char c) throws GrammarAnalyzeException {
                    if (__isWhitespace(c))
                        return true;
                    if (c == ';') {
                        if (mLexicon != null) {
                            if (mFunction.equals("define")) {
                                if (mArgs.size() == 1 || mArgs.size() == 2) {
                                    final String desc = mArgs.get(0).toString();
                                    if (mArgs.size() == 2) {
                                        final Object p2 = mArgs.get(1);
                                        if (!p2.getClass().equals(String.class))
                                            throw new GrammarAnalyzeException("function 'define' parameter 2 require string value");
                                        final String params = p2.toString();
                                        log("%s = define(\"%s\", \"%s\")", mLexicon, desc, params);
                                        mParser.addSlot(mLexicon, desc, params);
                                    } else {
                                        log("%s = define(\"%s\")", mLexicon, desc);
                                        mParser.addSlot(mLexicon, desc);
                                    }
                                } else {
                                    throw new GrammarAnalyzeException("function 'define' support only 1 or 2 parameters");
                                }
                            } else if (mFunction.equals("compile")) {
                                if (mArgs.size() == 1) {
                                    final String desc = mArgs.get(0).toString();
                                    log("%s = compile(\"%s\")", mLexicon, desc);
                                    mParser.addCompiledSlot(mLexicon, desc);
                                } else {
                                    throw new GrammarAnalyzeException("function 'compile' support only one parameter");
                                }
                            } else if (mFunction.equals("load")) {
                                if (mArgs.size() == 1 || mArgs.size() == 2) {
                                    final String fileName = mArgs.get(0).toString();
                                    boolean fuzzy = false;
                                    if (mArgs.size() == 2) {
                                        Object p2 = mArgs.get(1);
                                        if (!p2.getClass().equals(Boolean.class))
                                            throw new GrammarAnalyzeException("function 'load' parameter 2 require boolean value");
                                        fuzzy = (boolean) p2;
                                        log("%s = load(\"%s\", %s)", mLexicon, fileName, Boolean.toString(fuzzy));
                                    } else {
                                        log("%s = load(\"%s\")", mLexicon, fileName);
                                    }
                                    final Lexicon lexicon = loadLexicon(mLexicon, fuzzy, mStorage, fileName);
                                    if (lexicon != null)
                                        mParser.addSlot(mLexicon, lexicon);
                                    else
                                        Logger.e(TAG, "failed loading lexicon '%s'", mLexicon);
                                } else {
                                    throw new GrammarAnalyzeException("function 'define' support only 1 or 2 parameters");
                                }
                            } else {
                                throw new GrammarAnalyzeException("invalid function '" + mFunction + "'");
                            }
                        } else {
                            if (mFunction.equals("define")) {
                                if (mArgs.size() == 1 || mArgs.size() == 2) {
                                    final String desc = mArgs.get(0).toString();
                                    if (mArgs.size() == 2) {
                                        final Object p2 = mArgs.get(1);
                                        if (!p2.getClass().equals(String.class))
                                            throw new GrammarAnalyzeException("function 'define' parameter 2 require string value");
                                        final String params = p2.toString();
                                        log("define(\"%s\", \"%s\")", desc, params);
                                        mParser.addCommand(desc, params);
                                    } else {
                                        log("define(\"%s\")", desc);
                                        mParser.addCommand(desc);
                                    }
                                } else {
                                    throw new GrammarAnalyzeException("function 'define' support only 1 or 2 parameters");
                                }
                            } else if (mFunction.equals("include")) {
                                if (mArgs.size() == 1) {
                                    final String fileName = mArgs.get(0).toString();
                                    log("include(\"%s\")", fileName);
                                    try (final InputStream f = mStorage.open(fileName)) {
                                        new AnalyzeOneGrammar().load(f);
                                    } catch (final IOException e) {
                                    }
                                } else {
                                    throw new GrammarAnalyzeException("function 'include' support only one parameter");
                                }
                            } else {
                                throw new GrammarAnalyzeException("invalid function '" + mFunction + "'");
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

            void load(final InputStream s) throws IOException {
                int line = 0, pos = 0;
                try {
                    final BufferedReader in = new BufferedReader(new InputStreamReader(s, "UTF-8"));
                    for (; ; ++line) {
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
                    Logger.e(TAG, String.format("Grammar error at (%d,%d): %s", line + 1, pos + 1, e.getMessage()));
                }
            }
        }

        GrammarAnalyzer(final Storage storage) {
            mStorage = storage;
        }

        void load(final InputStream s) throws IOException {
            new AnalyzeOneGrammar().load(s);
        }

        Grammar compile() {
            return mParser.compileGrammar();
        }
    }

    /**
     * 从文件加载语法
     *
     * @param path 语法描述文件路径
     * @return 语法对象
     */
    public static Grammar loadGrammar(final String path) {
        final String fullPath = new File(path).getAbsolutePath();
        final int p = fullPath.lastIndexOf(File.separatorChar) + 1;
        final Storage storage = new FileStorage(fullPath.substring(0, p));
        return loadGrammar(storage, fullPath.substring(p));
    }

    /**
     * 从文件加载语法
     *
     * @param storage  语法描述文件存储
     * @param fileName 语法描述文件名
     * @return 语法对象
     */
    public static Grammar loadGrammar(final Storage storage, final String fileName) {
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
