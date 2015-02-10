package com.lza.pad.client.helper;

import android.text.TextUtils;

import com.lza.pad.client.utils.UniversalUtility;

import java.util.Map;

/**
 * 业务类，用于生成Url和Url参数
 *
 * @author xiads
 * @Date 15/1/22.
 */
public class UrlHelper {

    public static final String DEFAULT_URL = "";

    public static String generateUrl(Map<String, String> par) {
        String param = UniversalUtility.encodeUrl(par);
        StringBuilder builder = new StringBuilder();
        String defaultUrl = DEFAULT_URL;
        builder.append(defaultUrl).append(param);
        return builder.toString();
    }

    /**
     * 解析Url中的control参数
     *
     * @param url
     * @return
     */
    public static String parseControl(String url) {
        String control = "";
        if (TextUtils.isEmpty(url)) return control;
        int index = url.indexOf("control=");
        if (index > 0) {
            control = url.split("control=")[1];
            if (!TextUtils.isEmpty(control))
                control = control.split("&")[0];
        }
        return control;
    }


}
