package org.ybak.crawler.persistence.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by isaac on 16/6/17.
 */
public class Plan implements Serializable {

    public Date createDate;

    public String id;

    public String url;

    public String title;

    public Plan() {
    }

    public Plan(String url, String title, Date createDate) {
        this.createDate = createDate;
        this.url = url;
        this.title = title;
    }
}
