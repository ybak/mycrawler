package org.ybak.crawler.downloader.chengdu12345;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by isaac on 16/5/25.
 */
public class HtmlParser {

    static ThreadLocal<DateFormat> df = new ThreadLocal() {
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
    };

    public static String getContent(Document doc) {
        return doc.select(".rightside1 td.td2").get(1).text();
    }

    public static String getResult(Document doc) {
        Elements element = doc.select(".rightside1 tbody tr:last-child");
        return element.text();
    }

    public static Date getPublishTime(Document doc) {
        String publishString = doc.select(".rightside1 td.td32").get(0).text();
        if (StringUtil.isBlank(publishString)) {
            publishString = doc.select(".rightside1 td.td32").get(1).text();
        }
        try {
            return df.get().parse(publishString);
        } catch (Exception e) {
            System.out.println("解析时间出错:" + publishString);
            return new Date();
        }
    }



}
