package com.ziptooss.platform.zip.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author yukun.yan
 * @description FileUploadUtils 目标文件根据自定义规则校验
 * @date 2023/5/17 14:48
 */
public class FileUploadUtils {

    private FileUploadUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * 指定文件是否存在
     *
     * @param file
     * @return
     */
    public static boolean isFileExist(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    public static String getFileType(MultipartFile file) {
        if(!isFileExist(file)) {
            throw new RuntimeException("file is null");
        }
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            return "file";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
    }

}
