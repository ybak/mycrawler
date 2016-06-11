package org.ybak.crawler.web;

/**
 * Created by happy on 2016/6/10.
 */
public class CrawProgress {

    public int total;

    public int current;

    public CrawProgress(int total) {
        this.total = total;
    }

    public int getProgress(){
        return current * 100 / total;
    }
}
