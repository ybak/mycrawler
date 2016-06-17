package org.ybak.crawler.downloader.cdgh;

import com.github.davidmoten.rx.jdbc.Database;
import com.github.davidmoten.rx.jdbc.tuple.Tuple3;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.ybak.crawler.persistence.util.DBUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by happy on 2016/4/2.
 */
public class ContentProcessor {

    static ThreadLocal<DateFormat> df = new ThreadLocal() {
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    public static void main(String[] args) {
        Database db = DBUtil.getDB();
        Integer count = db.select("select count(1) from cdgh").getAs(Integer.class).toBlocking().single();
        int pages = count / 50 + (count % 50 == 0 ? 0 : 1);
        System.out.println("待处理页数:" + pages);

        for (int i = 0; i < pages; i++) {
            System.out.println("当前处理页数:" + i);

            Iterable<Tuple3<Integer, String, String>> results =
                    db.select("select id, url, html from cdgh limit ?,?").parameters(i * 50, 50).getAs(Integer.class, String.class, String.class).toBlocking().toIterable();
            for (Tuple3<Integer, String, String> result : results) {
                Integer id = result.value1();
                String url = result.value2();
                String html = result.value3();
                Document doc = Jsoup.parse(html);

                String title = getTitle(doc);
                Date publishTime = getPublishTime(doc);
                db.update("insert into cdgh_extract(id, url, title, publish_time) values (?,?,?,?)")
                        .parameters(id, url, title, publishTime).execute();
            }
        }
    }

    public static Date getPublishTime(Document doc) {
        String publishString = doc.select("#info_released_dtime").text();
        try {
            return df.get().parse(publishString);
        } catch (ParseException e) {
            System.out.println("解析时间出错:" + publishString);
            return new Date();
        }
    }

    public static String getTitle(Document doc) {
        String title = doc.title();
        String[] split = title.split("--");
        if (split.length > 1) {
            return split[split.length - 1];
        }
        return title;
    }
}
