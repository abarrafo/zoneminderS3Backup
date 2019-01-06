package com.andrewbarraford.s3backup.service;


import com.andrewbarraford.s3backup.domain.Event;
import com.andrewbarraford.s3backup.domain.EventRepository;
import com.andrewbarraford.s3backup.redis.models.QueueMessage;
import com.andrewbarraford.s3backup.redis.queue.QueueFactory;
import com.andrewbarraford.s3backup.util.RandomStringGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SM;
import org.apache.http.impl.cookie.DefaultCookieSpec;
import org.apache.http.impl.cookie.RFC6265StrictSpec;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@SuppressWarnings("unused")
@Component
@Slf4j
public class service {

    private static final String SLASH = "/";
    private static final String DASH = "-";

    private final transient EventRepository eventRepository;
    private final transient String eventsPath;
    private final transient String bucketName;
    private final transient String basePath;
    private final transient QueueFactory queueFactory;

    private final transient RestTemplate restTemplate;
    private final transient String zmUrl;
    private final transient String username;
    private final transient String password;

    @Autowired
    public service(final EventRepository eventRepository,
                   @Value("${cloud.aws.s3.bucket:invalid}") final String bucketName,
                   @Value("${events.path:invalid}") final String basePath,
                   @Value("${zm.url:invalid}") final String zmUrl,
                   @Value("${zm.username:invalid}") final String username,
                   @Value("${zm.password:invalid}") final String password,
                   final QueueFactory queueFactory,
                   final RestTemplate restTemplate) {
        this.eventRepository = eventRepository;
        this.eventsPath = basePath + "events/";
        this.bucketName = bucketName;
        this.queueFactory = queueFactory;
        this.basePath = basePath;
        this.restTemplate = restTemplate;
        this.zmUrl = zmUrl;
        this.username = username;
        this.password = password;
    }

    //Batch Process to run cloud backup.
    @Scheduled(fixedDelay = (5 * 60 * 1000))
    public void runBackup(){
        final Collection<Event> events = eventRepository.findAllByBackedUp(false);
        events.stream().filter(event -> event.getEndTime() != null).forEach(event -> {
            log.info("Event: [{}], Date: [{}], Monitor: [{}], Name: [{}]", event.getId(), event.getStartTime(), event
                    .getMonitorId(), event.getName());

            final ZoneId defaultZoneId = ZoneId.systemDefault();
            final Instant instant = event.getStartTime().toInstant();
            final LocalDateTime date = instant.atZone(defaultZoneId).toLocalDateTime();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            final String key = + event.getMonitorId() + SLASH + date.format(formatter) + SLASH + event.getId();

            final String s3Key = event.getMonitorId() + SLASH + String.valueOf(date.getYear()).substring
                    (2) +
                    SLASH
                    + padNumber(date.getMonthValue()) + SLASH + padNumber(date.getDayOfMonth()) + SLASH + padNumber(date
                    .getHour()) +
                    DASH + padNumber(date.getMinute()) + DASH + padNumber(date.getSecond());

            final String randomKey =  "package-" + RandomStringGenerator.randomStringForUseInBucketNames(5) + ".zip";
            final String path = eventsPath + key;
            final String tempPath = basePath + "temp" + SLASH + randomKey;

            log.info("Path to event: [{}], Path to temp: [{}]", path, tempPath);

            boolean errorOccurred = false;

            //Zip event
            try {
                ZipUtil.pack(new File(path), new File(tempPath));
            }
            catch (ZipException e){
                log.error("Error occurred - possible IO error, corrupted files or bad hardware. Log and move on," +
                                " e.message: [{}], e.cause: [{}], e.stack: [{}], e.class: [{}]",
                        e.getMessage(), e.getCause(), e.getStackTrace(), e.getClass());
                event.setBackedUp(true);
                event.setBackUpComplete(true);
                eventRepository.save(event);

                errorOccurred = true;
            }

            if(!errorOccurred){
                final QueueMessage message = new QueueMessage();
                message.setBucketName(bucketName);
                message.setId(event.getId());
                message.setKey("events/" + s3Key + DASH + randomKey);
                message.setPath(tempPath);

                queueFactory.pushToMessageQueue(message);

                event.setBackedUp(true);
                eventRepository.save(event);
            }

        });
    }

    @Scheduled(cron = "0 0 12 * * *")
    public void deleteBackedUpEvents(){
        final ZonedDateTime zonedDateTime =  ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("US/Eastern"))
                .minusDays(30);
        final Collection<Event> events = eventRepository.findAllByBackUpCompleteAndStartTimeBefore(true,
                Date.from(zonedDateTime.toInstant()));
        events.forEach(event -> {

            try {
                final List<Cookie> cookies = authenticateWithApi();
                final HttpHeaders headers = new HttpHeaders();

                RFC6265StrictSpec spec = new RFC6265StrictSpec();
                List<Header> headerList = spec.formatCookies(cookies);
                log.info("cookies: [{}]", cookies.toString());
                for (final Header cookieHead: headerList) {
                    headers.add(cookieHead.getName(), cookieHead.getValue());
                }

                final HttpEntity request = new HttpEntity<>(null, headers);
                log.info(zmUrl + "api/events/" + event.getId() + ".json");
                final ResponseEntity<Void> entity =restTemplate.exchange(zmUrl + "api/events/" + event.getId() + "" +
                                ".json",
                        HttpMethod.DELETE,
                        request,
                        Void.class);

                entity.getStatusCode();
            }catch (URISyntaxException e){
                log.error("Error occurred, e.message: [{}], e.cause: [{}], e.stack: [{}], e.class: [{}]",
                        e.getMessage(), e.getCause(), e.getStackTrace(), e.getClass());
            }

        });
    }

    @Cacheable(cacheNames = "authCache")
    public List<Cookie> authenticateWithApi() throws URISyntaxException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        final MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("username", username);
        map.add("password", password);
        map.add("action", "login");
        map.add("view", "console");

        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        final ResponseEntity<String> response =
                restTemplate.postForEntity( zmUrl + "index.php", request , String.class);

        log.info("Response Headers: [{}]", response.getHeaders().get("Set-Cookie"));
        return parseCookies(new URI(zmUrl), Objects.requireNonNull(response.getHeaders().get("Set-Cookie")));

    }


    private List<Cookie> parseCookies(URI uri, List<String> cookieHeaders) {
        CookieSpec cookieSpec = new DefaultCookieSpec();

        ArrayList<Cookie> cookies = new ArrayList<>();
        int port = 443;
        boolean secure = "https".equals(uri.getScheme());
        CookieOrigin origin = new CookieOrigin(uri.getHost(), port,
                uri.getPath(), secure);
        for (String cookieHeader : cookieHeaders) {
            BasicHeader header = new BasicHeader(SM.SET_COOKIE, cookieHeader);
            try {
                cookies.addAll(cookieSpec.parse(header, origin));
            } catch (MalformedCookieException e) {
                log.error("Bad cookie.");
            }
        }
        //Just need Auth Cookie
        cookies.remove(0);
        cookies.remove(0);
        cookies.remove(0);
        return cookies;
    }

    //Pad integers with zero to support ZM path format.
    private static String padNumber(final int number){
        return String.format("%02d", number);
    }

}
