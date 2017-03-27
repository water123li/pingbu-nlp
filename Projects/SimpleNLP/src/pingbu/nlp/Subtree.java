package pingbu.nlp;

import java.util.ArrayList;
import java.util.List;

abstract class Subtree {

    public static abstract class Cursor {

        public static class TravelInfo {
            public List<Unit> path = new ArrayList<Unit>();
            public String commandId = null;
            public List<Grammar.ItemParam> params = new ArrayList<Grammar.ItemParam>();
        }

        protected Cursor returnCursor;

        public Cursor(Cursor returnCursor) {
            this.returnCursor = returnCursor;
        }

        public abstract void travel(TravelInfo info);
    }

    public abstract Cursor newCursor(Cursor returnCursor);
}
