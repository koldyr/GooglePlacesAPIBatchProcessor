package com.koldyr.google.places;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koldyr.google.model.Place;
import com.koldyr.util.SSLUtil;

/**
 * Description of class BatchProcessor
 *
 * @created: 2018.08.15
 */
public class BatchProcessor {
    private static final Logger LOG = LogManager.getLogger(BatchProcessor.class);

    public static void main(String[] args) {
        final String apiKey = args[0];
        final String input = args[1];
        final String output = args[2];

        var executorService = Executors.newFixedThreadPool(10);
        try {
            var names = loadInputData(new FileInputStream(input));
            var readLock = new ReentrantReadWriteLock().readLock();
            var placesService = new FindPlaceService(getHttpClient(), apiKey);

            final Collection<Callable<List<Place>>> tasks = new ArrayList<>(10);
            for (int i = 0; i < 10; i++) {
                tasks.add(new EncodeProcessor(placesService, readLock, names));
            }

            final List<Future<List<Place>>> results = executorService.invokeAll(tasks);
            final Collection<Place> places = new LinkedList<>();
            for (Future<List<Place>> future : results) {
                places.addAll(future.get());
            }

            var mapper = new ObjectMapper();
            mapper.writeValue(new File(output), places);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            executorService.shutdown();
        }
    }

    public static List<String> loadInputData(InputStream inputData) throws IOException {
        final List<String> brands = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputData))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }

                brands.add(line.trim());
            }
        }

        LOG.debug("Loaded " + brands.size() + " names");
        return brands;
    }

    private static CloseableHttpClient getHttpClient() throws KeyManagementException, NoSuchAlgorithmException {
        int httpTimeOut = 15_000;
        var socketConfig = SocketConfig.custom().setSoTimeout(httpTimeOut).build();
        var proxy = getProxy();

        var requestConfig = RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setSocketTimeout(httpTimeOut)
                .setConnectionRequestTimeout(httpTimeOut)
                .setConnectTimeout(httpTimeOut)
                .setProxy(proxy)
                .build();

        var connectionManager = new PoolingHttpClientConnectionManager();
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

    private static HttpHost getProxy() {
        String proxyHost = System.getProperty("http.proxyHost");
        if (StringUtils.isNotEmpty(proxyHost)) {
            int proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));
            return new HttpHost(proxyHost, proxyPort);
        }
        return null;
    }
}
