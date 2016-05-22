package org.ybak.crawler.downloader.chengdu12345;

import com.github.davidmoten.rx.jdbc.Database;
import com.github.davidmoten.rx.jdbc.tuple.Tuple2;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.ybak.crawler.downloader.util.HtmlUtil;
import org.ybak.crawler.persistence.util.DBUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hello world!
 */
public class MailContentCrawler {

    static ThreadLocal<DateFormat> df = new ThreadLocal() {
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
    };
    static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(8);


    public static void main(String[] args) throws Exception {
        Database db = DBUtil.getDB();
        Integer count = db.select("select count(1) from chengdu12345").getAs(Integer.class).toBlocking().single();
        CountDownLatch tasks = new CountDownLatch(count);


        int pages = count / 50 + (count % 50 == 0 ? 0 : 1);
        System.out.println("待处理页数:" + pages);

        for (int i = 0; i < pages; i++) {
            System.out.println("当前处理页数:" + i);

            Iterable<Tuple2<Integer, String>> results =
                    db.select("select id, url from chengdu12345 limit ?,?").parameters(i * 50, 50).getAs(Integer.class, String.class).toBlocking().toIterable();
            for (Tuple2<Integer, String> result : results) {
                Integer id = result.value1();
                String url = result.value2();

                fixedThreadPool.execute(() -> {
                    try {
                        String html = HtmlUtil.getURLBody(url);
                        Document doc = Jsoup.parse(html);

                        String content = getContent(doc);
                        String handleResult = getResult(doc);
//                        Date publishTime = getPublishTime(doc);

                        db.update("update chengdu12345 set content = ?, result = ? where id = ?")
                                .parameters(content, handleResult, id).execute();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        tasks.countDown();
                        System.out.println("剩余任务数:" + tasks.getCount() + "/" + count);
                    }
                });
            }
        }

        tasks.await();
        fixedThreadPool.shutdown();
    }

    private static String getContent(Document doc) {
        return doc.select(".rightside1 td.td2").get(1).text();
    }

    private static String getResult(Document doc) {
        Elements element = doc.select(".rightside1 tbody tr:last-child");
        return element.text();
    }

    private static Date getPublishTime(Document doc) {
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
