package com.koldyr.google.places;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koldyr.google.model.Place;
import com.koldyr.util.SSLUtil;

/**
 * Description of class Main
 *
 * @created: 2018.08.15
 */
public class Main {
    private static Logger LOG = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        final String apiKey = args[0];

        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            final List<String> names = loadBrands("/retailers.txt");
            final ReentrantReadWriteLock.ReadLock readLock = new ReentrantReadWriteLock().readLock();
            final FindPlaceService placesService = new FindPlaceService(getHttpClient(), apiKey);

            final Collection<Callable<List<Place>>> tasks = new ArrayList<>(10);
            for (int i = 0; i < 10; i++) {
                tasks.add(new EncodeProcessor(placesService, readLock, names));
            }

            final List<Future<List<Place>>> results = executorService.invokeAll(tasks);
            final Collection<Place> places = new LinkedList<>();
            for (Future<List<Place>> future : results) {
                places.addAll(future.get());
            }

            final ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File("retailers.json"), places);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            executorService.shutdown();
        }
    }

    public static List<String> loadBrands(String brands) throws IOException {
        final List<String> retailers = new LinkedList<>();
        try (InputStream inputStream = Main.class.getResourceAsStream(brands)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }

                if (line.contains(",")) {
                    String[] strings = line.split(",");
                    for (String string : strings) {
                        retailers.add(string.trim());
                    }
                } else {
                    retailers.add(line.trim());
                }
            }
        }

        LOG.debug("Loaded " + retailers.size() + " retailers");
        return retailers;
    }

    private static CloseableHttpClient getHttpClient() throws KeyManagementException, NoSuchAlgorithmException {
        int httpTimeOut = 15_000;
        final SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(httpTimeOut).build();

        final RequestConfig requestConfig = RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setSocketTimeout(httpTimeOut)
                .setConnectionRequestTimeout(httpTimeOut)
                .setConnectTimeout(httpTimeOut)
                .setProxy(new HttpHost("gate-zrh.swissre.com", 8080))
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(150);
        connectionManager.setDefaultMaxPerRoute(150);

        return HttpClients
                .custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultSocketConfig(socketConfig)
                .setSSLContext(SSLUtil.getSslContextTrustAll())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
    }
}
