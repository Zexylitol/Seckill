package com.miaoshaproject.util;

import com.sun.tools.javac.jvm.Code;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author yzze
 * @create 2021-07-18 17:24
 */
public class CodeUtil {
    // 定义图片的width
    private static int width = 90;
    // 定义图片的height
    private static int height = 20;
    // 图片上显示验证码的个数
    private static int codeCount = 4;
    private static int xx = 15;
    private static int fontHeight = 18;
    private static int codeY = 16;
    private static char[] codeSequence = {'A', 'B','C', 'D', 'E', 'F', 'G', 'H', 'I', 'G', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    /**
     * 生成一个map集合
     * code为生成的验证码
     * codePic为生成的验证码BufferedImage对象
     * @return
     */
    public static Map<String, Object> generateCodeAndPic() {
        // 定义图像buffer
        BufferedImage buffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = buffImg.getGraphics();
        // 创建一个随机数生成器类
        Random random = new Random();
        // 将图像填充为白色
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);

        // 创建字体，字体的大小应该根据图片的高度来定
        Font font = new Font("Fixedsys", Font.BOLD, fontHeight);
        // 设置字体
        graphics.setFont(font);

        // 画边框
        graphics.setColor(Color.BLACK);
        graphics.drawRect(0, 0, width - 1, height - 1);

        // 随机产生 30 条干扰线，使图像中的认证码不易被其他程序探测到
        graphics.setColor(Color.BLACK);
        for (int i = 0; i < 30; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int xl = random.nextInt(12);
            int y1 = random.nextInt(12);
            graphics.drawLine(x, y, x + xl, y + y1);
        }

        // randomCode用于保存随机产生的验证码，以便用户登录后进行验证
        StringBuffer randomCode = new StringBuffer();
        int red = 0, green = 0, blue = 0;

        // 随机产生 codeCount 数字的验证码
        for (int i = 0; i < codeCount; i++) {
            // 得到随机产生的验证码数字
            String code = String.valueOf(codeSequence[random.nextInt(codeSequence.length)]);
            // 产生随机的颜色分量来构造颜色值，这样输出的每位数字的颜色值都将不同
            red = random.nextInt(255);
            green = random.nextInt(255);
            blue = random.nextInt(255);

            graphics.setColor(new Color(red, green, blue));
            graphics.drawString(code, (i + 1) * xx, codeY);

            randomCode.append(code);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("code", randomCode);
        map.put("codePic", buffImg);
        return map;
    }

    public static void main(String[] args) throws IOException {
        //OutputStream out = new FileOutputStream("/");
        Map<String, Object> map = CodeUtil.generateCodeAndPic();
        //ImageIO.write((RenderedImage)map.get("codePic"), "jpeg", out);
        System.out.println(map.get("code"));
    }
}
