package com.ziptooss.platform.zip.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.model.*;
import com.ziptooss.platform.zip.service.PartUploader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

import static com.aliyun.oss.internal.OSSConstants.DEFAULT_CHARSET_NAME;

/**
 * @author yukun.yan
 * @description OssService
 * @date 2023/5/29 16:31
 */
@Slf4j
public class OssUtils {

    private static OssUtils ossService;

    private final OSS ossClient;

    private final String bucketName;

    private final String region;

    private final String endpoint;

    private final Date expiration;

    private static final long partSize = 20 * 1024 * 1024L;

    @Autowired
    @Qualifier("thread-pool")
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 构造器执行之后, 做静态字段注入
     */
    @PostConstruct
    private void init() {
        OssUtils.ossService = this;
    }

    public OssUtils(OSS ossClient, String bucketName, String region, String endpoint) {
        this.ossClient = ossClient;
        this.bucketName = bucketName;
        this.region = region;
        this.endpoint = endpoint;
        this.expiration = new Date(System.currentTimeMillis() + 3600L * 1000 * 24 * 365 * 10);
    }

    /**
     * 如果是公共读写桶可以使用这个方法获取url
     *
     * @param key
     * @return
     */
    private static String urlEncodeKey(String key) {
        if (key.startsWith("/")) {
            return HttpUtil.urlEncode(key, DEFAULT_CHARSET_NAME);
        }
        StringBuilder resultUri = new StringBuilder();

        String[] keys = key.split("/");
        resultUri.append(HttpUtil.urlEncode(keys[0], DEFAULT_CHARSET_NAME));
        for (int i = 1; i < keys.length; i++) {
            resultUri.append("/").append(HttpUtil.urlEncode(keys[i], DEFAULT_CHARSET_NAME));
        }

        if (key.endsWith("/")) {
            for (int i = key.length() - 1; i >= 0; i--) {
                if (key.charAt(i) == '/') {
                    resultUri.append("/");
                } else {
                    break;
                }
            }
        }
        return "https://" + ossService.bucketName + "." + ossService.region + ".aliyuncs.com" + "/" + key;
    }

    /**
     * 获取上传后的url
     *
     * @param objectName
     */
    private static String getUrl(String objectName) {
        // 100年过期
        URL url;
        try {
            url = ossService.ossClient.generatePresignedUrl(ossService.bucketName, objectName, ossService.expiration);
            if (url == null) {
                throw new RuntimeException("OssService get oss url is null");
            }
            return url.toString();
        } catch (Exception e) {
            log.error("[OssService] get file Url error, fileName={}, e={}", objectName, e);
            throw new RuntimeException("OssService get file Url error");
        }
    }

    /**
     * 文件对象上传
     *
     * @param file      文件对象
     * @param bizSource 文件名
     * @return 返回有效期x天的url
     */
    public static String fileUpload(MultipartFile file, String bizSource) {
        Assert.isTrue(FileUploadUtils.isFileExist(file), "File does not exist");
        // 判断桶名是否存在，如果不存在就新建一个
        if (!ossService.ossClient.doesBucketExist(ossService.bucketName)) {
            ossService.ossClient.createBucket(ossService.bucketName);
        }
        String fileType = FileUploadUtils.getFileType(file);
        String filePath = getFilePath(bizSource, fileType, file.getOriginalFilename());
        try {
            ossService.ossClient.putObject(ossService.bucketName, filePath, file.getInputStream());
            return urlEncodeKey(filePath);
        } catch (Exception e) {
            log.error("[OssService] fileUpload error, bizSource={}, e={}", bizSource, e);
            throw new RuntimeException("OssService file upload error");
        }
    }

    /**
     * MultipartFile文件对象上传
     *
     * @param file
     * @param bizSource
     * @return
     */
    public static String fragmentUploadMultipartFile(MultipartFile file, String bizSource) {
        try {
            return fragmentUpload(file, bizSource);
        } catch (Exception e) {
            log.error("[OssService] fragmentUpload error, ", e);
            throw new RuntimeException("OssService fragmentUpload error");
        }
    }

    /**
     * 通过文件路径从oss下载, 并压缩, 再把压缩包上传到oss, 返回zip的oss的url
     *
     * @param ossFilePathList
     * @param bizSource
     * @return
     */
    public static String putZipToOss(List<String> ossFilePathList, String bizSource) {
        File tempFile = null;
        try {
            // 之前在内存里压缩, 几个g的大文件
            // org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.write 出现了oom
            // 现在使用临时文件压缩
            tempFile = File.createTempFile(UUID.randomUUID().toString(), ".zip");
            try (ZipArchiveOutputStream zipOutStream = new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
                int batchSize = 10;
                List<List<String>> batches = splitListIntoBatches(ossFilePathList, batchSize);
                for (List<String> batch : batches) {
                    for (String filePathEach : batch) {
                        OSSObject ossObject;
                        try {
                            ossObject = ossService.ossClient.getObject(ossService.bucketName, filePathEach);
                        } catch (Exception e) {
                            log.error("[OssService] putZip getObject error, ", e);
                            continue;
                        }
                        try (InputStream inputStream = new BufferedInputStream(ossObject.getObjectContent())) {
                            ZipArchiveEntry entry = new ZipArchiveEntry(filePathEach);
                            zipOutStream.putArchiveEntry(entry);
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                zipOutStream.write(buffer, 0, bytesRead);
                            }
                            zipOutStream.closeArchiveEntry();
                        } finally {
                            if (ossObject != null) {
                                try {
                                    ossObject.close();
                                } catch (IOException e) {
                                    log.error("Error closing OSSObject", e);
                                }
                            }
                        }
                    }
                }
            }
            String filePath = getFilePath(bizSource, "zip", tempFile.getName());
            ossService.ossClient.putObject(ossService.bucketName, filePath, tempFile);
            return urlEncodeKey(filePath);
        } catch (Exception e) {
            log.error("[OssService] putZip error, ossFilePath={}, e={}", ossFilePathList, e);
            throw new RuntimeException("OssService putZip error");
        } finally {
            if (tempFile != null) {
                if (tempFile.delete()) {
                    log.info("OssService putZip deleteTempFile");
                }
            }
        }
    }

    private static List<List<String>> splitListIntoBatches(List<String> list, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, list.size());
            batches.add(list.subList(i, endIndex));
        }
        return batches;
    }

    /**
     * 多线程, 大文件分片上传
     *
     * @param file
     * @param bizSource
     * @return
     */
    private static String fragmentUpload(MultipartFile file, String bizSource) {
        String fileType = FileUploadUtils.getFileType(file);
        String filePath = getFilePath(bizSource, fileType, file.getOriginalFilename());

        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(ossService.bucketName, filePath);
        InitiateMultipartUploadResult upResult = ossService.ossClient.initiateMultipartUpload(request);
        String uploadId = upResult.getUploadId();

        // 计算分片数量
        long fileLength = file.getSize();
        int partCount = (int) (fileLength / partSize);
        if (fileLength % partSize != 0) {
            partCount++;
        }
        if (partCount > 10000) {
            throw new RuntimeException("partCount > 10000");
        }

        List<PartETag> partEtagList = new ArrayList<>(partCount);
        CountDownLatch countDownLatch = new CountDownLatch(partCount);
        for (int i = 0; i < partCount; i++) {
            ossService.threadPoolExecutor.execute(new PartUploader(i, partSize, partCount, file,
                    fileLength, filePath, uploadId, ossService.ossClient, partEtagList,
                    countDownLatch, ossService.bucketName));
        }
        try {
            countDownLatch.await();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // 验证是否所有的分片都完成
        if (partEtagList.size() != partCount) {
            log.error("[OssService] Some files failed to upload, partETagsSize={}, partCount={}", partEtagList.size(), partCount);
            throw new RuntimeException("OssService Some files failed to upload");
        }
        partEtagList.sort(Comparator.comparingInt(PartETag::getPartNumber));
        CompleteMultipartUploadRequest completeUpload = new CompleteMultipartUploadRequest(ossService.bucketName, filePath, uploadId, partEtagList);
        // 完成上传
        ossService.ossClient.completeMultipartUpload(completeUpload);
        return urlEncodeKey(filePath);
    }

    /**
     * 拼接 业务系统/上传日期/文件类型/文件名(时间搓+uuid)
     * https://da-net-disk.oss-cn-hangzhou.aliyuncs.com/disk/2023-07-21/csv/1689905357258AccessKey.csv
     *
     * @param bizSource 业务系统标识
     * @param fileType  文件类型
     * @param fileName  文件名
     * @return
     */
    private static String getFilePath(String bizSource, String fileType, String fileName) {
        return bizSource + "/" + DateUtils.getCurrentDateStr(DateUtils.Style.YYYY_MM_DD) + "/" + fileType + "/" + System.currentTimeMillis() + fileName;
    }

    /**
     * 通过文件路径从oss下载, 并压缩, 再把压缩包写入到临时文件中, 返回临时文件的路径, 之后告诉网盘系统重定向到 /zipServicePlatform/downloadZipFile 根据临时文件路径下载
     *
     * @param ossFilePathList
     * @param bizSource
     */
    public static String putZipToTempFile(List<String> ossFilePathList, String bizSource) {
        File tempFile;
        try {
            tempFile = File.createTempFile(UUID.randomUUID().toString(), ".zip");
            try (ZipArchiveOutputStream zipOutStream = new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
                int batchSize = 10;
                List<List<String>> batches = splitListIntoBatches(ossFilePathList, batchSize);
                for (List<String> batch : batches) {
                    for (String filePathEach : batch) {
                        OSSObject ossObject;
                        try {
                            ossObject = ossService.ossClient.getObject(ossService.bucketName, filePathEach);
                        } catch (Exception e) {
                            log.error("[OssService] putZip getObject error, ", e);
                            continue;
                        }
                        try (InputStream inputStream = new BufferedInputStream(ossObject.getObjectContent())) {
                            ZipArchiveEntry entry = new ZipArchiveEntry(filePathEach);
                            zipOutStream.putArchiveEntry(entry);
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                zipOutStream.write(buffer, 0, bytesRead);
                            }
                            zipOutStream.closeArchiveEntry();
                        } finally {
                            if (ossObject != null) {
                                try {
                                    ossObject.close();
                                } catch (IOException e) {
                                    log.error("Error closing OSSObject", e);
                                }
                            }
                        }
                    }
                }
            }
            return tempFile.getPath();
        } catch (Exception e) {
            log.error("[OssService] putZip error, ossFilePath={}, e={}", ossFilePathList, e);
            throw new RuntimeException("OssService putZip error");
        }
    }

    /**
     * 释放资源
     */
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("[OssService] ossClient shutdown");
        }
    }

}
