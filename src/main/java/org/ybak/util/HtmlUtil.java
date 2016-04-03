package org.ybak.util;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by isaac on 16/4/3.
 */
public class HtmlUtil {

    static OkHttpClient client = new OkHttpClient();

    public static String getURLBody(String url) throws IOException {
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
}
