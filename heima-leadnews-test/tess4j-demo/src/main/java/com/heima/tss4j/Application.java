package com.heima.tss4j;


import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;

public class Application {
    /**
     * 识别图片中的文字
     * @param args
     */
    public static void main(String[] args) throws TesseractException {

        // 创建实例
        ITesseract tesseract = new Tesseract();

        // 设置字体库路径

        tesseract.setDatapath("D:\\code\\test2");

        //中文识别
        tesseract.setLanguage("chi_sim");

        //执行ocr识别
        String result = tesseract.doOCR(new File("D:\\test.jpg"));
        System.out.println(result.replaceAll("\\r|\\n","-"));
        //替换回车和tal键  使结果为一行

    }
}
