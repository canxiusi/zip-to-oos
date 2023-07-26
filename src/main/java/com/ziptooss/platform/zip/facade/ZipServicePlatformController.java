package com.ziptooss.platform.zip.facade;

import com.ziptooss.platform.zip.api.ZipRequest;
import com.ziptooss.platform.zip.service.ZipService;
import com.ziptooss.platform.zip.util.OssUtils;
import com.ziptooss.platform.zip.util.ServerlessResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @author yukun.yan
 * @description ZipServicePlatformController
 * @date 2023/7/19 19:12
 */
@RequestMapping("/zipServicePlatform")
@RestController
public class ZipServicePlatformController {

    @Resource
    private ZipService zipService;

    /**
     * 通过文件路径从oss下载文件流, 在serverless压缩为zip, 并上传到oss, 返回zip的url地址
     *
     * @param zipRequest
     * @return 上传到oss的zip文件路径
     */
    @PostMapping("/zipToOss")
    public ServerlessResponse<String> zipToOss(@RequestBody ZipRequest zipRequest) {
        zipService.zipToOss(zipRequest);
        return ServerlessResponse.ok("请求serverless成功, 异步处理中");
    }

    /**
     * 小文件不分片上传
     *
     * @param multipartFile
     * @param bizSource
     * @return
     */
    @PostMapping("/fileUpload")
    public ServerlessResponse<String> fileUpload(@RequestPart("file") MultipartFile multipartFile,
                                                 @RequestParam("bizSource") String bizSource) {
        return ServerlessResponse.ok(OssUtils.fileUpload(multipartFile, bizSource));
    }

    /**
     * 大文件分片上传
     *
     * @param multipartFile
     * @param bizSource
     * @return
     */
    @PostMapping("/fragmentUpload")
    public ServerlessResponse<String> fragmentUpload(@RequestPart("file") MultipartFile multipartFile,
                                                     @RequestParam("bizSource") String bizSource) {
        return ServerlessResponse.ok(OssUtils.fragmentUploadMultipartFile(multipartFile, bizSource));
    }

    /**
     * 通过文件路径从oss下载文件流, 在serverless压缩为zip, 并写入到临时文件, 返回临时文件在磁盘的地址
     *
     * @param zipRequest
     * @return 上传到oss的zip文件路径
     */
    @PostMapping("/zipToTempFile")
    public ServerlessResponse<String> zipToTempFile(@RequestBody ZipRequest zipRequest) {
        zipService.zipToTempFile(zipRequest);
        return ServerlessResponse.ok("请求serverless成功, 异步处理中");
    }

    /**
     * 网盘系统重定向到这个路径, 下载临时文件
     *
     * @param filePath
     * @param response
     */
    @GetMapping("/downloadZipFile")
    public void streamDownload(@RequestParam String filePath, HttpServletResponse response) {
        zipService.streamDownload(filePath, response);
    }

}
