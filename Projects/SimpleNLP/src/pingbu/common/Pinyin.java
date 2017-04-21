package pingbu.common;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Pinyin {

    private static final int CHAR_COUNT = 65536;
    private static final int TOTAL_WEIGHT = 100;

    private static short[] sCharSmYmTone, sNormalizedChar;
    private static byte[][] sSmDistance, sYmDistance, sToneDistance;

    public static synchronized void init(String path) throws IOException {
        String f = path + "/Pinyin.cache";
        if (!new File(f).exists()) {
            new Builder().build(path);
            save(f);
        } else {
            load(f);
        }
    }

    private static class Builder {
        private static final byte SM_WEIGHT = 30;
        private static final byte YM_WEIGHT = 50;
        private static final byte TONE_WEIGHT = 10;

        private final String[] mSmPrefix = "b,p,m,f,d,t,n,l,g,k,h,j,q,x,zh,ch,sh,r,z,c,s,y,w".split(",");
        private final String[] SMs = ",b,p,m,f,d,t,n,l,g,k,h,j,q,x,z,c,s,zh,ch,sh,r".split(",");
        private final String[] YMs = "a,ai,an,ang,ao,e,ei,en,eng,er,i,ia,ian,iang,iao,ie,in,ing,io,iong,iou,o,ong,ou,u,ua,uai,uan,uang,ue,uei,uen,ueng,uo,uong,v,van,ve,ven,I,E,m,n,ng"
                .split(",");
        private static final int TONE_COUNT = 5;

        private Map<String, Byte> mSMtoIndex, mYMtoIndex;
        private byte[] mNormalizedSMs, mNormalizedYMs;

        private void build(String path) throws IOException {
            mSMtoIndex = new HashMap<String, Byte>();
            for (byte i = 0; i < SMs.length; ++i)
                mSMtoIndex.put(SMs[i], i);

            mYMtoIndex = new HashMap<String, Byte>();
            for (byte i = 0; i < YMs.length; ++i)
                mYMtoIndex.put(YMs[i], i);

            mNormalizedSMs = new byte[SMs.length];
            for (byte i = 0; i < mNormalizedSMs.length; ++i)
                mNormalizedSMs[i] = i;
            mNormalizedSMs[mSMtoIndex.get("b")] = mSMtoIndex.get("p");
            mNormalizedSMs[mSMtoIndex.get("d")] = mSMtoIndex.get("t");
            mNormalizedSMs[mSMtoIndex.get("h")] = mSMtoIndex.get("f");
            mNormalizedSMs[mSMtoIndex.get("n")] = mSMtoIndex.get("l");
            mNormalizedSMs[mSMtoIndex.get("j")] = mSMtoIndex.get("z");
            mNormalizedSMs[mSMtoIndex.get("q")] = mSMtoIndex.get("c");
            mNormalizedSMs[mSMtoIndex.get("x")] = mSMtoIndex.get("s");
            mNormalizedSMs[mSMtoIndex.get("zh")] = mSMtoIndex.get("z");
            mNormalizedSMs[mSMtoIndex.get("ch")] = mSMtoIndex.get("c");
            mNormalizedSMs[mSMtoIndex.get("sh")] = mSMtoIndex.get("s");

            mNormalizedYMs = new byte[YMs.length];
            for (byte i = 0; i < mNormalizedYMs.length; ++i)
                mNormalizedYMs[i] = i;
            mNormalizedYMs[mYMtoIndex.get("ang")] = mYMtoIndex.get("an");
            mNormalizedYMs[mYMtoIndex.get("eng")] = mYMtoIndex.get("en");
            mNormalizedYMs[mYMtoIndex.get("ing")] = mYMtoIndex.get("in");
            mNormalizedYMs[mYMtoIndex.get("n")] = mYMtoIndex.get("en");
            mNormalizedYMs[mYMtoIndex.get("m")] = mYMtoIndex.get("en");
            mNormalizedYMs[mYMtoIndex.get("I")] = mYMtoIndex.get("i");
            mNormalizedYMs[mYMtoIndex.get("E")] = mYMtoIndex.get("ai");

            sCharSmYmTone = new short[CHAR_COUNT];
            for (int i = 0; i < CHAR_COUNT; ++i)
                sCharSmYmTone[i] = -1;
            sCharSmYmTone['0'] = pinyinToSmYmTone("líng");
            sCharSmYmTone['1'] = pinyinToSmYmTone("yī");
            sCharSmYmTone['2'] = pinyinToSmYmTone("èr");
            sCharSmYmTone['3'] = pinyinToSmYmTone("sān");
            sCharSmYmTone['4'] = pinyinToSmYmTone("sì");
            sCharSmYmTone['5'] = pinyinToSmYmTone("wǔ");
            sCharSmYmTone['6'] = pinyinToSmYmTone("liù");
            sCharSmYmTone['7'] = pinyinToSmYmTone("qī");
            sCharSmYmTone['8'] = pinyinToSmYmTone("bā");
            sCharSmYmTone['9'] = pinyinToSmYmTone("jiǔ");
            sCharSmYmTone['A'] = sCharSmYmTone['a'] = pinyinToSmYmTone("ei");
            sCharSmYmTone['B'] = sCharSmYmTone['b'] = pinyinToSmYmTone("bi");
            sCharSmYmTone['C'] = sCharSmYmTone['c'] = pinyinToSmYmTone("sei");
            sCharSmYmTone['D'] = sCharSmYmTone['d'] = pinyinToSmYmTone("di");
            sCharSmYmTone['E'] = sCharSmYmTone['e'] = pinyinToSmYmTone("yi");
            sCharSmYmTone['G'] = sCharSmYmTone['g'] = pinyinToSmYmTone("ji");
            sCharSmYmTone['I'] = sCharSmYmTone['i'] = pinyinToSmYmTone("ai");
            sCharSmYmTone['J'] = sCharSmYmTone['j'] = pinyinToSmYmTone("jei");
            sCharSmYmTone['K'] = sCharSmYmTone['k'] = pinyinToSmYmTone("kei");
            sCharSmYmTone['N'] = sCharSmYmTone['n'] = pinyinToSmYmTone("en");
            sCharSmYmTone['O'] = sCharSmYmTone['o'] = pinyinToSmYmTone("ou");
            sCharSmYmTone['P'] = sCharSmYmTone['p'] = pinyinToSmYmTone("pi");
            sCharSmYmTone['Q'] = sCharSmYmTone['q'] = pinyinToSmYmTone("kiu");
            sCharSmYmTone['R'] = sCharSmYmTone['r'] = pinyinToSmYmTone("a");
            sCharSmYmTone['T'] = sCharSmYmTone['t'] = pinyinToSmYmTone("ti");
            sCharSmYmTone['U'] = sCharSmYmTone['u'] = pinyinToSmYmTone("you");
            sCharSmYmTone['V'] = sCharSmYmTone['v'] = pinyinToSmYmTone("wei");
            sCharSmYmTone['Y'] = sCharSmYmTone['y'] = pinyinToSmYmTone("wai");

            FileInputStream f = new FileInputStream(path + "/Unihan_Readings.txt");
            InputStreamReader in = new InputStreamReader(f, "UTF-8");
            BufferedReader r = new BufferedReader(in);
            for (;;) {
                String l = r.readLine();
                if (l == null)
                    break;
                if (l.startsWith("U+")) {
                    String[] p = l.split("\t");
                    if (p[1].equals("kMandarin")) {
                        int u = Integer.parseInt(p[0].substring(2), 16);
                        if (u < CHAR_COUNT) {
                            String pinyin = p[2];
                            // System.out.printf("%d: %s\n", u, pinyin);
                            short SmYmTone = pinyinToSmYmTone(pinyin);
                            sCharSmYmTone[u] = SmYmTone;
                        }
                    }
                }
            }
            r.close();
            in.close();
            f.close();

            // for (int i = 0; i < PINYINs.length; ++i)
            // System.out.printf("%s\t%s\t%s\n", PINYINs[i],
            // SMs[sPinyin2SmYm[i]
            // >> 16], YMs[sPinyin2SmYm[i] & 0x0000FFFF]);

            sNormalizedChar = new short[CHAR_COUNT];
            for (int i = 0; i < CHAR_COUNT; ++i) {
                short SmYmTone = sCharSmYmTone[i];
                if (SmYmTone < 0) {
                    sNormalizedChar[i] = -1;
                } else {
                    int sm = SmYmTone >> 10;
                    int ym = (SmYmTone >> 4) & 0x003F;
                    // int tone = SmYmTone & 0x000F;
                    // System.out.printf("sm=%s, ym=%s, tone=%d\n",
                    // SMs[sm], YMs[ym], tone);
                    if (SMs[sm].equals("f") && mYMtoIndex.containsKey('u' + YMs[ym])) {
                        sm = mSMtoIndex.get("h");
                        ym = mYMtoIndex.get('u' + YMs[ym]);
                    } else
                        sm = mNormalizedSMs[sm];
                    ym = mNormalizedYMs[ym];
                    // System.out.printf("Normalized sm=%s, ym=%s\n",
                    // SMs[sm], YMs[ym]);
                    sNormalizedChar[i] = (short) ((sm << 10) | (ym << 4));
                }
            }

            sSmDistance = new byte[SMs.length][SMs.length];
            for (int i = 0; i < SMs.length; ++i)
                for (int j = 0; j < SMs.length; ++j)
                    if (i < j) {
                        if (SMs[i].isEmpty())
                            sSmDistance[i][j] = SMs[j].isEmpty() ? SM_WEIGHT : 0;
                        else if (",z-zh,c-ch,s-sh,".contains(',' + SMs[i] + '-' + SMs[j] + ','))
                            sSmDistance[i][j] = (byte) (SM_WEIGHT * .9);
                        else if (",b-p,f-h,d-t,n-l,g-k,j-z,j-zh,q-c,q-ch,x-s,x-sh,".contains(',' + SMs[i] + '-'
                                + SMs[j] + ','))
                            sSmDistance[i][j] = (byte) (SM_WEIGHT * .8);
                        else
                            sSmDistance[i][j] = 0;
                        // System.out.printf("%s-%s: %d\n", SMs[i], SMs[j],
                        // sSmDistance[i][j]);
                    } else if (i == j)
                        sSmDistance[i][j] = SM_WEIGHT;
                    else
                        sSmDistance[i][j] = sSmDistance[j][i];

            sYmDistance = new byte[YMs.length][YMs.length];
            for (int i = 0; i < YMs.length; ++i)
                for (int j = 0; j < YMs.length; ++j)
                    if (i < j) {
                        if (YMs[i].equals("an") && YMs[j].equals("ang"))
                            sYmDistance[i][j] = (byte) (YM_WEIGHT * .8);
                        else if (YMs[j].equals(YMs[i] + 'g'))
                            sYmDistance[i][j] = (byte) (YM_WEIGHT * .9);
                        else
                            sYmDistance[i][j] = (byte) (YM_WEIGHT * compareText(YMs[i], YMs[j]));
                        // System.out.printf("%s-%s: %d\n", YMs[i], YMs[j],
                        // sYmDistance[i][j]);
                    } else if (i == j)
                        sYmDistance[i][j] = YM_WEIGHT;
                    else
                        sYmDistance[i][j] = sYmDistance[j][i];

            sToneDistance = new byte[TONE_COUNT][TONE_COUNT];
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
        }

        private short pinyinToSmYmTone(String pinyin) {
            int tone = 0;
            for (int i = 0; i < 8 * 4; ++i) {
                char c = "āáǎàōóǒòēéěèīíǐìūúǔù_ǘǚǜ_ḿ___ńňǹ".charAt(i);
                int a = pinyin.indexOf(c);
                if (a >= 0) {
                    pinyin = pinyin.substring(0, a) + "aoeiuümn".charAt(i / 4) + pinyin.substring(a + 1);
                    tone = (i % 4) + 1;
                    break;
                }
            }
            pinyin = pinyin.replace('ü', 'v');
            String sm = "", ym = pinyin;
            if (ym.startsWith("yu"))
                ym = 'v' + ym.substring(2);
            else if (ym.startsWith("y"))
                ym = 'i' + ym.substring(1);
            else if (pinyin.startsWith("w"))
                ym = 'u' + ym.substring(1);
            else if (!pinyin.equals("m") && !pinyin.equals("n"))
                for (int i = 0; i < mSmPrefix.length; ++i)
                    if (ym.startsWith(mSmPrefix[i])) {
                        sm = mSmPrefix[i];
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
            int smIndex = mSMtoIndex.get(sm), ymIndex = mYMtoIndex.get(ym);
            return (short) ((smIndex << 10) | (ymIndex << 4) | tone);
        }

        // 计算字符串相似度
        private static float compareText(String str1, String str2) {
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
                    Matrix[i][j] = Math.min(Math.min(Matrix[i - 1][j] + 1, Matrix[i][j - 1] + 1), Matrix[i - 1][j - 1]
                            + temp);
                }
            }

            // for (int i = 0; i <= n; i++) {
            // for (int j = 0; j <= m; j++)
            // System.out.printf(" %d ", Matrix[i][j]);
            // System.out.println();
            // }

            return 1 - (float) Matrix[n][m] / Math.max(m, n);
        }
    }

    private static void load(String path) throws IOException {
        FileInputStream f = new FileInputStream(path);
        DataInputStream in = new DataInputStream(f);
        sCharSmYmTone = new short[CHAR_COUNT];
        sNormalizedChar = new short[CHAR_COUNT];
        for (int i = 0; i < CHAR_COUNT; ++i) {
            sCharSmYmTone[i] = in.readShort();
            sNormalizedChar[i] = in.readShort();
        }
        int n = in.readByte();
        sSmDistance = new byte[n][n];
        for (int i = 0; i < n; ++i) {
            sSmDistance[i] = new byte[n];
            in.read(sSmDistance[i]);
        }
        n = in.readByte();
        sYmDistance = new byte[n][n];
        for (int i = 0; i < n; ++i) {
            sYmDistance[i] = new byte[n];
            in.read(sYmDistance[i]);
        }
        n = in.readByte();
        sToneDistance = new byte[n][n];
        for (int i = 0; i < n; ++i) {
            sToneDistance[i] = new byte[n];
            in.read(sToneDistance[i]);
        }
        in.close();
        f.close();
    }

    private static void save(String path) throws IOException {
        FileOutputStream f = new FileOutputStream(path);
        DataOutputStream out = new DataOutputStream(f);
        for (int i = 0; i < CHAR_COUNT; ++i) {
            out.writeShort(sCharSmYmTone[i]);
            out.writeShort(sNormalizedChar[i]);
        }
        int n = sSmDistance.length;
        out.writeByte(n);
        for (int i = 0; i < n; ++i)
            out.write(sSmDistance[i]);
        n = sYmDistance.length;
        out.writeByte(n);
        for (int i = 0; i < n; ++i)
            out.write(sYmDistance[i]);
        n = sToneDistance.length;
        out.writeByte(n);
        for (int i = 0; i < n; ++i)
            out.write(sToneDistance[i]);
        out.close();
        f.close();
    }

    public static double compareChar(char c1, char c2) {
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

        return (double) (sSmDistance[sm1][sm2] + sYmDistance[ym1][ym2] + sToneDistance[tone1][tone2]) / TOTAL_WEIGHT;
    }

    public static short normailizeChar(char c) {
        return c < CHAR_COUNT ? sNormalizedChar[c] : -1;
    }
}
