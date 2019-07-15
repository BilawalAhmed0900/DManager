package com.BilawalAhmed0900;

import java.util.Arrays;

/*
    http://dotwhat.net/type/video-movie-files
 */
public class IsVideo
{
    public static boolean isVideo(String extractedFileName)
    {
        String[] extensions =
                {
                        "avi",
                        "mpg",
                        "flv",
                        "mov",
                        "wmv",
                        "264",
                        "3g2",
                        "3gp",
                        "3mm",
                        "3p2",
                        "60d",
                        "aaf",
                        "aec",
                        "aep",
                        "aepx",
                        "ajp",
                        "am4",
                        "amv",
                        "arf",
                        "arv",
                        "asd",
                        "asf",
                        "asx",
                        "avb",
                        "avd",
                        "avi",
                        "avp",
                        "avs",
                        "avs",
                        "ax",
                        "axm",
                        "bdmv",
                        "bik",
                        "bix",
                        "box",
                        "bpj",
                        "bup",
                        "camrec",
                        "cine",
                        "cpi",
                        "cvc",
                        "d2v",
                        "d3v",
                        "dav",
                        "dce",
                        "ddat",
                        "divx",
                        "dkd",
                        "dlx",
                        "dmb",
                        "dpg",
                        "dream",
                        "dsm",
                        "dv",
                        "dv2",
                        "dvm",
                        "dvr",
                        "dvx",
                        "dxr",
                        "edl",
                        "enc",
                        "evo",
                        "f4v",
                        "fbr",
                        "fbz",
                        "fcp",
                        "fcproject",
                        "flc",
                        "fli",
                        "flv",
                        "gts",
                        "gvi",
                        "gvp",
                        "h3r",
                        "hdmov",
                        "ifo",
                        "imovieproj",
                        "imovieproject",
                        "ircp",
                        "irf",
                        "irf",
                        "ivr",
                        "ivs",
                        "izz",
                        "izzy",
                        "m1pg",
                        "m21",
                        "m21",
                        "m2p",
                        "m2t",
                        "m2ts",
                        "m2v",
                        "m4e",
                        "m4u",
                        "m4v",
                        "mbf",
                        "mbt",
                        "mbv",
                        "mj2",
                        "mjp",
                        "mk3d",
                        "mkv",
                        "mnv",
                        "mocha",
                        "mod",
                        "moff",
                        "moi",
                        "mov",
                        "mp21",
                        "mp21",
                        "mp4",
                        "mp4v",
                        "mpeg",
                        "mpg",
                        "mpg2",
                        "mqv",
                        "msdvd",
                        "mswmm",
                        "mts",
                        "mtv",
                        "mvb",
                        "mvp",
                        "mxf",
                        "mzt",
                        "nsv",
                        "ogv",
                        "ogx",
                        "pds",
                        "pgi",
                        "piv",
                        "plb",
                        "pmf",
                        "pns",
                        "ppj",
                        "prproj",
                        "prtl",
                        "psh",
                        "pvr",
                        "pxv",
                        "qt",
                        "qtl",
                        "r3d",
                        "ratdvd",
                        "rm",
                        "rms",
                        "rmvb",
                        "roq",
                        "rpf",
                        "rpl",
                        "rum",
                        "rv",
                        "sdv",
                        "sfvidcap",
                        "slc",
                        "smk",
                        "spl",
                        "sqz",
                        "sub",
                        "svi",
                        "swf",
                        "tda3mt",
                        "thm",
                        "tivo",
                        "tod",
                        "tp0",
                        "trp",
                        "ts",
                        "udp",
                        "usm",
                        "vcr",
                        "veg",
                        "vft",
                        "vgz",
                        "viewlet",
                        "vlab",
                        "vmb",
                        "vob",
                        "vp6",
                        "vp7",
                        "vro",
                        "vsp",
                        "vvf",
                        "wd1",
                        "webm",
                        "wlmp",
                        "wmmp",
                        "wmv",
                        "wp3",
                        "wtv",
                        "xfl",
                        "xvid",
                        "zm1",
                        "zm2",
                        "zm3",
                        "zmv"
                };
        String lowerCased = extractedFileName.toLowerCase();
        return Arrays.stream(extensions).anyMatch(lowerCased::endsWith);
    }
}
