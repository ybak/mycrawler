package org.ybak.crawler.persistence.vo;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by happy on 2016/5/22.
 */
@Entity
@Table(name = "chengdu12345")
@NamedQuery(name = "Mail.search",
        query = "select m from Mail m where m.title like ?1 or m.content like ?1 or m.result like ?1")
public class Mail implements Serializable {

    @Id
    @GeneratedValue
    public Long id;

    @Column
    public String url;

    @Column
    public String sender;
    @Column
    public String title;
    @Column
    public String acceptUnit;
    @Column
    public String status;
    @Column
    public String category;
    @Column
    public int views;
    @Column
    public Date createDate;
    @Column
    public String content;
    @Column
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
