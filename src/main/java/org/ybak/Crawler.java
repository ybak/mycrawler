package org.ybak;

import com.github.davidmoten.rx.jdbc.Database;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hello world!
 */
public class Crawler {

    static String urlPrefix = "http://www.cdgh.gov.cn/ghgs/gggs/";
    static CountDownLatch tasks;
    static OkHttpClient client = new OkHttpClient();

    static Set<Integer> failedNumbers = new HashSet<Integer>();

    public static void main(String[] args) throws Exception {
        final Database db = DBUtil.getDB();

        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(8);

        int start = 5;
        int webpages = 10000;
        tasks = new CountDownLatch(webpages);

        for (int i = start; i < start + webpages; i++) {
            final int number = i;
            fixedThreadPool.execute(new Runnable() {
                public void run() {
                    try{
                        craw(number, db);
                    }finally {
                        tasks.countDown();
                    }
                }
            });
        }
        tasks.await();
        fixedThreadPool.shutdownNow();
        System.out.println(failedNumbers);
    }

    private static void craw(int number, Database db) {
        String url = urlPrefix + number + ".htm";
        boolean crawed = isURLCrawed(url, db);
        if(crawed){
            System.out.println("结束抓取：" + number + ", 页面已经抓取过.");
            return;
        }

        System.out.println("开始抓取：" + number);
        try {
            String html =getURLBody(url);
            int updates = db.update("insert into cdgh(url, html) values (?,?)").parameters(url, html).execute();
            if (updates > 0) {
                System.out.println("结束抓取：" + number + ", 抓取成功, 剩余任务：" + (tasks.getCount() - 1));
            } else {
                System.out.println("结束抓取：" + number + "插入数据库失败, 剩余任务：" + (tasks.getCount() - 1));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("结束抓取：" + number + ", status=" + e.getMessage() + ", 剩余任务：" + (tasks.getCount() - 1));
        } catch (IOException e) {
            failedNumbers.add(number);
            System.out.println("结束抓取：" + number + ", status=" + e.getMessage() + ", 剩余任务：" + (tasks.getCount() - 1));
        } finally {
//            try {
//                TimeUnit.MILLISECONDS.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    static String getURLBody(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        if(!response.isSuccessful()){
            response.body().close();
            throw new IllegalArgumentException(response.message());
        }
        return response.body().string();
    }

    private static boolean isURLCrawed(String url, Database db) {
        Integer count = db.select("select count(1) from cdgh where url = ?").parameter(url).getAs(Integer.class).toBlocking().single();
        return count > 0;
    }
}
