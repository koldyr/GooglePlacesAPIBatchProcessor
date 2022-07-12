package com.koldyr.google.places;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.koldyr.google.model.Place;

/**
 * Description of class EncodeProcessor
 *
 * @created: 2018.08.15
 */
class EncodeProcessor implements Callable<List<Place>> {
    private static final Logger logger = LogManager.getLogger(EncodeProcessor.class);

    private final FindPlaceService placesService;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final List<String> names;
    private final double total;

    EncodeProcessor(FindPlaceService placesService, ReentrantReadWriteLock.ReadLock readLock, List<String> names) {
        this.placesService = placesService;
        this.names = names;
        this.readLock = readLock;
        total = names.size();
    }

    @Override
    public List<Place> call() {
        final List<Place> result = new LinkedList<>();
        String name;
        while ((name = next()) != null) {
            List<Place> place = placesService.find(name);
            if (place != null) {
                result.addAll(place);
            }
            logger.debug("Processing...\t" + getProgress() + '%');
        }
        return result;
    }

    private double getProgress() {
        double percent = (total - names.size()) / names.size() * 100;
        return Math.round(percent * 100) / 100.0;
    }

    private String next() {
        readLock.lock();
        try {
            if (names.size() > 0) {
                return names.remove(0);
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }
}
