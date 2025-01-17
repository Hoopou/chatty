
package chatty.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A class that provides help for common tasks of requesting data that can be
 * mapped to key-value pairs:
 * 
 * <ul>
 * <li>Aggregating several keys into one request
 * <li>Calling result listeners
 * <li>Retrying requests on error
 * <li>Caching results
 * </ul>
 * 
 * <p>The actual data gathering is performed by the Requester, which has to be
 * implemented. Typically this would do e.g. an API request.
 * 
 * <p>Typical approach:
 * 
 * <ol>
 * <li>Request one or several keys with one of the request() methods
 * <li>The implemented Requester is called with the keys
 * <li>An API request is performed, returning either values or an error of sorts
 * <li>The result is set via one of the result methods, which adds it to the cache
 * <li>The manager calls the result listener for done key queries
 * </ol>
 * 
 * @author tduva
 * @param <Key> The class of keys
 * @param <Item> The class of values
 */
public class CachedBulkManager<Key,Item> {
    
    private static final Logger LOGGER = Logger.getLogger(CachedBulkManager.class.getName());
    
    private final Object LOCK = new Object();
    
    //---------
    // Options
    //---------
    /**
     * No special option. By default, requests are done on a timer, through the
     * "normal" set and query results are returned and the query removed as soon
     * as only errored or "not found" keys are left.
     */
    public static final int NONE = 0;
    
    /**
     * Keeps the query around when there are still errored keys left (except
     * "not found" ones). Partial results where only errored keys are left are
     * returned.
     */
    public static final int RETRY = 1;
    
    /**
     * Request missing keys immediately, request via "asap" set and reduce error
     * re-request delay.
     */
    public static final int ASAP = 2;
    
    /**
     * The query result is only returned and the query removed when all keys
     * (except for "not found") are available. Errors will attempt to be
     * re-requested
     */
    public static final int WAIT = 4;
    
    /**
     * Keys in this query are removed from the cache and the result for this
     * query only returned when all keys have had a result.
     * 
     * TODO: Could be problematic together with other requests, since some state
     * such as notFound or occured errors is not removed.
     */
    public static final int REFRESH = 8;
    
    /**
     * Runs the request timer in daemon mode. Only has an effect on instance
     * creation, not queries.
     */
    public static final int DAEMON = 16;
    
    /**
     * Don't add query if an equal query already exists (same keys, listener and
     * options).
     */
    public static final int UNIQUE = 32;
    
    /**
     * Returns each partial query result (as always, unless it is equal to the
     * previous result returned for the same query). The number of returned
     * results will depend on how results are set, for example if the result for
     * several keys is set in bulk or one by one.
     */
    public static final int PARTIAL = 64;
    
    /**
     * Don't replace query when using a unique object.
     */
    public static final int NO_REPLACE = 128;
    
    //-------
    // State
    //-------
    // Global options
    private final Options options;

    // Queries
    private final Map<Object, Query<Key, Item>> queries = new HashMap<>();
    
    // Requesting
    private final Requester<Key, Item> requester;
    private final Map<Key, Long> requestPending = new HashMap<>();
    
    // Data
    private final Map<Key, Item> cache = new HashMap<>();
    
    // Errors
    private final Map<Key, Long> lastError = new HashMap<>();
    private final Map<Key, Integer> errorCount = new HashMap<>();
    private final Set<Key> notFound = new HashSet<>();
    
    public CachedBulkManager(Requester<Key, Item> requester, int settings) {
        this.requester = requester;
        this.options = new Options(settings);
        int timerDelay = 10*1000;
        Timer timer = new Timer("CachedBulkManager", options.contains(DAEMON));
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                doRequests();
            }
        }, timerDelay, timerDelay);
    }
    
    //=============
    // Add Queries
    //=============
    @SafeVarargs
    public final void query(ResultListener<Key, Item> listener, int settings, Key... keys) {
        query(listener, settings, Arrays.asList(keys));
    }
    
    public Object query(ResultListener<Key, Item> listener, int settings, Collection<Key> keys) {
        return query(null, listener, settings, keys);
    }
    
    @SafeVarargs
    public final void query(Object unique, ResultListener<Key, Item> listener, int settings, Key... keys) {
        query(unique, listener, settings, Arrays.asList(keys));
    }
    
    /**
     * Add a new query, if the keys are not empty and depending on options.
     * 
     * @param unique If this object equals the object given for another query,
     * then that query will be overwritten. If null, then a new object will be
     * created as unique.
     * @param listener The listener for query results (optional)
     * @param settings Options (optonal)
     * @param keys The keys to request
     * @return The unique object, either the one given or the newly created one
     */
    public Object query(Object unique, ResultListener<Key, Item> listener, int settings, Collection<Key> keys) {
        if (keys.isEmpty()) {
            return null;
        }
        if (unique == null) {
            unique = new Object();
        }
        Query<Key, Item> query = new Query<>(listener, settings, keys);
        synchronized(LOCK) {
            if (option(query, UNIQUE) && queries.containsValue(query)) {
                return null;
            }
            if (option(query, NO_REPLACE) && queries.containsKey(unique)) {
                return null;
            }
            queries.put(unique, query);
            if (option(query, REFRESH)) {
                for (Key key : query.keys) {
                    cache.remove(key);
                }
            }
        }
        checkDoneQueries();
        
        if (doImmediately(query)) {
            doRequests();
        }
        return unique;
    }
    
    /**
     * Check if the given request should trigger requesting immediately.
     *
     * @param request
     * @return
     */
    private boolean doImmediately(Query<Key, Item> request) {
        synchronized (LOCK) {
            return option(request, ASAP)
                    && queries.containsValue(request);
        }
    }
    
    //=================
    // Get cached data
    //=================
    
    public Item get(Key key) {
        synchronized(LOCK) {
            return cache.get(key);
        }
    }
    
    public Item getOrQuerySingle(Object unique, ResultListener<Key, Item> listener, int settings, Key key) {
        synchronized(LOCK) {
            Item cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
        }
        query(unique, listener, settings, key);
        return null;
    }
    
    public Item getOrQuerySingle(ResultListener<Key, Item> listener, int settings, Key key) {
        return getOrQuerySingle(null, listener, settings, key);
    }
    
    @SafeVarargs
    public final Result<Key, Item> getOrQuery(Object unique, ResultListener<Key, Item> listener, int settings, Key... keys) {
        return getOrQuery(unique, listener, settings, Arrays.asList(keys));
    }
    
    public Result<Key, Item> getOrQuery(Object unique, ResultListener<Key, Item> listener, int settings, Collection<Key> keys) {
        Query<Key, Item> request = new Query<>(listener, settings, keys);
        Result<Key, Item> result = getResult(request);
        if (result == null || !result.hasAllKeys) {
            query(unique, listener, settings, keys);
        }
        return result;
    }
    
    //=================
    // Request results
    //=================
    
    @SafeVarargs
    public final void setError(Key... keys) {
        setError(Arrays.asList(keys));
    }
    
    public void setError(Collection<Key> keys) {
        synchronized(LOCK) {
            for (Key key : keys) {
                lastError.put(key, MiscUtil.ems());
                
                setResponseReceived(key);
                errorCount.put(key, errorCount.getOrDefault(key, 0) + 1);
            }
        }
        checkDoneQueries();
    }
    
    @SafeVarargs
    public final void setNotFound(Key... keys) {
        setNotFound(Arrays.asList(keys));
    }
    
    public void setNotFound(Collection<Key> keys) {
        synchronized(LOCK) {
            notFound.addAll(keys);
            for (Key key : keys) {
                errorCount.remove(key);
                setResponseReceived(key);
            }
        }
        checkDoneQueries();
    }
    
    public void setResult(Key key, Item item) {
        synchronized(LOCK) {
            setResultInternal(key, item);
        }
        checkDoneQueries();
    }
    
    public void setResult(Map<Key, Item> results) {
        synchronized(LOCK) {
            for (Map.Entry<Key, Item> entry : results.entrySet()) {
                setResultInternal(entry.getKey(), entry.getValue());
            }
        }
        checkDoneQueries();
    }
    
    /**
     * 
     * Synchronization: Within LOCK
     * 
     * @param key
     * @param item 
     */
    private void setResultInternal(Key key, Item item) {
        cache.put(key, item);
        errorCount.remove(key);
        notFound.remove(key);
        setResponseReceived(key);
    }
    
    /**
     * 
     * Synchronization: Within LOCK
     * 
     * @param key 
     */
    private void setResponseReceived(Key key) {
        requestPending.remove(key);
        for (Query<Key, Item> r : queries.values()) {
            r.responseReceived(key);
        }
    }
    
    //=================
    // Debugging/Stuff
    //=================
    
    public String debug() {
        synchronized(LOCK) {
            return String.format("requests: %d pending: %d",
                    queries.size(), requestPending.size());
        }
    }
    
    public int pendingRequests() {
        synchronized(LOCK) {
            return queries.size();
        }
    }
    
    //============
    // Requesting
    //============
    //---------------------
    // Requester interface
    //---------------------
    
    public void setRequested(Collection<Key> keys) {
        if (keys != null && !keys.isEmpty()) {
            synchronized (LOCK) {
                for (Key key : keys) {
                    requestPending.put(key, MiscUtil.ems());
                }
            }
        }
    }

    @SafeVarargs
    public final void setRequested(Key... keys) {
        setRequested(Arrays.asList(keys));
    }

    /**
     * From the given sets, add at most {@code limit} items to be requested, and
     * immediatelly set requested as well.
     * 
     * @param asap
     * @param normal
     * @param backlog
     * @param limit
     * @return 
     */
    public Set<Key> makeAndSetRequested(Set<Key> asap, Set<Key> normal, Set<Key> backlog, int limit) {
        Set<Key> toRequest = new HashSet<>();
        MiscUtil.addLimited(asap, toRequest, limit);
        MiscUtil.addLimited(normal, toRequest, limit);
        MiscUtil.addLimited(backlog, toRequest, limit);
        setRequested(toRequest);
        return toRequest;
    }
    
    //------------------
    // Perform requests
    //------------------

    private volatile boolean requestingInProgress;
    
    public void doRequests() {
        if (requestingInProgress) {
            LOGGER.warning("Ignored doRequests");
            return;
        }
        requestingInProgress = true;
        Set<Key> asap = new HashSet<>();
        Set<Key> normal = new HashSet<>();
        Set<Key> backlog = new HashSet<>();
        
        synchronized(LOCK) {
            for (Query<Key, Item> request : queries.values()) {
                addKeys(request, asap, normal, backlog);
            }
            // Remove duplicates
            for (Key key : asap) {
                normal.remove(key);
                backlog.remove(key);
            }
            for (Key key : normal) {
                backlog.remove(key);
            }
        }
        if (!asap.isEmpty() || !normal.isEmpty()) {
            requester.request(this, asap, normal, backlog);
        }
        requestingInProgress = false;
    }
    
    private void addKeys(Query<Key, Item> query, Set<Key> asap, Set<Key> normal, Set<Key> backlog) {
        for (Key key : query.keys) {
            if (!requestPending.containsKey(key)
                    && !query.isAccepted(key)) {
                if (checkError(key, query)) {
                    if (option(query, ASAP)) {
                        asap.add(key);
                    } else {
                        normal.add(key);
                    }
                } else {
                    backlog.add(key);
                }
            }
        }
    }
    
    private boolean checkError(Key key, Query<Key, Item> q) {
        if (option(q, REFRESH) && !q.isResponseReceived(key)) {
            return true;
        }
        if (lastError.containsKey(key)) {
            long delay = errorDelay(key, q);
            return secondsPassed(lastError, key) > delay;
        }
        return true;
    }
    
    private long errorDelay(Key key, Query<Key, Item> r) {
        int errors = errorCount.getOrDefault(key, 0);
        int base = option(r, ASAP) ? 2 : 10;
        return (long)Math.min(base * Math.pow(errors, 10), 1800);
    }
    
    //===================
    // Completed Queries
    //===================
    
    /**
     * Check if there are any completed requests and call the listener if there
     * are. Some requests may be kept, if there are still keys to retry
     * (depending on options).
     * 
     * Synchronization: Outside LOCK
     */
    private void checkDoneQueries() {
        Collection<Result<Key,Item>> completed = getDoneQueries();
        if (completed == null) {
            return;
        }
        for (Result<Key, Item> result : completed) {
            if (result.query.resultListener != null) {
                result.query.resultListener.result(result);
            }
        }
    }
    
    private Collection<Result<Key, Item>> getDoneQueries() {
        synchronized(LOCK) {
            if (queries.isEmpty()) {
                return null;
            }
            Collection<Result<Key, Item>> results = getDoneQueries2();
            return results;
        }
    }
    
    /**
     * Get all requests that are considered completed.
     * 
     * Synchronization: Within LOCK
     * 
     * @return 
     */
    private Collection<Result<Key, Item>> getDoneQueries2() {
        Collection<Result<Key, Item>> results = new ArrayList<>();
        Iterator<Map.Entry<Object, Query<Key, Item>>> it = queries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Query<Key, Item>> entry = it.next();
            Result<Key, Item> requestResult = getResult(entry.getValue());
            if (requestResult != null) {
                results.add(requestResult);
                if (requestResult.hasAllKeys) {
                    it.remove();
                }
            }
        }
        return results;
    }
    
    private Result<Key, Item> getResult(Query<Key, Item> q) {
        synchronized(LOCK) {
            Map<Key, Item> results = new HashMap<>();
            Set<Key> waitErrors = new HashSet<>();
            for (Key k : q.keys) {
                if (option(q, REFRESH) && !q.isResponseReceived(k)) {
                    continue;
                }

                if (cache.containsKey(k)) {
                    results.put(k, cache.get(k));
                }
                else if (notFound.contains(k)) {
                    results.put(k, null);
                }
                else if (secondsPassed(lastError, k) < errorDelay(k, q)) {
                    if (option(q, RETRY) || option(q, WAIT)) {
                        waitErrors.add(k);
                    }
                    else {
                        results.put(k, null);
                    }
                }
            }
            q.accepted.addAll(results.keySet());
            
            /**
             * The query will be removed in the caller method when it
             * "hasAllKeys", so when all keys (error or not) have been added to
             * the "results" map.
             *
             * Otherwise the query result will only be added when the PARTIAL or
             * RETRY option have been set and *something* new is returned, but
             * the query will be kept for further requests.
             */
            boolean hasAllKeys = results.size() == q.keys.size();
            boolean hasAllKeysOrErrors = results.size() + waitErrors.size() == q.keys.size();
            boolean partial = option(q, PARTIAL) || option(q, RETRY);
            boolean enoughKeys = option(q, PARTIAL) || hasAllKeysOrErrors;
            if (hasAllKeys || (partial && results.size() > 0 && enoughKeys)) {
                Result<Key, Item> result = new Result<>(results, q, hasAllKeys);
                if (!q.sameResult(result)) {
                    q.setResult(result);
                    return result;
                }
            }
            return null;
        }
    }
    
    //==================
    // Helper functions
    //==================
    
    private boolean option(Query<Key, Item> r, int option) {
        return r.options.contains(option) || options.contains(option);
    }
    
    /**
     * Helper function to get the amount of seconds passed for the given key,
     * from the given map with MiscUtil.ems() as values.
     * 
     * @param map
     * @param key
     * @return The number of seconds passed, or Long.MAX_VALUE if key not found
     */
    private long secondsPassed(Map<Key, Long> map, Key key) {
        if (!map.containsKey(key)) {
            return Long.MAX_VALUE;
        }
        return TimeUnit.MILLISECONDS.toSeconds(MiscUtil.ems() - map.get(key));
    }
    
    
    //====================
    // Interfaces/Classes
    //====================

    
    public interface ResultListener<Key, Item> {
        
        public void result(Result<Key, Item> result);
        
    }
    
    public interface Requester<Key, Item> {
        
        /**
         * Called with keys that need data, which could for example be provided
         * by an API request or read from disc.
         *
         * <p>
         * None or several of the requested keys can be acted upon, however the
         * implementation sees fit (for example depending on API rate limits or
         * whether bulk requests are possible). Any keys that *are* requested
         * must be set as such with one of the setRequested() methods of the
         * given manager. This is to prevent them from appearing in future
         * request calls.
         * 
         * <p>
         * Results can be set with one of the result methods of the given
         * manager, depending on what the result is (data, error, ..).
         *
         * @param manager
         * @param asap
         * @param normal
         * @param backlog 
         */
        public void request(CachedBulkManager<Key,Item> manager, Set<Key> asap, Set<Key> normal, Set<Key> backlog);
    }
    
    public static class Result<Key, Item> {
        
        private final Query<Key, Item> query;
        private final Map<Key, Item> results;
        private final boolean hasAllKeys;
        
        private Result(Map<Key, Item> results, Query<Key, Item> query, boolean hasAllKeys) {
            this.results = new HashMap<>(results);
            this.hasAllKeys = hasAllKeys;
            this.query = query;
        }
        
        /**
         * Get the result for a given key.
         * 
         * @param key
         * @return The result, or null if (some depending on options) the key
         * errored, was not found or has not been requested yet
         */
        public Item get(Key key) {
            return results.get(key);
        }
        
        /**
         * Check if this result contains all keys. The result may still contain
         * null values for some keys, however this means that the query that
         * produced this result has been counted as completed, and has thus been
         * removed.
         *
         * @return 
         */
        public boolean hasAllKeys() {
            return hasAllKeys;
        }
        
        /**
         * The result map. Should not be modified. May contain null values for
         * errored or not found keys, although it may also not contain entries
         * for some keys (depending on the options of the query that produced
         * this result).
         * 
         * @return 
         */
        public Map<Key, Item> getResults() {
            return results;
        }
        
        @Override
        public String toString() {
            return query+" -> "+results;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Result<?, ?> other = (Result<?, ?>) obj;
            if (!Objects.equals(this.results, other.results)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.results);
            return hash;
        }
        
    }
    
    private static class Options {
        
        private final int options;
        
        protected Options(int options) {
            this.options = options;
        }
        
        public boolean contains(int option) {
            return (options & option) == option;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Options other = (Options) obj;
            if (this.options != other.options) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this.options;
            return hash;
        }
        
    }
    
    private static class Query<Key, Item> {
        
        public final Options options;
        public final ResultListener<Key, Item> resultListener;
        public final Set<Key> keys;
        private final Set<Key> accepted = new HashSet<>();
        private final Set<Key> responseReceived = new HashSet<>();
        private Result<Key, Item> result;
        
        Query(ResultListener<Key, Item> resultListener, int settings, Collection<Key> keys) {
            this.options = new Options(settings);
            Set<Key> set = new HashSet<>(keys);
            this.keys = Collections.unmodifiableSet(set);
            this.resultListener = resultListener;
        }
        
        public void accepted(Key key) {
            accepted.add(key);
        }
        
        public boolean isAccepted(Key key) {
            return accepted.contains(key);
        }
        
        public void responseReceived(Key key) {
            if (keys.contains(key)) {
                responseReceived.add(key);
            }
        }
        
        public boolean isResponseReceived(Key key) {
            return responseReceived.contains(key);
        }
        
        public void setResult(Result<Key, Item> result) {
            this.result = result;
        }
        
        public Result<Key, Item> getResult() {
            return this.result;
        }
        
        public boolean sameResult(Result<Key, Item> result) {
            return Objects.equals(this.result, result);
        }
        
        @Override
        public String toString() {
            return keys.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Query<?, ?> other = (Query<?, ?>) obj;
            if (!Objects.equals(this.options, other.options)) {
                return false;
            }
            if (!Objects.equals(this.resultListener, other.resultListener)) {
                return false;
            }
            if (!Objects.equals(this.keys, other.keys)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 11 * hash + Objects.hashCode(this.options);
            hash = 11 * hash + Objects.hashCode(this.resultListener);
            hash = 11 * hash + Objects.hashCode(this.keys);
            return hash;
        }
        
    }
    
    // add(settings, keys) -> Request{settings, keys} -> pending[]
    
    
    
    /*
    
    stream_name -> set[] (request emotes, show in Emote Dialog)
set_id -> set (Emote info)
emote_id -> set (Emote info)

set{set_id,product,stream}

---------------------------------------------
v4/channels/<stream_id> -> set[]

    
    */
    
}
