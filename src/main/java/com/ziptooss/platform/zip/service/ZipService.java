package com.ziptooss.platform.zip.service;

import com.ziptooss.platform.zip.api.ZipRequest;

import javax.servlet.http.HttpServletResponse;

/**
 * @author yukun.yan
 * @description ZipService
 * @date 2023/7/19 19:14
 */
public interface ZipService {

    void zipToOss(ZipRequest zipRequest);

    void zipToTempFile(ZipRequest zipRequest);

    void streamDownload(String filePath, HttpServletResponse response);

}
