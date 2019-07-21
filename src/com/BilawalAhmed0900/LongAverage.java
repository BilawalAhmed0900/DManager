package com.BilawalAhmed0900;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.LongStream;

/*
    Average for speed. We don't want average speed for entire downloading.
    Instantaneous speed is sloppy. So, average for 16 or so instantaneous speed gives us a good average which can
    be relied upon
 */
public class LongAverage
{
    private int current;
    private int addedTill;
    private long[] values;

    public LongAverage(int numOfValues)
    {
        current = 0;
        addedTill = 0;
        values = new long[numOfValues];
    }

    public LongAverage()
    {
        this(16);
    }

    public void put(long newValue)
    {
        values[current++] = newValue;
        if (addedTill < values.length)
        {
            addedTill++;
        }

        if (current >= values.length)
        {
            current = 0;
        }
    }

    public BigDecimal average(RoundingMode roundingMode)
    {
        if (addedTill == 0)
            return BigDecimal.ZERO;

        BigDecimal bigDecimal = BigDecimal.ZERO;
        for (long e: values)
        {
            bigDecimal = bigDecimal.add(BigDecimal.valueOf(e));
        }

        return bigDecimal.divide(BigDecimal.valueOf(addedTill), roundingMode);
    }

    public BigDecimal average()
    {
        return average(RoundingMode.HALF_EVEN);
    }

    public double averageDouble()
    {
        if (addedTill == 0)
            return 0.0;

        return (double)LongStream.of(values).sum() / (double)addedTill;
    }

    public long averageLong()
    {
        return Math.round(this.averageDouble());
    }
}
