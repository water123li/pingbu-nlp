package pingbu.nlp;

import java.util.Collection;

/**
 * 语法子树基类
 * 
 * @author pingbu
 */
abstract class Subtree {

    /**
     * 语法树遍历游标
     */
    public static abstract class Cursor {

        /**
         * 语法树遍历事件处理接口
         */
        public interface Navigator {
            public boolean extendLexicon();

            public boolean pushUnit(Unit unit);

            public void popUnit();

            public void pushParams(Collection<Grammar.ItemParam> params);

            public void popParams(Collection<Grammar.ItemParam> params);

            /**
             * 遍历槽单元开始。
             * 
             * @return 当前位置。
             */
            public Object beginSlot();

            /**
             * 遍历槽单元结束。
             * 
             * @param name
             *            槽名称。
             * @param beginPos
             *            槽的起始位置，由beginSlot返回。
             */
            public void pushSlot(String name, Object beginPos);

            public void popSlot();

            public void endOnePath();
        }

        protected final Cursor mReturnCursor;

        public Cursor(Cursor returnCursor) {
            mReturnCursor = returnCursor;
        }

        protected final void navigateReturn(Navigator navigator) {
            if (mReturnCursor != null)
                mReturnCursor.navigate(navigator);
            else
                navigator.endOnePath();
        }

        public abstract void navigate(Navigator navigator);
    }

    public abstract Cursor newCursor(Cursor returnCursor);
}
