package com.mocretion.createnuclearized.content.reactor;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RateFormat {

    private static final int DECIMALS = 4;

    private RateFormat() {}

    public static String format(double rate) {
        BigDecimal rounded = BigDecimal.valueOf(rate).setScale(DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.signum() == 0 ? "0" : rounded.toPlainString();
    }

    public static String signed(double rate) {
        String text = format(rate);
        return rate > 0 && !text.equals("0") ? "+" + text : text;
    }
}
