package org.ybak.crawler.downloader.chengdu12345.db;

import com.github.davidmoten.rx.jdbc.Database;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.ybak.crawler.downloader.util.HtmlUtil;
import org.ybak.crawler.persistence.util.DBUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hello world!
 */
public class MailCrawler {

    static String urlPrefix = "http://12345.chengdu.gov.cn/";
    static CountDownLatch tasks;

    static Set<Integer> failedNumbers = new HashSet<Integer>();

    public static void main(String[] args) throws Exception {
        final Database db = DBUtil.getDB();

        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(8);

        int start = 1, end = 720;
        tasks = new CountDownLatch(end);

        for (int i = start; i <= end; i++) {
            final int number = i;
            fixedThreadPool.execute(() -> {
                try {
                    craw(number, db);
                } finally {
                    tasks.countDown();
                }
            });
        }
        tasks.await();
        fixedThreadPool.shutdownNow();
        System.out.println(failedNumbers);
    }

    private static void craw(int number, Database db) {
        String pageUrl = urlPrefix + "moreMail?page=" + number;
        System.out.println("开始抓取：" + number);
        try {
                String html = HtmlUtil.getURLBody(pageUrl);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.left5 ul li.f12px");
                for (Element element : elements) {
                    String url = urlPrefix + element.select("css").attr("href");

                boolean crawed = isURLCrawed(url, db);
                if (crawed) {
                    System.out.println("结束抓取：" + number + ", 页面已经抓取过.");
                    return;
                }

                Elements divs = element.select("div");
                String title = divs.get(0).text();
                String sender = divs.get(1).text();
                String receiveUnit = divs.get(2).text();
                String status = divs.get(3).text();
                String category = divs.get(4).text();
                String views = divs.get(5).text();
                String publishDate = "20" + divs.get(6).text() + " 00:00:00";


                int updates = db.update("insert into chengdu12345(url, title, sender, accept_unit, status, category, views, create_date) values (?,?,?,?,?,?,?,?)")
                        .parameters(url, title, sender, receiveUnit, status, category, views, publishDate)
                        .execute();
                if (updates > 0) {
                    System.out.println("结束抓取：" + number + ", 抓取成功, 剩余任务：" + (tasks.getCount() - 1));
                } else {
                    System.out.println("结束抓取：" + number + "插入数据库失败, 剩余任务：" + (tasks.getCount() - 1));
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("结束抓取：" + number + ", status=" + e.getMessage() + ", 剩余任务：" + (tasks.getCount() - 1));
        } catch (Exception e) {
            failedNumbers.add(number);
            System.out.println("结束抓取：" + number + ", status=" + e.getMessage() + ", 剩余任务：" + (tasks.getCount() - 1));
        } finally {
        }
    }

    private static boolean isURLCrawed(String url, Database db) {
        Integer count = db.select("select count(1) from chengdu12345 where url = ?").parameter(url).getAs(Integer.class).toBlocking().single();
        return count > 0;
    }
}
