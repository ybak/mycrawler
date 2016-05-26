package org.ybak.crawler.persistence.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by happy on 2016/5/22.
 */
//@Document(indexName = "chengdu12345", type = "mail")
public class Mail implements Serializable {

//    @Id
    public Long id;

    
    public String url;

    
    public String sender;
    
    public String title;
    
    public String acceptUnit;
    
    public String status;
    
    public String category;
    
    public int views;
    
    public Date createDate;
    
    public String content;
    
    public String result;

    public Mail() {
    }

    public Mail(String url, String sender, String title, String acceptUnit, String status, String category, int views, Date createDate, String content, String result) {
        this.url = url;
        this.sender = sender;
        this.title = title;
        this.acceptUnit = acceptUnit;
        this.status = status;
        this.category = category;
        this.views = views;
        this.createDate = createDate;
        this.content = content;
        this.result = result;
    }
}
