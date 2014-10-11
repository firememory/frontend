package com.coinport.sdk;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.HttpClients;

public class CPRestSDK {
    private String server;
    private int port;
    private String userId;
    private String apiToken;
    private String uriBase;
    private static final int METHOD_GET = 1;
    private static final int METHOD_POST = 2;

    BasicCookieStore cookieStore = new BasicCookieStore();
    CloseableHttpClient httpclient = HttpClients.custom()
        .setDefaultCookieStore(cookieStore)
        .build();

    public CPRestSDK(String server, int port, String userId, String apiToken) {
        this.server = server;
        this.port = port;
        this.userId = userId;
        this.apiToken = apiToken;
        uriBase = "https://" + server;
    }

    public String bid(Market market, double price, double amount) {
        String uri = uriBase + "/trade/" + market.toString() + "/bid" ;
        Map<String, String> params = new HashMap<>();
        params.put("type", "bid");
        params.put("price", price + "");
        params.put("amount", amount + "");
        String result = doPost(uri, params);
        return result;
    }

    public String ask(Market market, double price, double amount) {
        String uri = uriBase + "/trade/" + market.toString() + "/ask" ;
        Map<String, String> params = new HashMap<>();
        params.put("type", "ask");
        params.put("price", price + "");
        params.put("amount", amount + "");
        String result = doPost(uri, params);
        return result;
    }

    public String cancelOrder(Market market, String oid) {
        String uri = uriBase + "/trade/" + market.toString() + "/order/cancel/" + oid ;
        String result = doGet(uri, Collections.<String, String>emptyMap());
        return result;
    }

    public String queryAsset() {
        String uri = uriBase + "/api/asset/" + userId;
        String result = doGet(uri, Collections.<String, String>emptyMap());
        return result;
    }

    public String queryMyOrders(Market market) {
        String uri = uriBase + "/api/user/" + userId + "/order/" + market.toString();
        String result = doGet(uri, Collections.<String, String>emptyMap());
        return result;
    }

    public String queryMarketOrders(Market market) {
        String uri = uriBase + "/api/" + market.toString() + "/orders";
        String result = doGet(uri, Collections.<String, String>emptyMap());
        return result;
    }

    private String doGet(String uri, Map<String, String> params) {
        return request(uri, params, METHOD_GET);
    }

    private String doPost(String uri, Map<String, String> params) {
        return request(uri, params, METHOD_POST);
    }

    private String request(String uri, Map<String, String> params, int method) {
        System.out.println("uri: " + uri);
        RequestBuilder rb = METHOD_GET == method ?
            RequestBuilder.get() : RequestBuilder.post();
        try {
            rb.setUri(new URI(uri));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        rb.addHeader("USERID", userId);
        rb.addHeader("API-TOKEN", apiToken);

        for (Map.Entry<String, String> kv : params.entrySet()) {
            rb.addParameter(kv.getKey(), kv.getValue());
        }

        HttpUriRequest req = rb.build();

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            String responseContent = entity == null ?
                "null" : EntityUtils.toString(entity);
            EntityUtils.consume(entity);
            System.out.println(responseContent);
            return responseContent;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    public static enum Market {
        LTC_BTC,
        DOGE_BTC,
        BC_BTC,
        DRK_BTC,
        VRC_BTC,
        ZET_BTC,
        BTSX_BTC,
        NXT_BTC,
        XRP_BTC;

        @Override
        public String toString() {
            return super.toString().toLowerCase().replace('_', '-');
        }
    }


    public static void main(String[] args) throws Exception {
        String apiToken = "42158462697ce542272c8c4048045d2";
        CPRestSDK sdk = new CPRestSDK("coinport.com", 443, "1000000016", apiToken);
        // sdk.queryAsset();
        // sdk.queryMyOrders(Market.LTC_BTC);
        // sdk.queryMarketOrders(Market.LTC_BTC);
        for(int i = 0; i < 3; i ++) {
            sdk.bid(Market.LTC_BTC, 0.01, 1);
            Thread.currentThread().sleep(1000);
        }
        // sdk.ask(Market.LTC_BTC, 0.01, 20);
        // sdk.cancelOrder(Market.LTC_BTC, "123");
    }
}
