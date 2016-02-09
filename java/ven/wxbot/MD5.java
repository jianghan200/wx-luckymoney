package ven.wxbot;

import java.security.MessageDigest;

/**
 * Created by Han on 2/6/16.
 */
public class MD5 {
    private static MessageDigest md5 = null;

    private static MessageDigest getMD5MessageDigest(){
        if(md5 ==null){
            try{
                md5 = MessageDigest.getInstance("MD5");
            }catch(Exception e){
                System.err.print("MD5 error");
            }
        }
        return md5;
    }

    /**
     * 对外提供getMD5(String)方法
     */
    public static String getMD5(String val) {
        md5 = getMD5MessageDigest();
        md5.update(val.getBytes());
        byte[] m = md5.digest();//加密
        return getString(m);
    }
    private static String getString(byte[] b){
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < b.length; i ++){
            sb.append(b[i]);
        }
        return sb.toString();
    }
}
