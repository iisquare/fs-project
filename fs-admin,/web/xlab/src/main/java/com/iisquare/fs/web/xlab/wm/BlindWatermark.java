package com.iisquare.fs.web.xlab.wm;

/**
 * 在图片上加隐藏的盲水印
 * @see(https://github.com/ww23/BlindWatermark)
 * Encode: 原图 --- 变换 ---> 变换域 + 水印 --- 逆变换 ---> 带水印图
 * Decode: 带水印图 --- 变换 ---> 变换域
 * Usage: java -jar BlindWatermark.jar <commands>
 *     commands:
 *         encode <option> <original image> <watermark> <embedded image>
 *         decode <option> <original image> <embedded image>
 *     encode options:
 *         -c discrete cosine transform
 *         -f discrete fourier transform (Deprecated)
 *         -i image watermark
 *         -t text  watermark
 *     decode options:
 *         -c discrete cosine transform
 *         -f discrete fourier transform (Deprecated)
 *     example:
 *         encode -ct input.png watermark output.png
 *         decode -c  input.png output.png
 */
public class BlindWatermark {
    private static final String FOURIER = "f";
    private static final String COSINE = "c";
    private static final String IMAGE = "i";
    private static final String TEXT = "t";

    public static void main(String[] args) {

        if (args.length < 4) {
            help();
        }

        Converter converter = null;
        String option = args[1].substring(1);

        if (option.contains(FOURIER)) {
            converter = new DFTConverter();
        } else if (option.contains(COSINE)) {
            converter = new DCTConverter();
        } else {
            help();
        }

        switch (args[0]) {
            case "encode":
                Encoder encoder = null;
                if (option.contains(IMAGE)) {
                    encoder = new ImageEncoder(converter);
                } else if (option.contains(TEXT)) {
                    encoder = new TextEncoder(converter);
                } else {
                    help();
                }
                assert encoder != null;
                encoder.encode(args[2], args[3], args[4]);
                break;
            case "decode":
                Decoder decoder = new Decoder(converter);
                decoder.decode(args[2], args[3]);
                break;
            default:
                help();
        }
    }

    private static void help() {
        System.out.println("Usage: java -jar BlindWatermark.jar <commands>\n" +
                "   commands: \n" +
                "       encode <option> <original image> <watermark> <embedded image>\n" +
                "       decode <option> <original image> <embedded image>\n" +
                "   encode options: \n" +
                "       -c discrete cosine transform\n" +
                "       -f discrete fourier transform (Deprecated)\n" +
                "       -i image watermark\n" +
                "       -t text  watermark\n" +
                "   decode options: \n" +
                "       -c discrete cosine transform\n" +
                "       -f discrete fourier transform (Deprecated)\n" +
                "   example: \n" +
                "       encode -ct foo.png test bar.png" +
                "       decode -c  foo.png bar.png"
        );
        System.exit(-1);
    }
}
