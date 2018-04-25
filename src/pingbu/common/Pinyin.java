package pingbu.common;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 拼音模块，用于支持语义和词典建模、搜索
 * 
 * @author pingbu
 */
public final class Pinyin {
    private static final String TAG = Pinyin.class.getSimpleName();

    private static final int TOTAL_WEIGHT = 100;
    private static final byte SM_WEIGHT = 30;
    private static final byte YM_WEIGHT = 50;
    private static final byte TONE_WEIGHT = 10;

    private static final String[] sSmPrefix = "b,p,m,f,d,t,n,l,g,k,h,j,q,x,zh,ch,sh,r,z,c,s,y,w"
            .split(",");
    private static final String[] SMs = ",b,p,m,f,d,t,n,l,g,k,h,j,q,x,z,c,s,zh,ch,sh,r"
            .split(",");
    private static final String[] YMs = "a,ai,an,ang,ao,e,ei,en,eng,er,i,ia,ian,iang,iao,ie,in,ing,io,iong,iou,o,ong,ou,u,ua,uai,uan,uang,ue,uei,uen,ueng,uo,uong,v,van,ve,ven,I,E,m,n,ng"
            .split(",");
    private static final int TONE_COUNT = 5;

    private static final Map<String, Byte> sSMtoIndex = new HashMap<String, Byte>() {
        private static final long serialVersionUID = 1L;
        {
            for (byte i = 0; i < SMs.length; ++i)
                put(SMs[i], i);
        }
    };
    private static final Map<String, Byte> sYMtoIndex = new HashMap<String, Byte>() {
        private static final long serialVersionUID = 1L;
        {
            for (byte i = 0; i < YMs.length; ++i)
                put(YMs[i], i);
        }
    };

    private static final byte[] sNormalizedSMs = new byte[SMs.length];
    private static final byte[] sNormalizedYMs = new byte[YMs.length];
    private static final short[] sCharSmYmTone = new short[65536];

    private static final byte[][] sSmDistance = new byte[SMs.length][SMs.length];
    private static final byte[][] sYmDistance = new byte[YMs.length][YMs.length];
    private static final byte[][] sToneDistance = new byte[TONE_COUNT][TONE_COUNT];

    public static final void createModal(InputStream f) throws IOException {
        Logger.d(TAG, "==> createModal");

        try {
            for (byte i = 0; i < sNormalizedSMs.length; ++i)
                sNormalizedSMs[i] = i;
            sNormalizedSMs[sSMtoIndex.get("b")] = sSMtoIndex.get("p");
            sNormalizedSMs[sSMtoIndex.get("d")] = sSMtoIndex.get("t");
            sNormalizedSMs[sSMtoIndex.get("h")] = sSMtoIndex.get("f");
            sNormalizedSMs[sSMtoIndex.get("n")] = sSMtoIndex.get("l");
            sNormalizedSMs[sSMtoIndex.get("j")] = sSMtoIndex.get("z");
            sNormalizedSMs[sSMtoIndex.get("q")] = sSMtoIndex.get("c");
            sNormalizedSMs[sSMtoIndex.get("x")] = sSMtoIndex.get("s");
            sNormalizedSMs[sSMtoIndex.get("zh")] = sSMtoIndex.get("z");
            sNormalizedSMs[sSMtoIndex.get("ch")] = sSMtoIndex.get("c");
            sNormalizedSMs[sSMtoIndex.get("sh")] = sSMtoIndex.get("s");

            for (byte i = 0; i < sNormalizedYMs.length; ++i)
                sNormalizedYMs[i] = i;
            sNormalizedYMs[sYMtoIndex.get("ang")] = sYMtoIndex.get("an");
            sNormalizedYMs[sYMtoIndex.get("eng")] = sYMtoIndex.get("en");
            sNormalizedYMs[sYMtoIndex.get("ing")] = sYMtoIndex.get("in");
            sNormalizedYMs[sYMtoIndex.get("n")] = sYMtoIndex.get("en");
            sNormalizedYMs[sYMtoIndex.get("m")] = sYMtoIndex.get("en");
            sNormalizedYMs[sYMtoIndex.get("ng")] = sYMtoIndex.get("en");
            sNormalizedYMs[sYMtoIndex.get("I")] = sYMtoIndex.get("i");
            sNormalizedYMs[sYMtoIndex.get("E")] = sYMtoIndex.get("ai");

            for (int i = 0; i < sCharSmYmTone.length; ++i)
                sCharSmYmTone[i] = -1;
            sCharSmYmTone['0'] = _pinyinToSmYmTone("ling");
            sCharSmYmTone['1'] = _pinyinToSmYmTone("yi");
            sCharSmYmTone['2'] = _pinyinToSmYmTone("er");
            sCharSmYmTone['3'] = _pinyinToSmYmTone("san");
            sCharSmYmTone['4'] = _pinyinToSmYmTone("si");
            sCharSmYmTone['5'] = _pinyinToSmYmTone("wu");
            sCharSmYmTone['6'] = _pinyinToSmYmTone("liu");
            sCharSmYmTone['7'] = _pinyinToSmYmTone("qi");
            sCharSmYmTone['8'] = _pinyinToSmYmTone("ba");
            sCharSmYmTone['9'] = _pinyinToSmYmTone("jiu");
            sCharSmYmTone['A'] = sCharSmYmTone['a'] = _pinyinToSmYmTone("ei");
            sCharSmYmTone['B'] = sCharSmYmTone['b'] = _pinyinToSmYmTone("bi");
            sCharSmYmTone['C'] = sCharSmYmTone['c'] = _pinyinToSmYmTone("sei");
            sCharSmYmTone['D'] = sCharSmYmTone['d'] = _pinyinToSmYmTone("di");
            sCharSmYmTone['E'] = sCharSmYmTone['e'] = _pinyinToSmYmTone("yi");
            sCharSmYmTone['G'] = sCharSmYmTone['g'] = _pinyinToSmYmTone("ji");
            sCharSmYmTone['I'] = sCharSmYmTone['i'] = _pinyinToSmYmTone("ai");
            sCharSmYmTone['J'] = sCharSmYmTone['j'] = _pinyinToSmYmTone("jei");
            sCharSmYmTone['K'] = sCharSmYmTone['k'] = _pinyinToSmYmTone("kei");
            sCharSmYmTone['N'] = sCharSmYmTone['n'] = _pinyinToSmYmTone("en");
            sCharSmYmTone['O'] = sCharSmYmTone['o'] = _pinyinToSmYmTone("ou");
            sCharSmYmTone['P'] = sCharSmYmTone['p'] = _pinyinToSmYmTone("pi");
            sCharSmYmTone['Q'] = sCharSmYmTone['q'] = _pinyinToSmYmTone("kiu");
            sCharSmYmTone['R'] = sCharSmYmTone['r'] = _pinyinToSmYmTone("a");
            sCharSmYmTone['T'] = sCharSmYmTone['t'] = _pinyinToSmYmTone("ti");
            sCharSmYmTone['U'] = sCharSmYmTone['u'] = _pinyinToSmYmTone("you");
            sCharSmYmTone['V'] = sCharSmYmTone['v'] = _pinyinToSmYmTone("wei");
            sCharSmYmTone['Y'] = sCharSmYmTone['y'] = _pinyinToSmYmTone("wai");

            BufferedReader r = new BufferedReader(new InputStreamReader(f,
                    "UTF-8"));
            for (;;) {
                String l = r.readLine();
                if (l == null)
                    break;
                if (l.startsWith("U+")) {
                    String[] p = l.split("\t");
                    if (p[1].equals("kMandarin")) {
                        int u = Integer.parseInt(p[0].substring(2), 16);
                        String pinyin = p[2];
                        int tone = 0;
                        // System.out.printf("%d: %s\n", u, pinyin);
                        for (int i = 0; i < 8 * 4; ++i) {
                            char c = "āáǎàōóǒòēéěèīíǐìūúǔù_ǘǚǜ_ḿ___ńňǹ"
                                    .charAt(i);
                            int a = pinyin.indexOf(c);
                            if (a >= 0) {
                                pinyin = pinyin.substring(0, a)
                                        + "aoeiuümn".charAt(i / 4)
                                        + pinyin.substring(a + 1);
                                tone = (i % 4) + 1;
                                break;
                            }
                        }
                        pinyin = pinyin.replace('ü', 'v');
                        short SmYmTone = _pinyinToSmYmTone(pinyin);
                        if (u < sCharSmYmTone.length)
                            sCharSmYmTone[u] = (short) (SmYmTone | tone);
                    }
                }
            }

            // for (int i = 0; i < PINYINs.length; ++i)
            // System.out.printf("%s\t%s\t%s\n", PINYINs[i], SMs[sPinyin2SmYm[i]
            // >> 16], YMs[sPinyin2SmYm[i] & 0x0000FFFF]);

            for (int i = 0; i < SMs.length; ++i)
                for (int j = 0; j < SMs.length; ++j)
                    if (i < j) {
                        if (SMs[i].isEmpty())
                            sSmDistance[i][j] = SMs[j].isEmpty() ? SM_WEIGHT
                                    : 0;
                        else if (",z-zh,c-ch,s-sh,".contains(',' + SMs[i] + '-'
                                + SMs[j] + ','))
                            sSmDistance[i][j] = (byte) (SM_WEIGHT * .9);
                        else if (",b-p,f-h,d-t,n-l,g-k,j-z,j-zh,q-c,q-ch,x-s,x-sh,"
                                .contains(',' + SMs[i] + '-' + SMs[j] + ','))
                            sSmDistance[i][j] = (byte) (SM_WEIGHT * .8);
                        else
                            sSmDistance[i][j] = 0;
                        // System.out.printf("%s-%s: %d\n", SMs[i], SMs[j],
                        // sSmDistance[i][j]);
                    } else if (i == j)
                        sSmDistance[i][j] = SM_WEIGHT;
                    else
                        sSmDistance[i][j] = sSmDistance[j][i];

            for (int i = 0; i < YMs.length; ++i)
                for (int j = 0; j < YMs.length; ++j)
                    if (i < j) {
                        if (YMs[i].equals("an") && YMs[j].equals("ang"))
                            sYmDistance[i][j] = (byte) (YM_WEIGHT * .8);
                        else if (YMs[j].equals(YMs[i] + 'g'))
                            sYmDistance[i][j] = (byte) (YM_WEIGHT * .9);
                        else
                            sYmDistance[i][j] = (byte) (YM_WEIGHT * _compareText(
                                    YMs[i], YMs[j]));
                        // System.out.printf("%s-%s: %d\n", YMs[i], YMs[j],
                        // sYmDistance[i][j]);
                    } else if (i == j)
                        sYmDistance[i][j] = YM_WEIGHT;
                    else
                        sYmDistance[i][j] = sYmDistance[j][i];

            for (int i = 0; i < TONE_COUNT; ++i)
                for (int j = 0; j < TONE_COUNT; ++j) {
                    if (i < j) {
                        if (i == 0 && j == 1)
                            sToneDistance[i][j] = (byte) (TONE_WEIGHT * .8);
                        else if (i == 2 && j == 3)
                            sToneDistance[i][j] = (byte) (TONE_WEIGHT * .5);
                        else
                            sToneDistance[i][j] = 0;
                        // System.out.printf("%d-%d: %d\n", i, j,
                        // sToneDistance[i][j]);
                    } else if (i == j)
                        sToneDistance[i][j] = TONE_WEIGHT;
                    else
                        sToneDistance[i][j] = sToneDistance[j][i];
                }
        } finally {
            Logger.d(TAG, "<== createModal");
        }
    }

    public static final void saveModal(OutputStream f) throws IOException {
        Logger.d(TAG, "==> saveModal");
        try {
            DataOutputStream out = new DataOutputStream(f);
            out.write(sNormalizedSMs);
            out.write(sNormalizedYMs);
            for (int i = 0; i < sCharSmYmTone.length; ++i)
                out.writeShort(sCharSmYmTone[i]);
            for (int i = 0; i < SMs.length; ++i)
                out.write(sSmDistance[i]);
            for (int i = 0; i < YMs.length; ++i)
                out.write(sYmDistance[i]);
            for (int i = 0; i < TONE_COUNT; ++i)
                out.write(sToneDistance[i]);
        } finally {
            Logger.d(TAG, "<== saveModal");
        }
    }

    public static final void loadModal(InputStream f) throws IOException {
        Logger.d(TAG, "==> loadModal");
        try (DataInputStream in = new DataInputStream(f)) {
            in.read(sNormalizedSMs);
            in.read(sNormalizedYMs);
            for (int i = 0; i < sCharSmYmTone.length; ++i)
                sCharSmYmTone[i] = in.readShort();
            for (int i = 0; i < SMs.length; ++i)
                in.read(sSmDistance[i]);
            for (int i = 0; i < YMs.length; ++i)
                in.read(sYmDistance[i]);
            for (int i = 0; i < TONE_COUNT; ++i)
                in.read(sToneDistance[i]);
        } finally {
            Logger.d(TAG, "<== loadModal");
        }
    }

    public static final void init(String path) {
        init(new FileStorage(path));
    }

    public static final void init(Storage storage) {
        try (InputStream in = storage.open("Pinyin.modal")) {
            loadModal(in);
        } catch (IOException e0) {
            try (InputStream in = storage.open("Unihan_Readings.txt")) {
                createModal(in);
                try (OutputStream out = storage.create("Pinyin.modal")) {
                    saveModal(out);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private static final short _pinyinToSmYmTone(String pinyin) {
        String sm = "", ym;
        int tone = pinyin.charAt(pinyin.length() - 1);
        if (Character.isDigit(tone)) {
            ym = pinyin.substring(0, pinyin.length() - 1);
            tone = tone - '0';
        } else {
            ym = pinyin;
            tone = 0;
        }
        if (ym.startsWith("yu"))
            ym = 'v' + ym.substring(2);
        else if (ym.startsWith("y"))
            ym = 'i' + ym.substring(1);
        else if (pinyin.startsWith("w"))
            ym = 'u' + ym.substring(1);
        else if (!pinyin.equals("m") && !pinyin.equals("n"))
            for (int i = 0; i < sSmPrefix.length; ++i)
                if (ym.startsWith(sSmPrefix[i])) {
                    sm = sSmPrefix[i];
                    ym = ym.substring(sm.length());
                    break;
                }
        ym = ym.replace("ii", "i");
        ym = ym.replace("uu", "u");
        ym = ym.replace("iu", "iou");
        ym = ym.replace("ui", "uei");
        ym = ym.replace("un", "uen");
        ym = ym.replace("vn", "ven");
        if (!sm.isEmpty()) {
            if (ym.equals("i") && ",z,c,s,zh,ch,sh,r,".contains(',' + sm + ','))
                ym = "I";
            else if (ym.startsWith("u") && "jqx".contains(sm))
                ym = 'v' + ym.substring(1);
        }
        int smIndex = sSMtoIndex.get(sm), ymIndex = sYMtoIndex.get(ym);
        return (short) ((smIndex << 10) | (ymIndex << 4));
    }

    // 计算字符串相似度
    private static final double _compareText(String str1, String str2) {
        int n = str1.length(), m = str2.length();
        if (n == 0)
            return m;
        if (m == 0)
            return n;

        int[][] Matrix = new int[n + 1][m + 1];

        // 初始化第一列
        for (int i = 0; i <= n; i++)
            Matrix[i][0] = i;

        // 初始化第一行
        for (int j = 0; j <= m; j++)
            Matrix[0][j] = j;

        for (int i = 1; i <= n; i++) {
            char ch1 = str1.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                char ch2 = str2.charAt(j - 1);
                int temp = ch1 == ch2 ? 0 : 1;
                Matrix[i][j] = Math.min(
                        Math.min(Matrix[i - 1][j] + 1, Matrix[i][j - 1] + 1),
                        Matrix[i - 1][j - 1] + temp);
            }
        }

        // for (int i = 0; i <= n; i++) {
        // for (int j = 0; j <= m; j++)
        // System.out.printf(" %d ", Matrix[i][j]);
        // System.out.println();
        // }

        return 1. - (double) Matrix[n][m] / Math.max(m, n);
    }

    public static final double compareChar(char c1, char c2) {
        if (c1 == c2)
            return 1;

        if (c1 >= sCharSmYmTone.length || c2 >= sCharSmYmTone.length)
            return 0;

        short SmYmTone = sCharSmYmTone[c1];
        if (SmYmTone < 0)
            return 0;
        int sm1 = SmYmTone >> 10;
        int ym1 = (SmYmTone >> 4) & 0x003F;
        int tone1 = SmYmTone & 0x000F;

        SmYmTone = sCharSmYmTone[c2];
        if (SmYmTone < 0)
            return 0;
        int sm2 = SmYmTone >> 10;
        int ym2 = (SmYmTone >> 4) & 0x003F;
        int tone2 = SmYmTone & 0x000F;

        return (double) (sSmDistance[sm1][sm2] + sYmDistance[ym1][ym2] + sToneDistance[tone1][tone2])
                / TOTAL_WEIGHT;
    }

    public static final short normailizeChar(char c) {
        // System.out.printf("c=%c\n", c);
        short SmYmTone = sCharSmYmTone[c];
        if (SmYmTone < 0)
            return -1;
        int sm = SmYmTone >> 10;
        int ym = (SmYmTone >> 4) & 0x003F;
        // int tone = SmYmTone & 0x000F;
        // System.out.printf("sm=%s, ym=%s, tone=%d\n", SMs[sm], YMs[ym], tone);
        if (SMs[sm].equals("f") && sYMtoIndex.containsKey('u' + YMs[ym])) {
            sm = sSMtoIndex.get("h");
            ym = sYMtoIndex.get('u' + YMs[ym]);
        } else
            sm = sNormalizedSMs[sm];
        ym = sNormalizedYMs[ym];
        // System.out.printf("Normalized sm=%s, ym=%s\n", SMs[sm], YMs[ym]);
        return (short) ((sm << 10) | (ym << 4));
    }

    public static final int getSM(short normChar) {
        return normChar >> 10;
    }

    public static final int getYM(short normChar) {
        return (normChar >> 4) & 0x3F;
    }
}
