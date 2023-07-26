package com.ziptooss.platform.zip.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author yukun.yan
 * @description PartUploader
 * @date 2023/7/21 10:53
 */
public class PartUploader implements Runnable {

    private final int i;
    private final long partSize;
    private final int partCount;
    private final String bucketName;
    private final MultipartFile file;
    private final long fileLength;
    private final String objectName;
    private final String uploadId;
    private final OSS ossClient;
    private final List<PartETag> partEtagList;
    private final CountDownLatch countDownLatch;

    public PartUploader(int i, long partSize, int partCount,
                        MultipartFile file, long fileLength, String objectName,
                        String uploadId, OSS ossClient, List<PartETag> partEtagList,
                        CountDownLatch countDownLatch, String bucketName) {
        this.i = i;
        this.partSize = partSize;
        this.partCount = partCount;
        this.file = file;
        this.fileLength = fileLength;
        this.objectName = objectName;
        this.uploadId = uploadId;
        this.ossClient = ossClient;
        this.partEtagList = partEtagList;
        this.countDownLatch = countDownLatch;
        this.bucketName = bucketName;
    }

    @Override
    public void run() {
        InputStream instream = null;
        try {
            long startPos = i * partSize;
            long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
            instream = file.getInputStream();
            instream.skip(startPos);
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(objectName);
            uploadPartRequest.setUploadId(uploadId);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartSize(curPartSize);
            uploadPartRequest.setPartNumber(i + 1);
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            synchronized ("lock") {
                this.partEtagList.add(uploadPartResult.getPartETag());
            }
        } catch (Exception e) {
            //
        } finally {
            countDownLatch.countDown();
            if (instream != null) {
                try {
                    instream.close();
                } catch (IOException e) {
                    //
                }
            }
        }
    }

}
