package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data;

public final class DyeColors {
    public static String colorById(int id) {
        switch (id) {
            case 1:
                return "orange";
            case 2:
                return "magenta";
            case 3:
                return "light_blue";
            case 4:
                return "yellow";
            case 5:
                return "lime";
            case 6:
                return "pink";
            case 7:
                return "gray";
            case 8:
                return "light_gray";
            case 9:
                return "cyan";
            case 10:
                return "purple";
            case 11:
                return "blue";
            case 12:
                return "brown";
            case 13:
                return "green";
            case 14:
                return "red";
            case 15:
                return "black";
            default:
                return "white";
        }
    }
}
