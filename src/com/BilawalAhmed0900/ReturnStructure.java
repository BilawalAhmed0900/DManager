package com.BilawalAhmed0900;

enum ReturnCode
{
    OK, CANCELLED
}

public class ReturnStructure
{
    ReturnCode code;
    String path;

    public ReturnStructure()
    {
        code = ReturnCode.CANCELLED;
        path = "";
    }

    @Override public String toString()
    {
        return "ReturnStructure{" +
                "code=" + code +
                ", path='" + path + '\'' +
                '}';
    }
}
