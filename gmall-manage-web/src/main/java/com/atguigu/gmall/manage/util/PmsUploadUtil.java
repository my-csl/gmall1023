package com.atguigu.gmall.manage.util;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class PmsUploadUtil {


    public static String uploadImage(MultipartFile multiparFile) {

        String imgUrl = "http://192.168.2.200";

        //获得fdfs的全局链接地址
        String s = PmsUploadUtil.class.getResource("/tracker.conf").getPath();

        try {
            ClientGlobal.init(s);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TrackerClient trackerClient = new TrackerClient();

        //获得一个trackerServer的实例
        TrackerServer trackerServer = null;
        try {
            trackerServer = trackerClient.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //通过tracker获得一个Storage的链接客户端
        StorageClient storageClient = new StorageClient(trackerServer, null);

        try {

            byte[] bytes = multiparFile.getBytes();//获得上传的二进制对象

            //获得文件名后缀
            String originalFilename = multiparFile.getOriginalFilename();
            int i = originalFilename.lastIndexOf(".");
            String exitName = originalFilename.substring(i+1);

            String [] uploadInfos = storageClient.upload_file(bytes, exitName, null);

            for (String uploadInfo : uploadInfos) {
                imgUrl += "/"+uploadInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


//        System.out.println(url);

        return imgUrl;
    }
}
