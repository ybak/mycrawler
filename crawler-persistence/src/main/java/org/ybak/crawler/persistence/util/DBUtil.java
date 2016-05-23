package org.ybak.crawler.persistence.util;

import com.alibaba.fastjson.JSON;
import com.github.davidmoten.rx.jdbc.Database;
import com.github.davidmoten.rx.jdbc.tuple.TupleN;
import org.ybak.crawler.persistence.vo.Mail;
import rx.Single;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by happy on 2016/4/2.
 */
public class DBUtil {

    public static Database getDB() {
        return Database.builder()
                .url("jdbc:mysql://localhost:3306/crawler?characterEncoding=UTF-8")
                .username("root").password("111111")
                .pool(5, 10).build();
    }

    public static void main(String[] args) {
        TupleN<Object> mail = getDB().select("select * from chengdu12345;").getTupleN().toBlocking().first();

//        Integer count = getDB().select("select * from cdgh;").count().toBlocking().single();
//        Mail mail = getDB().select("select url, sender, title, accept_unit, status, category, views, create_date, content, result  from chengdu12345;").get(rs -> new Mail(
//                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
//                rs.getString(6), rs.getInt(7), rs.getDate(8), rs.getString(9), rs.getString(10)
//        )).toBlocking().first();
        System.out.println(JSON.toJSONString(mail));
    }

}
