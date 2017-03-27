package pingbu.common;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Pinyin {

    private static final int TOTAL_WEIGHT = 100;
    private static final byte SM_WEIGHT = 30;
    private static final byte YM_WEIGHT = 50;
    private static final byte TONE_WEIGHT = 10;

    private static final String[] sSmPrefix = "b,p,m,f,d,t,n,l,g,k,h,j,q,x,zh,ch,sh,r,z,c,s,y,w".split(",");
    private static final String[] SMs = ",b,p,m,f,d,t,n,l,g,k,h,j,q,x,z,c,s,zh,ch,sh,r".split(",");
    private static final String[] YMs = "a,ai,an,ang,ao,e,ei,en,eng,er,i,ia,ian,iang,iao,ie,in,ing,io,iong,iou,o,ong,ou,u,ua,uai,uan,uang,ue,uei,uen,ueng,uo,uong,v,van,ve,ven,I,E,m,n,ng"
            .split(",");
    private static final int TONE_COUNT = 5;

    private static Map<String, Byte> sSMtoIndex, sYMtoIndex;
    private static byte[] sNormalizedSMs, sNormalizedYMs;
    private static short[] sCharSmYmTone;
    private static byte[][] sSmDistance, sYmDistance, sToneDistance;

    public static synchronized void init(String path) {
        sSMtoIndex = new HashMap<String, Byte>();
        for (byte i = 0; i < SMs.length; ++i)
            sSMtoIndex.put(SMs[i], i);

        sYMtoIndex = new HashMap<String, Byte>();
        for (byte i = 0; i < YMs.length; ++i)
            sYMtoIndex.put(YMs[i], i);

        sNormalizedSMs = new byte[SMs.length];
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

        sNormalizedYMs = new byte[YMs.length];
        for (byte i = 0; i < sNormalizedYMs.length; ++i)
            sNormalizedYMs[i] = i;
        sNormalizedYMs[sYMtoIndex.get("ang")] = sYMtoIndex.get("an");
        sNormalizedYMs[sYMtoIndex.get("eng")] = sYMtoIndex.get("en");
        sNormalizedYMs[sYMtoIndex.get("ing")] = sYMtoIndex.get("in");
        sNormalizedYMs[sYMtoIndex.get("n")] = sYMtoIndex.get("en");
        sNormalizedYMs[sYMtoIndex.get("m")] = sYMtoIndex.get("en");
        sNormalizedYMs[sYMtoIndex.get("I")] = sYMtoIndex.get("i");
        sNormalizedYMs[sYMtoIndex.get("E")] = sYMtoIndex.get("ai");

        sCharSmYmTone = new short[65536];
        for (int i = 0; i < sCharSmYmTone.length; ++i)
            sCharSmYmTone[i] = -1;
        sCharSmYmTone['0'] = pinyinToSmYmTone("ling");
        sCharSmYmTone['1'] = pinyinToSmYmTone("yi");
        sCharSmYmTone['2'] = pinyinToSmYmTone("er");
        sCharSmYmTone['3'] = pinyinToSmYmTone("san");
        sCharSmYmTone['4'] = pinyinToSmYmTone("si");
        sCharSmYmTone['5'] = pinyinToSmYmTone("wu");
        sCharSmYmTone['6'] = pinyinToSmYmTone("liu");
        sCharSmYmTone['7'] = pinyinToSmYmTone("qi");
        sCharSmYmTone['8'] = pinyinToSmYmTone("ba");
        sCharSmYmTone['9'] = pinyinToSmYmTone("jiu");
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

        try {
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
                        String pinyin = p[2];
                        int tone = 0;
                        // System.out.printf("%d: %s\n", u, pinyin);
                        for (int i = 0; i < 8 * 4; ++i) {
                            char c = "āáǎàōóǒòēéěèīíǐìūúǔù_ǘǚǜ_ḿ___ńňǹ".charAt(i);
                            int a = pinyin.indexOf(c);
                            if (a >= 0) {
                                pinyin = pinyin.substring(0, a) + "aoeiuümn".charAt(i / 4)
                                        + pinyin.substring(a + 1);
                                tone = (i % 4) + 1;
                                break;
                            }
                        }
                        pinyin = pinyin.replace('ü', 'v');
                        short SmYmTone = pinyinToSmYmTone(pinyin);
                        if (u < sCharSmYmTone.length)
                            sCharSmYmTone[u] = (short) (SmYmTone | tone);
                    }
                }
            }
            r.close();
            in.close();
            f.close();

            // for (int i = 0; i < PINYINs.length; ++i)
            // System.out.printf("%s\t%s\t%s\n", PINYINs[i], SMs[sPinyin2SmYm[i]
            // >> 16], YMs[sPinyin2SmYm[i] & 0x0000FFFF]);

            sSmDistance = new byte[SMs.length][SMs.length];
            for (int i = 0; i < SMs.length; ++i)
                for (int j = 0; j < SMs.length; ++j)
                    if (i < j) {
                        if (SMs[i].isEmpty())
                            sSmDistance[i][j] = SMs[j].isEmpty() ? SM_WEIGHT : 0;
                        else if (",z-zh,c-ch,s-sh,".contains(',' + SMs[i] + '-' + SMs[j] + ','))
                            sSmDistance[i][j] = (byte) (SM_WEIGHT * .9);
                        else if (",b-p,f-h,d-t,n-l,g-k,j-z,j-zh,q-c,q-ch,x-s,x-sh,".contains(',' + SMs[i]
                                + '-' + SMs[j] + ','))
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

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static short pinyinToSmYmTone(String pinyin) {
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
                Matrix[i][j] = Math.min(Math.min(Matrix[i - 1][j] + 1, Matrix[i][j - 1] + 1),
                        Matrix[i - 1][j - 1] + temp);
            }
        }

        // for (int i = 0; i <= n; i++) {
        // for (int j = 0; j <= m; j++)
        // System.out.printf(" %d ", Matrix[i][j]);
        // System.out.println();
        // }

        return 1 - (float) Matrix[n][m] / Math.max(m, n);
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

        return (double) (sSmDistance[sm1][sm2] + sYmDistance[ym1][ym2] + sToneDistance[tone1][tone2])
                / TOTAL_WEIGHT;
    }

    public static short normailizeChar(char c) {
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
}
