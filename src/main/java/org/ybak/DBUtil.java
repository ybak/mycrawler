package org.ybak;

import com.github.davidmoten.rx.jdbc.Database;

/**
 * Created by happy on 2016/4/2.
 */
public class DBUtil {

    public static Database getDB() {
        return Database.builder()
                .url("jdbc:mysql://localhost:3306/crawler?characterEncoding=UTF-8")
                .username("root").password("111111")
                .pool(5,10).build();
    }

    public static void main(String[] args) {
        Integer count = getDB().select("select * from cdgh;").count().toBlocking().single();
        System.out.println(count);
    }

}
