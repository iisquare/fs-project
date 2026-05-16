package com.iisquare.fs.web.xlab.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 上古代码，仅用于旧项目测试使用
 * JavaScript@see(https://github.com/cookiebook/chatRobot)
 * Java@see(https://gist.github.com/kba977/96409e0577600e7306d462c58135a347)
 */
public class RSAUtils {
    char[] hexatrigesimalToChar = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'};
    char[] hexToChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    int[] highBitMasks = {0x0000, 0x8000, 0xC000, 0xE000, 0xF000, 0xF800, 0xFC00, 0xFE00,
            0xFF00, 0xFF80, 0xFFC0, 0xFFE0, 0xFFF0, 0xFFF8, 0xFFFC, 0xFFFE, 0xFFFF};
    int[] lowBitMasks = {0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F,
            0x00FF, 0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF};
    final BigInt bigZero;
    final BigInt bigOne;
    int biRadixBits = 16;
    int bitsPerDigit = biRadixBits;
    int biRadix = 1 << 16; //65535
    int biHalfRadix = biRadix >>> 1;
    long biRadixSquared = (long) biRadix * biRadix;
    int maxDigitVal = biRadix - 1;
    RSAKeyPair keyPair;
    BarrettMu barrettMu;

    public RSAUtils(String _encryptionExponent, String _decryptionExponent, String _modulus) {
        bigZero = new BigInt();
        bigOne = new BigInt();
        bigOne.digits.set(0, 1);
        keyPair = new RSAKeyPair(_encryptionExponent, _decryptionExponent, _modulus);
        barrettMu = new BarrettMu(keyPair.modulus);
    }


    class BigInt {
        public List<Integer> digits;
        public boolean isNeg;

        public BigInt() {
            this.digits = new ArrayList<>(130);
            for (int i = 0; i < 130; i++) {
                digits.add(0);
            }
            this.isNeg = false;
        }

        public void biCopy(BigInt src) {
            Collections.copy(this.digits, src.digits);
            this.isNeg = src.isNeg;
        }
    }

    class RSAKeyPair {
        BigInt encryptionExponent;
        BigInt decryptionExponent;
        BigInt modulus;
        int chunkSize;
        int radix;
        BarrettMu barrett;

        public RSAKeyPair(String _encryptionExponent, String _decryptionExponent, String _modulus) {
            this.encryptionExponent = biFromHex(_encryptionExponent);
            this.decryptionExponent = biFromHex(_decryptionExponent);
            this.modulus = biFromHex(_modulus);
            this.chunkSize = 2 * biHighIndex(this.modulus);
            this.radix = 16;
            this.barrett = new BarrettMu(this.modulus);
        }
    }

    class BarrettMu {
        BigInt modulus;
        BigInt mu;
        BigInt bkplus1;
        int k;

        public BarrettMu(BigInt bi) {
            this.modulus = new BigInt();
            this.modulus.biCopy(bi);
            this.k = biHighIndex(this.modulus) + 1;
            BigInt b2k = new BigInt();
            b2k.digits.set(2 * this.k, 1);
            this.mu = biDivide(b2k, this.modulus);
            this.bkplus1 = new BigInt();
            this.bkplus1.digits.set(this.k + 1, 1);
        }

        public BigInt modulo(BigInt x) {
            BigInt q1 = biDivideByRadixPower(x, this.k - 1);
            BigInt q2 = biMultiply(q1, this.mu);
            BigInt q3 = biDivideByRadixPower(q2, this.k + 1);
            BigInt r1 = biModuloByRadixPower(x, this.k + 1);
            BigInt r2term = biMultiply(q3, this.modulus);
            BigInt r2 = biModuloByRadixPower(r2term, this.k + 1);
            BigInt r = biSubtract(r1, r2);
            if (r.isNeg) {
                r = biAdd(r, this.bkplus1);
            }
            boolean rgtem = biCompare(r, this.modulus) >= 0;
            while (rgtem) {
                r = biSubtract(r, this.modulus);
                rgtem = biCompare(r, this.modulus) >= 0;
            }
            return r;
        }

        public BigInt multiplyMod(BigInt x, BigInt y) {
            BigInt xy = biMultiply(x, y);
            return this.modulo(xy);
        }

        public BigInt powMod(BigInt x, BigInt y) {
            BigInt result = new BigInt();
            result.digits.set(0, 1);
            BigInt a = x;
            BigInt k = y;
            while (true) {
                if ((k.digits.get(0) & 1) != 0) {
                    result = this.multiplyMod(result, a);
                }
                k = biShiftRight(k, 1);
                if (k.digits.get(0) == 0 && biHighIndex(k) == 0) {
                    break;
                }
                a = this.multiplyMod(a, a);
            }
            return result;
        }
    }

    /**
     * list 对象 copy
     *
     * @param src
     * @param srcStart
     * @param dest
     * @param destStart
     * @param n
     */
    public void listCopy(List<Integer> src, int srcStart, List<Integer> dest, int destStart, int n) {
        int m = Math.min(srcStart + n, src.size());
        for (int i = srcStart, j = destStart; i < m; ++i, ++j) {
            dest.set(j, src.get(i));
        }
    }

    /**
     * BigInt 对象比较
     *
     * @param x
     * @param y
     * @return
     */
    public int biCompare(BigInt x, BigInt y) {
        if (x.isNeg != y.isNeg) {
            return (int) (1 - 2 * objectToNumber(x.isNeg));
        }
        for (int i = x.digits.size() - 1; i >= 0; --i) {
            if (x.digits.get(i).intValue() != y.digits.get(i).intValue()) {
                if (x.isNeg) {
                    return (int) (1 - 2 * objectToNumber(x.digits.get(i) > y.digits.get(i)));
                } else {
                    return (int) (1 - 2 * objectToNumber(x.digits.get(i) < y.digits.get(i)));
                }
            }
        }
        return 0;
    }

    /**
     * BigInt 左移n位
     *
     * @param x
     * @param n
     * @return
     */
    public BigInt biShiftLeft(BigInt x, int n) {
        int digitCount = (int) Math.floor((double) n / bitsPerDigit);
        BigInt result = new BigInt();
        listCopy(x.digits, 0, result.digits, digitCount, result.digits.size() - digitCount);
        int bits = n % bitsPerDigit;
        int rightBits = bitsPerDigit - bits;
        int i, i1;
        for (i = result.digits.size() - 1, i1 = i - 1; i > 0; --i, --i1) {
            result.digits.set(i, ((result.digits.get(i) << bits) & maxDigitVal) |
                    ((result.digits.get(i1) & highBitMasks[bits]) >>> rightBits));
        }
        result.digits.set(0, ((result.digits.get(i) << bits) & maxDigitVal));
        result.isNeg = x.isNeg;
        return result;
    }

    /**
     * BigInt 右移n位
     *
     * @param x
     * @param n
     * @return
     */
    public BigInt biShiftRight(BigInt x, int n) {
        int digitCount = (int) Math.floor((double) n / bitsPerDigit);
        BigInt result = new BigInt();
        listCopy(x.digits, digitCount, result.digits, 0, x.digits.size() - digitCount);
        int bits = n % bitsPerDigit;
        int leftBits = bitsPerDigit - bits;
        for (int i = 0, i1 = i + 1; i < result.digits.size() - 1; ++i, ++i1) {
            result.digits.set(i, (result.digits.get(i) >>> bits) |
                    ((result.digits.get(i1) & lowBitMasks[bits]) << leftBits));
        }
        result.digits.set(result.digits.size() - 1, result.digits.get(result.digits.size() - 1) >>> bits);
        result.isNeg = x.isNeg;
        return result;
    }

    /**
     * 16 -> BigInt
     *
     * @param hexString
     * @return
     */
    public BigInt biFromHex(String hexString) {
        if (hexString == null || hexString.length() % 2 != 0) {
            return null;
        }
        BigInt result = new BigInt();
        int length = hexString.length();
        for (int i = length, j = 0; i > 0; i -= 4, ++j) {
            result.digits.set(j, hexToDigit(hexString.substring(Math.max(i - 4, 0), Math.max(i - 4, 0) + Math.min(i,
                    4))));
        }
        return result;
    }

    /**
     * 16 -> digit
     *
     * @param hex
     * @return
     */
    public int hexToDigit(String hex) {
        int result = 0;
        int sl = Math.min(hex.length(), 4);
        for (int i = 0; i < sl; ++i) {
            result <<= 4;
            result |= charToHex((int) hex.charAt(i));
        }
        return result;
    }

    /**
     * @param c 数字 字母的ascii码
     * @return
     */
    public int charToHex(int c) {
        int ZERO = 48;
        int NINE = ZERO + 9;
        int littleA = 97;
        int littleZ = littleA + 25;
        int bigA = 65;
        int bigZ = bigA + 25;
        int result;
        if (c >= ZERO && c <= NINE) {
            result = c - ZERO;
        } else if (c >= bigA && c <= bigZ) {
            result = 10 + c - bigA;
        } else if (c >= littleA && c <= littleZ) {
            result = 10 + c - littleA;
        } else {
            result = 0;
        }
        return result;
    }

    /**
     * bi -> 16
     *
     * @param bi
     * @return
     */
    public String biToHex(BigInt bi) {
        String result = "";
        for (int i = biHighIndex(bi); i > -1; --i) {
            result += digitToHex(bi.digits.get(i));
        }
        return result;
    }

    /**
     * 字符串反转
     *
     * @param c
     * @return
     */
    public String reverseStr(CharSequence c) {
        String result = "";
        for (int i = c.length() - 1; i > -1; --i) {
            result += c.charAt(i);
        }
        return result;
    }

    public String biToString(BigInt bi, int radix) {
        BigInt b = new BigInt();
        b.digits.set(0, radix);
        BigInt[] qr = biDivideModulo(bi, b);
        String result = String.valueOf(hexatrigesimalToChar[qr[1].digits.get(0)]);
        while (biCompare(qr[0], bigZero) == 1) {
            qr = biDivideModulo(qr[0], b);
            result += hexatrigesimalToChar[qr[1].digits.get(0)];
        }
        return (bi.isNeg ? "-" : "") + reverseStr(result);
    }

    public int biNumBits(BigInt x) {
        int n = biHighIndex(x);
        int d = x.digits.get(n);
        int m = (n + 1) * bitsPerDigit;
        int result;
        for (result = m; result > m - bitsPerDigit; --result) {
            if ((d & 0x8000) != 0) {
                break;
            }
            d <<= 1;
        }
        return result;
    }

    public BigInt biMultiplyByRadixPower(BigInt x, int n) {
        BigInt result = new BigInt();
        listCopy(x.digits, 0, result.digits, n, result.digits.size() - n);
        return result;
    }

    public BigInt biDivideByRadixPower(BigInt x, int n) {
        BigInt result = new BigInt();
        listCopy(x.digits, n, result.digits, 0, result.digits.size() - n);
        return result;
    }

    public BigInt biModuloByRadixPower(BigInt x, int n) {
        BigInt result = new BigInt();
        listCopy(x.digits, 0, result.digits, 0, n);
        return result;
    }

    public BigInt biMultiplyDigit(BigInt x, int y) {
        int n, c, uv;
        BigInt result = new BigInt();
        n = biHighIndex(x);
        c = 0;
        for (int j = 0; j <= n; ++j) {
            uv = result.digits.get(j) + x.digits.get(j) * y + c;
            result.digits.set(j, uv & maxDigitVal);
            c = uv >>> biRadixBits;
        }
        result.digits.set(1 + n, c);
        return result;
    }

    public BigInt[] biDivideModulo(BigInt x, BigInt y) {
        int nb = biNumBits(x);
        int tb = biNumBits(y);
        boolean origYIsNeg = y.isNeg;
        BigInt[] result = new BigInt[2];
        BigInt q = new BigInt();
        BigInt r = new BigInt();
        if (nb < tb) {
            if (x.isNeg) {
                q.biCopy(bigOne);
                q.isNeg = !y.isNeg;
                x.isNeg = false;
                y.isNeg = false;
                r = biSubtract(y, x);
                x.isNeg = true;
                y.isNeg = origYIsNeg;
            } else {
                r.biCopy(x);
            }
            result[0] = q;
            result[1] = r;
            return result;
        }
        q = new BigInt();
        r = x;
        int t = (int) Math.ceil((double) tb / bitsPerDigit) - 1;
        int lambda = 0;
        while (y.digits.get(t) < biHalfRadix) {
            y = biShiftLeft(y, 1);
            ++lambda;
            ++tb;
            t = (int) Math.ceil((double) tb / bitsPerDigit) - 1;
        }
        r = biShiftLeft(r, lambda);
        nb += lambda;
        int n = (int) (Math.ceil((double) nb / bitsPerDigit) - 1);

        BigInt b = biMultiplyByRadixPower(y, n - t);
        while (biCompare(r, b) != -1) {
            q.digits.set(n - t, q.digits.get(n - t) + 1);
            r = biSubtract(r, b);
        }
        for (int i = n; i > t; --i) {
            long ri = (i >= r.digits.size()) ? 0 : r.digits.get(i);
            long ri1 = (i - 1 >= r.digits.size()) ? 0 : r.digits.get(i - 1);
            long ri2 = (i - 2 >= r.digits.size()) ? 0 : r.digits.get(i - 2);
            long yt = (t >= y.digits.size()) ? 0 : y.digits.get(t);
            long yt1 = (t - 1 >= y.digits.size()) ? 0 : y.digits.get(t - 1);
            if (ri == yt) {
                q.digits.set(i - t - 1, maxDigitVal);
            } else {
                q.digits.set(i - t - 1, (int) Math.floor((double) (ri * biRadix + ri1) / yt));
            }
            long c1 = q.digits.get(i - t - 1) * (yt * biRadix + yt1);
            long c2 = ri * biRadixSquared + ri1 * biRadix + ri2;
            while (c1 > c2) {
                q.digits.set(i - t - 1, q.digits.get(i - t - 1) - 1);
                c1 = q.digits.get(i - t - 1) * ((yt * biRadix) | yt1);
                c2 = (ri * biRadix * biRadix) + ((ri1 * biRadix) + ri2);
            }
            b = biMultiplyByRadixPower(y, i - t - 1);
            r = biSubtract(r, biMultiplyDigit(b, q.digits.get(i - t - 1)));
            if (r.isNeg) {
                r = biAdd(r, b);
                q.digits.set(i - t - 1, q.digits.get(i - t - 1) - 1);
            }
        }
        r = biShiftRight(r, lambda);
        q.isNeg = x.isNeg != origYIsNeg;
        if (x.isNeg) {
            if (origYIsNeg) {
                q = biAdd(q, bigOne);
            } else {
                q = biSubtract(q, bigOne);
            }
            y = biShiftRight(y, lambda);
            r = biSubtract(y, r);
        }
        if (r.digits.get(0) == 0 && biHighIndex(r) == 0) {
            r.isNeg = false;
        }
        result[0] = q;
        result[1] = r;
        return result;
    }

    public BigInt biAdd(BigInt x, BigInt y) {
        BigInt result;

        if (x.isNeg != y.isNeg) {
            y.isNeg = !y.isNeg;
            result = biSubtract(x, y);
            y.isNeg = !y.isNeg;
        } else {
            result = new BigInt();
            long c = 0;
            long n;
            for (int i = 0; i < x.digits.size(); ++i) {
                n = x.digits.get(i) + y.digits.get(i) + c;
                result.digits.set(i, (int) (n % biRadix));
                c = objectToNumber(n >= biRadix);
            }
            result.isNeg = x.isNeg;
        }
        return result;
    }

    public long objectToNumber(Object obj) {
        if (obj instanceof String) {
            return Long.valueOf((String) obj);//???
        } else if (obj instanceof Boolean) {
            return ((boolean) obj) ? 1 : 0;
        } else if (obj instanceof Date) {
            return System.currentTimeMillis() - (new Date().getTime() - ((Date) obj).getTime());
        } else {
            throw new RuntimeException("NAN Exception");
        }
    }

    public BigInt biSubtract(BigInt x, BigInt y) {
        BigInt result;
        if (x.isNeg != y.isNeg) {
            y.isNeg = !y.isNeg;
            result = biAdd(x, y);
            y.isNeg = !y.isNeg;
        } else {
            result = new BigInt();
            long n, c;
            c = 0;
            for (int i = 0; i < x.digits.size(); ++i) {
                n = x.digits.get(i) - y.digits.get(i) + c;
                result.digits.set(i, (int) (n % biRadix));
                if (result.digits.get(i) < 0) {
                    result.digits.set(i, result.digits.get(i) + biRadix);
                }
                c = 0 - objectToNumber(n < 0);
            }
            if (c == -1) {
                c = 0;
                for (int i = 0; i < x.digits.size(); ++i) {
                    n = 0 - result.digits.get(i) + c;
                    result.digits.set(i, (int) (n % biRadix));
                    if (result.digits.get(i) < 0) {
                        result.digits.set(i, result.digits.get(i) + biRadix);
                    }
                    c = 0 - objectToNumber(n < 0);
                }
                result.isNeg = !x.isNeg;
            } else {
                result.isNeg = x.isNeg;
            }
        }
        return result;
    }

    public BigInt biDivide(BigInt x, BigInt y) {
        return biDivideModulo(x, y)[0];
    }

    @Deprecated
    public BigInt biModulo(BigInt x, BigInt y) {
        return biDivideModulo(x, y)[1];
    }

    @Deprecated
    public BigInt biMultiplyMod(BigInt x, BigInt y, BigInt m) {
        return biModulo(biMultiply(x, y), m);
    }


    /**
     * digit -> 16
     *
     * @param digit
     * @return
     */
    public String digitToHex(int digit) {
        int mask = 0xf;
        String result = "";
        for (int i = 0; i < 4; ++i) {
            result += hexToChar[digit & mask];
            digit >>>= 4;
        }
        return reverseStr(result);
    }

    public int biHighIndex(BigInt bi) {
        int result = bi.digits.size() - 1;
        while (result > 0 && bi.digits.get(result) == 0) {
            --result;
        }
        return result;
    }

    public BigInt biMultiply(BigInt x, BigInt y) {
        BigInt result = new BigInt();
        int c;
        int n = biHighIndex(x);
        int t = biHighIndex(y);
        int uv, k;

        for (int i = 0; i <= t; ++i) {
            c = 0;
            k = i;
            for (int j = 0; j <= n; ++j, ++k) {
                uv = result.digits.get(k) + x.digits.get(j) * y.digits.get(i) + c;
                result.digits.set(k, uv & maxDigitVal);
                c = uv >>> biRadixBits;
            }
            result.digits.set(i + n + 1, c);
        }
        result.isNeg = x.isNeg != y.isNeg;
        return result;
    }

    public String encryptedString(String data) {
        List<Integer> list = new ArrayList<>(130);
        int i = 0;
        while (i < data.length()) {
            list.add((int) data.charAt(i++));
        }
        while (list.size() % keyPair.chunkSize != 0) {
            list.add(0);
        }

        int length = list.size();
        String result = "";
        int j, k;
        BigInt block;
        for (i = 0; i < length; i += keyPair.chunkSize) {
            block = new BigInt();
            j = 0;
            for (k = i; k < i + keyPair.chunkSize; ++j) {
                Integer value = list.get(k++);
                value += list.get(k++) << 8;
                block.digits.set(j, value);
            }
            BigInt crypt = barrettMu.powMod(block, keyPair.encryptionExponent);
            String text = keyPair.radix == 16 ? biToHex(crypt) : biToString(crypt, keyPair.radix);
            result += text + " ";
        }
        return result.substring(0, result.length() - 1);
    }

}
